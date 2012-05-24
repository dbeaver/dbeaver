/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Progress monitor null implementation
 */
public class VoidProgressMonitor implements DBRProgressMonitor {

    public static final VoidProgressMonitor INSTANCE = new VoidProgressMonitor();

    private static final IProgressMonitor NESTED_INSTANCE = new NullProgressMonitor();

    private VoidProgressMonitor() {
    }

    @Override
    public IProgressMonitor getNestedMonitor()
    {
        return NESTED_INSTANCE;
    }

    @Override
    public void beginTask(String name, int totalWork)
    {
    }

    @Override
    public void done()
    {
    }

    @Override
    public void subTask(String name)
    {
    }

    @Override
    public void worked(int work)
    {
    }

    @Override
    public boolean isCanceled()
    {
        return false;
    }

    @Override
    public void startBlock(DBRBlockingObject object, String taskName)
    {
        // do nothing
    }

    @Override
    public void endBlock()
    {
        // do nothing
    }

    @Override
    public DBRBlockingObject getActiveBlock()
    {
        return null;
    }

}