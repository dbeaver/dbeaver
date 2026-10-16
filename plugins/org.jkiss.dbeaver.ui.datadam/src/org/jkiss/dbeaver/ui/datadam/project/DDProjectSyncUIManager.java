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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.auth.DDBundleCredentials;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectSyncService;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;

public final class DDProjectSyncUIManager {
    private static final DDProjectSyncUIManager INSTANCE = new DDProjectSyncUIManager();

    private final DDDesktopAccessService accessService = DDDesktopAccessService.getInstance();
    private DDProjectSyncService service;

    private DDProjectSyncUIManager() {
        accessService.addKeyBundleListener(this::refresh);
    }

    @NotNull
    public static DDProjectSyncUIManager getInstance() {
        return INSTANCE;
    }

    public boolean isDDEnabled() {
        return accessService.getKeyBundle() != null;
    }

    @NotNull
    public DDProjectSyncService getService() throws DBException {
        synchronized (this) {
            if (service == null) {
                DDKeyBundle bundle = accessService.getKeyBundle();
                if (bundle == null) {
                    throw new DBException("Import encryption keys for the current DataDam account in Preferences > DataDam");
                }
                service = new DDProjectSyncService(
                    DDEndpoints.getStorageBaseUrl(),
                    new DDBundleCredentials(bundle),
                    DBWorkbench.getPlatform().getWorkspace(),
                    bundle.accountId()
                );
            }
            return service;
        }
    }

    private void refresh() {
        synchronized (this) {
            service = null;
        }
        DDProjectSyncPropertyTester.firePropertyChange();
    }

    public void showMessage(@NotNull String message, boolean warning) {
        UIUtils.asyncExec(() ->
            DBWorkbench.getPlatformUI().showMessageBox(DDTrackingUIMessages.project_sync_title, message, warning));
    }

    public void showError(@NotNull String message, @NotNull DBException exception) {
        UIUtils.asyncExec(() ->
            DBWorkbench.getPlatformUI().showError(DDTrackingUIMessages.project_sync_title, message, exception));
    }
}
