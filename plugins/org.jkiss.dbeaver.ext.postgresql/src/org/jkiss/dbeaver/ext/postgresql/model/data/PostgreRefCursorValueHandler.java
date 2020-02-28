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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStructValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;

/**
 * PostgreArrayValueHandler
 */
public class PostgreRefCursorValueHandler extends JDBCStructValueHandler {
    private static final Log log = Log.getLog(PostgreRefCursorValueHandler.class);
    public static final PostgreRefCursorValueHandler INSTANCE = new PostgreRefCursorValueHandler();

    @Override
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws DBCException, SQLException {
        String cursorName = resultSet.getString(index);
        return new PostgreRefCursor(cursorName);
/*
        // Fetch as string (#1735)
        // Fetching cursor as object will close it so it won;'t be possible to use cursor in consequent queries
        Object object = resultSet.getObject(index);
        if (object instanceof ResultSet) {
            JDBCCursor cursor = new JDBCCursor(
                (JDBCSession) session,
                (ResultSet) object,
                type.getTypeName());
            // Set cursor name
            cursor.setCursorName(cursorName);
            // Disable resulset close on cursor release. Otherwise cusor can't be referred by other queries (#6074)
            cursor.setCloseResultsOnRelease(false);
            return cursor;
        }
        return object;
*/
    }

    @Override
    protected void bindParameter(
        JDBCSession session,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException
    {
        throw new DBCException("Cursor value binding not supported");
    }

}
