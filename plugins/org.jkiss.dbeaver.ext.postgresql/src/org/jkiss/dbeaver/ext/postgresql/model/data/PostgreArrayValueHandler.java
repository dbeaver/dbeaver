/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.postgresql.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.PostgreValueParser;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTypeType;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCArrayValueHandler;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.List;

/**
 * PostgreArrayValueHandler
 */
public class PostgreArrayValueHandler extends JDBCArrayValueHandler {
    public static final PostgreArrayValueHandler INSTANCE = new PostgreArrayValueHandler();
    private static final Log log = Log.getLog(PostgreArrayValueHandler.class);

    @Override
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws DBCException, SQLException {
        return super.fetchColumnValue(session, resultSet, type, index);
    }

    @Override
    public DBDCollection getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException
    {
        if (object != null) {
            String className = object.getClass().getName();
            if (object instanceof String ||
                PostgreUtils.isPGObject(object) ||
                className.equals(PostgreConstants.PG_ARRAY_CLASS))
            {
                final PostgreDataType arrayType = PostgreUtils.findDataType(session, (PostgreDataSource) session.getDataSource(), type);
                if (arrayType == null) {
                    throw new DBCException("Can't resolve data type " + type.getFullTypeName());
                }
                PostgreDataType itemType = arrayType.getElementType(session.getProgressMonitor());
                if (itemType == null && arrayType.getTypeType() == PostgreTypeType.d) {
                    // Domains store component type information in another field
                    itemType = arrayType.getBaseType(session.getProgressMonitor());
                }
                if (itemType == null) {
                    throw new DBCException("Array type " + arrayType.getFullTypeName() + " doesn't have a component type");
                }
                if (className.equals(PostgreConstants.PG_ARRAY_CLASS)) {
                    // Convert arrays to string representation (#7468)
                    // Otherwise we may have problems with domain types decoding (as they come in form of PgObject)
                    String strValue = object.toString();
                    return convertStringArrayToCollection(session, arrayType, itemType, strValue);
                } else if (PostgreUtils.isPGObject(object)) {
                    final Object value = PostgreUtils.extractPGObjectValue(object);
                    if (value instanceof String) {
                        return convertStringToCollection(session, type, itemType, (String) value);
                    } else {
                        log.error("Can't parse array");
                        return new JDBCCollection(
                            itemType,
                            DBUtils.findValueHandler(session, itemType),
                            value == null ? null : new Object[]{value});
                    }
                } else {
                    return convertStringToCollection(session, type, itemType, (String) object);
                }
            }
        }
        return super.getValueFromObject(session, type, object, copy, validateValue);
    }

    private JDBCCollection convertStringToCollection(@NotNull DBCSession session, @NotNull DBSTypedObject arrayType, @NotNull PostgreDataType itemType, @NotNull String value) throws DBCException {
        String delimiter;

        PostgreDataType arrayDataType = PostgreUtils.findDataType(session, (PostgreDataSource) session.getDataSource(), arrayType);
        if (arrayDataType != null) {
            delimiter = CommonUtils.toString(arrayDataType.getArrayDelimiter(), PostgreConstants.DEFAULT_ARRAY_DELIMITER);
        } else {
            delimiter = PostgreConstants.DEFAULT_ARRAY_DELIMITER;
        }
        if (itemType.getDataKind() == DBPDataKind.STRUCT) {
            // Items are structures. Parse them as CSV
            List<Object> itemStrings = PostgreValueParser.parseArrayString(value, delimiter);
            Object[] itemValues = new Object[itemStrings.size()];
            DBDValueHandler itemValueHandler = DBUtils.findValueHandler(session, itemType);
            for (int i = 0; i < itemStrings.size(); i++) {
                Object itemString = itemStrings.get(i);
                Object itemValue = itemValueHandler.getValueFromObject(session, itemType, itemString, false, false);
                itemValues[i] = itemValue;
            }
            return new JDBCCollection(itemType, itemValueHandler, itemValues);
        } else {
            List<Object> strings = PostgreValueParser.parseArrayString(value, delimiter);
            Object[] contents = new Object[strings.size()];
            for (int i = 0; i < strings.size(); i++) {
                contents[i] = PostgreValueParser.convertStringToValue(session, itemType, String.valueOf(strings.get(i)));
            }
            return new JDBCCollection(itemType, DBUtils.findValueHandler(session, itemType), contents);
        }
    }

    private JDBCCollection convertStringArrayToCollection(@NotNull DBCSession session, @NotNull PostgreDataType arrayType, @NotNull PostgreDataType itemType, @NotNull String strValue) throws DBCException {
        Object parsedArray = PostgreValueParser.convertStringToValue(session, arrayType, strValue);
        if (parsedArray instanceof Object[]){
            return new JDBCCollection(itemType, DBUtils.findValueHandler(session, itemType), (Object[]) parsedArray);
        } else {
            log.error("Can't parse array");
            return new JDBCCollection(itemType, DBUtils.findValueHandler(session, itemType), new Object[]{parsedArray});
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        return convertArrayToString(column, value, format, false);
    }

    private String convertArrayToString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format, boolean nested) {
        if (!DBUtils.isNullValue(value) && value instanceof DBDCollection) {
            DBDCollection collection = (DBDCollection) value;
            boolean isNativeFormat = format == DBDDisplayFormat.NATIVE;
            boolean isStringArray = collection.getComponentType().getDataKind() == DBPDataKind.STRING;

            DBDValueHandler valueHandler = collection.getComponentValueHandler();
            StringBuilder str = new StringBuilder();
            if (isNativeFormat && !nested) {
                str.append("'");
            }
            str.append("{");
            for (int i = 0; i < collection.getItemCount(); i++) {
                if (i > 0) {
                    str.append(','); //$NON-NLS-1$
                }
                final Object item = collection.getItem(i);
                String itemString;
                if (item instanceof JDBCCollection) {
                    // Multi-dimensional arrays case
                    itemString = convertArrayToString(column, item, format, true);
                } else {
                    itemString = valueHandler.getValueDisplayString(collection.getComponentType(), item, DBDDisplayFormat.NATIVE);
                }

                if (isNativeFormat) {
                    if (item instanceof String) str.append('"');
                    str.append(SQLUtils.escapeString(collection.getComponentType().getDataSource(), itemString));
                    if (item instanceof String) str.append('"');
                } else {
                    str.append(itemString);
                }
            }
            str.append("}");
            if (isNativeFormat && !nested) {
                str.append("'");
            }

            return str.toString();
        }
        return super.getValueDisplayString(column, value, format);
    }
}
