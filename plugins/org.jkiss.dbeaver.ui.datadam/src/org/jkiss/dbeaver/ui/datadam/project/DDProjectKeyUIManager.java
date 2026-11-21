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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.DDDesktopTokenUIManager;
import org.jkiss.dbeaver.ui.datadam.DDImportKeyDialog;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerProjectSetActive;

import java.util.UUID;

/** Interactive recovery for account encryption resets, before project operations start. */
public final class DDProjectKeyUIManager {
    private static final DDProjectKeyUIManager INSTANCE = new DDProjectKeyUIManager();

    private DDProjectKeyUIManager() {
    }

    @NotNull
    public static DDProjectKeyUIManager getInstance() {
        return INSTANCE;
    }

    public synchronized boolean ensureKeysReady(@NotNull DBRProgressMonitor monitor) throws DBException {
        var access = DDDesktopAccessService.getInstance();
        if (!DDDesktopTokenUIManager.getInstance().ensureTokenReady(monitor)) {
            return false;
        }
        var bundle = access.getKeyBundle();
        if (bundle == null || monitor.isCanceled()) {
            return false;
        }
        if (access.validateProjectKeys()) {
            return !monitor.isCanceled();
        }
        if (!bundle.equals(access.getKeyBundle()) || monitor.isCanceled()) {
            return false;
        }
        Recovery recovery = UIUtils.syncExec(new RunnableWithResult<>() {
            @Override
            @Nullable
            public Recovery runWithResult() {
                int choice = new MessageDialog(UIUtils.getActiveWorkbenchShell(), DDTrackingUIMessages.project_keys_mismatch_title,
                    null, DDTrackingUIMessages.project_keys_mismatch_message, MessageDialog.WARNING,
                    new String[] { DDTrackingUIMessages.project_keys_reset_local, DDTrackingUIMessages.project_keys_enter_new,
                        IDialogConstants.CANCEL_LABEL }, 2).open();
                if (choice == 0) {
                    return new Recovery(null);
                }
                if (choice == 1) {
                    DDImportKeyDialog dialog = new DDImportKeyDialog(UIUtils.getActiveWorkbenchShell());
                    if (dialog.open() == Window.OK) {
                        return new Recovery(dialog.getPhrase());
                    }
                }
                return null;
            }
        });
        if (recovery == null || monitor.isCanceled() || !bundle.equals(access.getKeyBundle())) {
            return false;
        }
        if (recovery.phrase() != null) {
            access.importKeys(recovery.phrase());
        }
        DDProjectManager manager = DDProjectManager.getInstance();
        prepareProjectDeletion(manager, bundle.accountId());
        manager.deleteAccountProjects(bundle.accountId());
        if (recovery.phrase() == null) {
            access.forgetKeys();
        }
        // Callers may already hold project objects that were removed during recovery.
        return false;
    }

    private void prepareProjectDeletion(@NotNull DDProjectManager manager, @NotNull UUID accountId) throws DBException {
        var accountProjects = manager.getAccountProjects(accountId);
        var workspace = DBPPlatformDesktop.getInstance().getWorkspace();
        DBPProject currentProject = workspace.getActiveProject();
        if (currentProject == null || !accountProjects.contains(currentProject)) {
            return;
        }
        DBPProject replacement = null;
        for (var project : workspace.getProjects()) {
            if (manager.getBindingStatus(project) == DDProjectManager.BindingStatus.UNBOUND) {
                replacement = project;
                break;
            }
        }
        if (replacement == null) {
            String name = DBConstants.DEFAULT_PROJECT_NAME;
            int suffix = 1;
            while (workspace.getProject(name) != null) {
                name = DBConstants.DEFAULT_PROJECT_NAME + " " + suffix++;
            }
            replacement = workspace.createProject(name, null);
        }
        DBPProject activeProject = replacement;
        UIUtils.syncExec(() -> NavigatorHandlerProjectSetActive.setActiveProject(activeProject));
    }

    private record Recovery(@Nullable String phrase) {
    }
}
