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

import com.dbeaver.datadam.dam.remote.api.DDRemoteDamSettingKeys;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.rpc.DDRemoteDamDesktopSessionService;
import org.jkiss.dbeaver.ui.datadam.DDDesktopDamService;
import org.jkiss.dbeaver.ui.datadam.internal.DDManagementMessages;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;

import java.util.concurrent.atomic.AtomicReference;

public class DDManagementMenu extends CompoundContributionItem {
    @Override
    @NotNull
    protected IContributionItem[] getContributionItems() {
        boolean enabled = DDProductConfig.isIntegrationEnabled();
        boolean signedIn = DDDesktopAccessService.getInstance().getStatus() == DDDesktopAccessService.Status.SIGNED_IN;
        var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        Action configure = action(DDManagementMessages.configuration, IAction.AS_PUSH_BUTTON, DDManagementActions::configure);
        Action tracking = action(DDManagementMessages.tracking_title, IAction.AS_CHECK_BOX, () -> {
            var view = page.findView(DDTrackingView.ID);
            if (view != null && page.isPartVisible(view)) {
                page.hideView(view);
            } else {
                DDTrackingView.show();
            }
        });
        var view = page.findView(DDTrackingView.ID);
        tracking.setChecked(view != null && page.isPartVisible(view));
        Action integration = new Action(DDManagementMessages.integration_enabled, IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
                DDManagementActions.setIntegrationEnabled(isChecked());
            }
        };
        integration.setChecked(enabled);
        Action login = action(signedIn ? DDTrackingUIMessages.sync_preference_page_desktop_sign_out
            : DDTrackingUIMessages.sync_preference_page_desktop_sign_in, IAction.AS_PUSH_BUTTON, DDManagementActions::toggleLogin);
        login.setEnabled(enabled);
        AtomicReference<DDAccountClient.DamSettingsState> settings = new AtomicReference<>();
        Action mcp = new Action(DDTrackingUIMessages.sync_preference_page_mcp_enabled, IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
                var previous = settings.get();
                if (previous == null || !DDProductConfig.isIntegrationEnabled()
                    || DDDesktopAccessService.getInstance().getStatus() != DDDesktopAccessService.Status.SIGNED_IN) {
                    return;
                }
                setEnabled(false);
                DDDesktopDamService.getInstance().createJob("Update DataDam MCP status",
                    account -> account.setDamSetting(DDRemoteDamDesktopSessionService.getInstance().getClientDamId(),
                        DDRemoteDamSettingKeys.MCP_ENABLED, Boolean.toString(isChecked()), previous.accountId()),
                    updated -> {}, DDManagementActions::showError);
            }
        };
        mcp.setEnabled(false);
        if (enabled && signedIn) {
            DDDesktopDamService.getInstance().createJob("Read DataDam MCP status",
                account -> account.getDamSettings(DDRemoteDamDesktopSessionService.getInstance().getClientDamId()),
                state -> {
                    settings.set(state);
                    boolean accountEnabled = state.settings().isAccountEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, true);
                    mcp.setChecked(accountEnabled && state.settings().isDamEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, false));
                    mcp.setEnabled(accountEnabled && state.canManage() && DDProductConfig.isIntegrationEnabled());
                }, error -> mcp.setEnabled(false));
        }
        return new IContributionItem[] {
            new ActionContributionItem(configure), new ActionContributionItem(tracking),
            new ActionContributionItem(integration), new ActionContributionItem(mcp),
            new Separator(), new ActionContributionItem(login)
        };
    }

    @NotNull
    private static Action action(@NotNull String label, int style, @NotNull Runnable runnable) {
        return new Action(label, style) {
            @Override
            public void run() {
                runnable.run();
            }
        };
    }
}
