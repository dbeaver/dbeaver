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
import org.eclipse.jface.viewers.TreeViewer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.AIContextSettingsDataSource;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessService;
import org.jkiss.dbeaver.model.datadam.rpc.DBDamSettingsStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ai.preferences.McpBooleanColumn;
import org.jkiss.dbeaver.ui.ai.preferences.McpConnectionsPreferencePage.McpConnectionsPageRegistry.McpConnectionsPageContribution;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.datadam.internal.DDMcpMessages;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

/** DataDam MCP permission column for the shared AI connections page. */
public class DDMcpConnectionsContribution implements McpConnectionsPageContribution {
    private final Map<Runnable, VisibilityListeners> visibilityListeners = new IdentityHashMap<>();

    @Override
    public boolean isVisible() {
        DDRemoteDamSettings settings = DBDamSettingsStore.getInstance().getSettings();
        return DDProductConfig.isIntegrationEnabled()
            && DBDamAccessService.getInstance().getStatus() == DBDamAccessService.Status.SIGNED_IN
            && settings != null && settings.isEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, true, false);
    }

    @Override
    public synchronized void addVisibilityListener(@NotNull Runnable listener) {
        if (visibilityListeners.containsKey(listener)) {
            return;
        }
        DBDamSettingsStore.Listener settingsListener = settings -> listener.run();
        DBDamAccessListener accessListener = new DBDamAccessListener() {
            @Override
            public void accessChanging() {
            }

            @Override
            public void accessChanged() {
                listener.run();
            }
        };
        DBPPreferenceListener preferenceListener = event -> {
            if (DDProductConfig.INTEGRATION_ENABLED.equals(event.getProperty())) {
                listener.run();
            }
        };
        visibilityListeners.put(listener, new VisibilityListeners(settingsListener, accessListener, preferenceListener));
        DBDamSettingsStore.getInstance().addListener(settingsListener);
        DBDamAccessService.getInstance().addAccessListener(accessListener);
        DBWorkbench.getPlatform().getPreferenceStore().addPropertyChangeListener(preferenceListener);
    }

    @Override
    public synchronized void removeVisibilityListener(@NotNull Runnable listener) {
        VisibilityListeners listeners = visibilityListeners.remove(listener);
        if (listeners != null) {
            DBDamSettingsStore.getInstance().removeListener(listeners.settings());
            DBDamAccessService.getInstance().removeAccessListener(listeners.access());
            DBWorkbench.getPlatform().getPreferenceStore().removePropertyChangeListener(listeners.preferences());
        }
    }

    @Override
    @NotNull
    public McpBooleanColumn createColumns(
        @NotNull TreeViewer viewer,
        @NotNull ViewerColumnController<McpBooleanColumn, Object> columnController,
        @NotNull Function<DBPDataSourceContainer, AIContextSettingsDataSource> settings
    ) {
        return McpBooleanColumn.create(
            viewer,
            columnController,
            DDMcpMessages.mcp_connection_settings_group,
            DDMcpMessages.mcp_connection_settings_enabled_tip,
            connection -> settings.apply(connection).isDatadamMcpEnabled(),
            (connection, checked) -> settings.apply(connection).setDatadamMcpEnabled(checked),
            this::isVisible
        );
    }

    private record VisibilityListeners(
        @NotNull DBDamSettingsStore.Listener settings,
        @NotNull DBDamAccessListener access,
        @NotNull DBPPreferenceListener preferences
    ) {
    }
}
