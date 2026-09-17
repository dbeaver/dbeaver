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
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectSyncUIManager;

public class DDProjectUpdateHandler extends AbstractHandler {
    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) {
        DDProjectSyncUIManager manager = DDProjectSyncUIManager.getInstance();
        DBPProject project = manager.getSelectedProject(event);
        if (project == null) {
            return null;
        }
        if (!manager.isEnabled()) {
            return null;
        }
        DDProjectSyncService service = manager.getService();
        try {
            DDProjectSyncSnapshot snapshot = UIUtils.runWithDialog(
                monitor -> service.getProjectSyncSnapshot(project));
            switch (snapshot.change()) {
                case UNCHANGED -> manager.showMessage(
                    DDTrackingUIMessages.project_sync_update_unchanged, false);
                case LOCAL -> {
                    UIUtils.runWithDialog(monitor -> {
                        service.pushFiles(project, snapshot.binding(), snapshot.preparedFiles());
                        return null;
                    });
                    manager.showMessage(DDTrackingUIMessages.project_sync_update_success, false);
                }
                case SERVER -> throw new DBException(DDTrackingUIMessages.project_sync_update_server_changed);
                case CONFLICT -> throw new DBException(DDTrackingUIMessages.project_sync_update_conflict);
            }
        } catch (DBException e) {
            manager.showError(DDTrackingUIMessages.project_sync_update_failed, e);
        }
        return null;
    }
}
