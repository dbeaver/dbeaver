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
package org.jkiss.dbeaver.ext.clickhouse.ui.auth;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.clickhouse.model.auth.ClickhouseAuthModelJWTBase;
import org.jkiss.dbeaver.ext.clickhouse.model.auth.ClickhouseJWTProviderRegistry;
import org.jkiss.dbeaver.ext.clickhouse.ui.internal.ClickhouseMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.DatabaseNativeAuthModelConfigurator;

/**
 * Configuration of the ClickHouse Cloud SSO auth model.
 * <p>
 * There is nothing to configure: the endpoints are derived from the host name and the tokens are
 * obtained interactively. The panel only explains what will happen and allows to drop cached tokens.
 */
public class ClickhouseCloudSSOAuthConfigurator extends DatabaseNativeAuthModelConfigurator {

    private DBPDataSourceContainer dataSource;
    private Button signOutButton;

    @Override
    public void createControl(
        @NotNull Composite authPanel,
        @Nullable DBAAuthModel<?> object,
        @NotNull Runnable propertyChangeListener
    ) {
        // Neither user name nor password are applicable, so the base class renders nothing
        super.createControl(authPanel, object, propertyChangeListener);

        UIUtils.createInfoLabel(authPanel, ClickhouseMessages.dialog_connection_auth_cloud_sso_info,
            GridData.FILL_HORIZONTAL, 2);

        signOutButton = UIUtils.createDialogButton(authPanel,
            ClickhouseMessages.dialog_connection_auth_cloud_sso_sign_out,
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (dataSource != null) {
                        // Both halves are needed: the cached provider and the tokens in the secure storage
                        ClickhouseJWTProviderRegistry.reset(dataSource.getId());
                        ClickhouseAuthModelJWTBase.clearStoredTokens(dataSource);
                    }
                }
            });
        signOutButton.setToolTipText(ClickhouseMessages.dialog_connection_auth_cloud_sso_sign_out_tip);
        signOutButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    }

    @Override
    public void loadSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.loadSettings(dataSource);
        this.dataSource = dataSource;
        if (signOutButton != null && !signOutButton.isDisposed()) {
            signOutButton.setEnabled(!dataSource.isTemporary());
        }
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.saveSettings(dataSource);
        // No password is stored, so the connect flow must not prompt for one
        dataSource.setSavePassword(true);
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
