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
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.AIContextSettingsDataSource;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.rpc.DDRemoteDamSettingsStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.preferences.McpConnectionsPreferencePage.McpConnectionsPageRegistry.McpConnectionsPageContribution;
import org.jkiss.dbeaver.ui.controls.CellControlColumn;
import org.jkiss.dbeaver.ui.datadam.internal.DDMcpMessages;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

/** DataDam MCP permission column for the shared AI connections page. */
public class DDMcpConnectionsContribution implements McpConnectionsPageContribution {
    private final Map<Runnable, VisibilityListeners> visibilityListeners = new IdentityHashMap<>();

    @Override
    public boolean isVisible() {
        DDRemoteDamSettings settings = DDRemoteDamSettingsStore.getInstance().getSettings();
        return DDProductConfig.isIntegrationEnabled()
            && DDDesktopAccessService.getInstance().getStatus() == DDDesktopAccessService.Status.SIGNED_IN
            && settings != null && settings.isEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, true, false);
    }

    @Override
    public synchronized void addVisibilityListener(@NotNull Runnable listener) {
        if (visibilityListeners.containsKey(listener)) {
            return;
        }
        DDRemoteDamSettingsStore.Listener settingsListener = settings -> listener.run();
        DDDesktopAccessListener accessListener = new DDDesktopAccessListener() {
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
        DDRemoteDamSettingsStore.getInstance().addListener(settingsListener);
        DDDesktopAccessService.getInstance().addAccessListener(accessListener);
        DBWorkbench.getPlatform().getPreferenceStore().addPropertyChangeListener(preferenceListener);
    }

    @Override
    public synchronized void removeVisibilityListener(@NotNull Runnable listener) {
        VisibilityListeners listeners = visibilityListeners.remove(listener);
        if (listeners != null) {
            DDRemoteDamSettingsStore.getInstance().removeListener(listeners.settings());
            DDDesktopAccessService.getInstance().removeAccessListener(listeners.access());
            DBWorkbench.getPlatform().getPreferenceStore().removePropertyChangeListener(listeners.preferences());
        }
    }

    @Override
    public void createColumns(
        @NotNull TreeViewer viewer,
        @NotNull Function<DBPDataSourceContainer, AIContextSettingsDataSource> settings
    ) {
        TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.CENTER);
        column.getColumn().setText(DDMcpMessages.mcp_connection_settings_group);
        column.getColumn().setWidth(100);
        UIUtils.attachCellControlColumn(
            viewer,
            column,
            DBPDataSourceContainer.class,
            (parent, connection) -> {
                Button checkbox = UIUtils.createCheckbox(parent, false);
                checkbox.setToolTipText(DDMcpMessages.mcp_connection_settings_enabled_tip);
                checkbox.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
                    if (!isVisible() || column.getColumn().isDisposed() || column.getColumn().getWidth() == 0) {
                        checkbox.setSelection(settings.apply(connection).isDatadamMcpEnabled());
                        return;
                    }
                    settings.apply(connection).setDatadamMcpEnabled(checkbox.getSelection());
                }));
                return checkbox;
            },
            (checkbox, connection) -> {
                checkbox.setSelection(settings.apply(connection).isDatadamMcpEnabled());
                CellControlColumn.updateSelectionColors(checkbox, viewer, connection);
            },
            this::isVisible
        );
    }

    private record VisibilityListeners(
        @NotNull DDRemoteDamSettingsStore.Listener settings,
        @NotNull DDDesktopAccessListener access,
        @NotNull DBPPreferenceListener preferences
    ) {
    }
}
