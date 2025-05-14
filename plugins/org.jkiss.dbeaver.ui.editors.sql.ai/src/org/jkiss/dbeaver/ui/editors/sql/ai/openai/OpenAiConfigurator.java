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
package org.jkiss.dbeaver.ui.editors.sql.ai.openai;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.LegacyAISettings;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionEngine;
import org.jkiss.dbeaver.model.ai.openai.OpenAIModel;
import org.jkiss.dbeaver.model.ai.openai.OpenAIProperties;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.ai.internal.AIUIMessages;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

public class OpenAiConfigurator implements IObjectPropertyConfigurator<DAICompletionEngine, LegacyAISettings<OpenAIProperties>> {
    private static final String API_KEY_URL = "https://platform.openai.com/account/api-keys";
    protected String token = "";
    protected String model = "";
    private String temperature = "0.0";
    private boolean logQuery = false;

    @Nullable
    protected Text tokenText;
    private Text temperatureText;
    private Combo modelCombo;
    private Button logQueryCheck;

    @Override
    public void createControl(
        @NotNull Composite parent,
        DAICompletionEngine object,
        @NotNull Runnable propertyChangeListener
    ) {
        Composite composite = UIUtils.createComposite(parent, 2);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createConnectionParameters(composite);

        createModelParameters(composite);

        createAdditionalSettings(composite);
        UIUtils.syncExec(this::applySettings);
    }

    @Override
    public void loadSettings(@NotNull LegacyAISettings<OpenAIProperties> configuration) {
        token = CommonUtils.toString(configuration.getProperties().getToken());
        model = readModel(configuration).getName();
        temperature = CommonUtils.toString(configuration.getProperties().getTemperature(), "0.0");
        logQuery = CommonUtils.toBoolean(configuration.getProperties().isLoggingEnabled());
        applySettings();
    }

    @Override
    public void saveSettings(@NotNull LegacyAISettings<OpenAIProperties> configuration) {
        configuration.getProperties().setToken(token);
        configuration.getProperties().setModel(model);
        configuration.getProperties().setTemperature(Double.parseDouble(temperature));
        configuration.getProperties().setLoggingEnabled(logQuery);
    }

    @Override
    public void resetSettings(@NotNull LegacyAISettings<OpenAIProperties> openAIPropertiesLegacyAISettings) {

    }

    protected void createAdditionalSettings(@NotNull Composite parent) {
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

    protected void createModelParameters(@NotNull Composite parent) {
        if (isUsesModel()) {
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
        }
        temperatureText = UIUtils.createLabelText(parent, AIUIMessages.gpt_preference_page_text_temperature, "0.0");
        temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        UIUtils.createInfoLabel(parent, "Lower temperatures give more precise results", GridData.FILL_HORIZONTAL, 2);
        temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        temperatureText.addModifyListener((e) -> temperature = temperatureText.getText());
    }

    @NotNull
    protected OpenAIModel[] getSupportedGPTModels() {
        return OpenAIModel.values();
    }

    protected void createConnectionParameters(@NotNull Composite parent) {
        tokenText = UIUtils.createLabelText(
            parent,
            AIUIMessages.gpt_preference_page_selector_token,
            "",
            SWT.BORDER | SWT.PASSWORD
        );
        tokenText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        tokenText.addModifyListener((e -> token = tokenText.getText()));
        tokenText.setMessage("API access token");
        createURLInfoLink(parent);
    }

    protected void createURLInfoLink(@NotNull Composite parent) {
        Link link = UIUtils.createLink(
            parent,
            NLS.bind(AIUIMessages.gpt_preference_page_token_info, getApiKeyURL()),
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    UIUtils.openWebBrowser(getApiKeyURL());
                }
            }
        );
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        link.setLayoutData(gd);
    }

    protected String getApiKeyURL() {
        return API_KEY_URL;
    }

    private OpenAIModel readModel(@NotNull LegacyAISettings<OpenAIProperties> configuration) {
        return OpenAIModel.getByName(CommonUtils.toString(configuration.getProperties().getModel(), getDefaultModel()));
    }

    protected String getDefaultModel() {
        return OpenAIModel.GPT_TURBO.getName();
    }

    protected void applySettings() {
        if (tokenText != null) {
            tokenText.setText(token);
        }
        if (isUsesModel()) {
            modelCombo.setText(model);
        }
        temperatureText.setText(temperature);
        logQueryCheck.setSelection(logQuery);
    }

    protected boolean isUsesModel() {
        return true;
    }

    @Override
    public boolean isComplete() {
        return tokenText == null || !tokenText.getText().isEmpty();
    }
}
