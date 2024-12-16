/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.PostgreValueParser;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTypeType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCArrayValueHandler;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.sql.Types;
import java.util.StringJoiner;

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
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException
    {
        if (object != null) {
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

            String className = object.getClass().getName();
            if (object instanceof String ||
                PostgreUtils.isPGObject(object) ||
                className.equals(PostgreConstants.PG_ARRAY_CLASS))
            {
                if (className.equals(PostgreConstants.PG_ARRAY_CLASS)) {
                    // Convert arrays to string representation (#7468)
                    // Otherwise we may have problems with domain types decoding (as they come in form of PgObject)
                    String strValue = object.toString();
                    return convertStringArrayToCollection(session, arrayType, itemType, strValue);
                } else if (PostgreUtils.isPGObject(object)) {
                    final Object value = PostgreUtils.extractPGObjectValue(object);
                    if (value instanceof String) {
                        return convertStringArrayToCollection(session, arrayType, itemType, (String) value);
                    } else {
                        log.error("Can't parse array");
                        return new JDBCCollection(
                                session.getProgressMonitor(), itemType,
                            DBUtils.findValueHandler(session, itemType),
                            value == null ? null : new Object[]{value}
                        );
                    }
                } else {
                    return convertStringArrayToCollection(session, arrayType, itemType, (String) object);
                }
            } else if (object instanceof Object[]) {
                return new JDBCCollection(
                    session.getProgressMonitor(),
                    itemType,
                    DBUtils.findValueHandler(session, itemType),
                    (Object[]) object
                );
            }
        }
        return super.getValueFromObject(session, type, object, copy, validateValue);
    }

    @Override
    protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value) throws DBCException, SQLException {
        if (value instanceof DBDCollection && !((DBDValue) value).isNull()) {
            statement.setObject(paramIndex, getValueDisplayString(paramType, value, DBDDisplayFormat.NATIVE), Types.OTHER);
        } else {
            super.bindParameter(session, statement, paramType, paramIndex, value);
        }
    }

    private JDBCCollection convertStringArrayToCollection(@NotNull DBCSession session, @NotNull PostgreDataType arrayType, @NotNull PostgreDataType itemType, @NotNull String strValue) throws DBCException {
        Object parsedArray = PostgreValueParser.convertStringToValue(session, arrayType, strValue);
        if (parsedArray instanceof Object[]){
            return new JDBCCollection(session.getProgressMonitor(), itemType, DBUtils.findValueHandler(session, itemType), (Object[]) parsedArray);
        } else {
            log.error("Can't parse array");
            return new JDBCCollection(session.getProgressMonitor(), itemType, DBUtils.findValueHandler(session, itemType), new Object[]{parsedArray});
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        if (!DBUtils.isNullValue(value) && value instanceof DBDCollection collection) {
            final StringJoiner output = new StringJoiner(
                PostgreUtils.getArrayDelimiter(collection.getComponentType()), "{", "}");

            for (int i = 0; i < collection.getItemCount(); i++) {
                final Object item = collection.getItem(i);
                final String member;

                if (item instanceof DBDCollection) {
                    member = getArrayMemberDisplayString(column, this, item, format);
                } else {
                    final PostgreDataType componentType = (PostgreDataType) collection.getComponentType();
                    final DBDValueHandler componentHandler = collection.getComponentValueHandler();
                    member = getArrayMemberDisplayString(componentType, componentHandler, item, format);
                }

                output.add(member);
            }

            return output.toString();
        }

        return super.getValueDisplayString(column, value, format);
    }

    @NotNull
    private static String getArrayMemberDisplayString(
        @NotNull DBSTypedObject type,
        @NotNull DBDValueHandler handler,
        @Nullable Object value,
        @NotNull DBDDisplayFormat format
    ) {
        if (DBUtils.isNullValue(value)) {
            return SQLConstants.NULL_VALUE;
        }

        final String string = handler.getValueDisplayString(type, value, format);

        if (isQuotingRequired(type, string)) {
            return '"' + string.replaceAll("[\\\\\"]", "\\\\$0") + '"';
        }

        return string;
    }

    /**
     * @see <a href="https://www.postgresql.org/docs/current/arrays.html#ARRAYS-IO">8.15.6. Array Input and Output Syntax</a>
     */
    private static boolean isQuotingRequired(@NotNull DBSTypedObject type, @NotNull String value) {
        switch (type.getDataKind()) {
            case ARRAY:
            case NUMERIC:
                return false;
            default:
                break;
        }

        if (value.isEmpty() || value.equalsIgnoreCase(SQLConstants.NULL_VALUE)) {
            return true;
        }

        for (int index = 0; index < value.length(); index++) {
            switch (value.charAt(index)) {
                case '{':
                case '}':
                case '"':
                case ' ':
                case '\\':
                    return true;
                default:
                    break;
            }
        }

        return value.equals(PostgreUtils.getArrayDelimiter(type));
    }
}
