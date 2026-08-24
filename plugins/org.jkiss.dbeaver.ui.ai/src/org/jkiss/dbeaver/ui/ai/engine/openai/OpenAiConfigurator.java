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
package org.jkiss.dbeaver.ui.ai.engine.openai;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.engine.openai.AIAccountAuthenticator;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIAccountAuthenticator;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIClientResponses;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIEngine;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIModels;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceAuth;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.ai.model.CachedValue;
import org.jkiss.dbeaver.ui.ai.model.ContextWindowSizeField;
import org.jkiss.dbeaver.ui.ai.model.ModelSelectorField;
import org.jkiss.dbeaver.ui.ai.preferences.AbstractAIEngineConfigurator;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class OpenAiConfigurator<ENGINE extends AIEngineDescriptor, PROPERTIES extends OpenAIProperties>
    extends AbstractAIEngineConfigurator<ENGINE, PROPERTIES> {

    private static final String API_KEY_URL = "https://platform.openai.com/account/api-keys";
    protected String baseUrl;
    protected volatile String token = "";
    private String temperature = "0.0";

    @Nullable
    protected Text baseUrlText;

    protected Text tokenText;
    protected Text temperatureText;
    protected ModelSelectorField modelSelectorField;
    protected ContextWindowSizeField contextWindowSizeField;

    protected final CachedValue<List<AIModel>> modelsCache = new CachedValue<>(this::fetchOpenAiModels);
    private AIAccountAuthenticator accountAuthenticator;
    protected PROPERTIES properties;
    private volatile List<AIModel> availableModels = List.of();
    @Nullable
    private Button apiTokenAuthenticationButton;
    @Nullable
    private Button accountAuthenticationButton;
    private Label tokenLabel;
    private Label accountLabel;
    private Text accountText;
    private Button accountActionButton;
    private Link tokenInfoLink;
    private Runnable propertyChangeListener;
    private String apiToken;
    private volatile boolean accountAuthentication;
    private volatile long settingsGeneration;

    @Override
    public void createControl(
        @NotNull Composite parent,
        @NotNull AIEngineDescriptor object,
        @NotNull Runnable propertyChangeListener
    ) {
        this.propertyChangeListener = propertyChangeListener;
        Composite composite = UIUtils.createComposite(parent, 3);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createConnectionParameters(composite);

        createModelParameters(composite);
        createBaseUrlParameter(composite);

        createAdditionalSettings(composite);
    }

    @Override
    public void loadSettings(@NotNull PROPERTIES configuration) {
        settingsGeneration++;
        properties = configuration;
        accountAuthenticator = createAccountAuthenticator(configuration.getTimeout());
        baseUrl = CommonUtils.toString(configuration.getBaseUrl());
        if (baseUrl.isEmpty()) {
            baseUrl = OpenAIClientResponses.OPENAI_ENDPOINT;
        }
        token = CommonUtils.toString(configuration.getToken());
        apiToken = token;
        modelSelectorField.setSelectedModel(CommonUtils.toString(configuration.getModel()));
        temperature = CommonUtils.toString(configuration.getTemperature(), "0.0");

        applySettings();
        loadAdvancedSettings(configuration);

        contextWindowSizeField.setValue(configuration.getContextWindowSize());

        boolean useAccountAuthentication = isAccountAuthenticationSupported()
            && configuration.isAccountAuthentication();
        if (apiTokenAuthenticationButton != null) {
            apiTokenAuthenticationButton.setSelection(!useAccountAuthentication);
        }
        if (accountAuthenticationButton != null) {
            accountAuthenticationButton.setSelection(useAccountAuthentication);
        }
        updateAuthenticationControls();
        if (!isAccountAuthentication() || configuration.isAccountConnected()) {
            modelSelectorField.refreshModelListSilently(true);
        }
    }

    @Override
    public void saveSettings(@NotNull PROPERTIES configuration) {
        configuration.setBaseUrl(baseUrl);
        configuration.setToken(token);
        configuration.setAuthentication(isAccountAuthentication()
            ? getAccountAuthenticationId()
            : OpenAIProperties.AUTHENTICATION_API_TOKEN);
        if (configuration.isAccountAuthentication()) {
            configuration.setToken(apiToken);
            configuration.copyAccountTokensFrom(properties);
        }
        configuration.setModel(modelSelectorField.getSelectedModelName());
        configuration.setContextWindowSize(contextWindowSizeField.getValue());
        configuration.setTemperature(CommonUtils.toDouble(temperature));
        saveAdvancedSettings(configuration);
    }

    @Override
    public void resetSettings(@NotNull PROPERTIES openAIPropertiesLegacyAISettings) {

    }

    protected void createAdditionalSettings(@NotNull Composite parent) {
        createAdvancedSettings(parent);
    }

    protected void createModelParameters(@NotNull Composite parent) {
        modelSelectorField = ModelSelectorField.builder()
            .withParent(parent)
            .withGridData(new GridData(GridData.FILL_HORIZONTAL))
            .withRequiredSetting(tokenText, AIUIMessages.model_selector_token_required)
            .withModelListSupplier(
                (monitor, forceRefresh) -> modelsCache.get(monitor, forceRefresh).stream()
                    .filter(it -> it.features().contains(AIModelFeature.CHAT))
                    .toList()
            )
            .withModifyListener(() -> {
                OpenAIModels.getModelByName(modelSelectorField.getSelectedModelName())
                    .ifPresentOrElse(
                        model -> {
                            contextWindowSizeField.setValue(model.contextWindowSize());
                            temperatureText.setText(String.valueOf(model.defaultTemperature()));
                            temperatureText.setEnabled(OpenAIModels.isTemperatureEditable(model));
                        }, () -> {
                            contextWindowSizeField.setValue(null);
                            temperatureText.setText("0.0");
                            temperatureText.setEnabled(true);
                        }
                    );

                AIModel selectedModel = modelSelectorField.getSelectedModel();
                    if (selectedModel != null && selectedModel.contextWindowSize() != null) {
                        contextWindowSizeField.setValue(selectedModel.contextWindowSize());
                    }
                })
                .build();

        contextWindowSizeField = ContextWindowSizeField.builder()
            .withParent(parent)
            .withGridData(GridDataFactory.fillDefaults().span(2, 1).create())
            .build();

        temperatureText = UIUtils.createLabelText(parent, AIUIMessages.gpt_preference_page_text_temperature, "0.0");
        temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        temperatureText.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());
        temperatureText.setToolTipText(AIUIMessages.openai_configurator_temperature_tip);
        temperatureText.addModifyListener((e) -> temperature = temperatureText.getText());
    }

    @NotNull
    protected List<AIModel> fetchOpenAiModels(@NotNull DBRProgressMonitor monitor) throws DBException {
        PROPERTIES currentProperties = properties;
        AIAccountAuthenticator currentAuthenticator = accountAuthenticator;
        boolean useAccountAuthentication = isAccountAuthentication();
        String currentToken = useAccountAuthentication ? apiToken : token;
        String currentBaseUrl = baseUrl;
        if (currentProperties != null && useAccountAuthentication) {
            return fetchAccountModels(currentProperties, currentAuthenticator).stream()
                .map(model -> new AIModel(
                    model,
                    OpenAIProperties.DEFAULT_ACCOUNT_CONTEXT_WINDOW_SIZE,
                    OpenAIModels.detectModelFeatures(model)
                ))
                .toList();
        }
        OpenAIProperties properties = new OpenAIProperties();
        properties.setToken(currentToken);
        properties.setBaseUrl(currentBaseUrl);
        if (!CommonUtils.isEmpty(currentToken)) {
            try (OpenAIEngine<OpenAIProperties> engine = new OpenAIEngine<>(properties)) {
                return engine.getModels(monitor);
            }
        }
        return Collections.emptyList();
    }

    @NotNull
    protected List<String> fetchAccountModels(
        @NotNull PROPERTIES properties,
        @NotNull AIAccountAuthenticator authenticator
    ) throws DBException {
        return ((OpenAIAccountAuthenticator) authenticator).listModels(properties);
    }

    @NotNull
    protected List<AIModel> loadModels(
        @NotNull DBRProgressMonitor monitor,
        boolean forceRefresh
    ) throws DBException {
        long generation = settingsGeneration;
        List<AIModel> models = modelsCache.get(monitor, forceRefresh);
        return generation == settingsGeneration ? models : List.of();
    }

    protected void createConnectionParameters(@NotNull Composite parent) {
        boolean accountAuthenticationSupported = isAccountAuthenticationSupported();
        if (accountAuthenticationSupported) {
            UIUtils.createControlLabel(parent, AIUIMessages.openai_configurator_login_method_label);
            Composite authenticationComposite = UIUtils.createComposite(parent, 2);
            GridData authenticationLayout = new GridData(GridData.FILL_HORIZONTAL);
            authenticationLayout.horizontalSpan = 2;
            authenticationComposite.setLayoutData(authenticationLayout);
            Button apiTokenButton = new Button(authenticationComposite, SWT.RADIO);
            apiTokenButton.setText(AIUIMessages.openai_configurator_authentication_api_token);
            apiTokenButton.setSelection(true);
            apiTokenButton.addListener(SWT.Selection, event -> {
                if (apiTokenButton.getSelection()) {
                    authenticationChanged();
                }
            });
            apiTokenAuthenticationButton = apiTokenButton;
            Button accountButton = new Button(authenticationComposite, SWT.RADIO);
            accountButton.setText(NLS.bind(
                AIUIMessages.openai_configurator_authentication_chatgpt_account,
                getAccountProviderName()
            ));
            accountButton.addListener(SWT.Selection, event -> {
                if (accountButton.getSelection()) {
                    authenticationChanged();
                }
            });
            accountAuthenticationButton = accountButton;
        }

        tokenLabel = UIUtils.createControlLabel(parent, AIUIMessages.gpt_preference_page_selector_token);
        tokenText = new Text(parent, SWT.BORDER | SWT.PASSWORD);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        gd.widthHint = 150;
        tokenText.setLayoutData(gd);
        tokenText.addModifyListener(e -> {
            token = tokenText.getText();
            settingsGeneration++;
        });
        tokenText.setMessage(AIUIMessages.openai_configurator_token_placeholder);
        createURLInfoLink(parent);
        accountLabel = UIUtils.createControlLabel(parent, AIUIMessages.openai_configurator_account_label);
        accountText = new Text(parent, SWT.READ_ONLY);
        UIUtils.fixReadonlyTextBackground(accountText);
        accountText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        accountActionButton = UIUtils.createPushButton(parent, AIUIMessages.openai_configurator_sign_in, null);
        accountActionButton.addListener(SWT.Selection, event -> {
            if (properties != null && properties.isAccountConnected()) {
                signOut();
            } else {
                signIn();
            }
        });
    }

    protected void createBaseUrlParameter(@NotNull Composite parent) {
        Label baseUrlLabel = UIUtils.createControlLabel(parent, AIUIMessages.gpt_preference_page_selector_base_url);
        baseUrlText = new Text(parent, SWT.BORDER);
        baseUrlText.setData("label", baseUrlLabel);
        baseUrlText.addModifyListener(e -> {
            baseUrl = baseUrlText.getText();
            settingsGeneration++;
        });
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        gd.widthHint = 150;
        baseUrlText.setLayoutData(gd);
    }

    protected void createURLInfoLink(@NotNull Composite parent) {
        tokenInfoLink = UIUtils.createLink(
            parent,
            NLS.bind(AIUIMessages.gpt_preference_page_token_info, getApiKeyURL()),
            new SelectionAdapter() {
                @Override
                public void widgetSelected(@NotNull SelectionEvent e) {
                    UIUtils.openWebBrowser(getApiKeyURL());
                }
            }
        );
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        tokenInfoLink.setLayoutData(gd);
    }

    @NotNull
    protected String getApiKeyURL() {
        return API_KEY_URL;
    }

    protected void applySettings() {
        if (baseUrlText != null) {
            baseUrlText.setText(baseUrl);
        }
        if (tokenText != null) {
            tokenText.setText(token);
        }

        temperatureText.setText(temperature);
    }

    @Override
    public boolean isComplete() {
        if (!contextWindowSizeField.isComplete()) {
            return false;
        }
        return isAccountAuthentication()
            ? properties != null && properties.isAccountConnected()
            : tokenText != null && !tokenText.getText().isEmpty();
    }

    @Override
    @NotNull
    public Optional<AIEngineProperties> getCurrentProperties() {
        OpenAIProperties propertiesCopy = new OpenAIProperties();
        propertiesCopy.setBaseUrl(baseUrl);
        propertiesCopy.setAuthentication(isAccountAuthentication()
            ? OpenAIProperties.AUTHENTICATION_CHATGPT_ACCOUNT
            : OpenAIProperties.AUTHENTICATION_API_TOKEN);
        propertiesCopy.setToken(isAccountAuthentication() ? apiToken : token);
        if (properties != null) {
            propertiesCopy.copyAccountTokensFrom(properties);
            propertiesCopy.useAccountCredentialsFrom(properties);
        }
        propertiesCopy.setModel(modelSelectorField.getSelectedModelName());
        propertiesCopy.setContextWindowSize(contextWindowSizeField.getValue());
        propertiesCopy.setTemperature(CommonUtils.toDouble(temperature));
        saveAdvancedSettings(propertiesCopy);
        return Optional.of(propertiesCopy);
    }

    private void updateAuthenticationControls() {
        if (tokenText == null || accountText == null) {
            return;
        }
        accountAuthentication = accountAuthenticationButton != null && accountAuthenticationButton.getSelection();
        boolean account = isAccountAuthentication();
        String accountTokenPlaceholder = getAccountProviderName() + " account";
        if (account && !accountTokenPlaceholder.equals(tokenText.getText())) {
            apiToken = tokenText.getText();
            tokenText.setText(accountTokenPlaceholder);
        } else if (!account) {
            tokenText.setText(CommonUtils.notEmpty(apiToken));
        }
        boolean connected = properties != null && properties.isAccountConnected();
        String accountEmail = properties == null ? null : properties.getAccountEmail();
        accountText.setText(connected
            ? CommonUtils.isEmpty(accountEmail)
                ? NLS.bind(AIUIMessages.openai_configurator_account_connected, getAccountProviderName())
                : NLS.bind(AIUIMessages.openai_configurator_account_connected_as, accountEmail)
            : AIUIMessages.openai_configurator_account_not_connected);
        accountActionButton.setText(connected
            ? AIUIMessages.openai_configurator_sign_out
            : NLS.bind(AIUIMessages.openai_configurator_sign_in, getAccountProviderName()));
        accountActionButton.setEnabled(true);
        if (modelSelectorField != null) {
            modelSelectorField.setRefreshEnabled(
                !account || connected,
                NLS.bind(AIUIMessages.openai_configurator_sign_in_to_refresh_models, getAccountProviderName())
            );
        }
        setVisible(accountLabel, account);
        setVisible(accountText, account);
        setVisible(accountActionButton, account);
        setVisible(tokenLabel, !account);
        setVisible(tokenText, !account);
        setVisible(tokenInfoLink, !account);
        if (baseUrlText != null) {
            setVisible(baseUrlText, !account);
        }
        accountText.getParent().layout(true, true);
    }

    private void authenticationChanged() {
        settingsGeneration++;
        updateAuthenticationControls();
        propertyChangeListener.run();
        boolean canLoadModels = properties != null && (isAccountAuthentication()
            ? properties.isAccountConnected()
            : !CommonUtils.isEmpty(apiToken));
        if (canLoadModels) {
            modelSelectorField.refreshModelListSilently(true);
        }
    }

    protected boolean isAccountAuthentication() {
        return accountAuthentication;
    }

    protected boolean isAccountAuthenticationSupported() {
        return AIAccountAuthenticator.isSupported();
    }

    @NotNull
    protected String getAccountAuthenticationId() {
        return OpenAIProperties.AUTHENTICATION_CHATGPT_ACCOUNT;
    }

    @NotNull
    protected String getAccountProviderName() {
        return "ChatGPT";
    }

    @NotNull
    protected AIAccountAuthenticator createAccountAuthenticator(int timeoutSeconds) {
        return new OpenAIAccountAuthenticator(timeoutSeconds);
    }

    private static void setVisible(@NotNull Control control, boolean visible) {
        control.setVisible(visible);
        setExcluded(control, !visible);
        Object label = control.getData("label");
        if (label instanceof Control labelControl) {
            labelControl.setVisible(visible);
            setExcluded(labelControl, !visible);
        }
    }

    private static void setExcluded(@NotNull Control control, boolean excluded) {
        Object layoutData = control.getLayoutData();
        if (layoutData == null) {
            GridData gridData = new GridData();
            gridData.exclude = excluded;
            control.setLayoutData(gridData);
        } else if (layoutData instanceof GridData gridData) {
            gridData.exclude = excluded;
        }
    }

    private void signIn() {
        OpenAIAuthMethodDialog.Method method = OpenAIAuthMethodDialog.Method.DEVICE_CODE;
        if (accountAuthenticator.supportsBrowserAuthorization()) {
            OpenAIAuthMethodDialog methodDialog = new OpenAIAuthMethodDialog(UIUtils.getActiveShell());
            if (methodDialog.open() != IDialogConstants.OK_ID) {
                return;
            }
            method = methodDialog.getSelectedMethod();
        }
        OpenAIAuthMethodDialog.Method selectedMethod = method;
        PROPERTIES targetProperties = properties;
        OpenAIProperties currentProperties = (OpenAIProperties) getCurrentProperties().orElse(targetProperties);
        AIAccountAuthenticator targetAuthenticator = createAccountAuthenticator(currentProperties.getTimeout());
        accountAuthenticator = targetAuthenticator;
        accountActionButton.setEnabled(false);
        new AbstractJob("Sign in to " + getAccountProviderName()) {
            @NotNull
            @Override
            protected org.eclipse.core.runtime.IStatus run(@NotNull DBRProgressMonitor monitor) {
                CompletableFuture<Void> popupCompletion = new CompletableFuture<>();
                Thread authorizationThread = Thread.currentThread();
                popupCompletion.whenComplete((result, error) -> {
                    if (popupCompletion.isCancelled()) {
                        authorizationThread.interrupt();
                    }
                });
                try {
                    AIAccountAuthenticator.Tokens tokens = selectedMethod == OpenAIAuthMethodDialog.Method.BROWSER
                        ? signInWithBrowser(targetAuthenticator, popupCompletion)
                        : signInHeadless(targetAuthenticator, popupCompletion);
                    if (!popupCompletion.complete(null)) {
                        return org.eclipse.core.runtime.Status.CANCEL_STATUS;
                    }
                    targetProperties.setAccountTokens(tokens);
                    DBException persistenceError = saveAccountTokens(targetProperties);
                    UIUtils.asyncExec(() -> {
                        if (persistenceError != null) {
                            DBWorkbench.getPlatformUI().showError(
                                NLS.bind(
                                    AIUIMessages.openai_configurator_credentials_save_error_title,
                                    getAccountProviderName()
                                ),
                                AIUIMessages.openai_configurator_credentials_save_error_message,
                                persistenceError
                            );
                        }
                        if (accountText.isDisposed() || properties != targetProperties) {
                            return;
                        }
                        settingsGeneration++;
                        updateAuthenticationControls();
                        modelSelectorField.refreshModelListSilently(true);
                        propertyChangeListener.run();
                    });
                    return persistenceError == null
                        ? org.eclipse.core.runtime.Status.OK_STATUS
                        : org.eclipse.core.runtime.Status.error(
                            "Unable to save " + getAccountProviderName() + " account credentials",
                            persistenceError
                        );
                } catch (Exception e) {
                    UIUtils.asyncExec(() -> {
                        if (accountText.isDisposed()) {
                            return;
                        }
                        if (properties == targetProperties) {
                            if (popupCompletion.isCancelled()) {
                                updateAuthenticationControls();
                            } else {
                                accountText.setText(AIUIMessages.openai_configurator_sign_in_failed);
                                accountActionButton.setEnabled(true);
                            }
                        }
                    });
                    if (popupCompletion.isCancelled()) {
                        return org.eclipse.core.runtime.Status.CANCEL_STATUS;
                    }
                    return org.eclipse.core.runtime.Status.error("Unable to sign in to " + getAccountProviderName(), e);
                } finally {
                    popupCompletion.complete(null);
                }
            }
        }.schedule();
    }

    private void signOut() {
        properties.clearAccountTokens();
        DBException persistenceError = saveAccountTokens(properties);
        settingsGeneration++;
        updateAuthenticationControls();
        propertyChangeListener.run();
        if (persistenceError != null) {
            DBWorkbench.getPlatformUI().showError(
                NLS.bind(
                    AIUIMessages.openai_configurator_sign_out_save_error_title,
                    getAccountProviderName()
                ),
                AIUIMessages.openai_configurator_sign_out_save_error_message,
                persistenceError
            );
        }
    }

    @Nullable
    private static DBException saveAccountTokens(@NotNull OpenAIProperties properties) {
        try {
            properties.saveAccountTokens();
            return null;
        } catch (DBException e) {
            return e;
        }
    }

    @NotNull
    private AIAccountAuthenticator.Tokens signInWithBrowser(
        @NotNull AIAccountAuthenticator authenticator,
        @NotNull CompletableFuture<Void> popupCompletion
    ) throws DBException {
        AIAccountAuthenticator.BrowserAuthorization authorization = authenticator.startBrowserAuthorization();
        popupCompletion.whenComplete((result, error) -> {
            if (popupCompletion.isCancelled()) {
                authenticator.cancelBrowserAuthorization();
            }
        });
        showBrowserAuthorizationPopup(authorization.authorizationUri(), popupCompletion);
        return authenticator.completeBrowserAuthorization();
    }

    @NotNull
    private AIAccountAuthenticator.Tokens signInHeadless(
        @NotNull AIAccountAuthenticator authenticator,
        @NotNull CompletableFuture<Void> popupCompletion
    ) throws DBException {
        UIServiceAuth service = DBWorkbench.getService(UIServiceAuth.class);
        if (service == null) {
            throw new DBException("No authentication UI service is available");
        }
        AIAccountAuthenticator.DeviceAuthorization authorization = authenticator.startDeviceAuthorization();
        service.showCodePopup(authorization.verificationUri(), authorization.userCode(), popupCompletion);
        return authenticator.completeDeviceAuthorization(authorization, popupCompletion);
    }

    private static void showBrowserAuthorizationPopup(
        @NotNull URI authorizationUri,
        @NotNull CompletableFuture<Void> completion
    ) {
        UIUtils.asyncExec(() -> {
            var shell = UIUtils.getActiveWorkbenchShell();
            if (shell == null) {
                completion.cancel(false);
                return;
            }
            OpenAIAccountAuthDialog dialog = new OpenAIAccountAuthDialog(shell, authorizationUri, completion);
            completion.whenComplete((result, error) -> UIUtils.asyncExec(dialog::close));
            UIUtils.openWebBrowser(authorizationUri.toString());
            dialog.open();
        });
    }
}
