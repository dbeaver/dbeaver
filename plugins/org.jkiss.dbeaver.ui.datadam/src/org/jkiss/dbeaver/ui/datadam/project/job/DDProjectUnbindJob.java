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
package org.jkiss.dbeaver.ui.datadam.project.job;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectSyncPropertyTester;

/** Removes the local DataDam binding without deleting either project. */
public class DDProjectUnbindJob extends DDProjectOperationJob<DBPProject> {
    @NotNull
    private final DBPProject project;

    public DDProjectUnbindJob(@NotNull DBPProject project) {
        super(DDTrackingUIMessages.project_sync_unbind_job, DDTrackingUIMessages.project_sync_unbind_failed);
        this.project = project;
    }

    @Override
    protected boolean canRun(@NotNull DBRProgressMonitor monitor) {
        return !monitor.isCanceled();
    }

    @Override
    @NotNull
    protected DBPProject perform(@NotNull DBRProgressMonitor monitor) throws DBException {
        uiManager.getProjectManager().unbindProject(project);
        DDProjectSyncPropertyTester.firePropertyChange();
        return project;
    }
}
