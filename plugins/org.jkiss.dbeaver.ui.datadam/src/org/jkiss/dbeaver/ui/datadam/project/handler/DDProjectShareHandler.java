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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectSyncService;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectSyncSnapshot;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectSyncUI;

public class DDProjectShareHandler extends AbstractHandler {
    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) {
        DBPProject project = DDProjectSyncUI.getSelectedProject(event);
        if (project == null) {
            return null;
        }
        DDProjectSyncService service = DDProjectSyncUI.createService();
        if (service == null) {
            return null;
        }
        try {
            DDProjectSyncUI.runInProgress(() -> {
                service.shareProject(project, null);
                DDProjectSyncSnapshot snapshot = service.getProjectSyncSnapshot(project);
                service.pushFiles(project, snapshot.binding(), snapshot.preparedFiles());
            });
            DDProjectSyncUI.showMessage(DDTrackingUIMessages.project_sync_share_success, false);
        } catch (DBException e) {
            DDProjectSyncUI.showError(DDTrackingUIMessages.project_sync_share_failed, e);
        }
        return null;
    }
}
