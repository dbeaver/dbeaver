/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.debug.internal;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.debug.DBGBaseController;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.DBGSession;
import org.jkiss.dbeaver.ext.postgresql.debug.internal.impl.PostgreDebugSessionManager;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class PostgreDebugController extends DBGBaseController {

    private PostgreDebugSessionManager sessionManager;

    public PostgreDebugController(DBPDataSourceContainer dataSourceDescriptor) {
        super(dataSourceDescriptor);
    }

    private PostgreDebugSessionManager getSessionManager(DBRProgressMonitor monitor) throws DBGException  {
        if (sessionManager == null) {
            try {
                JDBCExecutionContext controllerContext = (JDBCExecutionContext) getDataSourceContainer().getDataSource().openIsolatedContext(monitor, "Debug controller");
                sessionManager = new PostgreDebugSessionManager(controllerContext);
            } catch (Exception e) {
                throw new DBGException("Can't initiate debug session manager", e);
            }
        }
        return sessionManager;
    }

    @Override
    protected DBGSession createSession(DBRProgressMonitor monitor, DBPDataSource dataSource) throws DBGException {
        PostgreDebugSessionManager sessionManager = getSessionManager(monitor);
        try {
            JDBCExecutionContext sessionContext = (JDBCExecutionContext) getDataSourceContainer().getDataSource().openIsolatedContext(monitor, "Debug session");
            return sessionManager.createDebugSession(sessionContext);
        } catch (DBException e) {
            throw new DBGException("Can't initiate debug session", e);
        }
    }

    @Override
    public void dispose() {
        if (sessionManager != null) {
            sessionManager.dispose();
            sessionManager = null;
        }
    }
}
