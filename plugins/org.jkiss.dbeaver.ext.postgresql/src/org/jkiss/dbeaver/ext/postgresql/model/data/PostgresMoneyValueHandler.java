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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCNumberValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStringValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;

/**
 * JDBC strict string value handler.
 */
public class PostgresMoneyValueHandler extends JDBCStringValueHandler {

    public static final PostgresMoneyValueHandler INSTANCE = new PostgresMoneyValueHandler();

    @Override
    protected Object fetchColumnValue(
        DBCSession session,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index)
        throws SQLException
    {
        return resultSet.getString(index);
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