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
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

/** Navigator selection, job scheduling and completion shared by DataDam project commands. */
abstract class DDProjectHandler<J extends Job> extends AbstractHandler {
    @NotNull
    protected final DDProjectUIManager uiManager = DDProjectUIManager.getInstance();

    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) {
        if (!uiManager.isDDReady()) {
            return null;
        }
        DBPProject project = NavigatorUtils.getSelectedProject(
            HandlerUtil.getCurrentSelection(event), HandlerUtil.getActivePart(event));
        if (project != null) {
            J job = createJob(event, project);
            if (job != null) {
                scheduleJob(job);
            }
        }
        return null;
    }

    @Nullable
    protected abstract J createJob(@NotNull ExecutionEvent event, @NotNull DBPProject project);

    protected abstract void onSuccess(@NotNull J job);

    protected void showMessage(@NotNull String message) {
        DBWorkbench.getPlatformUI().showMessageBox(DDTrackingUIMessages.project_sync_title, message, false);
    }

    protected void showError(@NotNull String message, @NotNull Throwable error) {
        DBWorkbench.getPlatformUI().showError(DDTrackingUIMessages.project_sync_title, message, error);
    }

    private void scheduleJob(@NotNull J job) {
        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(@NotNull IJobChangeEvent event) {
                UIUtils.asyncExec(() -> {
                    IStatus status = event.getResult();
                    if (status.isOK()) {
                        onSuccess(job);
                    } else if (status.getSeverity() == IStatus.ERROR) {
                        showError(status.getMessage(), new DBException(status.getMessage(), status.getException()));
                    }
                });
            }
        });
        job.schedule();
    }
}
