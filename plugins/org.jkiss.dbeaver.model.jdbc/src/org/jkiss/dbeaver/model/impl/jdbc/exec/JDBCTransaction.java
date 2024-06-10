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
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Simple transaction wrapper
 */
public class JDBCTransaction implements AutoCloseable {

    private static final Log log = Log.getLog(JDBCTransaction.class);

    private final Connection dbCon;
    private final boolean oldAutoCommit;

    public JDBCTransaction(Connection dbCon) throws SQLException {
        this.dbCon = dbCon;
        this.oldAutoCommit = dbCon.getAutoCommit();
        if (oldAutoCommit) {
            dbCon.setAutoCommit(false);
        }
    }

    public void commit() throws SQLException {
        dbCon.commit();
    }

    public void rollback() throws SQLException {
        try {
            dbCon.rollback();
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
        if (oldAutoCommit) {
            try {
                this.dbCon.setAutoCommit(true);
            } catch (SQLException e) {
                log.error("Error recovering auto-commit mode after transaction end", e);
            }
        }
    }
}
