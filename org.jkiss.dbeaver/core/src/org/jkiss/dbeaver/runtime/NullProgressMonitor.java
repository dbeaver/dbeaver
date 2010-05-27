/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime;

import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Progress monitor null implementation
 */
public class NullProgressMonitor implements DBRProgressMonitor {

    public static final NullProgressMonitor INSTANCE = new NullProgressMonitor();

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