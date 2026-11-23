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
package org.jkiss.dbeaver.ui.datadam.management;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.rpc.DDRemoteDamDesktopSessionService;
import org.jkiss.dbeaver.model.datadam.transport.DDConnectionMonitor;
import org.jkiss.dbeaver.model.impl.GlobalPropertyTester;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager;
import org.jkiss.dbeaver.ui.datadam.project.navigator.DDProjectSyncPropertyTester;

import java.lang.reflect.InvocationTargetException;

public final class DDManagementActions {
    public static final String PREFERENCE_PAGE = "org.jkiss.dbeaver.preferences.synchronization";

    private DDManagementActions() {
    }

    public static void configure() {
        UIUtils.showPreferencesFor(UIUtils.getActiveWorkbenchShell(), null,
            PREFERENCE_PAGE, PREFERENCE_PAGE + ".projects");
    }

    public static void setIntegrationEnabled(boolean enabled) {
        DDProductConfig.setIntegrationEnabled(enabled);
        ActionUtils.evaluatePropertyState(GlobalPropertyTester.NAMESPACE + "." + GlobalPropertyTester.PROP_HAS_PREFERENCE);
        DDProjectSyncPropertyTester.firePropertyChange();
        AbstractJob job = new AbstractJob(DDTrackingUIMessages.integration_enabled) {
            @Override
            @NotNull
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    DDRemoteDamDesktopSessionService.getInstance();
                    boolean active = DDProductConfig.isIntegrationEnabled();
                    if (active) {
                        DDDesktopAccessService.getInstance().load();
                    }
                    return Status.OK_STATUS;
                } catch (DBException e) {
                    showError(e);
                    return Status.CANCEL_STATUS;
                }
            }
        };
        job.setSystem(true);
        job.schedule();
    }

    public static void toggleLogin() {
        boolean signedIn = DDDesktopAccessService.getInstance().getStatus() == DDDesktopAccessService.Status.SIGNED_IN;
        String failure = signedIn ? DDTrackingUIMessages.sync_preference_page_logout_failed
            : DDTrackingUIMessages.sync_preference_page_login_failed;
        try {
            UIUtils.runInProgressDialog(monitor -> {
                if (!DDProductConfig.isIntegrationEnabled()) {
                    return;
                }
                try {
                    var access = DDDesktopAccessService.getInstance();
                    if (signedIn) {
                        access.signOut();
                    } else {
                        access.signIn(monitor);
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(DDTrackingUIMessages.sync_preference_page_title, failure, e.getTargetException());
        }
        if (signedIn && DDDesktopAccessService.getInstance().getStatus() == DDDesktopAccessService.Status.SIGNED_OUT) {
            DDProjectUIManager.getInstance().refreshProjectVisibility();
        }
    }

    @Nullable
    public static DDConnectionMonitor getMonitor() {
        try {
            return DDRemoteDamDesktopSessionService.getInstance().getMonitor();
        } catch (DBException e) {
            return null;
        }
    }

    public static void showError(@NotNull Throwable error) {
        UIUtils.asyncExec(() -> DBWorkbench.getPlatformUI().showError(
            DDTrackingUIMessages.sync_preference_page_title, error.getMessage(), error));
    }
}
