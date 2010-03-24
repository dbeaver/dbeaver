package org.jkiss.dbeaver.runtime;

import org.jkiss.dbeaver.model.DBPProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Progress monitor default implementation
 */
public class DefaultProgressMonitor implements DBPProgressMonitor {

    private IProgressMonitor nestedMonitor;

    public DefaultProgressMonitor(IProgressMonitor nestedMonitor)
    {
        this.nestedMonitor = nestedMonitor;
    }

    public IProgressMonitor getNestedMonitor()
    {
        return nestedMonitor;
    }

    public void beginTask(String name, int totalWork)
    {
        nestedMonitor.beginTask(name, totalWork);
    }

    public void done()
    {
        nestedMonitor.done();
    }

    public void subTask(String name)
    {
        nestedMonitor.subTask(name);
    }

    public void worked(int work)
    {
        nestedMonitor.worked(work);
    }

    public boolean isCanceled()
    {
        return nestedMonitor.isCanceled();
    }

}
