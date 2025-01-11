/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
    private volatile long cancelTimestamp = -1;
    private AbstractJob attachedJob = null;
    private boolean skipErrorOnCanceling;
    private volatile boolean runDirectly = false;

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

    private boolean isSkipErrorOnCanceling() {
        return skipErrorOnCanceling;
    }

    protected void setSkipErrorOnCanceling(boolean skipErrorOnCanceling) {
        this.skipErrorOnCanceling = skipErrorOnCanceling;
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
        runDirectly = true;
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

            IStatus result = this.run(progressMonitor);
            if (!logErrorStatus(result)) {
                if (!result.isOK() && result != Status.CANCEL_STATUS) {
                    log.error("Error running job '" + getName() + "' execution: " + result.getMessage());
                }
            }
            return result;
        } catch (Throwable e) {
            log.error(e);
            return GeneralUtils.makeExceptionStatus(e);
        } finally {
            CURRENT_JOB.remove();
            finished = true;
            currentThread.setName(oldThreadName);
        }
    }

    public final void schedule(@NotNull Duration delay) {
        schedule(delay.toMillis());
    }

    private boolean logErrorStatus(IStatus status) {
        if (status.getException() != null) {
            log.error("Error during job '" + getName() + "' execution", status.getException());
            return true;
        } else if (status instanceof MultiStatus) {
            for (IStatus cStatus : status.getChildren()) {
                if (logErrorStatus(cStatus)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected abstract IStatus run(DBRProgressMonitor monitor);

    public boolean isCanceled() {
        return cancelTimestamp > 0;
    }

    public long getCancelTimestamp() {
        return cancelTimestamp;
    }

    @Override
    protected void canceling()
    {
        if (cancelTimestamp == -1) {
            cancelTimestamp = System.currentTimeMillis();
        }
        if (attachedJob != null) {
            attachedJob.canceling();
            return;
        }
        // Run canceling job
        if (!blockCanceled) {
            runBlockCanceler();
        }
    }

    public boolean isForceCancel() {
        return true;
    }

    private void runBlockCanceler() {
        final List<DBRBlockingObject> activeBlocks = new ArrayList<>(
            CommonUtils.safeList(progressMonitor.getActiveBlocks()));
        if (activeBlocks.isEmpty()) {
            // Nothing to cancel
            return;
        }

        if (!isForceCancel() && activeBlocks.size() < 2) {
            return;
        }

        final DBRBlockingObject lastBlock = activeBlocks.remove(activeBlocks.size() - 1);

        try {
            new JobCanceler(lastBlock).schedule();
        } catch (Exception e) {
            // If this happens during shutdown and job manager is not active
            log.debug(e);
        }

        if (!activeBlocks.isEmpty()) {
            DBPPreferenceStore preferenceStore;
            if (activeBlocks.get(0) instanceof DBCSession) {
                DBPDataSource dataSource = ((DBCSession) activeBlocks.get(0)).getDataSource();
                if (dataSource == null) {
                    return;
                }
                preferenceStore = dataSource.getContainer().getPreferenceStore();
            } else {
                preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
            }

            int cancelCheckTimeout = preferenceStore.getInt(ModelPreferences.EXECUTE_CANCEL_CHECK_TIMEOUT);

            if (cancelCheckTimeout > 0) {
                // There are other blocks. If last one can't be canceled then try others
                Job cancelChecker = new Job("Cancel checker block") { //$NON-N LS-1$
                    {
                        setSystem(true);
                        setUser(false);
                    }

                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        if (!finished) {
                            DBRBlockingObject nextBlock = activeBlocks.remove(activeBlocks.size() - 1);
                            new JobCanceler(nextBlock).schedule();
                            if (!activeBlocks.isEmpty()) {
                                schedule(cancelCheckTimeout);
                            }
                        }
                        return Status.OK_STATUS;
                    }
                };
                cancelChecker.schedule(cancelCheckTimeout);
            }
        }

    }

    private class JobCanceler extends Job {
        private final DBRBlockingObject block;

        public JobCanceler(DBRBlockingObject block) {
            super("Operation cancel"); //$NON-N LS-1$
            this.block = block;
            setSystem(true);
            setUser(false);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            if (!finished) {
                try {
                    BlockCanceler.cancelBlock(progressMonitor, block);
                } catch (DBException e) {
                    log.debug("Block cancel error", e); //$NON-N LS-1$
                    if (!isSkipErrorOnCanceling()) {
                        return GeneralUtils.makeExceptionStatus(e);
                    }
                } catch (Throwable e) {
                    log.debug("Block cancel internal error: " + e.getMessage()); //$NON-N LS-1$
                    return Status.CANCEL_STATUS;
                }
                blockCanceled = true;
            }
            return Status.OK_STATUS;
        }
    }

    public boolean isRunDirectly() {
        return runDirectly;
    }
}