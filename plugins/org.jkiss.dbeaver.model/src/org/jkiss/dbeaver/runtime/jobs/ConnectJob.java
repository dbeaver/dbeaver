/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.runtime.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * Connect job.
 * Always returns OK status.
 * To get real status use getConectStatus.
 */
public class ConnectJob extends AbstractJob {

    private volatile Thread connectThread;
    protected boolean initialize = true;
    protected boolean reflect = true;
    protected Throwable connectError;
    protected IStatus connectStatus;
    protected final DBPDataSourceContainer container;

    public ConnectJob(@NotNull DBPDataSourceContainer container) {
        super("Connect to '" + container.getName() + "'");
        setUser(true);
        this.container = container;
    }

    public @Nullable IStatus getConnectStatus() {
        return connectStatus;
    }

    public @Nullable Throwable getConnectError() {
        return connectError;
    }

    @NotNull
    @Override
    protected IStatus run(@NotNull DBRProgressMonitor monitor) {
        try {
            if (container.getDriver().getDriverStub() != null) {
                throw new DBException(
                    "Driver " + container.getDriver().getFullName()+ " is not available." +
                    " Please see the connection page for more info.");
            }

            Thread connectionThread = getThread();
            connectThread = connectionThread;
            String oldName = connectionThread == null ? null : connectionThread.getName();
            if (reflect && connectionThread != null) {
                connectionThread.setName(getName());
            }

            try {
                final boolean connected = container.connect(monitor, initialize, reflect);
                connectThread = null;

                if (monitor.isCanceled()) {
                    if (connected) {
                        // Some drivers ignore interruption and complete initialization after cancellation.
                        // Use a fresh monitor so the cleanup itself cannot be canceled.
                        boolean interrupted = Thread.interrupted();
                        try {
                            container.disconnect(new VoidProgressMonitor());
                        } finally {
                            if (interrupted) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                    connectStatus = Status.CANCEL_STATUS;
                } else {
                    connectStatus = connected ? Status.OK_STATUS : Status.CANCEL_STATUS;
                }
            } finally {
                if (oldName != null) {
                    connectionThread.setName(oldName);
                }
                connectThread = null;
            }
        }
        catch (Throwable ex) {
            connectError = ex;
            connectStatus = GeneralUtils.makeExceptionStatus(ex);
        }

        return Status.OK_STATUS;
    }

    public @NotNull IStatus runSync(@NotNull DBRProgressMonitor monitor) {
        AbstractJob curJob = CURRENT_JOB.get();
        if (curJob != null) {
            curJob.setAttachedJob(this);
        }
        Thread cancelWatcher = null;
        try {
            setThread(Thread.currentThread());
            reflect = false;
            if (curJob == null) {
                // Runnable contexts have no owner job to forward monitor cancellation.
                cancelWatcher = Thread.ofVirtual().name("Connection cancel watcher").start(() -> {
                    try {
                        while (true) {
                            if (monitor.isCanceled() && connectThread != null) {
                                canceling();
                                return;
                            }
                            Thread.sleep(50);
                        }
                    } catch (InterruptedException ignored) {
                    }
                });
            }
            return run(monitor);
        } finally {
            if (cancelWatcher != null) {
                cancelWatcher.interrupt();
            }
            if (curJob != null) {
                curJob.setAttachedJob(null);
            }
        }
    }

    @Override
    public boolean belongsTo(Object family)
    {
        return container == family;
    }

    @Override
    protected void canceling() {
        if (connectThread != null) {
            connectThread.interrupt();
        }
    }

}
