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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager.LocalProjectInfo;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.DDUIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectKeyUIManager;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager;

import java.util.UUID;

public abstract class AbstractProjectEventJob extends AbstractJob {
    protected final Log log = Log.getLog(getClass());
    protected final DDProjectUIManager uiManager;
    protected final UUID remoteProjectId;
    private final String failureMessage;

    protected AbstractProjectEventJob(
        @NotNull DDProjectUIManager uiManager,
        @NotNull UUID remoteProjectId,
        @NotNull String name,
        @NotNull String failureMessage
    ) {
        super(name);
        this.uiManager = uiManager;
        this.remoteProjectId = remoteProjectId;
        this.failureMessage = failureMessage;
    }

    @NotNull
    @Override
    protected final IStatus run(@NotNull DBRProgressMonitor monitor) {
        try {
            if (!DDProjectKeyUIManager.getInstance().ensureKeysReady(monitor)) {
                return Status.CANCEL_STATUS;
            }
            return perform(monitor);
        } catch (DBException e) {
            showError(e);
            return Status.error(failureMessage, e);
        }
    }

    @NotNull
    protected abstract IStatus perform(@NotNull DBRProgressMonitor monitor) throws DBException;

    @Nullable
    protected LocalProjectInfo findLocalProject() throws DBException {
        return uiManager.getLocalProjects().stream()
            .filter(local -> remoteProjectId.equals(local.remoteProjectId()))
            .findFirst().orElse(null);
    }

    protected void showError(@NotNull DBException error) {
        UIUtils.asyncExec(() -> DDUIUtils.showNotification(DDTrackingUIMessages.project_sync_title,
            failureMessage, DBPMessageType.ERROR,
            () -> DDUIUtils.showError(DDTrackingUIMessages.project_sync_title, failureMessage, error)));
    }
}
