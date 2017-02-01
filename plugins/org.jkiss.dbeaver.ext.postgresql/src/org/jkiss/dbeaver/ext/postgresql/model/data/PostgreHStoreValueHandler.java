/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStringValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.sql.Types;

/**
 * PostgreHStoreValueHandler
 */
public class PostgreHStoreValueHandler extends JDBCStringValueHandler {
    private static final Log log = Log.getLog(PostgreHStoreValueHandler.class);
    public static final PostgreHStoreValueHandler INSTANCE = new PostgreHStoreValueHandler();

    @Override
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws SQLException {
        return super.fetchColumnValue(session, resultSet, type, index);
    }

    @Override
    public void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value) throws SQLException {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else {
            statement.setObject(paramIndex, value.toString(), Types.OTHER);
        }
    }
}
