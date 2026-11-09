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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.jkiss.dbeaver.model.datadam.mcp.DDMcpConnectionSettings;
import org.jkiss.dbeaver.model.datadam.rpc.DDRemoteDamDesktopSessionService;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.preferences.PrefPageDataSourceAI;
import org.jkiss.dbeaver.ui.datadam.DDServerCatalog;
import org.jkiss.dbeaver.ui.datadam.internal.DDMcpMessages;

public class DDMcpConnectionConfigurator implements IObjectPropertyConfigurator<PrefPageDataSourceAI, PrefPageDataSourceAI> {
    private static final Log log = Log.getLog(DDMcpConnectionConfigurator.class);
    private Button enableCheck;
    private boolean resetPending;
    @Nullable
    private AbstractJob statusJob;

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
        Label loading = new Label(parent, SWT.NONE);
        loading.setText(DDMcpMessages.mcp_connection_status_loading);
        loading.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        parent.addDisposeListener(e -> {
            if (statusJob != null) {
                statusJob.cancel();
            }
        });
        try {
            statusJob = DDServerCatalog.getInstance().createJob(
                "Check DataDam MCP availability",
                account -> account.getDamSettings(
                    DDRemoteDamDesktopSessionService.getInstance().getClientDamId(), DDRemoteDamSettingKeys.MCP_ENABLED),
                status -> showStatus(parent, loading, page, status),
                error -> {
                    log.debug("Cannot read DataDam MCP status", error);
                    showStatus(parent, loading, page, null);
                });
        } catch (DBException e) {
            log.debug("Cannot read DataDam MCP status", e);
            showStatus(parent, loading, page, null);
        }
    }

    private void showStatus(
        @NotNull Composite parent,
        @NotNull Label loading,
        @NotNull PrefPageDataSourceAI page,
        @Nullable DDAccountClient.DamSettingsState status
    ) {
        if (parent.isDisposed()) {
            return;
        }
        loading.dispose();
        if (status != null && status.settings().isEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, true, false)) {
            Composite group = UIUtils.createTitledComposite(
                parent, DDMcpMessages.mcp_connection_settings_group, 1, GridData.FILL_HORIZONTAL);
            enableCheck = UIUtils.createCheckbox(
                group,
                DDMcpMessages.mcp_connection_settings_enabled,
                DDMcpMessages.mcp_connection_settings_enabled_tip,
                false,
                1
            );
            if (!resetPending) {
                loadSettings(page);
            }
        }
        parent.layout(true, true);
    }

    @Override
    public void loadSettings(@NotNull PrefPageDataSourceAI page) {
        resetPending = false;
        DBPDataSourceContainer connection = page.getConnection();
        if (connection != null && enableCheck != null) {
            enableCheck.setSelection(DDMcpConnectionSettings.isEnabled(connection));
        }
    }

    @Override
    public void saveSettings(@NotNull PrefPageDataSourceAI page) {
        DBPDataSourceContainer connection = page.getConnection();
        if (connection != null && (enableCheck != null || resetPending)) {
            DDMcpConnectionSettings.setEnabled(connection, enableCheck != null && enableCheck.getSelection());
            resetPending = false;
        }
    }

    @Override
    public void resetSettings(@NotNull PrefPageDataSourceAI page) {
        resetPending = true;
        if (enableCheck != null) {
            enableCheck.setSelection(false);
        }
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
