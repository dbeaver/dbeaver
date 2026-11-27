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
package org.jkiss.dbeaver.ui.datadam;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessService;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.keys.DDProjectKeyUIManager;

/**
 * Interactive access check performed before a user-requested network operation.
 */
public class DBDamTokenUIManager {
    private static final Log log = Log.getLog(DBDamTokenUIManager.class);
    private static final DBDamTokenUIManager INSTANCE = new DBDamTokenUIManager();

    private final DBDamAccessService accessService = DBDamAccessService.getInstance();

    private DBDamTokenUIManager() {
    }

    @NotNull
    public static DBDamTokenUIManager getInstance() {
        return INSTANCE;
    }

    public boolean ensureTokenReady(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (monitor.isCanceled() || !DDProductConfig.isIntegrationEnabled()) {
            return false;
        }
        DBDamAccessService.Status status = accessService.getStatus();
        if (status == DBDamAccessService.Status.UNLOADED) {
            try {
                accessService.load();
            } catch (DBException e) {
                log.debug("Could not load DataDam desktop access", e);
            }
            status = accessService.getStatus();
        }
        if (status == DBDamAccessService.Status.INVALID_TOKEN) {
            DDKeyBundle keys = accessService.getKeyBundle();
            String message = keys == null ? DDTrackingUIMessages.desktop_relogin_message
                : DDTrackingUIMessages.desktop_relogin_delete_projects_message;
            ReloginChoice choice = promptRelogin(message);
            if (choice == ReloginChoice.SIGN_OUT) {
                DDProjectKeyUIManager.getInstance().signOut(keys);
            }
            if (choice != ReloginChoice.SIGN_IN) {
                return false;
            }

            try {
                accessService.signIn(monitor);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return !monitor.isCanceled() && accessService.getStatus() == DBDamAccessService.Status.SIGNED_IN;
    }

    @Nullable
    private ReloginChoice promptRelogin(@NotNull String message) {
        return UIUtils.syncExec(new RunnableWithResult<>() {
            @Nullable
            @Override
            public ReloginChoice runWithResult() {
                MessageDialog dialog = new MessageDialog(
                    UIUtils.getActiveWorkbenchShell(), DDTrackingUIMessages.desktop_relogin_title,
                    null, message, MessageDialog.QUESTION,
                    new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
                return switch (dialog.open()) {
                    case 0 -> ReloginChoice.SIGN_IN;
                    case 1 -> ReloginChoice.SIGN_OUT;
                    default -> ReloginChoice.CANCEL;
                };
            }
        });
    }

    private enum ReloginChoice {
        SIGN_IN, SIGN_OUT, CANCEL
    }
}
