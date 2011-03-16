/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Progress monitor default implementation
 */
public class ProxyProgressMonitor implements DBRProgressMonitor {

    static final Log log = LogFactory.getLog(ProxyProgressMonitor.class);

    private DBRProgressMonitor original;

    public ProxyProgressMonitor(DBRProgressMonitor original)
    {
        this.original = original;
    }

    public IProgressMonitor getNestedMonitor()
    {
        return original.getNestedMonitor();
    }

    public void beginTask(String name, int totalWork)
    {
        original.beginTask(name, totalWork);
    }

    public void done()
    {
        original.done();
    }

    public void subTask(String name)
    {
        original.subTask(name);
    }

    public void worked(int work)
    {
        original.worked(work);
    }

    public boolean isCanceled()
    {
        return original.isCanceled();
    }

    public synchronized void startBlock(DBRBlockingObject object, String taskName)
    {
        original.startBlock(object, taskName);
    }

    public synchronized void endBlock()
    {
        original.endBlock();
    }

    public DBRBlockingObject getActiveBlock()
    {
        return original.getActiveBlock();
    }

}
