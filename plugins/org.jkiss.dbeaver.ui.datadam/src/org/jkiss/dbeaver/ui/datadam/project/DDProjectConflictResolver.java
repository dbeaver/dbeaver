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
package org.jkiss.dbeaver.ui.datadam.project;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.sync.project.ConflictResolution;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager.ProjectRevisions;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.DDDesktopTokenUIManager;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager.ProjectJobBatchResult;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.List;

/** Coordinates revision loading, conflict choice and the corresponding project operation. */
public class DDProjectConflictResolver {
    private final DDProjectUIManager uiManager;

    public DDProjectConflictResolver(@NotNull DDProjectUIManager uiManager) {
        this.uiManager = uiManager;
    }

    @Nullable
    public Choice openConflictDialog(@NotNull Shell shell, @NotNull DBPProject project) throws DBException {
        DBRRunnableWithResult<ConflictDetails> loader = new DBRRunnableWithResult<>() {
            @Override
            public void run(@NotNull DBRProgressMonitor monitor) throws InvocationTargetException {
                monitor.beginTask(DDTrackingUIMessages.project_sync_revisions_job, 1);
                try {
                    if (!DDDesktopTokenUIManager.getInstance().ensureTokenReady(monitor)) {
                        return;
                    }
                    DDProjectManager manager = uiManager.getProjectManager();
                    LocalDateTime startTime = manager.getLocalProject(project).lastSyncedRevision();
                    ProjectRevisions revisions = manager.getProjectRevisions(project, startTime, null);
                    if (!monitor.isCanceled()) {
                        result = new ConflictDetails(project, revisions);
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            UIUtils.runInProgressDialog(loader);
        } catch (InvocationTargetException e) {
            throw new DBException(DDTrackingUIMessages.project_sync_revisions_failed, e.getTargetException());
        }
        ConflictDetails details = loader.getResult();
        if (details == null) {
            return null;
        }
        int buttonId = new DDProjectConflictDialog(shell, details).open();
        Choice choice = switch (buttonId) {
            case DDProjectConflictDialog.USE_LOCAL_ID -> Choice.USE_LOCAL;
            case DDProjectConflictDialog.USE_SERVER_ID -> Choice.USE_SERVER;
            case DDProjectConflictDialog.STOP_SYNC_ID -> Choice.UNBIND;
            default -> null;
        };
        if (choice == Choice.UNBIND && !UIUtils.confirmAction(
            shell, DDTrackingUIMessages.project_sync_unbind_confirm_title,
            NLS.bind(DDTrackingUIMessages.project_sync_unbind_confirm_message, project.getName()))) {
            return null;
        }
        return choice;
    }

    @NotNull
    public ProjectJobBatchResult<?> applyConflictChoice(@NotNull DBPProject project, @NotNull Choice choice) {
        return switch (choice) {
            case USE_LOCAL -> uiManager.resolveConflict(project, ConflictResolution.USE_LOCAL_VERSION);
            case USE_SERVER -> uiManager.resolveConflict(project, ConflictResolution.USE_SERVER_VERSION);
            case UNBIND -> uiManager.unbindProjects(List.of(project));
        };
    }

    public record ConflictDetails(@NotNull DBPProject project, @NotNull ProjectRevisions revisions) {
    }

    public enum Choice {
        USE_LOCAL,
        USE_SERVER,
        UNBIND
    }
}
