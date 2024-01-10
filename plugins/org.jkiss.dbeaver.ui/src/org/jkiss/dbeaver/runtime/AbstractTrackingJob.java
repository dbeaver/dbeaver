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
package org.jkiss.dbeaver.runtime;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.lang.reflect.InvocationTargetException;

public abstract class AbstractTrackingJob extends AbstractJob {
    private DBRProgressMonitor ownerMonitor;
    protected Throwable connectError;

    protected AbstractTrackingJob(String name) {
        super(name);
        setUser(false);
        setSystem(true);
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor) {
        if (ownerMonitor != null) {
            monitor = ownerMonitor;
        }

        try {
            execute(monitor);
        } catch (Throwable e) {
            connectError = e;
        }

        return Status.OK_STATUS;
    }

    public void setOwnerMonitor(@Nullable DBRProgressMonitor ownerMonitor) {
        this.ownerMonitor = ownerMonitor;
    }

    @Nullable
    public Throwable getConnectError() {
        return connectError;
    }

    protected abstract void execute(@NotNull DBRProgressMonitor monitor) throws Throwable;

    public static void executeInProgressMonitor(AbstractTrackingJob job) throws InvocationTargetException {
        UIUtils.runInProgressDialog(monitor -> {
            job.setOwnerMonitor(monitor);
            job.schedule();

            while (job.getState() == Job.WAITING || job.getState() == Job.RUNNING) {
                if (monitor.isCanceled()) {
                    job.cancel();
                    throw new InvocationTargetException(null);
                }
                RuntimeUtils.pause(50);
            }

            if (job.getConnectError() != null) {
                throw new InvocationTargetException(job.getConnectError());
            }
        });
    }
}
