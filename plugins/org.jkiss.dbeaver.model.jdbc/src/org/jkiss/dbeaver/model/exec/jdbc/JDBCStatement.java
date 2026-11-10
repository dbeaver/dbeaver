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
package org.jkiss.dbeaver.model.exec.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCScriptStatement;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBC statement
 */
public interface JDBCStatement extends Statement, DBCStatement, DBCScriptStatement, DBRBlockingObject {

    /**
     * Attach a {@link JDBCResultSetMetaDataSnapshot} resolved before executing this statement
     * when the caller opts into
     * {@link DBPDataSource#FEATURE_JDBC_TABLE_READ_REQUIRES_PREPARED_COLUMN_METADATA}.
     * The result set uses it instead of querying driver column details with an active cursor.
     * Pass {@code null} to clear the snapshot.
     */
    void setResultSetMetaDataSnapshot(@Nullable JDBCResultSetMetaDataSnapshot snapshot);

    /**
     * Return the optional pre-execution {@link JDBCResultSetMetaDataSnapshot} for
     * {@link DBPDataSource#FEATURE_JDBC_TABLE_READ_REQUIRES_PREPARED_COLUMN_METADATA},
     * or {@code null} for ordinary result-set metadata handling.
     */
    @Nullable
    JDBCResultSetMetaDataSnapshot getResultSetMetaDataSnapshot();

    @Override
    @NotNull
    JDBCSession getConnection();

    @Nullable
    @Override
    JDBCResultSet openResultSet() throws DBCException;

    @Nullable
    @Override
    JDBCResultSet openGeneratedKeysResultSet() throws DBCException;

    @Nullable
    @Override
    JDBCResultSet executeQuery(String sql)
        throws SQLException;

    @Nullable
    @Override
    JDBCResultSet getResultSet()
        throws SQLException;

    @Override
    void close();
}
