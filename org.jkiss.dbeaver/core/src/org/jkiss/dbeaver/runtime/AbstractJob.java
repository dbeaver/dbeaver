/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
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
import org.jkiss.dbeaver.utils.DBeaverUtils;

/**
 * Abstract Database Job
 */
public abstract class AbstractJob extends Job
{
    static final Log log = LogFactory.getLog(AbstractJob.class);

    private DBRProgressMonitor progressMonitor;
    private boolean finished = false;
    private boolean blockCanceled = false;

    protected AbstractJob(String name)
    {
        super(name);
    }

    protected DBRProgressMonitor getProgressMonitor()
    {
        return progressMonitor;
    }

    protected final IStatus run(IProgressMonitor monitor)
    {
        progressMonitor = DBeaverUtils.makeMonitor(monitor);
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
            cancelJob.schedule(1*1000);
        }
    }

}