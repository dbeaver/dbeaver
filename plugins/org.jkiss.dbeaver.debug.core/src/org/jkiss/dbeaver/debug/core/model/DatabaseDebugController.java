package org.jkiss.dbeaver.debug.core.model;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.core.DebugCore;
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
                String message = NLS.bind("Unable to connect to {0}", dataSourceDescriptor);
                IStatus error = new Status(IStatus.ERROR, DebugCore.BUNDLE_SYMBOLIC_NAME, message, e);
                log.error(message, e);
                return error;
            }
        }
        try {
            DBPDataSource dataSource = dataSourceDescriptor.getDataSource();
            this.debugContext = dataSource.openIsolatedContext(dbrMonitor, "Debug");
            this.debugSession = debugContext.openSession(dbrMonitor, DBCExecutionPurpose.UTIL, "Debug queries");
            afterSessionOpen(debugSession);
        } catch (DBException e) {
            String message = NLS.bind("Unable to open debug context for {0}", dataSourceDescriptor);
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
