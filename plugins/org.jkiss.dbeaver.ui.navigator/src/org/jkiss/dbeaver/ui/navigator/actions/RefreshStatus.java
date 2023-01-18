/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;

public class RefreshStatus {
    public final Job job;
    public final boolean initiated;
    public final boolean completed;

    private RefreshStatus(Job job, boolean initiated, boolean completed) {
        this.job = job;
        this.initiated = initiated;
        this.completed = completed;
    }

    public boolean join(long timeoutMills, IProgressMonitor monitor) throws OperationCanceledException, InterruptedException {
        if (job != null) {
            return job.join(timeoutMills, monitor);
        } else if (completed) {
            return true;
        } else {
            return false;
        }
    }

    public static RefreshStatus initiated(Job job) {
        return new RefreshStatus(job, true, false);
    }

    public static RefreshStatus completed() {
        return new RefreshStatus(null, true, true);
    }

    public static RefreshStatus skipped() {
        return new RefreshStatus(null, false, false);
    }
}