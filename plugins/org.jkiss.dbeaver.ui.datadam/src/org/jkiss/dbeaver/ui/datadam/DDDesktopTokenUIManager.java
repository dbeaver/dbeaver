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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;

/**
 * Interactive access check performed before a user-requested network operation.
 */
public class DDDesktopTokenUIManager {
    private static final DDDesktopTokenUIManager INSTANCE = new DDDesktopTokenUIManager();

    private final DDDesktopAccessService accessService = DDDesktopAccessService.getInstance();

    private DDDesktopTokenUIManager() {
    }

    @NotNull
    public static DDDesktopTokenUIManager getInstance() {
        return INSTANCE;
    }

    public boolean ensureTokenReady(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (monitor.isCanceled() || !DDProductConfig.isIntegrationEnabled()) {
            return false;
        }
        DDDesktopAccessService.Status status = accessService.getStatus();
        if (status == DDDesktopAccessService.Status.UNLOADED) {
            accessService.load();
            status = accessService.getStatus();
        }
        if (status == DDDesktopAccessService.Status.EXPIRED) {
            Boolean accepted = UIUtils.syncExec(new RunnableWithResult<>() {
                @Override
                public Boolean runWithResult() {
                    return UIUtils.confirmAction(
                        UIUtils.getActiveWorkbenchShell(),
                        DDTrackingUIMessages.desktop_relogin_title,
                        DDTrackingUIMessages.desktop_relogin_message
                    );
                }
            });
            if (!Boolean.TRUE.equals(accepted)) {
                return false;
            }

            try {
                accessService.signIn(monitor);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return !monitor.isCanceled() && accessService.getStatus() == DDDesktopAccessService.Status.SIGNED_IN;
    }
}
