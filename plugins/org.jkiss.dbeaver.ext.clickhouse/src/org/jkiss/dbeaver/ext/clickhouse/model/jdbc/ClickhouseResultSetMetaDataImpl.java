/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.clickhouse.model.jdbc;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetMetaDataImpl;

import java.sql.SQLException;

/**
 * JDBCUtils#normalizeIdentifier method is usually used for columns/tables name reading.
 * normalizeIdentifier trim names, but we can create tables/columns with spaces around their names
 * Let's just read names from the driver directly
 */
public class ClickhouseResultSetMetaDataImpl extends JDBCResultSetMetaDataImpl {

    ClickhouseResultSetMetaDataImpl(JDBCResultSet resultSet) throws SQLException {
        super(resultSet);
    }

    @Override
    public String getTableName(int column) throws SQLException {
        return original.getTableName(column);
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        return original.getColumnName(column);
    }
}
