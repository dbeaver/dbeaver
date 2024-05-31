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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import java.sql.SQLException;

public abstract class AltibaseViewAbs extends GenericView implements DBPStatefulObject {

    protected abstract String getQry4RefreshState();
    
    protected String schemaName = null;
    protected boolean valid = false;

    public AltibaseViewAbs(GenericStructContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
        schemaName = container.getName();
    }

    @Override
    public String getSchemaName() {
        return schemaName;
    }

    // Work-around to use generic metadata
    public void refreshState(JDBCSession session) throws DBCException {
        try (JDBCPreparedStatement dbStat = session.prepareStatement(getQry4RefreshState())) {
            dbStat.setString(1, schemaName);
            dbStat.setString(2, getName());

            dbStat.executeStatement();

            try (JDBCResultSet dbResult = dbStat.getResultSet()) {
                if (dbResult != null && dbResult.next()) {
                    valid = JDBCUtils.safeGetBoolean(dbResult, 1, "0"); // 0 is Valid, 1 is invalid
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @Override
    public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, 
                "Refresh state of view '" + this.getName() + "'")) {
            refreshState(session);
        }
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }
}
