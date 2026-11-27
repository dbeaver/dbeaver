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

import com.dbeaver.datadam.share.api.model.DDSharedProject;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;

/** Updates the name and description of a DataDam project. */
public class DDProjectUpdateJob extends DDProjectOperationJob<DDSharedProject> {
    @NotNull
    private final DDSharedProject project;
    @NotNull
    private final String name;
    @Nullable
    private final String description;

    public DDProjectUpdateJob(@NotNull DDSharedProject project, @NotNull String name, @Nullable String description) {
        super(DDTrackingUIMessages.project_edit_job, DDTrackingUIMessages.project_edit_failed);
        this.project = project;
        this.name = name;
        this.description = description;
    }

    @NotNull
    @Override
    protected DDSharedProject perform(@NotNull DBRProgressMonitor monitor) throws DBException {
        return uiManager.getProjectManager().updateProject(project, name, description);
    }
}
