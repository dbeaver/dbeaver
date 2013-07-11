/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.runtime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Abstract Database Job
 */
public abstract class AbstractJob extends Job
{
    static final Log log = LogFactory.getLog(AbstractJob.class);

    public static final int TIMEOUT_BEFORE_BLOCK_CANCEL = 1 * 1000;

    private DBRProgressMonitor progressMonitor;
    private volatile boolean finished = false;
    private volatile boolean blockCanceled = false;
    private int cancelTimeout = TIMEOUT_BEFORE_BLOCK_CANCEL;

    protected AbstractJob(String name)
    {
        super(name);
    }

    public int getCancelTimeout()
    {
        return cancelTimeout;
    }

    public void setCancelTimeout(int cancelTimeout)
    {
        this.cancelTimeout = cancelTimeout;
    }

    protected Thread getActiveThread()
    {
        final Thread thread = getThread();
        return thread == null ? Thread.currentThread() : thread;
    }

    public final IStatus runDirectly(DBRProgressMonitor monitor)
    {
        progressMonitor = monitor;
        blockCanceled = false;
        try {
            finished = false;
            IStatus result;
            try {
                result = this.run(progressMonitor);
            } catch (Throwable e) {
                result = RuntimeUtils.makeExceptionStatus(e);
            }
            return result;
        } finally {
            finished = true;
        }
    }

    @Override
    protected final IStatus run(IProgressMonitor monitor)
    {
        progressMonitor = RuntimeUtils.makeMonitor(monitor);
        blockCanceled = false;
        try {
            finished = false;
            return this.run(progressMonitor);
        } finally {
            finished = true;
        }
    }

    protected abstract IStatus run(DBRProgressMonitor monitor);


    @Override
    protected void canceling()
    {
        // Run canceling job
        if (!blockCanceled) {
            Job cancelJob = new Job("Cancel block") { //$NON-NLS-1$
                @Override
                protected IStatus run(IProgressMonitor monitor)
                {
                    if (!finished && !blockCanceled) {
                        DBRBlockingObject block = progressMonitor.getActiveBlock();
                        if (block != null) {
                            try {
                                block.cancelBlock();
                            } catch (DBException e) {
                                log.error("Can't interrupt operation " + block, e); //$NON-NLS-1$
                            }
                            blockCanceled = true;
                        }
                    }
                    return Status.OK_STATUS;
                }
            };
            // Cancel it in three seconds
            cancelJob.schedule(cancelTimeout);
        }
    }

}