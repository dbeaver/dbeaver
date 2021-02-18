/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hive.model.jdbc;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetMetaDataImpl;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * HiveResultSetMetaDataImpl.
 *
 * Fixes Hive driver results metadata. All column names/labels have table name prefix (weird)
 */
public class HiveResultSetMetaDataImpl extends JDBCResultSetMetaDataImpl
{

    public HiveResultSetMetaDataImpl(JDBCResultSet resultSet) throws SQLException {
        super(resultSet);
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        return removeTableNamePrefix(super.getColumnName(column));
    }

    @Override
    public String getColumnLabel(int column) throws SQLException {
        return removeTableNamePrefix(super.getColumnLabel(column));
    }

    private String removeTableNamePrefix(String columnName) throws SQLException {
        if (!CommonUtils.isEmpty(columnName)) {
            int divPos = columnName.indexOf('.');
            if (divPos > 0 && divPos < columnName.length() - 1) {
                // Remove table name prefix
                return columnName.substring(divPos + 1);
            }
        }
        return columnName;
    }

}
