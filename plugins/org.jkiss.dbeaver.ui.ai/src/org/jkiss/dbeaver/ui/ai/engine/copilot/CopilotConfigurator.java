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
package org.jkiss.dbeaver.ui.ai.engine.copilot;


import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.engine.copilot.*;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceAuth;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.ai.model.CachedValue;
import org.jkiss.dbeaver.ui.ai.model.ContextWindowSizeField;
import org.jkiss.dbeaver.ui.ai.model.ModelSelectorField;
import org.jkiss.dbeaver.ui.ai.preferences.AIIObjectPropertyConfigurator;
import org.jkiss.utils.CommonUtils;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CopilotConfigurator<ENGINE extends AIEngineDescriptor, PROPERTIES extends CopilotProperties>
    implements AIIObjectPropertyConfigurator<ENGINE, PROPERTIES> {

    private Text temperatureText;
    private ContextWindowSizeField contextWindowSizeField;
    private ModelSelectorField modelSelectorField;
    private Button logQueryCheck;
    private Text accessTokenText;

    protected volatile String accessToken;
    protected String token = "";
    private String temperature = "0.0";
    private boolean logQuery = false;

    protected final CachedValue<List<AIModel>> modelsCache = new CachedValue<>(this::fetchCopilotModels);

    @NotNull
    private List<AIModel> fetchCopilotModels(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new DBException("Access token is not set");
        }

        try (CopilotCompletionEngine engine = createEngine()) {
            return engine.getModels(monitor);
        }
    }

    @Override
    public void createControl(
        @NotNull Composite parent,
        AIEngineDescriptor object,
        @NotNull Runnable propertyChangeListener
    ) {
        Composite composite = UIUtils.createComposite(parent, 3);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createConnectionParameters(composite);
        createModelParameters(composite);
        createAdditionalSettings(composite);
        UIUtils.syncExec(this::applySettings);
    }

    @Override
    public void loadSettings(@NotNull PROPERTIES configuration) {
        token = CommonUtils.toString(configuration.getToken());
        modelSelectorField.setSelectedModel(configuration.getModel());
        contextWindowSizeField.setValue(configuration.getContextWindowSize());
        temperature = CommonUtils.toString(configuration.getTemperature(), "0.0");
        logQuery = CommonUtils.toBoolean(configuration.isLoggingEnabled());
        accessToken = CommonUtils.toString(configuration.getToken(), "");
        accessTokenText.setText(accessToken);
        applySettings();

        modelSelectorField.refreshModelListSilently(true);
    }

    @Override
    public void saveSettings(@NotNull PROPERTIES properties) {
        properties.setToken(accessToken);
        properties.setModel(modelSelectorField.getSelectedModel());
        properties.setContextWindowSize(contextWindowSizeField.getValue());
        properties.setTemperature(CommonUtils.toDouble(temperature));
        properties.setLoggingEnabled(logQuery);
    }

    @Override
    public void resetSettings(@NotNull PROPERTIES copilotPropertiesLegacyAISettings) {

    }

    @Override
    public boolean isComplete() {
        return true;
    }

    protected void createModelParameters(@NotNull Composite parent) {
        ModelSelectorField.ModelListProvider modelListProvider = (monitor, forceRefresh) -> {
            return modelsCache.get(monitor, forceRefresh).stream()
                .filter(it -> it.features().contains(AIModelFeature.CHAT))
                .map(AIModel::name)
                .toList();
        };

        modelSelectorField = ModelSelectorField.builder()
            .withParent(parent)
            .withGridData(new GridData(GridData.FILL_HORIZONTAL))
            .withModifyListener(() -> {
                CopilotModels.getModelByName(modelSelectorField.getSelectedModel())
                    .ifPresentOrElse(
                        model -> {
                            contextWindowSizeField.setValue(model.contextWindowSize());
                            temperatureText.setText(String.valueOf(model.defaultTemperature()));
                        }, () -> {
                            contextWindowSizeField.setValue(null);
                            temperatureText.setText("0.0");
                        }
                    );
            })
            .withModelListSupplier(modelListProvider)
            .build();

        contextWindowSizeField = ContextWindowSizeField.builder()
            .withParent(parent)
            .withGridData(GridDataFactory.fillDefaults().span(2, 1).create())
            .build();

        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        temperatureText = UIUtils.createLabelText(parent, AIUIMessages.gpt_preference_page_text_temperature, "0.0");
        temperatureText.setLayoutData(gridData);
        temperatureText.setToolTipText("Lower temperatures give more precise results");
        temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        temperatureText.addModifyListener((e) -> temperature = temperatureText.getText());
    }

    private void createAdditionalSettings(@NotNull Composite parent) {
        logQueryCheck = UIUtils.createCheckbox(
            parent,
            "Write AI queries to debug log",
            "Write AI queries with metadata info in the debug logs",
            false,
            2
        );
        logQueryCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                logQuery = logQueryCheck.getSelection();
            }
        });
    }

    private void applySettings() {
        temperatureText.setText(temperature);
        logQueryCheck.setSelection(logQuery);
    }

    protected void createConnectionParameters(@NotNull Composite parent) {

        accessTokenText = UIUtils.createLabelText(
            parent,
            CopilotMessages.copilot_access_token,
            "",
            SWT.BORDER | SWT.PASSWORD
        );
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 150;
        accessTokenText.setLayoutData(gd);
        accessTokenText.addModifyListener((e -> accessToken = accessTokenText.getText()));
        accessTokenText.setMessage(CopilotMessages.copilot_preference_page_token_info);
        UIUtils.createDialogButton(parent, CopilotMessages.copilot_access_token_authorize, SelectionListener.widgetSelectedAdapter(e -> {
            try {
                accessToken = UIUtils.runWithDialog(monitor -> {
                    var future = new CompletableFuture<Void>();
                    try {
                        return acquireAccessToken(monitor, future);
                    } finally {
                        future.complete(null);
                    }
                });
            } catch (DBException ex) {
                DBWorkbench.getPlatformUI().showError(
                    CopilotMessages.oauth_auth_title,
                    NLS.bind(CopilotMessages.oauth_auth_error_message, ex.getMessage()),
                    ex
                );
                return;
            }

            UIUtils.showMessageBox(
                UIUtils.getActiveShell(),
                CopilotMessages.oauth_auth_title,
                CopilotMessages.oauth_auth_success_message,
                SWT.ICON_INFORMATION
            );
            if (accessTokenText != null && !accessTokenText.isDisposed()) {
                accessTokenText.setText(accessToken);
                accessTokenText = UIUtils.recreateTextControl(accessTokenText, SWT.BORDER);
            }
            modelSelectorField.refreshModelListSilently(true);
        }));
    }

    @NotNull
    private String acquireAccessToken(@NotNull DBRProgressMonitor monitor, @NotNull CompletableFuture<Void> future) throws DBException {
        var service = DBWorkbench.getService(UIServiceAuth.class);
        if (service == null) {
            throw new DBException("No authentication service available");
        }
        try (var client = new CopilotClient(getCurrentAuthURL())) {
            monitor.subTask("Requesting device code");
            var deviceCodeResponse = client.requestDeviceCode(monitor);

            service.showCodePopup(URI.create(deviceCodeResponse.verificationUri()), deviceCodeResponse.userCode(), future);

            monitor.subTask("Awaiting access token");
            return client.requestAccessToken(monitor, deviceCodeResponse, future);
        } catch (InterruptedException e) {
            throw new DBException("Authorization was interrupted", e);
        }
    }

    @NotNull
    protected String getCurrentAuthURL() {
        return CopilotConstants.BASE_AUTH_URL;
    }

    @NotNull
    @Override
    public Optional<AIEngineProperties> getCurrentProperties() {
        CopilotProperties copilotPropertiesCopy = new CopilotProperties();
        saveSettings((PROPERTIES) copilotPropertiesCopy);
        return Optional.of(copilotPropertiesCopy);
    }

    @NotNull
    protected CopilotCompletionEngine createEngine() {
        CopilotProperties properties = new CopilotProperties();
        properties.setToken(accessToken);

        return new CopilotCompletionEngine(properties);
    }
}
