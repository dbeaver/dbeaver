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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager.SyncResult;
import org.jkiss.dbeaver.model.datadam.sync.project.ProjectStatus;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectConflictResolver;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectConflictResolver.Choice;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager.ProjectListing;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class DDProjectSyncAllHandler extends AbstractHandler {
    private final DDProjectUIManager uiManager = DDProjectUIManager.getInstance();
    private final DDProjectConflictResolver conflictResolver = new DDProjectConflictResolver(uiManager);

    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) {
        Shell shell = HandlerUtil.getActiveShell(event);
        List<DBPProject> projects = loadBoundProjects();
        if (projects == null || projects.isEmpty()) {
            return null;
        }

        var syncResult = uiManager.synchronizeProjects(projects);
        List<String> errors = new ArrayList<>();
        syncResult.failures().forEach(failure -> errors.add(failure.projectName() + ": " + failure.error().getMessage()));
        if (!syncResult.canceled()) {
            for (SyncResult result : syncResult.results()) {
                if (result.status() != ProjectStatus.CONFLICT) {
                    continue;
                }
                try {
                    Choice choice = conflictResolver.openConflictDialog(shell, result.project());
                    if (choice != null) {
                        var resolution = conflictResolver.applyConflictChoice(result.project(), choice);
                        resolution.failures().forEach(failure ->
                            errors.add(failure.projectName() + ": " + failure.error().getMessage()));
                    }
                } catch (DBException e) {
                    errors.add(result.project().getName() + ": " + e.getMessage());
                }
            }
        }
        if (!errors.isEmpty()) {
            MessageDialog.openError(shell, DDTrackingUIMessages.project_sync_title,
                NLS.bind(DDTrackingUIMessages.projects_errors, String.join("\n", errors)));
        }
        return null;
    }

    @Nullable
    private List<DBPProject> loadBoundProjects() {
        DBRRunnableWithResult<List<DBPProject>> loader = new DBRRunnableWithResult<>() {
            @Override
            public void run(@NotNull DBRProgressMonitor monitor) throws InvocationTargetException {
                monitor.beginTask(DDTrackingUIMessages.projects_sync_all, IProgressMonitor.UNKNOWN);
                try {
                    List<ProjectListing> listings = uiManager.loadProjectListings(monitor);
                    if (listings == null || monitor.isCanceled()) {
                        return;
                    }
                    List<DBPProject> bound = listings.stream()
                        .filter(listing -> listing.localProject() != null && listing.remoteProject() != null)
                        .map(listing -> listing.localProject().project())
                        .toList();
                    if (!monitor.isCanceled()) {
                        result = bound;
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
            return loader.getResult();
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(DDTrackingUIMessages.project_sync_title,
                DDTrackingUIMessages.projects_sync_all_load_failed, e.getTargetException());
            return null;
        }
    }
}
