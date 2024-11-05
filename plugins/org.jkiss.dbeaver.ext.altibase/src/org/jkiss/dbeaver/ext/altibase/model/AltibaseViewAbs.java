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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.altibase.AltibaseUtils;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

public abstract class AltibaseViewAbs extends GenericView implements DBPStatefulObject {

    private static final Log log = Log.getLog(AltibaseViewAbs.class);
    
    protected String schemaName = null;
    protected boolean isValid = false;

    public AltibaseViewAbs(JDBCSession session, GenericStructContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
        schemaName = container.getName();

        // Create a new view
        if (session == null) {
            isValid = true;
        // Fetch from a database
        } else {
            try {
                isValid = AltibaseUtils.getViewStatus(session, tableType, getSchemaName(), getName());
            } catch (DBCException e) {
                log.warn("Can't update view status.", e);
            }
        }
    }

    @Override
    public String getSchemaName() {
        return schemaName;
    }

    @Override
    public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, 
                "Refresh state of view '" + this.getName() + "'")) {
            isValid = AltibaseUtils.getViewStatus(session, getTableType(), getSchemaName(), getName());
        }
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        return isValid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }
}
