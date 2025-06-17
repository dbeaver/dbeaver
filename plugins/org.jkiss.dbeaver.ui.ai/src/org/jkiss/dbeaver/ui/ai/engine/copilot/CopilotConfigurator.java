/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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


import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIEngine;
import org.jkiss.dbeaver.model.ai.engine.LegacyAISettings;
import org.jkiss.dbeaver.model.ai.engine.copilot.CopilotClient;
import org.jkiss.dbeaver.model.ai.engine.copilot.CopilotProperties;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceAuth;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.utils.CommonUtils;

import java.net.URI;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class CopilotConfigurator implements IObjectPropertyConfigurator<AIEngine, LegacyAISettings<CopilotProperties>> {

    @Nullable
    protected Text tokenText;
    private Text temperatureText;
    private Combo modelCombo;
    private Button logQueryCheck;
    private Text accessTokenText;

    private String accessToken;
    protected String token = "";
    protected String model = "";
    private String temperature = "0.0";
    private boolean logQuery = false;

    @Override
    public void createControl(
        @NotNull Composite parent,
        AIEngine object,
        @NotNull Runnable propertyChangeListener
    ) {
        Composite authorizeComposite = UIUtils.createComposite(parent, 3);
        authorizeComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createConnectionParameters(authorizeComposite);
        Composite composite = UIUtils.createComposite(parent, 2);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createModelParameters(composite);

        createAdditionalSettings(composite);
        UIUtils.syncExec(this::applySettings);
    }

    @Override
    public void loadSettings(@NotNull LegacyAISettings<CopilotProperties> configuration) {
        token = CommonUtils.toString(configuration.getProperties().getToken());
        model = readModel(configuration).getName();
        temperature = CommonUtils.toString(configuration.getProperties().getTemperature(), "0.0");
        logQuery = CommonUtils.toBoolean(configuration.getProperties().isLoggingEnabled());
        accessToken = CommonUtils.toString(configuration.getProperties().getToken(), "");
        accessTokenText.setText(accessToken);
        applySettings();
    }

    @Override
    public void saveSettings(@NotNull LegacyAISettings<CopilotProperties> copilotSettings) {
        copilotSettings.getProperties().setToken(accessToken);
        copilotSettings.getProperties().setModel(model);
        copilotSettings.getProperties().setTemperature(Double.parseDouble(temperature));
        copilotSettings.getProperties().setLoggingEnabled(logQuery);
    }

    @Override
    public void resetSettings(@NotNull LegacyAISettings<CopilotProperties> copilotPropertiesLegacyAISettings) {

    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @NotNull
    protected OpenAIModel[] getSupportedGPTModels() {
        return new OpenAIModel[] {
            OpenAIModel.GPT_4,
            OpenAIModel.GPT_TURBO
        };
    }

    protected String getDefaultModel() {
        return OpenAIModel.GPT_4.getName();
    }

    private void createModelParameters(@NotNull Composite parent) {
        modelCombo = UIUtils.createLabelCombo(parent, AIUIMessages.gpt_preference_page_combo_engine, SWT.READ_ONLY);
        for (OpenAIModel model : getSupportedGPTModels()) {
            if (model.getDeprecationReplacementModel() == null) {
                modelCombo.add(model.getName());
            }
        }
        modelCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                model = modelCombo.getText();
            }
        });
        temperatureText = UIUtils.createLabelText(parent, AIUIMessages.gpt_preference_page_text_temperature, "0.0");
        temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        UIUtils.createInfoLabel(parent, "Lower temperatures give more precise results", GridData.FILL_HORIZONTAL, 2);
        temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        temperatureText.addModifyListener((e) -> temperature = temperatureText.getText());
    }

    private void createAdditionalSettings(@NotNull Composite parent) {
        logQueryCheck = UIUtils.createCheckbox(
            parent,
            "Write GPT queries to debug log",
            "Write GPT queries with metadata info in debug logs",
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
        if (tokenText != null) {
            tokenText.setText(token);
        }
        modelCombo.setText(model);
        temperatureText.setText(temperature);
        logQueryCheck.setSelection(logQuery);
    }

    private OpenAIModel readModel(@NotNull LegacyAISettings<CopilotProperties> aiSettings) {
        return OpenAIModel.getByName(CommonUtils.toString(aiSettings.getProperties().getModel(), getDefaultModel()));
    }

    private void createConnectionParameters(@NotNull Composite parent) {

        accessTokenText = UIUtils.createLabelText(
            parent,
            CopilotMessages.copilot_access_token,
            "",
            SWT.BORDER | SWT.PASSWORD
        );
        accessTokenText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
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
            }
        }));
    }

    @NotNull
    private String acquireAccessToken(@NotNull DBRProgressMonitor monitor, @NotNull CompletableFuture<Void> future) throws DBException {
        var service = DBWorkbench.getService(UIServiceAuth.class);
        if (service == null) {
            throw new DBException("No authentication service available");
        }
        try (var client = new CopilotClient()) {
            monitor.subTask("Requesting device code");
            var deviceCodeResponse = client.requestDeviceCode(monitor);

            service.showCodePopup(URI.create(deviceCodeResponse.verificationUri()), deviceCodeResponse.userCode(), future);

            monitor.subTask("Awaiting access token");
            return client.requestAccessToken(monitor, deviceCodeResponse, future);
        } catch (InterruptedException e) {
            throw new DBException("Authorization was interrupted", e);
        }
    }
}
