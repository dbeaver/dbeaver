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
package org.jkiss.dbeaver.ui.ai.engine.minimax;

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
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.engine.minimax.MiniMaxConstants;
import org.jkiss.dbeaver.model.ai.engine.minimax.MiniMaxModels;
import org.jkiss.dbeaver.model.ai.engine.minimax.MiniMaxProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.ai.model.ContextWindowSizeField;
import org.jkiss.dbeaver.ui.ai.model.ModelSelectorField;
import org.jkiss.dbeaver.ui.ai.preferences.AIIObjectPropertyConfigurator;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * UI configurator for the MiniMax AI engine
 */
public class MiniMaxConfigurator
    implements AIIObjectPropertyConfigurator<AIEngineDescriptor, MiniMaxProperties> {

    private static final String API_KEY_URL =
        "https://platform.minimax.io/user-center/basic-information/interface-key"; //$NON-NLS-1$

    private Text tokenText;
    private Text baseUrlText;
    private Text temperatureText;
    private ModelSelectorField modelSelectorField;
    private ContextWindowSizeField contextWindowSizeField;
    private Button logQueryCheck;

    private String baseUrl = MiniMaxConstants.MINIMAX_ENDPOINT;
    private volatile String token = ""; //$NON-NLS-1$
    private String temperature = "1.0"; //$NON-NLS-1$
    private boolean logQuery = false;

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
    public void loadSettings(@NotNull MiniMaxProperties configuration) {
        baseUrl = CommonUtils.toString(configuration.getBaseUrl());
        if (baseUrl.isEmpty()) {
            baseUrl = MiniMaxConstants.MINIMAX_ENDPOINT;
        }
        token = CommonUtils.toString(configuration.getToken());
        modelSelectorField.setSelectedModel(
            CommonUtils.toString(configuration.getModel(), MiniMaxConstants.DEFAULT_MODEL)
        );
        temperature = CommonUtils.toString(
            configuration.getTemperature(),
            String.valueOf(MiniMaxConstants.DEFAULT_TEMPERATURE)
        );
        logQuery = CommonUtils.toBoolean(configuration.isLoggingEnabled());
        contextWindowSizeField.setValue(configuration.getContextWindowSize());

        applySettings();
    }

    @Override
    public void saveSettings(@NotNull MiniMaxProperties configuration) {
        configuration.setBaseUrl(baseUrl);
        configuration.setToken(token);
        configuration.setModel(modelSelectorField.getSelectedModel());
        configuration.setContextWindowSize(contextWindowSizeField.getValue());
        configuration.setTemperature(CommonUtils.toDouble(temperature));
        configuration.setLoggingEnabled(logQuery);
    }

    @Override
    public void resetSettings(@NotNull MiniMaxProperties configuration) {
    }

    @Override
    public boolean isComplete() {
        return tokenText != null
            && !tokenText.getText().isEmpty()
            && contextWindowSizeField.isComplete();
    }

    @Override
    public Optional<AIEngineProperties> getCurrentProperties() {
        MiniMaxProperties propertiesCopy = new MiniMaxProperties();
        propertiesCopy.setBaseUrl(baseUrl);
        propertiesCopy.setToken(token);
        propertiesCopy.setModel(modelSelectorField.getSelectedModel());
        propertiesCopy.setContextWindowSize(contextWindowSizeField.getValue());
        propertiesCopy.setTemperature(CommonUtils.toDouble(temperature));
        propertiesCopy.setLoggingEnabled(logQuery);
        return Optional.of(propertiesCopy);
    }

    private void createConnectionParameters(@NotNull Composite parent) {
        tokenText = UIUtils.createLabelText(
            parent,
            AIUIMessages.gpt_preference_page_selector_token,
            "", //$NON-NLS-1$
            SWT.BORDER | SWT.PASSWORD
        );
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 150;
        tokenText.setLayoutData(gd);
        tokenText.addModifyListener(e -> token = tokenText.getText());
        tokenText.setMessage(AIUIMessages.openai_configurator_token_placeholder);

        Link link = UIUtils.createLink(
            parent,
            NLS.bind(AIUIMessages.gpt_preference_page_token_info, API_KEY_URL),
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    UIUtils.openWebBrowser(API_KEY_URL);
                }
            }
        );
        GridData linkGd = new GridData(GridData.FILL_HORIZONTAL);
        linkGd.horizontalSpan = 3;
        link.setLayoutData(linkGd);
    }

    private void createModelParameters(@NotNull Composite parent) {
        // MiniMax models are fixed, no need to fetch from API
        ModelSelectorField.ModelListProvider modelListProvider =
            (monitor, forceRefresh) -> MiniMaxModels.KNOWN_MODELS.values().stream()
                .filter(it -> it.features().contains(AIModelFeature.CHAT))
                .map(AIModel::name)
                .toList();

        modelSelectorField = ModelSelectorField.builder()
            .withParent(parent)
            .withGridData(new GridData(GridData.FILL_HORIZONTAL))
            .withModelListSupplier(modelListProvider)
            .withModifyListener(() ->
                MiniMaxModels.getModelByName(modelSelectorField.getSelectedModel())
                    .ifPresentOrElse(
                        model -> {
                            contextWindowSizeField.setValue(model.contextWindowSize());
                            temperatureText.setText(
                                String.valueOf(model.defaultTemperature())
                            );
                        },
                        () -> {
                            contextWindowSizeField.setValue(null);
                            temperatureText.setText(
                                String.valueOf(MiniMaxConstants.DEFAULT_TEMPERATURE)
                            );
                        }
                    ))
            .build();

        contextWindowSizeField = ContextWindowSizeField.builder()
            .withParent(parent)
            .withGridData(GridDataFactory.fillDefaults().span(2, 1).create())
            .build();

        temperatureText = UIUtils.createLabelText(
            parent,
            AIUIMessages.gpt_preference_page_text_temperature,
            String.valueOf(MiniMaxConstants.DEFAULT_TEMPERATURE)
        );
        temperatureText.addVerifyListener(
            UIUtils.getNumberVerifyListener(Locale.getDefault())
        );
        temperatureText.setLayoutData(
            GridDataFactory.fillDefaults().span(2, 1).create()
        );
        temperatureText.setToolTipText(
            "Temperature range: (0.0, 1.0]. Default: 1.0" //$NON-NLS-1$
        );
        temperatureText.addModifyListener(e -> temperature = temperatureText.getText());
    }

    private void createBaseUrlParameter(@NotNull Composite parent) {
        baseUrlText = UIUtils.createLabelText(
            parent,
            AIUIMessages.gpt_preference_page_selector_base_url,
            "" //$NON-NLS-1$
        );
        baseUrlText.addModifyListener(e -> baseUrl = baseUrlText.getText());
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 150;
        baseUrlText.setLayoutData(gd);
    }

    private void createAdditionalSettings(@NotNull Composite parent) {
        logQueryCheck = UIUtils.createCheckbox(
            parent,
            AIUIMessages.openai_configurator_log_query_label,
            AIUIMessages.openai_configurator_log_query_tip,
            false,
            2
        );
        logQueryCheck.addSelectionListener(
            SelectionListener.widgetSelectedAdapter(
                e -> logQuery = logQueryCheck.getSelection()
            )
        );
    }

    private void applySettings() {
        if (baseUrlText != null) {
            baseUrlText.setText(baseUrl);
        }
        if (tokenText != null) {
            tokenText.setText(token);
        }
        temperatureText.setText(temperature);
        logQueryCheck.setSelection(logQuery);
    }
}
