package org.jkiss.dbeaver.runtime;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Progress monitor null implementation
 */
public class NullProgressMonitor implements DBRProgressMonitor {

    public IProgressMonitor getNestedMonitor()
    {
        return null;
    }

    public void beginTask(String name, int totalWork)
    {
    }

    public void done()
    {
    }

    public void subTask(String name)
    {
    }

    public void worked(int work)
    {
    }

    public boolean isCanceled()
    {
        return false;
    }

}