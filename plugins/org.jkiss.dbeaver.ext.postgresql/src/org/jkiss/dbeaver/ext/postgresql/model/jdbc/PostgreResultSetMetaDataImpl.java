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
package org.jkiss.dbeaver.ext.postgresql.model.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSetMetaDataSnapshot;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetMetaDataImpl;

import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * PostgreResultSetMetaDataImpl
 */
public class PostgreResultSetMetaDataImpl extends JDBCResultSetMetaDataImpl
{

    public PostgreResultSetMetaDataImpl(JDBCResultSet resultSet) throws SQLException {
        super(resultSet);
    }

    public PostgreResultSetMetaDataImpl(@NotNull JDBCResultSet resultSet, @NotNull ResultSetMetaData original) throws SQLException {
        super(resultSet, original);
    }

    public PostgreResultSetMetaDataImpl(
        @NotNull JDBCSession session,
        @NotNull JDBCStatement statement,
        @NotNull ResultSetMetaData original
    ) throws SQLException {
        super(session, statement, original);
    }

    /**
     * Always return current instance as catalog name
     */
    @NotNull
    @Override
    public String getCatalogName(int column) throws SQLException {
        return getSession().getExecutionContext().getOwnerInstance().getName();
    }

    @Nullable
    @Override
    public String getSchemaName(int column) throws SQLException {
        if (original instanceof JDBCResultSetMetaDataSnapshot.SnapshotMetaData) {
            return super.getSchemaName(column);
        }
        String schemaName;
        try {
            schemaName = (String) original.getClass().getMethod("getBaseSchemaName", Integer.TYPE).invoke(original, column);
        } catch (InvocationTargetException e) {
            throw new SQLException("Error getting schema name", e.getTargetException());
        } catch (Exception e) {
            throw new SQLException("Error getting schema name", e);
        }
        return JDBCUtils.normalizeIdentifier(schemaName);
    }

    @Nullable
    @Override
    public String getTableName(int column) throws SQLException {
        if (original instanceof JDBCResultSetMetaDataSnapshot.SnapshotMetaData) {
            return super.getTableName(column);
        }
        String tableName;
        try {
            tableName = (String) original.getClass().getMethod("getBaseTableName", Integer.TYPE).invoke(original, column);
        } catch (InvocationTargetException e) {
            throw new SQLException("Error getting table name", e.getTargetException());
        } catch (Exception e) {
            throw new SQLException("Error getting table name", e);
        }
        return JDBCUtils.normalizeIdentifier(tableName);
    }
}
