package org.jkiss.dbeaver.runtime.load;

import org.jkiss.dbeaver.model.DBPProgressMonitor;

/**
 * Lazy loading service
 * @param <RESULT> result type
 */
public abstract class AbstractLoadService<RESULT> implements ILoadService<RESULT> {
    private String serviceName;
    private DBPProgressMonitor progressMonitor;
    private ILoadService nestedService;

    protected AbstractLoadService(String serviceName)
    {
        this.serviceName = serviceName;
    }

    protected AbstractLoadService()
    {
        this("Loading");
    }

    public String getServiceName()
    {
        return serviceName;
    }

    public DBPProgressMonitor getProgressMonitor()
    {
        return progressMonitor;
    }

    public void setProgressMonitor(DBPProgressMonitor monitor)
    {
        this.progressMonitor = monitor;
    }

    public void setNestedService(ILoadService nested)
    {
        assert(this.nestedService == null);
        assert(nested != null);
        this.nestedService = nested;
        this.nestedService.setProgressMonitor(getProgressMonitor());
    }

    public void clearNestedService()
    {
        assert(this.nestedService != null);
        this.nestedService = null;
    }

    public boolean cancel()
    {
        // Invoke nested service cancel
        return nestedService != null && nestedService.cancel();
    }

}