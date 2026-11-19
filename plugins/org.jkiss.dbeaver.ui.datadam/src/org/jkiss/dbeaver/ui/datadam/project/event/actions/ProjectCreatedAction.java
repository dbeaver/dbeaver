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
package org.jkiss.dbeaver.ui.datadam.project.event.actions;

import com.dbeaver.datadam.share.api.model.DDSharedProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager;
import org.jkiss.dbeaver.ui.datadam.project.handler.DDProjectImportHandler;

import java.util.List;
import java.util.UUID;

public class ProjectCreatedAction extends AbstractProjectEventJob {
    public ProjectCreatedAction(@NotNull DDProjectUIManager uiManager, @NotNull UUID remoteProjectId) {
        super(uiManager, remoteProjectId, DDTrackingUIMessages.project_sync_import_list_job,
            DDTrackingUIMessages.project_sync_import_list_failed);
    }

    @NotNull
    @Override
    protected IStatus perform(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (uiManager.getProjectManager().isCreatingProject(remoteProjectId)) {
            log.debug("Ignoring shared project creation: project is being shared locally: " + remoteProjectId);
            return Status.OK_STATUS;
        }
        if (findLocalProject() != null) {
            log.debug("Ignoring shared project creation: project is already imported: " + remoteProjectId);
            return Status.OK_STATUS;
        }
        DDSharedProject remote = uiManager.getProjectManager().getRemoteProjects().stream()
            .filter(project -> project.id().equals(remoteProjectId)).findFirst().orElse(null);
        if (remote == null) {
            log.debug("Ignoring shared project creation: remote project not found: " + remoteProjectId);
            return Status.CANCEL_STATUS;
        }
        if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }
        return new DDProjectImportHandler().importProjects(List.of(remote), monitor);
    }
}
