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
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDValueDefaultGenerator;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.sql.SQLException;

/**
 * JDBC string value handler
 */
public class JDBCStringValueHandler extends JDBCAbstractValueHandler implements DBDValueDefaultGenerator {

    public static final JDBCStringValueHandler INSTANCE = new JDBCStringValueHandler();

    @Override
    protected Object fetchColumnValue(
        @NotNull DBCSession session,
        @NotNull JDBCResultSet resultSet,
        @NotNull DBSTypedObject type,
        int index)
        throws SQLException
    {
        // Use getObject instead of getString because sometimes CHAR/VARCHAR holds something specific. E.g. FOR BIT DATA
        return resultSet.getObject(index);
    }

    @Override
    public void bindParameter(
        @NotNull JDBCSession session, @NotNull JDBCPreparedStatement statement, @NotNull DBSTypedObject paramType,
        int paramIndex, Object value)
        throws SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else {
            statement.setString(paramIndex, value.toString());
        }
    }

    @NotNull
    @Override
    public Class<String> getValueObjectType(@NotNull DBSTypedObject attribute)
    {
        return String.class;
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object, boolean copy, boolean validateValue) throws DBCException
    {
        if (object == null || object instanceof String) {
            return object;
        } else if (object instanceof char[] chars) {
            return new String(chars);
        } else if (object instanceof byte[] bytes) {
            return new String(bytes);
        } else if (object instanceof DBDContent content) {
            return ContentUtils.getContentStringValue(session.getProgressMonitor(), content);
        } else if (object.getClass().isArray()) {
            // Special workaround for #798 - convert array to string (weird stuff)
            return GeneralUtils.makeDisplayString(object);
        } else {
            return object.toString();
        }
    }

    @NotNull
    @Override
    public String getDefaultValueLabel() {
        return "Empty string";
    }

    @NotNull
    @Override
    public Object generateDefaultValue(@NotNull DBCSession session, @NotNull DBSTypedObject type) {
        return "";
    }
}