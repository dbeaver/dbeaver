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
package org.jkiss.dbeaver.ui.ai.engine.openai;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIClient;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIEngine;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIModels;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.ai.model.CachedValue;
import org.jkiss.dbeaver.ui.ai.model.ContextWindowSizeField;
import org.jkiss.dbeaver.ui.ai.model.ModelSelectorField;
import org.jkiss.dbeaver.ui.ai.preferences.AIIObjectPropertyConfigurator;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class OpenAiConfigurator<ENGINE extends AIEngineDescriptor, PROPERTIES extends OpenAIProperties>
    implements AIIObjectPropertyConfigurator<ENGINE, PROPERTIES> {
    private static final String API_KEY_URL = "https://platform.openai.com/account/api-keys";
    protected String baseUrl;
    protected volatile String token = "";
    private String temperature = "0.0";
    private boolean logQuery = false;

    @Nullable
    private Text baseUrlText;

    @Nullable
    protected Text tokenText;
    private Text temperatureText;
    private ModelSelectorField modelSelectorField;
    private ContextWindowSizeField contextWindowSizeField;
    private Button logQueryCheck;

    protected final CachedValue<List<AIModel>> modelsCache = new CachedValue<>(this::fetchOpenAiModels);

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
        createBaseUrlParameter(composite);

        createAdditionalSettings(composite);
    }

    @Override
    public void loadSettings(@NotNull PROPERTIES configuration) {
        baseUrl = CommonUtils.toString(configuration.getBaseUrl());
        if (baseUrl.isEmpty()) {
            baseUrl = OpenAIClient.OPENAI_ENDPOINT;
        }
        token = CommonUtils.toString(configuration.getToken());
        modelSelectorField.setSelectedModel(
            CommonUtils.toString(configuration.getModel(), OpenAIModels.DEFAULT_MODEL)
        );
        temperature = CommonUtils.toString(configuration.getTemperature(), "0.0");
        logQuery = CommonUtils.toBoolean(configuration.isLoggingEnabled());
        applySettings();

        contextWindowSizeField.setValue(configuration.getContextWindowSize());

        modelSelectorField.refreshModelListSilently(false);
    }

    @Override
    public void saveSettings(@NotNull PROPERTIES configuration) {
        configuration.setBaseUrl(baseUrl);
        configuration.setToken(token);
        configuration.setModel(modelSelectorField.getSelectedModel());
        configuration.setContextWindowSize(contextWindowSizeField.getValue());
        configuration.setTemperature(CommonUtils.toDouble(temperature));
        configuration.setLoggingEnabled(logQuery);
    }

    @Override
    public void resetSettings(@NotNull PROPERTIES openAIPropertiesLegacyAISettings) {

    }

    protected void createAdditionalSettings(@NotNull Composite parent) {
        logQueryCheck = UIUtils.createCheckbox(
            parent,
            "Write AI queries to debug log",
            "Write AI queries with metadata info in debug logs",
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
        modelSelectorField = ModelSelectorField.builder()
            .withParent(parent)
            .withGridData(new GridData(GridData.FILL_HORIZONTAL))
            .withModelListSupplier(
                (monitor, forceRefresh) -> modelsCache.get(monitor, forceRefresh).stream()
                    .filter(it -> it.features().contains(AIModelFeature.CHAT))
                    .map(AIModel::name)
                    .toList()
            )
            .withSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
                OpenAIModels.getModelByName(modelSelectorField.getSelectedModel())
                    .ifPresentOrElse(
                        model -> {
                            contextWindowSizeField.setValue(model.contextWindowSize());
                            temperatureText.setText(String.valueOf(model.defaultTemperature()));
                        }, () -> {
                            contextWindowSizeField.setValue(null);
                            temperatureText.setText("0.0");
                        }
                    );
            }))
            .build();

        contextWindowSizeField = ContextWindowSizeField.builder()
            .withParent(parent)
            .withGridData(GridDataFactory.fillDefaults().span(2, 1).create())
            .build();

        temperatureText = UIUtils.createLabelText(parent, AIUIMessages.gpt_preference_page_text_temperature, "0.0");
        temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        temperatureText.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());

        temperatureText.setToolTipText("Lower temperatures give more precise results");
        temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        temperatureText.addModifyListener((e) -> temperature = temperatureText.getText());
    }

    private List<AIModel> fetchOpenAiModels(DBRProgressMonitor monitor) throws DBException {
        if (token == null || token.isEmpty()) {
            throw new DBException("Token is not set");
        }

        OpenAIProperties properties = new OpenAIProperties();
        properties.setToken(token);
        properties.setBaseUrl(baseUrl);

        try (OpenAIEngine<OpenAIProperties> engine = new OpenAIEngine<>(properties)) {
            return engine.getModels(monitor);
        }
    }

    protected void createConnectionParameters(@NotNull Composite parent) {
        tokenText = UIUtils.createLabelText(
            parent,
            AIUIMessages.gpt_preference_page_selector_token,
            "",
            SWT.BORDER | SWT.PASSWORD
        );
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 150;
        tokenText.setLayoutData(gd);
        tokenText.addModifyListener((e -> token = tokenText.getText()));
        tokenText.setMessage("API access token");
        createURLInfoLink(parent);
    }

    protected void createBaseUrlParameter(@NotNull Composite parent) {
        baseUrlText = UIUtils.createLabelText(
            parent,
            AIUIMessages.gpt_preference_page_selector_base_url,
            ""
        );
        baseUrlText.addModifyListener((e -> baseUrl = baseUrlText.getText()));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 150;
        baseUrlText.setLayoutData(gd);
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
        gd.horizontalSpan = 3;
        link.setLayoutData(gd);
    }

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
        logQueryCheck.setSelection(logQuery);
    }

    @Override
    public boolean isComplete() {
        return tokenText != null
            && !tokenText.getText().isEmpty()
            && contextWindowSizeField.isComplete();
    }

    @Override
    public Optional<AIEngineProperties> getCurrentProperties() {
        OpenAIProperties propertiesCopy = new OpenAIProperties();
        propertiesCopy.setBaseUrl(baseUrl);
        propertiesCopy.setToken(token);
        propertiesCopy.setModel(modelSelectorField.getSelectedModel());
        propertiesCopy.setContextWindowSize(contextWindowSizeField.getValue());
        propertiesCopy.setTemperature(CommonUtils.toDouble(temperature));
        propertiesCopy.setLoggingEnabled(logQuery);
        return Optional.of(propertiesCopy);
    }
}
