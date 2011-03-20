/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

    public static final int TIMEOUT_BEFORE_BLOCK_CANCEL = 1*1000;

    private DBRProgressMonitor progressMonitor;
    private boolean finished = false;
    private volatile boolean blockCanceled = false;
    private int cancelTimeout = TIMEOUT_BEFORE_BLOCK_CANCEL;

    protected AbstractJob(String name)
    {
        super(name);
    }

    protected DBRProgressMonitor getProgressMonitor()
    {
        return progressMonitor;
    }

    public int getCancelTimeout()
    {
        return cancelTimeout;
    }

    public void setCancelTimeout(int cancelTimeout)
    {
        this.cancelTimeout = cancelTimeout;
    }

    protected final IStatus run(IProgressMonitor monitor)
    {
        progressMonitor = RuntimeUtils.makeMonitor(monitor);
        blockCanceled = false;
        IStatus status;
        try {
            finished = false;
            status = this.run(progressMonitor);
        } finally {
            finished = true;
        }
        return status;
    }

    protected abstract IStatus run(DBRProgressMonitor monitor);


    protected void canceling()
    {
/*
        if (!cancelRequested) {
            cancelRequested = true;
            return;
        }
*/
        // do it only on second request
        if (!blockCanceled) {
            Job cancelJob = new Job("Cancel block") {
                @Override
                protected IStatus run(IProgressMonitor monitor)
                {
                    if (!finished && !blockCanceled) {
                        DBRBlockingObject block = getProgressMonitor().getActiveBlock();
                        if (block != null) {
                            try {
                                block.cancelBlock();
                            } catch (DBException e) {
                                log.error("Could not interrupt operation" + block, e);
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