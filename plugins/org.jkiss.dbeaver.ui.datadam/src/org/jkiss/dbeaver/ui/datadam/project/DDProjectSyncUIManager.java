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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.datadam.auth.DDBundleCredentials;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyStore;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectSyncService;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.DDSyncPreferencePage;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.utils.CommonUtils;

import java.util.UUID;

public final class DDProjectSyncUIManager {
    private static final DDProjectSyncUIManager INSTANCE = new DDProjectSyncUIManager();

    private DDProjectSyncService service;

    private DDProjectSyncUIManager() {
    }

    @NotNull
    public static DDProjectSyncUIManager getInstance() {
        return INSTANCE;
    }

    public boolean isDDEnabled() {
        return getCurrentContext() != null;
    }

    @NotNull
    public DDProjectSyncService getService() {
        synchronized (this) {
            if (service == null) {
                ServiceContext currentContext = getCurrentContext();
                if (currentContext == null) {
                    throw new IllegalStateException("DataDam is not enabled");
                }
                service = new DDProjectSyncService(
                    currentContext.url(),
                    new DDBundleCredentials(currentContext.keyBundle()),
                    currentContext.workspace(),
                    currentContext.accountId()
                );
            }
        }
        return service;
    }

    public void refresh() {
        synchronized (this) {
            service = null;
        }
    }

    @Nullable
    private static ServiceContext getCurrentContext() {
        String url = DDSyncPreferencePage.getGatewayUrl();
        if (CommonUtils.isEmpty(url)) {
            return null;
        }
        DDKeyBundle bundle = DDKeyStore.load();
        UUID accountId = getAccountId(bundle);
        return accountId == null ? null : new ServiceContext(
            url, bundle, DBWorkbench.getPlatform().getWorkspace(), accountId);
    }

    @Nullable
    private static UUID getAccountId(@Nullable DDKeyBundle bundle) {
        if (bundle == null) {
            return null;
        }
        try {
            return UUID.fromString(bundle.accountId());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void showMessage(@NotNull String message, boolean warning) {
        UIUtils.asyncExec(() ->
            DBWorkbench.getPlatformUI().showMessageBox(DDTrackingUIMessages.project_sync_title, message, warning));
    }

    public void showError(@NotNull String message, @NotNull DBException exception) {
        UIUtils.asyncExec(() ->
            DBWorkbench.getPlatformUI().showError(DDTrackingUIMessages.project_sync_title, message, exception));
    }

    private record ServiceContext(
        @NotNull String url,
        @NotNull DDKeyBundle keyBundle,
        @NotNull DBPWorkspace workspace,
        @NotNull UUID accountId
    ) {
    }
}
