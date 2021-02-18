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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDCursor;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetImpl;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Result set holder
 */
public class JDBCCursor implements DBDCursor {

    private static final Log log = Log.getLog(JDBCCursor.class);

    private JDBCSession session;
    private JDBCResultSet resultSet;
    private String cursorName;
    private boolean closeResultsOnRelease = true;

    public JDBCCursor(JDBCSession session, ResultSet resultSet, String description) throws SQLException {
        //super(session, null, original, description, true);
        this.session = session;
        this.resultSet = JDBCResultSetImpl.makeResultSet(session, null, resultSet, description, true);
    }

    @Override
    public Object getRawValue() {
        return resultSet;
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
        if (resultSet != null) {
            if (closeResultsOnRelease) {
                try {
                    resultSet.close();
                } catch (Exception e) {
                    log.error(e);
                }
            }
            resultSet = null;
        }
    }

    @Override
    public DBCResultSet openResultSet(DBCSession session) {
        if (resultSet != null) {
            // Scroll to the beginning
            try {
                resultSet.absolute(0);
            } catch (SQLException e) {
                log.debug(e);
            }
        }
        return resultSet;
    }

    @Nullable
    @Override
    public String getCursorName() {
        return cursorName;
    }

    public void setCursorName(String cursorName) {
        this.cursorName = cursorName;
    }

    public void setCloseResultsOnRelease(boolean closeResultsOnRelease) {
        this.closeResultsOnRelease = closeResultsOnRelease;
    }

    @Override
    public String toString() {
        if (cursorName != null) {
            return cursorName;
        }
        if (resultSet == null) {
            return DBConstants.NULL_VALUE_LABEL;
        }
        return CommonUtils.toString(resultSet.getSourceStatement().getQueryString());
    }

}
