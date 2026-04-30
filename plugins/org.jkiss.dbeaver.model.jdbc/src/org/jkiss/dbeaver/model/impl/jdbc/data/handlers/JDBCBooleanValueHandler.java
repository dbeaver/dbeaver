/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.jdbc.data.handlers;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDValueDefaultGenerator;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;

/**
 * JDBC number value handler
 */
public class JDBCBooleanValueHandler extends JDBCAbstractValueHandler implements DBDValueDefaultGenerator {

    public static final JDBCBooleanValueHandler INSTANCE = new JDBCBooleanValueHandler();

    private static final Log log = Log.getLog(JDBCBooleanValueHandler.class);

    @Override
    protected Object fetchColumnValue(@NotNull DBCSession session, @NotNull JDBCResultSet resultSet, @NotNull DBSTypedObject type, int index)
        throws SQLException
    {
        boolean value = resultSet.getBoolean(index);
        return resultSet.wasNull() ? null : value;
    }

    @Override
    protected void bindParameter(
        @NotNull JDBCSession session, @NotNull JDBCPreparedStatement statement, @NotNull DBSTypedObject paramType,
        int paramIndex, Object value) throws SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else if (value instanceof Boolean) {
            statement.setBoolean(paramIndex, (Boolean)value);
        } else if (value instanceof Number) {
            statement.setBoolean(paramIndex, ((Number)value).byteValue() != 0);
        } else {
            statement.setBoolean(paramIndex, Boolean.valueOf(value.toString()));
        }
    }

    @NotNull
    @Override
    public Class<Boolean> getValueObjectType(@NotNull DBSTypedObject attribute)
    {
        return Boolean.class;
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object, boolean copy, boolean validateValue) throws DBCException
    {
        if (object == null) {
            return null;
        } else if (object instanceof Boolean) {
            return object;
        } else if (object instanceof String) {
            return Boolean.valueOf((String)object);
        } else if (object instanceof Number) {
            return ((Number) object).byteValue() != 0;
        } else {
            log.warn("Unrecognized type '" + object.getClass().getName() + "' - can't convert to boolean");
            return null;
        }
    }

    @NotNull
    @Override
    public String getDefaultValueLabel() {
        return "False";
    }

    @NotNull
    @Override
    public Object generateDefaultValue(@NotNull DBCSession session, @NotNull DBSTypedObject type) {
        return false;
    }

}