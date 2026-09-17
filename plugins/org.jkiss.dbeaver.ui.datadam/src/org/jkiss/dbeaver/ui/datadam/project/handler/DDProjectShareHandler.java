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
package org.jkiss.dbeaver.ui.datadam.project.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectSyncService;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectSyncSnapshot;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectSyncPropertyTester;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectSyncUIManager;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

public class DDProjectShareHandler extends AbstractHandler {
    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) {
        DBPProject project = NavigatorUtils.getSelectedProject(
            HandlerUtil.getCurrentSelection(event), HandlerUtil.getActivePart(event));
        if (project == null) {
            return null;
        }
        DDProjectSyncUIManager ddManager = DDProjectSyncUIManager.getInstance();
        if (!ddManager.isDDEnabled()) {
            return null;
        }
        DDProjectSyncService service = ddManager.getService();
        new AbstractJob(DDTrackingUIMessages.project_sync_share_job) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    service.shareProject(project, null);
                    DDProjectSyncSnapshot snapshot = service.getProjectSyncSnapshot(project);
                    service.pushFiles(project, snapshot.binding(), snapshot.preparedFiles());
                    DDProjectSyncPropertyTester.firePropertyChange();
                    ddManager.showMessage(DDTrackingUIMessages.project_sync_share_success, false);
                } catch (DBException e) {
                    ddManager.showError(DDTrackingUIMessages.project_sync_share_failed, e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
        return null;
    }
}
