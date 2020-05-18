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
import org.jkiss.dbeaver.model.data.DBDCursor;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

import java.sql.SQLException;

/**
 * Cursor references (named)
 */
public class PostgreRefCursor implements DBDCursor {

    private static final Log log = Log.getLog(PostgreRefCursor.class);

    private String cursorName;
    private JDBCPreparedStatement cursorStatement;

    public PostgreRefCursor(@NotNull String cursorName) throws SQLException {
        this.cursorName = cursorName;
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
        if (cursorStatement != null) {
            cursorStatement.close();
            cursorStatement = null;
        }
    }

    @Override
    public DBCResultSet openResultSet(DBCSession session) throws DBCException {
        release();
        try {
            cursorStatement = ((JDBCSession)session).prepareStatement("FETCH ALL IN \"" + cursorName + "\"");
            return cursorStatement.executeQuery();
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
