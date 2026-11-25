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
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

/**
 * Protects a fallible JDBC operation inside an existing transaction.
 */
public class JDBCTransactionSavepoint implements AutoCloseable {
    private static final Log log = Log.getLog(JDBCTransactionSavepoint.class);

    private final Connection connection;
    @Nullable
    private final Savepoint savepoint;

    public JDBCTransactionSavepoint(@NotNull Connection connection) throws SQLException {
        this.connection = connection;
        if (connection.getAutoCommit()) {
            savepoint = null;
        } else {
            savepoint = connection.setSavepoint();
            if (savepoint == null) {
                throw new SQLException("Unable to create JDBC savepoint");
            }
        }
    }

    public void rollback() throws SQLException {
        if (savepoint == null) {
            return;
        }
        try {
            connection.rollback(savepoint);
        } catch (SQLException e) {
            if (JDBCUtils.isRollbackWarning(e)) {
                log.debug("Rollback warning: " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    @Override
    public void close() {
        if (savepoint != null) {
            try {
                connection.releaseSavepoint(savepoint);
            } catch (SQLException e) {
                log.error("Error releasing JDBC savepoint", e);
            }
        }
    }
}
