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
package org.jkiss.dbeaver.ext.kingbase.model.data;

import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringJoiner;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.ext.kingbase.KingbaseValueParser;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataSource;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataType;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataTypeAttribute;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTypeType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDComposite;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructImpl;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCComposite;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCompositeStatic;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStructValueHandler;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * KingbaseArrayValueHandler
 */
public class KingbaseStructValueHandler extends JDBCStructValueHandler {
    private static final Log log = Log.getLog(KingbaseStructValueHandler.class);
    public static final KingbaseStructValueHandler INSTANCE = new KingbaseStructValueHandler();

    @Override
    protected void bindParameter(
        JDBCSession session,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, Types.STRUCT);
        } else if (value instanceof DBDComposite) {
            DBDComposite struct = (DBDComposite) value;
            if (struct.isNull()) {
                statement.setNull(paramIndex, Types.STRUCT);
            } else if (struct instanceof JDBCComposite) {
                final Object[] values = ((JDBCComposite) struct).getValues();
                final String string = KingbaseValueParser.generateObjectString(values);
                statement.setObject(paramIndex, string, Types.OTHER);
            }
        } else {
            throw new DBCException("Struct parameter type '" + value.getClass().getName() + "' not supported");
        }
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException
    {
        KingbaseDataType structType = KingbaseUtils.findDataType(session, (KingbaseDataSource)session.getDataSource(), type);
        if (structType == null) {
            log.debug("Can't resolve struct type '" + type.getTypeName() + "'");
            return object;
        }
        if (structType.getTypeType() == KingbaseTypeType.d) {
            structType = structType.getBaseType(session.getProgressMonitor());
        }
        try {
            if (object == null) {
                return new JDBCCompositeStatic(session, structType, null);
            } else if (object instanceof JDBCCompositeStatic) {
                return copy ? ((JDBCCompositeStatic) object).cloneValue(session.getProgressMonitor()) : object;
            } else {
                Object value;
                if (KingbaseUtils.isKBObject(object)) {
                    value = KingbaseUtils.extractKBObjectValue(object);
                } else {
                    value = object.toString();
                }
                return convertStringToStruct(session, structType, (String) value);
            }
        } catch (DBException e) {
            throw new DBCException("Error converting string to composite type", e, session.getExecutionContext());
        }
    }

    @NotNull
    @Override
    public synchronized String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        if (!DBUtils.isNullValue(value) && value instanceof JDBCComposite) {
            final JDBCComposite composite = (JDBCComposite) value;
            final StringJoiner output = new StringJoiner(",", "(", ")");

            for (DBSAttributeBase attribute : composite.getAttributes()) {
                final DBDValueHandler handler = DBUtils.findValueHandler(composite.getDataType().getDataSource(), attribute);
                final Object item = composite.getAttributeValue(attribute);
                final String member = getStructMemberDisplayString(attribute, handler, item, format);

                output.add(member);
            }

            return output.toString();
        }

        return super.getValueDisplayString(column, value, format);
    }

    @NotNull
    private static String getStructMemberDisplayString(
        @NotNull DBSTypedObject type,
        @NotNull DBDValueHandler handler,
        @Nullable Object value,
        DBDDisplayFormat format) {
        if (DBUtils.isNullValue(value)) {
            return "";
        }

        final String string = handler.getValueDisplayString(type, value, DBDDisplayFormat.NATIVE);

        if (format == DBDDisplayFormat.NATIVE && isQuotingRequired(string)) {
            return '"' + string.replace("\"", "\"\"") + '"';
        }

        return string;
    }

    
    private static boolean isQuotingRequired(@NotNull String value) {
        if (value.isEmpty()) {
            return true;
        }

        for (int index = 0; index < value.length(); index++) {
            switch (value.charAt(index)) {
                case '(':
                case ')':
                case '"':
                case ',':
                case '\\':
                    return true;
                default:
                    break;
            }
        }

        return false;
    }

    private JDBCCompositeStatic convertStringToStruct(@NotNull DBCSession session, @NotNull KingbaseDataType compType, @NotNull String value) throws DBException {
        if (value.startsWith("(") && value.endsWith(")")) {
            value = value.substring(1, value.length() - 1);
        }
        final Collection<KingbaseDataTypeAttribute> attributes = compType.getAttributes(session.getProgressMonitor());
        if (attributes == null) {
            throw new DBException("Composite type '" + compType.getTypeName() + "' has no attributes");
        }
        String[] parsedValues = KingbaseValueParser.parseSingleObject(value);
        if (parsedValues.length != attributes.size()) {
            log.debug("Number of attributes (" + attributes.size() + ") doesn't match actual number of parsed strings (" + parsedValues.length + ")");
        }
        Object[] attrValues = new Object[attributes.size()];

        Iterator<KingbaseDataTypeAttribute> attrIter = attributes.iterator();
        for (int i = 0; i < parsedValues.length && attrIter.hasNext(); i++) {
            final KingbaseDataTypeAttribute itemAttr = attrIter.next();
            attrValues[i] = KingbaseValueParser.convertStringToValue(session, itemAttr, parsedValues[i]);
        }

        Struct contents = new JDBCStructImpl(compType.getTypeName(), attrValues, value);
        return new JDBCCompositeStatic(session, compType, contents);
    }

}
