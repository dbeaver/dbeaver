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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessService;
import org.jkiss.dbeaver.model.datadam.rpc.DBDamSessionService;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.preferences.McpConnectionsPreferencePage;
import org.jkiss.dbeaver.ui.datadam.DBDamService;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.dialogs.WizardDialogModeless;

import java.util.concurrent.atomic.AtomicReference;

public class DDManagementMenu extends CompoundContributionItem {
    @Override
    @NotNull
    protected IContributionItem[] getContributionItems() {
        boolean enabled = DDProductConfig.isIntegrationEnabled();
        boolean signedIn = DBDamAccessService.getInstance().getStatus() == DBDamAccessService.Status.SIGNED_IN;
        Action login = action(signedIn ? DDTrackingUIMessages.sync_preference_page_desktop_sign_out
            : DDTrackingUIMessages.sync_preference_page_desktop_sign_in, IAction.AS_PUSH_BUTTON, DDManagementActions::toggleLogin);
        login.setEnabled(enabled);
        if (!signedIn) {
            return new IContributionItem[] {new ActionContributionItem(login)};
        }
        var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        Action configure = action(DDTrackingUIMessages.configuration, IAction.AS_PUSH_BUTTON, DDManagementActions::configure);
        Action tracking = action(DDTrackingUIMessages.tracking_title, IAction.AS_CHECK_BOX, () -> {
            var view = page.findView(DDTrackingView.ID);
            if (view != null && page.isPartVisible(view)) {
                page.hideView(view);
            } else {
                DDTrackingView.show();
            }
        });
        var view = page.findView(DDTrackingView.ID);
        tracking.setChecked(view != null && page.isPartVisible(view));
        Action manageConnections = action(DDTrackingUIMessages.manage_database_connections, IAction.AS_PUSH_BUTTON,
            () -> {
                try {
                    WizardDialogModeless.showPage(UIUtils.getActiveWorkbenchShell(), McpConnectionsPreferencePage.PAGE_ID);
                } catch (DBException e) {
                    DDManagementActions.showError(e);
                }
            });
        manageConnections.setEnabled(false);
        AtomicReference<DDAccountClient.DamSettingsState> settings = new AtomicReference<>();
        Action mcp = new Action(DDTrackingUIMessages.sync_preference_page_mcp_enabled, IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
                var previous = settings.get();
                if (previous == null || !DDProductConfig.isIntegrationEnabled()
                    || DBDamAccessService.getInstance().getStatus() != DBDamAccessService.Status.SIGNED_IN) {
                    return;
                }
                setEnabled(false);
                manageConnections.setEnabled(false);
                DBDamService.getInstance().createJob("Update DataDam MCP status",
                    account -> account.setDamSetting(DBDamSessionService.getInstance().getClientDamId(),
                        DDRemoteDamSettingKeys.MCP_ENABLED, Boolean.toString(isChecked()), previous.accountId()),
                    updated -> manageConnections.setEnabled(DDProductConfig.isIntegrationEnabled()
                        && updated.isAccountEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, true)
                        && updated.isDamEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, false)), DDManagementActions::showError);
            }
        };
        mcp.setEnabled(false);
        if (enabled) {
            DBDamService.getInstance().createJob("Read DataDam MCP status",
                account -> account.getDamSettings(DBDamSessionService.getInstance().getClientDamId()),
                state -> {
                    settings.set(state);
                    boolean accountEnabled = state.settings().isAccountEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, true);
                    mcp.setChecked(accountEnabled && state.settings().isDamEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, false));
                    mcp.setEnabled(accountEnabled && state.canManage() && DDProductConfig.isIntegrationEnabled());
                    manageConnections.setEnabled(mcp.isChecked() && DDProductConfig.isIntegrationEnabled());
                }, error -> mcp.setEnabled(false));
        }
        return new IContributionItem[] {
            new ActionContributionItem(configure), new ActionContributionItem(tracking),
            new ActionContributionItem(mcp), new ActionContributionItem(manageConnections),
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
