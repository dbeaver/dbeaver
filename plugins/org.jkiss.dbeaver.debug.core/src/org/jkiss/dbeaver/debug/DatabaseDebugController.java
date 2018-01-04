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

import java.util.Map;

import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.internal.DebugMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.DBRResult;
import org.jkiss.dbeaver.runtime.DefaultResult;

public class DatabaseDebugController implements DBGController {
    
    private static final Log log = Log.getLog(DatabaseDebugController.class);

    private final String datasourceId;
    
    private DBCExecutionContext debugContext;
    private DBCSession debugSession;
    
    public DatabaseDebugController(String datasourceId, String databaseName, Map<String, Object> attributes)
    {
        this.datasourceId = datasourceId;
    }

    @Override
    public DBRResult connect(DBRProgressMonitor monitor)
    {
        DataSourceDescriptor dataSourceDescriptor = DataSourceRegistry.findDataSource(datasourceId);
        if (dataSourceDescriptor == null) {
            String message = NLS.bind("Unable to find data source with id {0}", datasourceId);
            
            return DefaultResult.error(message);
        }
        DBPDataSource dataSource = dataSourceDescriptor.getDataSource();
        if (!dataSourceDescriptor.isConnected()) {
            
            try {
                //FIXME: AF: the contract of this call is not clear, we need some utility for this 
                dataSourceDescriptor.connect(monitor, true, true);
            } catch (DBException e) {
                String message = NLS.bind(DebugMessages.DatabaseDebugController_e_connecting_datasource, dataSourceDescriptor);
                log.error(message, e);
                return DefaultResult.error(message, e);
            }
        }
        try {
            this.debugContext = dataSource.openIsolatedContext(monitor, DebugMessages.DatabaseDebugController_debug_context_purpose);
            this.debugSession = debugContext.openSession(monitor, DBCExecutionPurpose.UTIL, DebugMessages.DatabaseDebugController_debug_session_name);
            afterSessionOpen(debugSession);
        } catch (DBException e) {
            String message = NLS.bind(DebugMessages.DatabaseDebugController_e_opening_debug_context, dataSourceDescriptor);
            log.error(message, e);
            return DefaultResult.error(message, e);
        }
        return DefaultResult.ok();
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
