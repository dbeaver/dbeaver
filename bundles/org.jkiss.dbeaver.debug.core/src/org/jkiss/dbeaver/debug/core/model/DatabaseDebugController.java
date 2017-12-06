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
package org.jkiss.dbeaver.debug.core.model;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.debug.internal.core.DebugCoreMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;

public class DatabaseDebugController implements IDatabaseDebugController {
    
    private static final Log log = Log.getLog(DatabaseDebugController.class);

    private final String datasourceId;
    
    private DBCExecutionContext debugContext;
    private DBCSession debugSession;
    
    public DatabaseDebugController(String datasourceId, String databaseName, Map<String, Object> attributes)
    {
        this.datasourceId = datasourceId;
    }

    @Override
    public IStatus connect(IProgressMonitor monitor)
    {
        DataSourceDescriptor dataSourceDescriptor = DataSourceRegistry.findDataSource(datasourceId);
        if (dataSourceDescriptor == null) {
            //FIXME:AF: provide error status
            return Status.CANCEL_STATUS;
        }
        DefaultProgressMonitor dbrMonitor = new DefaultProgressMonitor(monitor);
        if (!dataSourceDescriptor.isConnected()) {
            
            try {
                dataSourceDescriptor.connect(dbrMonitor, true, true);
            } catch (DBException e) {
                String message = NLS.bind(DebugCoreMessages.DatabaseDebugController_e_connecting_datasource, dataSourceDescriptor);
                IStatus error = new Status(IStatus.ERROR, DebugCore.BUNDLE_SYMBOLIC_NAME, message, e);
                log.error(message, e);
                return error;
            }
        }
        try {
            DBPDataSource dataSource = dataSourceDescriptor.getDataSource();
            this.debugContext = dataSource.openIsolatedContext(dbrMonitor, DebugCoreMessages.DatabaseDebugController_debug_context_purpose);
            this.debugSession = debugContext.openSession(dbrMonitor, DBCExecutionPurpose.UTIL, DebugCoreMessages.DatabaseDebugController_debug_session_name);
            afterSessionOpen(debugSession);
        } catch (DBException e) {
            String message = NLS.bind(DebugCoreMessages.DatabaseDebugController_e_opening_debug_context, dataSourceDescriptor);
            IStatus error = new Status(IStatus.ERROR, DebugCore.BUNDLE_SYMBOLIC_NAME, message, e);
            log.error(message, e);
            return error;
        }
        return Status.OK_STATUS;
    }

    protected void afterSessionOpen(DBCSession session)
    {
        //do nothing by default
    }

    protected void beforeSessionClose(DBCSession session)
    {
        //do nothing by default
    }

    @Override
    public void resume()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void suspend()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void terminate()
    {
        beforeSessionClose(this.debugSession);
        if (this.debugSession != null) {
            this.debugSession.close();
            this.debugSession = null;
        }

        if (this.debugContext != null) {
            this.debugContext.close();
            this.debugContext = null;
        }
    }
    
}
