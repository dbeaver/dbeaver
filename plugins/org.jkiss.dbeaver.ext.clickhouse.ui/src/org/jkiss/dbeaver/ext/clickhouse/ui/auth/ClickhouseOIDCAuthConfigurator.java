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
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.clickhouse.model.auth.ClickhouseAuthModelJWTBase;
import org.jkiss.dbeaver.ext.clickhouse.model.auth.ClickhouseAuthModelOIDC;
import org.jkiss.dbeaver.ext.clickhouse.model.auth.ClickhouseJWTProviderRegistry;
import org.jkiss.dbeaver.ext.clickhouse.model.auth.ClickhouseOIDCProvider;
import org.jkiss.dbeaver.ext.clickhouse.model.auth.ClickhouseOIDCSettings;
import org.jkiss.dbeaver.ext.clickhouse.ui.internal.ClickhouseMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.DatabaseNativeAuthModelConfigurator;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.stream.Stream;

/**
 * Configuration of the external OpenID Connect provider auth model.
 */
public class ClickhouseOIDCAuthConfigurator extends DatabaseNativeAuthModelConfigurator {

    private Text emailText;
    private Text issuerText;
    private Text clientIdText;
    private Text clientSecretText;
    private Text audienceText;
    private Text scopesText;
    private Spinner callbackPortSpinner;
    private Button useDeviceCodeCheck;

    @Override
    public void createControl(
        @NotNull Composite authPanel,
        @Nullable DBAAuthModel<?> object,
        @NotNull Runnable propertyChangeListener
    ) {
        super.createControl(authPanel, object, propertyChangeListener);

        emailText = createText(authPanel, ClickhouseMessages.dialog_connection_auth_oidc_email,
            ClickhouseMessages.dialog_connection_auth_oidc_email_tip, SWT.BORDER, propertyChangeListener);
        issuerText = createText(authPanel, ClickhouseMessages.dialog_connection_auth_oidc_issuer,
            ClickhouseMessages.dialog_connection_auth_oidc_issuer_tip, SWT.BORDER, propertyChangeListener);
        clientIdText = createText(authPanel, ClickhouseMessages.dialog_connection_auth_oidc_client_id,
            null, SWT.BORDER, propertyChangeListener);
        clientSecretText = createText(authPanel, ClickhouseMessages.dialog_connection_auth_oidc_client_secret,
            ClickhouseMessages.dialog_connection_auth_oidc_client_secret_tip, SWT.BORDER | SWT.PASSWORD, propertyChangeListener);
        audienceText = createText(authPanel, ClickhouseMessages.dialog_connection_auth_oidc_audience,
            ClickhouseMessages.dialog_connection_auth_oidc_audience_tip, SWT.BORDER, propertyChangeListener);
        scopesText = createText(authPanel, ClickhouseMessages.dialog_connection_auth_oidc_scopes,
            null, SWT.BORDER, propertyChangeListener);

        callbackPortSpinner = UIUtils.createLabelSpinner(authPanel,
            ClickhouseMessages.dialog_connection_auth_oidc_callback_port,
            ClickhouseMessages.dialog_connection_auth_oidc_callback_port_tip,
            ClickhouseOIDCProvider.DEFAULT_CALLBACK_PORT, 1024, 65535);
        callbackPortSpinner.addModifyListener(e -> propertyChangeListener.run());

        useDeviceCodeCheck = UIUtils.createCheckbox(authPanel,
            ClickhouseMessages.dialog_connection_auth_oidc_use_device_code,
            ClickhouseMessages.dialog_connection_auth_oidc_use_device_code_tip,
            false, 2);
        useDeviceCodeCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                callbackPortSpinner.setEnabled(!useDeviceCodeCheck.getSelection());
                propertyChangeListener.run();
            }
        });
    }

    @NotNull
    private static Text createText(
        @NotNull Composite panel,
        @NotNull String label,
        @Nullable String tip,
        int style,
        @NotNull Runnable propertyChangeListener
    ) {
        Text text = UIUtils.createLabelText(panel, label, "", style);
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (tip != null) {
            text.setToolTipText(tip);
        }
        text.addModifyListener(e -> propertyChangeListener.run());
        return text;
    }

    @Override
    public void loadSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.loadSettings(dataSource);
        DBPConnectionConfiguration configuration = dataSource.getConnectionConfiguration();

        setText(emailText, configuration.getAuthProperty(ClickhouseAuthModelOIDC.PROP_EMAIL));
        setText(issuerText, configuration.getAuthProperty(ClickhouseAuthModelOIDC.PROP_ISSUER));
        setText(clientIdText, configuration.getAuthProperty(ClickhouseAuthModelOIDC.PROP_CLIENT_ID));
        setText(clientSecretText, configuration.getAuthProperty(ClickhouseAuthModelOIDC.PROP_CLIENT_SECRET));
        setText(audienceText, configuration.getAuthProperty(ClickhouseAuthModelOIDC.PROP_AUDIENCE));
        String scopes = configuration.getAuthProperty(ClickhouseAuthModelOIDC.PROP_SCOPES);
        setText(scopesText, CommonUtils.isEmpty(scopes) ? ClickhouseOIDCProvider.DEFAULT_SCOPES : scopes);

        if (callbackPortSpinner != null && !callbackPortSpinner.isDisposed()) {
            callbackPortSpinner.setSelection(CommonUtils.toInt(
                configuration.getAuthProperty(ClickhouseAuthModelOIDC.PROP_CALLBACK_PORT),
                ClickhouseOIDCProvider.DEFAULT_CALLBACK_PORT));
        }
        if (useDeviceCodeCheck != null && !useDeviceCodeCheck.isDisposed()) {
            boolean useDeviceCode = CommonUtils.toBoolean(
                configuration.getAuthProperty(ClickhouseAuthModelOIDC.PROP_USE_DEVICE_CODE));
            useDeviceCodeCheck.setSelection(useDeviceCode);
            callbackPortSpinner.setEnabled(!useDeviceCode);
        }
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.saveSettings(dataSource);
        DBPConnectionConfiguration configuration = dataSource.getConnectionConfiguration();

        // The provider is built once from these settings and then cached, so a change in any of
        // them must drop the cached provider, not just a change of the identity or the client
        List<String> settingsBefore = readProviderSettings(configuration);

        setAuthProperty(configuration, ClickhouseAuthModelOIDC.PROP_EMAIL, getText(emailText));
        setAuthProperty(configuration, ClickhouseAuthModelOIDC.PROP_ISSUER, getText(issuerText));
        setAuthProperty(configuration, ClickhouseAuthModelOIDC.PROP_CLIENT_ID, getText(clientIdText));
        setAuthProperty(configuration, ClickhouseAuthModelOIDC.PROP_CLIENT_SECRET, getText(clientSecretText));
        setAuthProperty(configuration, ClickhouseAuthModelOIDC.PROP_AUDIENCE, getText(audienceText));
        setAuthProperty(configuration, ClickhouseAuthModelOIDC.PROP_SCOPES, getText(scopesText));
        if (callbackPortSpinner != null) {
            setAuthProperty(configuration, ClickhouseAuthModelOIDC.PROP_CALLBACK_PORT,
                String.valueOf(callbackPortSpinner.getSelection()));
        }
        if (useDeviceCodeCheck != null) {
            setAuthProperty(configuration, ClickhouseAuthModelOIDC.PROP_USE_DEVICE_CODE,
                String.valueOf(useDeviceCodeCheck.getSelection()));
        }
        // No password is stored, so the connect flow must not prompt for one
        dataSource.setSavePassword(true);

        if (!settingsBefore.equals(readProviderSettings(configuration))) {
            // Tokens obtained with the previous settings are not valid anymore
            ClickhouseJWTProviderRegistry.reset(dataSource.getId());
            ClickhouseAuthModelJWTBase.clearStoredTokens(dataSource);
        }
    }

    /**
     * All settings the provider is constructed from, in a stable order.
     */
    @NotNull
    private static List<String> readProviderSettings(@NotNull DBPConnectionConfiguration configuration) {
        return Stream.of(
                ClickhouseAuthModelOIDC.PROP_EMAIL,
                ClickhouseAuthModelOIDC.PROP_ISSUER,
                ClickhouseAuthModelOIDC.PROP_CLIENT_ID,
                ClickhouseAuthModelOIDC.PROP_CLIENT_SECRET,
                ClickhouseAuthModelOIDC.PROP_AUDIENCE,
                ClickhouseAuthModelOIDC.PROP_SCOPES,
                ClickhouseAuthModelOIDC.PROP_CALLBACK_PORT,
                ClickhouseAuthModelOIDC.PROP_USE_DEVICE_CODE)
            .map(name -> CommonUtils.notEmpty(configuration.getAuthProperty(name)))
            .toList();
    }

    @Override
    public boolean isComplete() {
        // The issuer may be left empty: it is detected from the email domain for Microsoft Entra ID.
        // An email without a domain cannot be resolved, so it must not pass validation either.
        return ClickhouseOIDCSettings.canResolveIssuer(getText(issuerText), getText(emailText))
            && !CommonUtils.isEmpty(getText(clientIdText));
    }

    private static void setAuthProperty(
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull String name,
        @Nullable String value
    ) {
        configuration.setAuthProperty(name, CommonUtils.isEmptyTrimmed(value) ? null : value);
    }

    @Nullable
    private static String getText(@Nullable Text text) {
        return text == null || text.isDisposed() ? null : text.getText().trim();
    }

    private static void setText(@Nullable Text text, @Nullable String value) {
        if (text != null && !text.isDisposed()) {
            text.setText(CommonUtils.notEmpty(value));
        }
    }
}
