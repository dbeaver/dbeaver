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
package org.jkiss.dbeaver.ext.clickhouse.model.data;

import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseTableColumn;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStringValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.util.Map;

public class ClickhouseEnumValueHandler extends JDBCStringValueHandler {
    public static final ClickhouseEnumValueHandler INSTANCE = new ClickhouseEnumValueHandler();

    @Override
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws SQLException {
        return resultSet.getString(index);
    }

    @Override
    public void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value) throws SQLException {
        DBSTypedObject type = paramType;
        if (type instanceof DBDAttributeBinding) {
            type = ((DBDAttributeBinding) type).getAttribute();
        }
        if (type instanceof ClickhouseTableColumn) {
            final Map<String, Integer> entries = ((ClickhouseTableColumn) type).getEnumEntries();
            final Integer ordinal = entries.get((String) value);
            if (ordinal != null) {
                statement.setInt(paramIndex, ordinal);
                return;
            }
        }
        super.bindParameter(session, statement, paramType, paramIndex, value);
    }
}
