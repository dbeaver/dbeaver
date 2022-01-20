/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSetMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCFactoryDefault;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetMetaDataImpl;

import java.sql.SQLException;

/**
 * PostgreJdbcFactory
 */
public class PostgreJdbcFactory extends JDBCFactoryDefault
{
    @Override
    public JDBCResultSetMetaData createResultSetMetaData(@NotNull JDBCResultSet resultSet) throws SQLException {
        if (resultSet.getSession().getDataSource().isDriverVersionAtLeast(9, 0)) {
            // Only for real PG driver
            return new PostgreResultSetMetaDataImpl(resultSet);
        } else {
            return new JDBCResultSetMetaDataImpl(resultSet);
        }
    }
}
