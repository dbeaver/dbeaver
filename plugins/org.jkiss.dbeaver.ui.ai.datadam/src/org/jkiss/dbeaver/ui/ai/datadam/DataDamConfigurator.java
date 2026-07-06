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
package org.jkiss.dbeaver.ui.ai.datadam;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.datadam.DataDamCompletionEngine;
import org.jkiss.dbeaver.model.ai.datadam.DataDamProperties;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.datadam.internal.DataDamUIMessages;
import org.jkiss.dbeaver.ui.ai.model.CachedValue;
import org.jkiss.dbeaver.ui.ai.model.ContextWindowSizeField;
import org.jkiss.dbeaver.ui.ai.model.ModelSelectorField;
import org.jkiss.dbeaver.ui.ai.preferences.AIIObjectPropertyConfigurator;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DataDamConfigurator implements AIIObjectPropertyConfigurator<AIEngineDescriptor, DataDamProperties> {

    private static final String DEFAULT_MODEL = "datadam-fast";

    private String baseUrl = DataDamProperties.DEFAULT_ENDPOINT;
    private volatile String token = "";
    private String temperature = "0.0";

    private Text tokenText;
    private Text baseUrlText;
    private Text temperatureText;
    private ModelSelectorField modelSelectorField;
    private ContextWindowSizeField contextWindowSizeField;

    private final CachedValue<List<AIModel>> modelsCache = new CachedValue<>(this::fetchModels);

    @Override
    public void createControl(
        @NotNull Composite parent,
        AIEngineDescriptor object,
        @NotNull Runnable propertyChangeListener
    ) {
        Composite composite = UIUtils.createComposite(parent, 3);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        tokenText = UIUtils.createLabelText(composite, DataDamUIMessages.datadam_configurator_label_api_key, "", SWT.BORDER | SWT.PASSWORD);
        tokenText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());
        tokenText.addModifyListener(e -> token = tokenText.getText());

        baseUrlText = UIUtils.createLabelText(composite, DataDamUIMessages.datadam_configurator_label_endpoint, "");
        baseUrlText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());
        baseUrlText.addModifyListener(e -> baseUrl = baseUrlText.getText());

        modelSelectorField = ModelSelectorField.builder()
            .withParent(composite)
            .withGridData(new GridData(GridData.FILL_HORIZONTAL))
            .withModelListSupplier((monitor, forceRefresh) -> modelsCache.get(monitor, forceRefresh).stream()
                .map(AIModel::name)
                .toList())
            .withModifyListener(() -> {
            })
            .build();

        contextWindowSizeField = ContextWindowSizeField.builder()
            .withParent(composite)
            .withGridData(GridDataFactory.fillDefaults().span(2, 1).create())
            .build();

        temperatureText = UIUtils.createLabelText(composite, DataDamUIMessages.datadam_configurator_label_temperature, "0.0");
        temperatureText.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());
        temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        temperatureText.addModifyListener(e -> temperature = temperatureText.getText());
    }

    @Override
    public void loadSettings(@NotNull DataDamProperties configuration) {
        baseUrl = CommonUtils.toString(configuration.getBaseUrl(), DataDamProperties.DEFAULT_ENDPOINT);
        token = CommonUtils.toString(configuration.getToken());
        temperature = CommonUtils.toString(configuration.getTemperature(), "0.0");

        baseUrlText.setText(baseUrl);
        tokenText.setText(token);
        temperatureText.setText(temperature);
        modelSelectorField.setSelectedModel(CommonUtils.toString(configuration.getModel(), DEFAULT_MODEL));
        contextWindowSizeField.setValue(configuration.getContextWindowSize());

        modelSelectorField.refreshModelListSilently(false);
    }

    @Override
    public void saveSettings(@NotNull DataDamProperties configuration) {
        configuration.setBaseUrl(baseUrl);
        configuration.setToken(token);
        configuration.setModel(modelSelectorField.getSelectedModel());
        configuration.setContextWindowSize(contextWindowSizeField.getValue());
        configuration.setTemperature(CommonUtils.toDouble(temperature));
    }

    @Override
    public void resetSettings(@NotNull DataDamProperties configuration) {

    }

    @Override
    public boolean isComplete() {
        return tokenText != null
            && !tokenText.getText().isEmpty()
            && contextWindowSizeField.isComplete();
    }

    @NotNull
    @Override
    public Optional<AIEngineProperties> getCurrentProperties() {
        DataDamProperties propertiesCopy = new DataDamProperties();
        propertiesCopy.setBaseUrl(baseUrl);
        propertiesCopy.setToken(token);
        propertiesCopy.setModel(modelSelectorField.getSelectedModel());
        propertiesCopy.setContextWindowSize(contextWindowSizeField.getValue());
        propertiesCopy.setTemperature(CommonUtils.toDouble(temperature));
        return Optional.of(propertiesCopy);
    }

    @NotNull
    private List<AIModel> fetchModels(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (CommonUtils.isEmpty(token)) {
            throw new DBException(DataDamUIMessages.datadam_configurator_error_api_key_not_set);
        }

        DataDamProperties properties = new DataDamProperties();
        properties.setToken(token);
        properties.setBaseUrl(baseUrl);

        try (DataDamCompletionEngine engine = new DataDamCompletionEngine(properties)) {
            return engine.getModels(monitor);
        }
    }
}
