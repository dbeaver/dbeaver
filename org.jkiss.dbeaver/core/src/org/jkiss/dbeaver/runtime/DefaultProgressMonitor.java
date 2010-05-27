/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime;

import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Progress monitor default implementation
 */
public class DefaultProgressMonitor implements DBRProgressMonitor {

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
