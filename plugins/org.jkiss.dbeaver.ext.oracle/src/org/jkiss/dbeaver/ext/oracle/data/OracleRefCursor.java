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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDCursor;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetImpl;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Cursor references (named)
 */
public class OracleRefCursor implements DBDCursor {

    private static final Log log = Log.getLog(OracleRefCursor.class);

    private final JDBCSession session;
    private final JDBCStatement sourceStatement;
    private final Object cursorValue;

    public OracleRefCursor(JDBCSession session, JDBCStatement sourceStatement, @Nullable Object cursorValue) throws SQLException {
        this.session = session;
        this.sourceStatement = sourceStatement;
        this.cursorValue = cursorValue;
    }

    @Override
    public Object getRawValue() {
        return cursorValue;
    }

    @Override
    public boolean isNull() {
        return cursorValue == null;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void release() {
        if (cursorValue instanceof ResultSet) {
            try {
                ((ResultSet) cursorValue).close();
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    @Override
    public DBCResultSet openResultSet(DBCSession session) throws DBCException {
        if (cursorValue instanceof ResultSet) {
            try {
                return JDBCResultSetImpl.makeResultSet((JDBCSession) session, sourceStatement, (ResultSet) cursorValue, null, false);
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
        throw new DBCException("Unsupported cursor value: " + cursorValue);
/*
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
*/
    }

    @Nullable
    @Override
    public String getCursorName() {
        if (cursorValue instanceof ResultSet) {
            try {
                return ((ResultSet) cursorValue).getCursorName();
            } catch (SQLException e) {
                log.error(e);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return cursorValue == null ? DBConstants.NULL_VALUE_LABEL : OracleConstants.TYPE_NAME_REFCURSOR;
    }

}
