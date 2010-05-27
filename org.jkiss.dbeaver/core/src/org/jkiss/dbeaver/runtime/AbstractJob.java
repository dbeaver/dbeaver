/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.DBeaverUtils;

/**
 * Abstract Database Job
 */
public abstract class AbstractJob extends Job
{

    protected AbstractJob(String name)
    {
        super(name);
    }

    protected final IStatus run(IProgressMonitor monitor)
    {
        return this.run(DBeaverUtils.makeMonitor(monitor));
    }

    protected abstract IStatus run(DBRProgressMonitor monitor);

}