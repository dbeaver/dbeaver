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
import org.jkiss.dbeaver.model.ai.engine.AIEngine;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.engine.LegacyAISettings;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAICompletionEngine;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIModels;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIProperties;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.function.ThrowableFunction;

import java.util.List;
import java.util.Locale;

public class OpenAiConfigurator<ENGINE extends AIEngine, PROPERTIES extends OpenAIProperties>
    implements IObjectPropertyConfigurator<ENGINE, LegacyAISettings<PROPERTIES>> {
    private static final String API_KEY_URL = "https://platform.openai.com/account/api-keys";
    protected volatile String token = "";
    private String temperature = "0.0";
    private boolean logQuery = false;

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
        AIEngine object,
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
    public void loadSettings(@NotNull LegacyAISettings<PROPERTIES> configuration) {
        token = CommonUtils.toString(configuration.getProperties().getToken());
        modelSelectorField.setSelectedModel(
            CommonUtils.toString(configuration.getProperties().getModel(), OpenAIModels.DEFAULT_MODEL)
        );
        temperature = CommonUtils.toString(configuration.getProperties().getTemperature(), "0.0");
        logQuery = CommonUtils.toBoolean(configuration.getProperties().isLoggingEnabled());
        applySettings();

        contextWindowSizeField.setValue(configuration.getProperties().getContextWindowSize());

        modelSelectorField.refreshModelListSilently(false);
    }

    @Override
    public void saveSettings(@NotNull LegacyAISettings<PROPERTIES> configuration) {
        configuration.getProperties().setToken(token);
        configuration.getProperties().setModel(modelSelectorField.getSelectedModel());
        configuration.getProperties().setContextWindowSize(contextWindowSizeField.getValue());
        configuration.getProperties().setTemperature(Double.parseDouble(temperature));
        configuration.getProperties().setLoggingEnabled(logQuery);
    }

    @Override
    public void resetSettings(@NotNull LegacyAISettings<PROPERTIES> openAIPropertiesLegacyAISettings) {

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
                contextWindowSizeField.setValue(OpenAIModels.getContextWindowSize(modelSelectorField.getSelectedModel()));
            }))
            .build();

        contextWindowSizeField = ContextWindowSizeField.builder()
            .withParent(parent)
            .withGridData(GridDataFactory.fillDefaults().span(2, 1).create())
            .build();

        temperatureText = UIUtils.createLabelText(parent, AIUIMessages.gpt_preference_page_text_temperature, "0.0");
        temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        temperatureText.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());

        UIUtils.createInfoLabel(parent, "Lower temperatures give more precise results", GridData.FILL_HORIZONTAL, 3);
        temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        temperatureText.addModifyListener((e) -> temperature = temperatureText.getText());
    }

    private List<AIModel> fetchOpenAiModels(DBRProgressMonitor monitor) throws DBException {
        if (token == null || token.isEmpty()) {
            throw new DBException("Token is not set");
        }

        OpenAIProperties properties = new OpenAIProperties();
        properties.setToken(token);

        try (OpenAICompletionEngine<OpenAIProperties> engine = new OpenAICompletionEngine<>(properties)) {
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

    protected static class CachedValue<T> {
        private volatile T value;

        private final ThrowableFunction<DBRProgressMonitor, T, DBException> supplier;

        protected CachedValue(ThrowableFunction<DBRProgressMonitor, T, DBException> supplier) {
            this.supplier = supplier;
        }

        public T get(DBRProgressMonitor monitor, boolean refresh) throws DBException {
            if (value == null || refresh) {
                synchronized (this) {
                    if (value == null || refresh) {
                        value = supplier.apply(monitor);
                    }
                }
            }
            return value;
        }
    }
}
