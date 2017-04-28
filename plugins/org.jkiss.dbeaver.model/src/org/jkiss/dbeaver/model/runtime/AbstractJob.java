/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.runtime;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
 * Abstract Database Job
 */
public abstract class AbstractJob extends Job
{
    private static final Log log = Log.getLog(AbstractJob.class);

    public static final int TIMEOUT_BEFORE_BLOCK_CANCEL = 250;

    private DBRProgressMonitor progressMonitor;
    private volatile boolean finished = false;
    private volatile boolean blockCanceled = false;
    private AbstractJob attachedJob = null;

    // Attached job may be used to "overwrite" current job.
    // It happens if some other AbstractJob runs in sync mode
    protected final static ThreadLocal<AbstractJob> CURRENT_JOB = new ThreadLocal<>();

    protected AbstractJob(String name)
    {
        super(name);
    }

    public boolean isFinished() {
        return finished;
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
        final Thread currentThread = Thread.currentThread();
        final String oldThreadName = currentThread.getName();
        try {
            finished = false;
            RuntimeUtils.setThreadName(getName());

            return this.run(progressMonitor);
        } catch (Throwable e) {
            log.error(e);
            return GeneralUtils.makeExceptionStatus(e);
        } finally {
            CURRENT_JOB.remove();
            finished = true;
            currentThread.setName(oldThreadName);
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
            Job cancelJob = new Job("Cancel block") { //$NON-N LS-1$
                @Override
                protected IStatus run(IProgressMonitor monitor)
                {
                    if (!finished && !blockCanceled) {
                        try {
                            BlockCanceler.cancelBlock(progressMonitor, getActiveThread());
                        } catch (DBException e) {
                            return GeneralUtils.makeExceptionStatus(e);
                        } catch (Throwable e) {
                            log.debug("Cancel error", e);
                            return Status.CANCEL_STATUS;
                        }
                        blockCanceled = true;
                    }
                    return Status.OK_STATUS;
                }
            };
            try {
                // Schedule cancel after short pause
                cancelJob.schedule(TIMEOUT_BEFORE_BLOCK_CANCEL);
            } catch (Exception e) {
                // If this happens during shutdown and job manager is not active
                log.debug(e);
            }
        }
    }

}