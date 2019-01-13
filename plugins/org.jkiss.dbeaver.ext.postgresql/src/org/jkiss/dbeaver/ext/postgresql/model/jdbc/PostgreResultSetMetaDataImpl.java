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
package org.jkiss.dbeaver.ext.postgresql.model.jdbc;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetMetaDataImpl;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

/**
 * PostgreResultSetMetaDataImpl
 */
public class PostgreResultSetMetaDataImpl extends JDBCResultSetMetaDataImpl
{

    public PostgreResultSetMetaDataImpl(JDBCResultSet resultSet) throws SQLException {
        super(resultSet);
    }

    /**
     * Always return current instance as catalog name
     */
    @Override
    public String getCatalogName(int column) throws SQLException {
        return resultSet.getSession().getExecutionContext().getOwnerInstance().getName();
    }

    @Override
    public String getSchemaName(int column) throws SQLException {
        String schemaName;
        try {
            schemaName = (String)original.getClass().getMethod("getBaseSchemaName", Integer.TYPE).invoke(original, column);
        } catch (InvocationTargetException e) {
            throw new SQLException("Error getting schema name", e.getTargetException());
        } catch (Exception e) {
            throw new SQLException("Error getting schema name", e);
        }
        return JDBCUtils.normalizeIdentifier(schemaName);
    }

    @Override
    public String getTableName(int column) throws SQLException {
        String tableName;
        try {
            tableName = (String)original.getClass().getMethod("getBaseTableName", Integer.TYPE).invoke(original, column);
        } catch (InvocationTargetException e) {
            throw new SQLException("Error getting table name", e.getTargetException());
        } catch (Exception e) {
            throw new SQLException("Error getting table name", e);
        }
        return JDBCUtils.normalizeIdentifier(tableName);
    }
}
