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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.auth.DDBundleCredentials;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyStore;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectSyncService;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

final class DDProjectSyncUI {
    private DDProjectSyncUI() {
    }

    @Nullable
    static DDProjectSyncService createService() {
        DDKeyBundle bundle = DDKeyStore.load();
        if (bundle == null) {
            showMessage(DDTrackingUIMessages.sync_preference_page_log_in_first, true);
            return null;
        }
        String url = DDSyncPreferencePage.getGatewayUrl();
        if (CommonUtils.isEmpty(url)) {
            showMessage(DDTrackingUIMessages.sync_preference_page_url_not_configured, true);
            return null;
        }
        try {
            return new DDProjectSyncService(
                url,
                new DDBundleCredentials(bundle),
                DBWorkbench.getPlatform().getWorkspace(),
                UUID.fromString(bundle.accountId()));
        } catch (IllegalArgumentException e) {
            DBWorkbench.getPlatformUI().showError(
                DDTrackingUIMessages.project_sync_title,
                DDTrackingUIMessages.project_sync_invalid_account,
                e);
            return null;
        }
    }

    @Nullable
    static DBPProject selectProject(
        @NotNull Shell shell,
        @NotNull DBPProject[] projects,
        @NotNull String message,
        @NotNull String emptyMessage
    ) {
        if (projects.length == 0) {
            showMessage(emptyMessage, true);
            return null;
        }
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(shell, new LabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                return ((DBPProject) element).getName();
            }
        });
        dialog.setTitle(DDTrackingUIMessages.project_sync_title);
        dialog.setMessage(message);
        dialog.setElements(projects);
        if (dialog.open() != Window.OK) {
            return null;
        }
        return (DBPProject) dialog.getFirstResult();
    }

    @NotNull
    static <T> T runInProgress(@NotNull DBProjectSyncSupplier<T> supplier) throws DBException {
        AtomicReference<T> result = new AtomicReference<>();
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    result.set(supplier.get());
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof DBException dbException) {
                throw dbException;
            }
            throw new DBException(String.valueOf(target.getMessage()), target);
        }
        return result.get();
    }

    static void runInProgress(@NotNull DBProjectSyncRunnable runnable) throws DBException {
        runInProgress(() -> {
            runnable.run();
            return Boolean.TRUE;
        });
    }

    static void showMessage(@NotNull String message, boolean warning) {
        DBWorkbench.getPlatformUI().showMessageBox(DDTrackingUIMessages.project_sync_title, message, warning);
    }

    static void showError(@NotNull String message, @NotNull DBException exception) {
        DBWorkbench.getPlatformUI().showError(DDTrackingUIMessages.project_sync_title, message, exception);
    }

    @FunctionalInterface
    interface DBProjectSyncSupplier<T> {
        T get() throws DBException;
    }

    @FunctionalInterface
    interface DBProjectSyncRunnable {
        void run() throws DBException;
    }
}
