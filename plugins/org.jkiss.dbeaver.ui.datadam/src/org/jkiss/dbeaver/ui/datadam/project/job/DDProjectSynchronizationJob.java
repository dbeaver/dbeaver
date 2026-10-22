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
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectCatalog;

import java.util.ArrayList;
import java.util.List;

/** Synchronizes one or more bound projects, reporting conflicts without modifying them. */
public class DDProjectSynchronizationJob extends DDProjectOperationJob<List<DDProjectCatalog.SyncResult>> {
    @NotNull
    private final List<DBPProject> projects;

    public DDProjectSynchronizationJob(@NotNull List<DBPProject> projects) {
        super(DDTrackingUIMessages.project_sync_update_job, DDTrackingUIMessages.project_sync_update_failed);
        this.projects = List.copyOf(projects);
    }

    @Override
    @NotNull
    protected List<DDProjectCatalog.SyncResult> perform(@NotNull DBRProgressMonitor monitor) {
        List<DDProjectCatalog.SyncResult> collected = new ArrayList<>(projects.size());
        for (DBPProject project : projects) {
            if (monitor.isCanceled()) {
                break;
            }
            collected.add(catalog.synchronizeProject(project));
        }
        return List.copyOf(collected);
    }
}
