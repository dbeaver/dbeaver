/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime;

import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Progress monitor default implementation
 */
public class ProxyProgressMonitor implements DBRProgressMonitor {

    private DBRProgressMonitor original;

    public ProxyProgressMonitor(DBRProgressMonitor original)
    {
        this.original = original;
    }

    @Override
    public IProgressMonitor getNestedMonitor()
    {
        return original.getNestedMonitor();
    }

    @Override
    public void beginTask(String name, int totalWork)
    {
        original.beginTask(name, totalWork);
    }

    @Override
    public void done()
    {
        original.done();
    }

    @Override
    public void subTask(String name)
    {
        original.subTask(name);
    }

    @Override
    public void worked(int work)
    {
        original.worked(work);
    }

    @Override
    public boolean isCanceled()
    {
        return original.isCanceled();
    }

    @Override
    public synchronized void startBlock(DBRBlockingObject object, String taskName)
    {
        original.startBlock(object, taskName);
    }

    @Override
    public synchronized void endBlock()
    {
        original.endBlock();
    }

    @Override
    public DBRBlockingObject getActiveBlock()
    {
        return original.getActiveBlock();
    }

}
