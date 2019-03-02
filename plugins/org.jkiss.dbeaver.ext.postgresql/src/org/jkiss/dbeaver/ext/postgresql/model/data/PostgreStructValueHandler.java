/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataTypeAttribute;
import org.jkiss.dbeaver.model.data.DBDComposite;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructImpl;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCComposite;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCompositeStatic;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStructValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;
import java.util.Collection;
import java.util.Iterator;

/**
 * PostgreArrayValueHandler
 */
public class PostgreStructValueHandler extends JDBCStructValueHandler {
    private static final Log log = Log.getLog(PostgreStructValueHandler.class);
    public static final PostgreStructValueHandler INSTANCE = new PostgreStructValueHandler();

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
                final String string = PostgreUtils.generateObjectString(values);
                statement.setObject(paramIndex, string, Types.OTHER);
            }
        } else {
            throw new DBCException("Struct parameter type '" + value.getClass().getName() + "' not supported");
        }
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        PostgreDataType structType = PostgreUtils.findDataType((PostgreDataSource)session.getDataSource(), type);
        if (structType == null) {
            throw new DBCException("Can't resolve struct type '" + type.getTypeName() + "'");
        }
        try {
            if (object == null) {
                return new JDBCCompositeStatic(session, structType, new JDBCStructImpl(structType.getTypeName(), null, ""));
            } else if (object instanceof JDBCCompositeStatic) {
                return copy ? ((JDBCCompositeStatic) object).cloneValue(session.getProgressMonitor()) : object;
            } else {
                Object value;
                if (PostgreUtils.isPGObject(object)) {
                    value = PostgreUtils.extractPGObjectValue(object);
                } else {
                    value = object.toString();
                }
                return convertStringToStruct(session, structType, (String) value);
            }
        } catch (DBException e) {
            throw new DBCException("Error converting string to composite type", e, session.getDataSource());
        }
    }

    private JDBCCompositeStatic convertStringToStruct(@NotNull DBCSession session, @NotNull PostgreDataType compType, @NotNull String value) throws DBException {
        if (value.startsWith("(") && value.endsWith(")")) {
            value = value.substring(1, value.length() - 1);
        }
        final Collection<PostgreDataTypeAttribute> attributes = compType.getAttributes(session.getProgressMonitor());
        if (attributes == null) {
            throw new DBException("Composite type '" + compType.getTypeName() + "' has no attributes");
        }
        String[] parsedValues = PostgreUtils.parseObjectString(value);
        if (parsedValues.length != attributes.size()) {
            log.debug("Number o attributes (" + attributes.size() + ") doesn't match actual number of parsed strings (" + parsedValues.length + ")");
        }
        Object[] attrValues = new Object[attributes.size()];

        Iterator<PostgreDataTypeAttribute> attrIter = attributes.iterator();
        for (int i = 0; i < parsedValues.length && attrIter.hasNext(); i++) {
            final PostgreDataTypeAttribute itemAttr = attrIter.next();
            attrValues[i] = PostgreUtils.convertStringToValue(session, itemAttr, parsedValues[i], true);
        }

        Struct contents = new JDBCStructImpl(compType.getTypeName(), attrValues, value);
        return new JDBCCompositeStatic(session, compType, contents);
    }

}
