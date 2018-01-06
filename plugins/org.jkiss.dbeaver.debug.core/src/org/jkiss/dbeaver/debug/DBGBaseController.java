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
package org.jkiss.dbeaver.debug;

import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.internal.DebugMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public abstract class DBGBaseController implements DBGController {

    private static final Log log = Log.getLog(DBGBaseController.class);

    private DBPDataSourceContainer dataSourceContainer;

    public DBGBaseController(DBPDataSourceContainer dataSourceContainer) {
        this.dataSourceContainer = dataSourceContainer;
    }

    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    @Override
    public DBGSession connect(DBRProgressMonitor monitor) throws DBGException {
        DBPDataSource dataSource = dataSourceContainer.getDataSource();
        if (!dataSourceContainer.isConnected()) {
            throw new DBGException("Not connected to database");
        }
        try {
            return createSession(monitor, dataSource);
        } catch (DBException e) {
            String message = NLS.bind(DebugMessages.DatabaseDebugController_e_opening_debug_context,
                dataSourceContainer);
            log.error(message, e);
            throw new DBGException(message, e);
        }
    }

    protected abstract DBGSession createSession(DBRProgressMonitor monitor, DBPDataSource dataSource) throws DBGException;

    @Override
    public void resume(DBRProgressMonitor monitor, DBGSession session) throws DBGException {
        // TODO Auto-generated method stub

    }

    @Override
    public void suspend(DBRProgressMonitor monitor, DBGSession session) throws DBGException {
        // TODO Auto-generated method stub

    }

    @Override
    public void terminate(DBRProgressMonitor monitor, DBGSession session) throws DBGException {

    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub

    }

}
