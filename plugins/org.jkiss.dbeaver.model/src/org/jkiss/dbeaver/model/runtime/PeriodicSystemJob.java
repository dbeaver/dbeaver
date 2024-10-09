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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBPPlatform;

public abstract class PeriodicSystemJob extends AbstractJob {

    @NotNull
    protected final DBPPlatform platform;
    private final long periodMs;

    public PeriodicSystemJob(@NotNull String name, @NotNull DBPPlatform platform, long periodMs) {
        super(name);
        this.platform = platform;
        this.periodMs = periodMs;

        setUser(false);
        setSystem(true);
    }

    @Override
    protected IStatus run(@NotNull DBRProgressMonitor monitor) {
        if (platform.isShuttingDown()) {
            return Status.OK_STATUS;
        }

        doJob(monitor);

        // If the platform is still running after the job is completed, reschedule the job
        if (!platform.isShuttingDown()) {
            scheduleMonitor();
        }

        return Status.OK_STATUS;
    }

    protected abstract void doJob(@NotNull DBRProgressMonitor monitor);

    public void scheduleMonitor() {
        schedule(periodMs);
    }
}
