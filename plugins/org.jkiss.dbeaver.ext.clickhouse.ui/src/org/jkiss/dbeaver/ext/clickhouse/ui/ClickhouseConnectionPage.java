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
package org.jkiss.dbeaver.ext.clickhouse.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.clickhouse.ui.internal.ClickhouseMessages;
import org.jkiss.dbeaver.ext.generic.views.GenericConnectionPage;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

public class ClickhouseConnectionPage extends GenericConnectionPage {
    private static final Log log = Log.getLog(ClickhouseConnectionPage.class);
    private static final String SSL_PARAM = "ssl"; //$NON-NLS-1$
    private static final String SSL_PATH = "sslcert"; //$NON-NLS-1$
    private static final String SSL_KEY_PASSWORD = "sslkey"; //$NON-NLS-1$
    private static final String SSL_MODE = "sslmode"; //$NON-NLS-1$

    @Nullable
    private Button userSSLCheck;

    @Nullable
    private TextWithOpenFile keyStorePathText;

    @Nullable
    private Text keyStorePasswordText;

    @Nullable
    private Combo sslModeCombo;

    enum SSLModes {
        STRICT,
        NONE
    }

    @Override
    public void createAdvancedSettingsGroup(Composite composite) {

        if (getProperty(SSL_PARAM) != null) {
            Group advancedSettingsGroup = UIUtils.createControlGroup(composite,
                ClickhouseMessages.dialog_connection_page_advanced_settings,
                1,
                GridData.FILL_HORIZONTAL,
                0
            );
            {
                userSSLCheck = UIUtils.createCheckbox(advancedSettingsGroup,
                    ClickhouseMessages.dialog_connection_page_checkbox_use_ssl,
                    ClickhouseMessages.dialog_connection_page_checkbox_tip_use_ssl,
                    false,
                    0
                );
                userSSLCheck.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
            }
            Composite sslComposite = UIUtils.createPlaceholder(advancedSettingsGroup, 1);
            sslComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

            if (getProperty(SSL_PATH) != null) {
                UIUtils.createControlLabel(sslComposite, ClickhouseMessages.dialog_connection_page_text_ssl_file_path);
                keyStorePathText = new TextWithOpenFile(sslComposite,
                    UIConnectionMessages.dialog_setting_ssl_configurator_keystore_title,
                    new String[]{"*.jks;*.pfx", "*.*"}
                );
                keyStorePathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            }
            if (getProperty(SSL_KEY_PASSWORD) != null) {
                UIUtils.createControlLabel(sslComposite, ClickhouseMessages.dialog_connection_page_text_ssl_file_key);
                keyStorePasswordText = new Text(sslComposite, SWT.BORDER | SWT.PASSWORD);
                keyStorePasswordText.setToolTipText(ClickhouseMessages.dialog_connection_page_text_ssl_file_key_tip);
                keyStorePasswordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                keyStorePasswordText.setToolTipText(ClickhouseMessages.dialog_connection_page_text_ssl_file_path_tip);
            }
            if (getProperty(SSL_MODE) != null) {
                sslModeCombo = UIUtils.createLabelCombo(sslComposite,
                    ClickhouseMessages.dialog_connection_page_text_ssl_mode,
                    ClickhouseMessages.dialog_connection_page_text_ssl_mode_tip,
                    SWT.READ_ONLY
                );
                sslModeCombo.add(SSLModes.STRICT.name()); //$NON-NLS-1$
                sslModeCombo.add(SSLModes.NONE.name()); //$NON-NLS-1$
                sslModeCombo.select(0);
            }

            userSSLCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateSSLSection();
                }
            });
        }
    }

    private void updateSSLSection() {
        if (userSSLCheck != null) {
            if (sslModeCombo != null) {
                sslModeCombo.setEnabled(userSSLCheck.getSelection());
            }
            if (keyStorePathText != null) {
                keyStorePathText.setEnabled(userSSLCheck.getSelection());
            }
            if (keyStorePasswordText != null) {
                keyStorePasswordText.setEnabled(userSSLCheck.getSelection());
            }
        }
    }

    protected DBPPropertyDescriptor getProperty(@NotNull String property) {
        DBPPropertyDescriptor[] connectionProperties = new DBPPropertyDescriptor[0];
        try {
            connectionProperties = site.getDriver()
                .getDataSourceProvider()
                .getConnectionProperties(new VoidProgressMonitor(), site.getDriver(), new DBPConnectionConfiguration());
        } catch (DBException e) {
            log.error(e);
        }
        for (DBPPropertyDescriptor it : connectionProperties) {
            if (property.equals(it.getId())) {
                return it;
            }
        }
        return null;
    }

    @Override
    public void loadSettings() {
        super.loadSettings();
        UIUtils.syncExec(() -> {
            if (userSSLCheck != null) {
                String ssl = site.getActiveDataSource().getConnectionConfiguration().getProperty(SSL_PARAM);
                if (!CommonUtils.isEmpty(ssl)) {
                    userSSLCheck.setSelection(ssl.equals("true")); //$NON-NLS-1$
                    updateSSLSection();
                }
                String sslPath = site.getActiveDataSource().getConnectionConfiguration().getProperty(SSL_PATH);
                if (!CommonUtils.isEmpty(sslPath) && keyStorePathText != null) {
                    keyStorePathText.setText(sslPath);
                }
                String sslPassword =
                    site.getActiveDataSource().getConnectionConfiguration().getProperty(SSL_KEY_PASSWORD);
                if (!CommonUtils.isEmpty(sslPassword) && keyStorePasswordText != null) {
                    keyStorePasswordText.setText(sslPassword);
                }
                String sslMode = site.getActiveDataSource().getConnectionConfiguration().getProperty(SSL_MODE);
                if (sslModeCombo != null) {
                    if (!CommonUtils.isEmpty(sslMode)) {
                        sslModeCombo.setText(sslMode); //$NON-NLS-1$
                    } else {
                        sslModeCombo.select(SSLModes.STRICT.ordinal());
                    }
                }
            }
        });
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        super.saveSettings(dataSource);
        if (userSSLCheck != null) {
            dataSource.getConnectionConfiguration()
                .setProperty(SSL_PARAM, userSSLCheck.getSelection() ? "true" : "false"); //$NON-NLS-1$
            if (keyStorePathText != null) {
                if (!CommonUtils.isEmpty(keyStorePathText.getText()) && userSSLCheck.getSelection()) {
                    dataSource.getConnectionConfiguration().setProperty(SSL_PATH, keyStorePathText.getText());
                } else {
                    dataSource.getConnectionConfiguration().removeProperty(SSL_PATH);
                }
            }
            if (keyStorePasswordText != null) {
                if (!CommonUtils.isEmpty(keyStorePasswordText.getText()) && userSSLCheck.getSelection()) {
                    dataSource.getConnectionConfiguration().setProperty(SSL_KEY_PASSWORD, keyStorePasswordText.getText());
                } else {
                    dataSource.getConnectionConfiguration().removeProperty(SSL_KEY_PASSWORD);
                }
            }
            if (sslModeCombo != null) {
                if (!CommonUtils.isEmpty(sslModeCombo.getText()) && userSSLCheck.getSelection()) {
                    dataSource.getConnectionConfiguration().setProperty(SSL_MODE, sslModeCombo.getText().toLowerCase());
                } else {
                    dataSource.getConnectionConfiguration().removeProperty(SSL_MODE);
                }
            }
        }
    }
}