/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

    public IProgressMonitor getNestedMonitor()
    {
        return NESTED_INSTANCE;
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

    public void startBlock(DBRBlockingObject object, String taskName)
    {
        // do nothing
    }

    public void endBlock()
    {
        // do nothing
    }

    public DBRBlockingObject getActiveBlock()
    {
        return null;
    }

    public int getBlockCount() {
        return 0;
    }

}