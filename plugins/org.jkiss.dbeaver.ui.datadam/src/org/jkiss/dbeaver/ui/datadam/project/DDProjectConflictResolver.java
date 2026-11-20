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
import org.jkiss.dbeaver.model.datadam.sync.project.ProjectStatus;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.DDDesktopTokenUIManager;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;

import java.time.LocalDateTime;

/** Coordinates revision loading, conflict choice and the corresponding project operation. */
public class DDProjectConflictResolver {
    private final DDProjectUIManager uiManager;

    public DDProjectConflictResolver(@NotNull DDProjectUIManager uiManager) {
        this.uiManager = uiManager;
    }

    @Nullable
    public ProjectStatus resolve(
        @NotNull DBPProject project, @NotNull DBRProgressMonitor monitor, @NotNull Shell shell
    ) throws DBException {
        ConflictDetails details = loadConflictDetails(project, monitor);
        if (details == null || monitor.isCanceled()) {
            return null;
        }
        Choice choice = UIUtils.syncExec(new RunnableWithResult<>() {
            @Nullable
            @Override
            public Choice runWithResult() {
                return openConflictDialog(shell, details);
            }
        });
        return choice == null || monitor.isCanceled() ? null : applyConflictChoice(project, choice, monitor);
    }

    @Nullable
    public ConflictDetails loadConflictDetails(@NotNull DBPProject project, @NotNull DBRProgressMonitor monitor) throws DBException {
        monitor.beginTask(DDTrackingUIMessages.project_sync_revisions_job, 1);
        try {
            if (!DDDesktopTokenUIManager.getInstance().ensureTokenReady(monitor)) {
                return null;
            }
            DDProjectManager manager = uiManager.getProjectManager();
            LocalDateTime startTime = manager.getLocalProject(project).lastSyncedRevision();
            ProjectRevisions revisions = manager.getProjectRevisions(project, startTime, null);
            return monitor.isCanceled() ? null : new ConflictDetails(project, revisions);
        } finally {
            monitor.done();
        }
    }

    @Nullable
    public Choice openConflictDialog(@NotNull Shell shell, @NotNull ConflictDetails details) {
        DBPProject project = details.project();
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

    @Nullable
    public ProjectStatus applyConflictChoice(
        @NotNull DBPProject project, @NotNull Choice choice, @NotNull DBRProgressMonitor monitor
    ) throws DBException {
        monitor.beginTask(DDTrackingUIMessages.project_sync_resolve_job, 1);
        try {
            if (monitor.isCanceled()) {
                return null;
            }
            DDProjectManager manager = uiManager.getProjectManager();
            if (choice == Choice.UNBIND) {
                manager.unbindProject(project);
                return monitor.isCanceled() ? null : ProjectStatus.NOT_SYNC;
            }
            if (!DDDesktopTokenUIManager.getInstance().ensureTokenReady(monitor)) {
                return null;
            }
            ConflictResolution resolution = choice == Choice.USE_LOCAL
                ? ConflictResolution.USE_LOCAL_VERSION : ConflictResolution.USE_SERVER_VERSION;
            ProjectStatus status = manager.resolveConflict(project, resolution);
            return monitor.isCanceled() ? null : status;
        } finally {
            monitor.done();
        }
    }

    public record ConflictDetails(@NotNull DBPProject project, @NotNull ProjectRevisions revisions) {
    }

    public enum Choice {
        USE_LOCAL,
        USE_SERVER,
        UNBIND
    }
}
