/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDCursor;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

import java.sql.SQLException;

/**
 * Cursor references (named)
 */
public class PostgreRefCursor implements DBDCursor {

    private static final Log log = Log.getLog(PostgreRefCursor.class);

    private final JDBCSession session;
    private String cursorName;
    private boolean isOpen;
    private JDBCStatement cursorStatement;

    public PostgreRefCursor(JDBCSession session, @NotNull String cursorName) throws SQLException {
        this.session = session;
        this.cursorName = cursorName;
        this.isOpen = true;
    }

    @Override
    public Object getRawValue() {
        return cursorName;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void release() {
        if (this.isOpen) {
            try {
                JDBCUtils.executeStatement(session, "CLOSE \"" + cursorName + "\"");
            } catch (Exception e) {
                log.error(e);
            }
        }
        if (cursorStatement != null) {
            cursorStatement.close();
            cursorStatement = null;
        }
    }

    @Override
    public DBCResultSet openResultSet(DBCSession session) throws DBCException {
        try {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
            if (txnManager != null && txnManager.isAutoCommit()) {
                throw new DBCException("Ref cursors are not available in auto-commit mode");
            }
            if (cursorStatement != null) {
                cursorStatement.close();
            }
            JDBCUtils.executeStatement(this.session, "MOVE ABSOLUTE 0 IN \"" + cursorName + "\"");
            cursorStatement = this.session.createStatement();
            return cursorStatement.executeQuery("FETCH ALL IN \"" + cursorName + "\"");
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @Nullable
    @Override
    public String getCursorName() {
        return cursorName;
    }

    @Override
    public String toString() {
        return cursorName;
    }

}
