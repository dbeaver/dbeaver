/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Abstract Database Job
 */
public abstract class AbstractUIJob extends UIJob
{
    static protected final Log log = LogFactory.getLog(AbstractUIJob.class);

    protected AbstractUIJob(String name)
    {
        super(name);
    }

    public IStatus runInUIThread(IProgressMonitor monitor)
    {
        return this.runInUIThread(RuntimeUtils.makeMonitor(monitor));
    }

    protected abstract IStatus runInUIThread(DBRProgressMonitor monitor);

}