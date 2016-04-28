/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.runtime;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
 * Abstract Database Job
 */
public abstract class AbstractJob extends Job
{
    private static final Log log = Log.getLog(AbstractJob.class);

    public static final int TIMEOUT_BEFORE_BLOCK_CANCEL = 400;

    private DBRProgressMonitor progressMonitor;
    private volatile boolean finished = false;
    private volatile boolean blockCanceled = false;
    private int cancelTimeout = TIMEOUT_BEFORE_BLOCK_CANCEL;
    private AbstractJob attachedJob = null;

    // Attached job may be used to "overwrite" current job.
    // It happens if some other AbstractJob runs in sync mode
    protected final static ThreadLocal<AbstractJob> CURRENT_JOB = new ThreadLocal<AbstractJob>();

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

    public void setAttachedJob(AbstractJob attachedJob) {
        this.attachedJob = attachedJob;
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
                result = GeneralUtils.makeExceptionStatus(e);
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
        CURRENT_JOB.set(this);
        try {
            finished = false;
            return this.run(progressMonitor);
        } finally {
            CURRENT_JOB.remove();
            finished = true;
        }
    }

    protected abstract IStatus run(DBRProgressMonitor monitor);


    @Override
    protected void canceling()
    {
        if (attachedJob != null) {
            attachedJob.canceling();
            return;
        }
        // Run canceling job
        if (!blockCanceled) {
            // Try to interrupt thread first
            Thread activeThread = getActiveThread();
            activeThread.interrupt();

            // Schedule block cancel after timeout (let activeThread.interrupt to finish it's job)
            Job cancelJob = new Job("Cancel block") { //$NON-N LS-1$
                @Override
                protected IStatus run(IProgressMonitor monitor)
                {
                    if (!finished && !blockCanceled) {
                        DBRBlockingObject block = progressMonitor.getActiveBlock();
                        if (block != null) {
                            RuntimeUtils.setThreadName("Operation canceler [" + block + "]");
                            try {
                                block.cancelBlock();
                            } catch (DBException e) {
                                return GeneralUtils.makeExceptionStatus("Can't interrupt operation " + block, e); //$NON-NLS-1$
                            } catch (Throwable e) {
                                log.debug("Cancel error", e);
                                return Status.CANCEL_STATUS;
                            }
                            blockCanceled = true;
                        }
                    }
                    return Status.OK_STATUS;
                }
            };
            try {
                // Cancel it in three seconds
                cancelJob.schedule(cancelTimeout);
            } catch (Exception e) {
                // If this happens during shutdown and job manager is not active
                log.debug(e);
            }
        }
    }

}