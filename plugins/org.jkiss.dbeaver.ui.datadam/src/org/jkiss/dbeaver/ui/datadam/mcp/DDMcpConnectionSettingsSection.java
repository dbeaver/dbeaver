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

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.mcp.DDMcpConnectionSettings;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.preferences.AIConnectionSettingsSection;
import org.jkiss.dbeaver.ui.datadam.internal.DDMcpMessages;

public class DDMcpConnectionSettingsSection implements AIConnectionSettingsSection {
    private static final Log log = Log.getLog(DDMcpConnectionSettingsSection.class);

    private DBPDataSourceContainer connection;
    private Button enableCheck;

    @Override
    public boolean appliesTo(@NotNull DBPDataSourceContainer connection) {
        if (!DDProductConfig.isIntegrationEnabled()) {
            return false;
        }
        DDDesktopAccessService access = DDDesktopAccessService.getInstance();
        if (access.getStatus() != DDDesktopAccessService.Status.SIGNED_IN) {
            return false;
        }
        var keys = access.getKeyBundle();
        if (keys == null) {
            return false;
        }
        try {
            return DDProjectManager.getInstance(keys).isBound(connection.getProject());
        } catch (DBException e) {
            log.debug("Cannot check DataDam project binding", e);
            return false;
        }
    }

    @Override
    public void createControls(
        @NotNull Composite parent,
        @NotNull DBPDataSourceContainer connection,
        @Nullable DBRRunnableContext runnableContext
    ) {
        this.connection = connection;
        Composite group = UIUtils.createTitledComposite(
            parent, DDMcpMessages.mcp_connection_settings_group, 1, GridData.FILL_HORIZONTAL);
        enableCheck = UIUtils.createCheckbox(
            group,
            DDMcpMessages.mcp_connection_settings_enabled,
            DDMcpMessages.mcp_connection_settings_enabled_tip,
            DDMcpConnectionSettings.isEnabled(connection),
            1
        );
    }

    @Override
    public void save() {
        DDMcpConnectionSettings.setEnabled(connection, enableCheck.getSelection());
    }
}
