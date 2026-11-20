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
package org.jkiss.dbeaver.ui.ai.engine.ollama;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.ollama.OllamaCompletionEngine;
import org.jkiss.dbeaver.model.ai.engine.ollama.OllamaProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.ai.model.CachedValue;
import org.jkiss.dbeaver.ui.ai.model.ModelSelectorField;
import org.jkiss.dbeaver.ui.ai.preferences.AbstractAIEngineConfigurator;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class OllamaConfigurator extends AbstractAIEngineConfigurator<AIEngineDescriptor, OllamaProperties> {
    @Override
    protected void environmentSettingsChanged() {
        super.environmentSettingsChanged();
        if (!CommonUtils.isEmptyTrimmed(createProperties().getEffectiveHostname())) {
            modelSelectorField.refreshModelListSilently(true);
        }
    }

    private String hostname = "";
    private String temperature = "";
    private String contextSize = "";

    private Text hostnameText;
    private Text temperatureText;
    private Text contextSizeText;
    private ModelSelectorField modelSelectorField;

    private final CachedValue<List<AIModel>> modelsCache = new CachedValue<>(this::fetchModels);

    @Override
    public void createControl(@NotNull Composite parent, @NotNull AIEngineDescriptor object, @NotNull Runnable propertyChangeListener) {
        Composite composite = UIUtils.createComposite(parent, 3);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        hostnameText = UIUtils.createLabelText(
            composite,
            AIUIMessages.ollama_preference_page_hostname,
            "",
            SWT.BORDER
        );
        hostnameText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());
        hostnameText.addModifyListener(e -> {
            if (!isUpdatingEnvironmentControls()) {
                hostname = hostnameText.getText();
            }
        });
        hostnameText.setMessage(AIUIMessages.ollama_preference_page_hostname);

        modelSelectorField = ModelSelectorField.builder()
            .withParent(composite)
            .withModelLabel(AIUIMessages.ollama_preference_page_model)
            .withGridData(new GridData(GridData.FILL_HORIZONTAL))
            .withRequiredSetting(hostnameText, AIUIMessages.ollama_preference_page_hostname)
            .withModelListSupplier(modelsCache::get)
            .build();

        contextSizeText = UIUtils.createLabelText(
            composite,
            AIUIMessages.ollama_preference_page_context_size,
            "",
            SWT.BORDER
        );
        contextSizeText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());
        contextSizeText.addModifyListener(e -> contextSize = contextSizeText.getText());
        contextSizeText.setMessage(AIUIMessages.ollama_preference_page_context_size);
        contextSizeText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));

        temperatureText = UIUtils.createLabelText(composite, AIUIMessages.ollama_preference_page_temperature, "0.0");
        temperatureText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());
        temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        temperatureText.addModifyListener(e -> temperature = temperatureText.getText());
        temperatureText.setMessage(AIUIMessages.ai_engine_configurator_temperature_default);
        temperatureText.setToolTipText(AIUIMessages.ollama_preference_page_temperature_info);

        createAdvancedSettings(composite);
        createEnvironmentSettings(propertyChangeListener);
    }

    @NotNull
    private List<AIModel> fetchModels(@NotNull DBRProgressMonitor monitor) throws DBException {
        OllamaProperties properties = createProperties();
        if (CommonUtils.isEmptyTrimmed(properties.getEffectiveHostname())) {
            return List.of();
        }
        try (OllamaCompletionEngine engine = new OllamaCompletionEngine(properties)) {
            return engine.getModels(monitor).stream().toList();
        }
    }

    @Override
    public void loadSettings(@NotNull OllamaProperties configuration) {
        hostname = CommonUtils.toString(configuration.getHostname(), "");
        contextSize = CommonUtils.toString(configuration.getContextWindowSize(), "");
        temperature = configuration.getRequestTemperature() == null ? "" : String.valueOf(configuration.getTemperature());
        modelSelectorField.setSelectedModel(CommonUtils.toString(configuration.getModel(), ""));
        applySettings();
        loadAdvancedSettings(configuration);

        updateEnvironmentControls();
        if (!CommonUtils.isEmptyTrimmed(configuration.getEffectiveHostname())) {
            modelSelectorField.refreshModelListSilently(false);
        }
    }

    @Override
    public void saveSettings(@NotNull OllamaProperties configuration) {
        configuration.setHostname(hostname);
        configuration.setModel(CommonUtils.toString(modelSelectorField.getSelectedModelName(), ""));
        configuration.setContextWindowSize(CommonUtils.toInt(contextSize));
        saveTemperature(configuration, temperature);
        saveAdvancedSettings(configuration);
    }

    @Override
    protected void updateEnvironmentControls() {
        updateEnvironmentText(hostnameText, hostname, getEnvironmentValue(OllamaProperties.ENV_HOST));
        AIEngineProperties currentProperties = createProperties();
        modelSelectorField.setEnvironmentModel(
            currentProperties.isModelSelectionSupported() ? null : currentProperties.getEffectiveModel());
    }

    @Override
    public void resetSettings(@NotNull OllamaProperties ollamaProperties) {

    }

    @NotNull
    @Override
    public Optional<AIEngineProperties> getCurrentProperties() {
        return Optional.of(createProperties());
    }

    @NotNull
    private OllamaProperties createProperties() {
        OllamaProperties properties = new OllamaProperties();
        saveSettings(properties);
        return properties;
    }

    private void applySettings() {
        hostnameText.setText(hostname);
        contextSizeText.setText(contextSize);
        temperatureText.setText(temperature);
    }

    @Override
    public boolean isComplete() {
        return createProperties().isValidConfiguration();
    }
}
