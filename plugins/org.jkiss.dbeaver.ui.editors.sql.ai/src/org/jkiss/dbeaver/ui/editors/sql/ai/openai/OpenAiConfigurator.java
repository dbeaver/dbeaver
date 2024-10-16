/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AIEngineSettings;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionEngine;
import org.jkiss.dbeaver.model.ai.openai.GPTModel;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.ai.internal.AIUIMessages;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

public class OpenAiConfigurator implements IObjectPropertyConfigurator<DAICompletionEngine<?>, AIEngineSettings> {
    private static final String API_KEY_URL = "https://platform.openai.com/account/api-keys";
    protected String token = "";
    private String temperature = "0.0";
    private String model = "";
    private boolean logQuery = false;

    @Nullable
    protected Text tokenText;
    private Text temperatureText;
    private Combo modelCombo;
    private Button logQueryCheck;

    @Override
    public void createControl(
        @NotNull Composite parent,
        DAICompletionEngine<?> object,
        @NotNull Runnable propertyChangeListener
    ) {
        Composite composite = UIUtils.createComposite(parent, 2);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createConnectionParameters(composite);

        createModelParameters(composite);

        createAdditionalSettings(composite);
        UIUtils.syncExec(this::applySettings);
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
            for (GPTModel model : getSupportedGPTModels()) {
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
            UIUtils.createInfoLabel(parent, NLS.bind(AIUIMessages.gpt_preference_page_info_model, getDefaultModel()), GridData.FILL_HORIZONTAL, 2);
        }
        temperatureText = UIUtils.createLabelText(parent, AIUIMessages.gpt_preference_page_text_temperature, "0.0");
        temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        UIUtils.createInfoLabel(parent, "Lower temperatures give more precise results", GridData.FILL_HORIZONTAL, 2);
        temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        temperatureText.addModifyListener((e) -> temperature = temperatureText.getText());
    }

    @NotNull
    protected GPTModel[] getSupportedGPTModels() {
        return GPTModel.values();
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

    @Override
    public void loadSettings(@NotNull AIEngineSettings aiSettings) {
        token = CommonUtils.toString(aiSettings.getProperties().get(AIConstants.GPT_API_TOKEN), "");
        model = isUsesModel() ? readModel(aiSettings).getName() : "";
        temperature = CommonUtils.toString(aiSettings.getProperties().get(
            AIConstants.AI_TEMPERATURE),
            "0.0"
        );
        logQuery = CommonUtils.toBoolean(aiSettings.getProperties().get(AIConstants.AI_LOG_QUERY)) ;
        applySettings();
    }

    private GPTModel readModel(@NotNull AIEngineSettings aiSettings) {
        return GPTModel.getByName(CommonUtils.toString(aiSettings.getProperties().get(AIConstants.GPT_MODEL), getDefaultModel()));
    }

    protected String getDefaultModel() {
        return GPTModel.GPT_TURBO.getName();
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

    @Override
    public void saveSettings(@NotNull AIEngineSettings aiSettings) {
        aiSettings.getProperties().put(AIConstants.GPT_API_TOKEN, token);
        if (isUsesModel()) {
            aiSettings.getProperties().put(AIConstants.GPT_MODEL, model);
        }
        aiSettings.getProperties().put(AIConstants.AI_TEMPERATURE, temperature);
        aiSettings.getProperties().put(AIConstants.AI_LOG_QUERY, logQuery);
    }

    @Override
    public void resetSettings(@NotNull AIEngineSettings aiSettings) {

    }

    protected boolean isUsesModel() {
        return true;
    }

    @Override
    public boolean isComplete() {
        return tokenText == null || !tokenText.getText().isEmpty();
    }
}
