/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

/**
 * PgPass UI config (user name only)
 */
public class PostgreAuthPgPassConfigurator implements IObjectPropertyConfigurator<Object, DBPDataSourceContainer> {

    protected Text usernameText;
    private Text overriddenHostnameText;
    private Button overrideHostname;


    @Override
    public void createControl(@NotNull Composite authPanel, Object object, @NotNull Runnable propertyChangeListener) {
        int fontHeight = UIUtils.getFontHeight(authPanel);

        Label usernameLabel = UIUtils.createLabel(authPanel, UIConnectionMessages.dialog_connection_auth_label_username);
        usernameLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        usernameText = new Text(authPanel, SWT.BORDER);
        overrideHostname = UIUtils.createCheckbox(authPanel, false);
        overrideHostname.setText(UIConnectionMessages.dialog_connection_pgpass_hostname_override);
        overrideHostname.setToolTipText(UIConnectionMessages.dialog_connection_pgpass_hostname_override_tip);
        overriddenHostnameText = new Text(authPanel, SWT.BORDER);
        overrideHostname.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                overriddenHostnameText.setEnabled(overrideHostname.getSelection());
            }
        });

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = fontHeight * 20;
        overriddenHostnameText.setLayoutData(gd);
        usernameText.setLayoutData(gd);
        usernameText.addModifyListener(e -> propertyChangeListener.run());
    }


    @Override
    public void loadSettings(@NotNull DBPDataSourceContainer dataSource) {
        final DBPConnectionConfiguration connectionConfiguration = dataSource.getConnectionConfiguration();
        this.overrideHostname.setSelection(!CommonUtils.isEmpty(connectionConfiguration.getProviderProperty(
            PostgreConstants.PG_PASS_HOSTNAME)));
        overriddenHostnameText.setEnabled(overrideHostname.getSelection());
        if (overrideHostname.getSelection()
            && !CommonUtils.isEmpty(connectionConfiguration.getProviderProperty(PostgreConstants.PG_PASS_HOSTNAME))) {
            this.overriddenHostnameText.setText(
                connectionConfiguration.getProviderProperty(PostgreConstants.PG_PASS_HOSTNAME));

        }
        this.usernameText.setText(CommonUtils.notEmpty(connectionConfiguration.getUserName()));
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        dataSource.getConnectionConfiguration().setUserName(this.usernameText.getText());
        if (overrideHostname.getSelection()) {
            dataSource.getConnectionConfiguration().setProviderProperty(
                PostgreConstants.PG_PASS_HOSTNAME,
                overriddenHostnameText.getText());
        } else {
            dataSource.getConnectionConfiguration().setProviderProperty(PostgreConstants.PG_PASS_HOSTNAME, null);
        }
        dataSource.setSavePassword(true);
    }

    @Override
    public void resetSettings(@NotNull DBPDataSourceContainer dataSource) {
        loadSettings(dataSource);
    }

    @Override
    public boolean isComplete() {
        return true;
    }


}
