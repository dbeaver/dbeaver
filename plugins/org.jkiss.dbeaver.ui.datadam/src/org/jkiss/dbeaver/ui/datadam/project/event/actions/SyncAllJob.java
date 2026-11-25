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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager.ProjectListing;
import org.jkiss.dbeaver.ui.datadam.project.handler.DDProjectImportHandler;
import org.jkiss.dbeaver.ui.datadam.project.handler.DDProjectSyncAllHandler;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.List;

public class SyncAllJob extends Job {
    private static final Log log = Log.getLog(SyncAllJob.class);
    private final DDProjectUIManager uiManager;
    private final Shell shell;

    public SyncAllJob(@NotNull DDProjectUIManager uiManager, @NotNull Shell shell) {
        super(DDTrackingUIMessages.projects_sync_all);
        this.uiManager = uiManager;
        this.shell = shell;
    }

    @NotNull
    @Override
    protected IStatus run(@NotNull IProgressMonitor monitor) {
        log.debug("DataDam full project sync started");
        IStatus outcome = null;
        DBRProgressMonitor progress = RuntimeUtils.makeMonitor(monitor);
        try {
            List<ProjectListing> listings = uiManager.loadProjectListings(progress);
            if (listings == null || progress.isCanceled()) {
                return outcome = Status.CANCEL_STATUS;
            }
            log.debug("Importing missing DataDam projects");
            List<DDSharedProject> missingLocal = listings.stream()
                .filter(listing -> listing.localProject() == null && listing.remoteProject() != null)
                .map(ProjectListing::remoteProject).toList();
            if (!missingLocal.isEmpty()) {
                IStatus result = new DDProjectImportHandler().importProjects(missingLocal, progress);
                if (!result.isOK()) {
                    return outcome = result;
                }
            }
            if (progress.isCanceled()) {
                return outcome = Status.CANCEL_STATUS;
            }
            log.debug("Synchronizing existing DataDam projects");
            List<DBPProject> existing = listings.stream()
                .filter(listing -> listing.localProject() != null && listing.remoteProject() != null)
                .map(listing -> listing.localProject().project()).toList();
            if (!existing.isEmpty()) {
                IStatus result = new DDProjectSyncAllHandler().synchronizeProjects(existing, progress, shell);
                if (!result.isOK()) {
                    return outcome = result;
                }
            }
            if (progress.isCanceled()) {
                return outcome = Status.CANCEL_STATUS;
            }
            log.debug("Checking deleted DataDam projects");
            List<ProjectListing> current = uiManager.loadProjectListings(progress);
            if (current == null || progress.isCanceled()) {
                return outcome = Status.CANCEL_STATUS;
            }
            for (ProjectListing listing : current) {
                if (progress.isCanceled()) {
                    return outcome = Status.CANCEL_STATUS;
                }
                if (listing.localProject() != null && listing.localProject().remoteProjectId() != null
                    && listing.remoteProject() == null) {
                    IStatus result = new ProjectDeletedAction(uiManager, listing.localProject().remoteProjectId())
                        .runDirectly(progress);
                    if (result.getSeverity() == IStatus.ERROR) {
                        return outcome = result;
                    }
                }
            }
            return outcome = Status.OK_STATUS;
        } catch (DBException e) {
            return outcome = Status.error(DDTrackingUIMessages.projects_sync_all_load_failed, e);
        } finally {
            String result = outcome == null ? "failed"
                : outcome.isOK() ? "completed" : outcome.matches(IStatus.CANCEL) ? "canceled" : "failed";
            log.debug("DataDam full project sync finished: " + result);
        }
    }
}
