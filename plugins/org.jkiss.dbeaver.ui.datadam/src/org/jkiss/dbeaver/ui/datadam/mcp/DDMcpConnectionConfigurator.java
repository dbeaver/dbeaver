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
package org.jkiss.dbeaver.ui.datadam.mcp;

import com.dbeaver.datadam.dam.remote.api.DDRemoteDamSettingKeys;
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSettings;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.mcp.DDMcpConnectionCatalog;
import org.jkiss.dbeaver.model.datadam.rpc.DDRemoteDamSettingsStore;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.preferences.PrefPageDataSourceAI;
import org.jkiss.dbeaver.ui.datadam.internal.DDMcpMessages;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;

public class DDMcpConnectionConfigurator implements IObjectPropertyConfigurator<PrefPageDataSourceAI, PrefPageDataSourceAI> {
    private final DDMcpConnectionCatalog mcpConnectionCatalog = DDMcpConnectionCatalog.getInstance();
    private final DDRemoteDamSettingsStore settingsStore = DDRemoteDamSettingsStore.getInstance();
    private Button enableCheck;
    private boolean available;

    @Override
    public void createControl(
        @NotNull Composite parent,
        @NotNull PrefPageDataSourceAI page,
        @NotNull Runnable propertyChangeListener
    ) {
        DBPDataSourceContainer connection = page.getConnection();
        if (connection == null) {
            return;
        }
        Composite group = UIUtils.createTitledComposite(
            parent, DDMcpMessages.mcp_connection_settings_group, 2, GridData.FILL_HORIZONTAL);
        enableCheck = UIUtils.createCheckbox(
            group,
            DDMcpMessages.mcp_connection_settings_enabled,
            DDMcpMessages.mcp_connection_settings_enabled_tip,
            false,
            1
        );
        UIUtils.createLink(
            group, "<a>" + DDTrackingUIMessages.sync_preference_page_mcp_settings_link + "</a>",
            SelectionListener.widgetSelectedAdapter(e -> PreferencesUtil.createPreferenceDialogOn(
                enableCheck.getShell(), DDMcpConnectionsPreferencePage.PAGE_ID,
                new String[]{DDMcpConnectionsPreferencePage.PAGE_ID}, connection).open())
        );
        Composite section = group.getParent();
        showStatus(parent, section, connection, settingsStore.getSettings());
        Runnable refresh = () -> UIUtils.asyncExec(() -> {
            if (!section.isDisposed()) {
                showStatus(parent, section, connection, settingsStore.getSettings());
            }
        });
        DDRemoteDamSettingsStore.Listener listener = settings -> refresh.run();
        DDDesktopAccessListener accessListener = new DDDesktopAccessListener() {
            @Override
            public void accessChanging() {
            }

            @Override
            public void accessChanged() {
                refresh.run();
            }
        };
        settingsStore.addListener(listener);
        DDDesktopAccessService.getInstance().addAccessListener(accessListener);
        section.addDisposeListener(e -> {
            settingsStore.removeListener(listener);
            DDDesktopAccessService.getInstance().removeAccessListener(accessListener);
        });
    }

    private void showStatus(
        @NotNull Composite parent,
        @NotNull Composite section,
        @NotNull DBPDataSourceContainer connection,
        @Nullable DDRemoteDamSettings settings
    ) {
        boolean visible = isVisible();
        boolean enabled = isAvailable(settings);
        section.setVisible(visible);
        ((GridData) section.getLayoutData()).exclude = !visible;
        if (!enabled) {
            enableCheck.setSelection(false);
        } else if (!available) {
            enableCheck.setSelection(mcpConnectionCatalog.isEnabled(connection));
        }
        enableCheck.setEnabled(enabled);
        available = enabled;
        parent.layout(true, true);
    }

    @Override
    public void loadSettings(@NotNull PrefPageDataSourceAI page) {
        DBPDataSourceContainer connection = page.getConnection();
        if (connection != null && enableCheck != null && available) {
            enableCheck.setSelection(mcpConnectionCatalog.isEnabled(connection));
        }
    }

    @Override
    public void saveSettings(@NotNull PrefPageDataSourceAI page) {
        DBPDataSourceContainer connection = page.getConnection();
        if (connection != null && enableCheck != null && enableCheck.isEnabled()
            && isAvailable(settingsStore.getSettings())) {
            mcpConnectionCatalog.setEnabled(connection, enableCheck.getSelection());
        }
    }

    private boolean isAvailable(@Nullable DDRemoteDamSettings settings) {
        return isVisible() && settings != null
            && settings.isEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, true, false);
    }

    private boolean isVisible() {
        return DDProductConfig.isIntegrationEnabled()
            && DDDesktopAccessService.getInstance().getStatus() == DDDesktopAccessService.Status.SIGNED_IN;
    }

    @Override
    public void resetSettings(@NotNull PrefPageDataSourceAI page) {
        if (enableCheck != null && available) {
            enableCheck.setSelection(false);
        }
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
