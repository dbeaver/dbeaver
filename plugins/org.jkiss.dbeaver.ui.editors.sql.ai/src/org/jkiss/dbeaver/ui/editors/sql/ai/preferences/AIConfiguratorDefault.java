/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.ai.preferences;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AICompletionConstants;
import org.jkiss.dbeaver.model.ai.AIEngineSettings;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.gpt3.GPTCompletionEngine;
import org.jkiss.dbeaver.model.ai.gpt3.GPTConstants;
import org.jkiss.dbeaver.model.ai.gpt3.GPTModel;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.ai.internal.AIUIMessages;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

public class AIConfiguratorDefault implements IObjectPropertyConfigurator<GPTCompletionEngine, AISettings> {

    private static final String API_KEY_URL = "https://platform.openai.com/account/api-keys";

    private Button enableAICheck;

    private Button includeSourceTextInCommentCheck;
//    private Text maxCompletionChoicesText;
    private Button executeQueryImmediatelyCheck;

    private Text tokenText;

    private Combo modelCombo;
    private Text temperatureText;
    private Button logQueryCheck;
    private ExpansionAdapter expansionAdapter;

    @Override
    public void createControl(@NotNull Composite placeholder, GPTCompletionEngine object, @NotNull Runnable propertyChangeListener) {
        ScrolledComposite scrolledComposite = UIUtils.createScrolledComposite(placeholder);

        final Composite composite = new Composite(scrolledComposite, SWT.NONE);
        final GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.widthHint = UIUtils.getFontHeight(composite) * 80;
        composite.setLayoutData(gridData);
        composite.setLayout(new GridLayout(1, false));

        scrolledComposite.setContent(composite);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);

        enableAICheck = UIUtils.createCheckbox(
            composite,
            AIUIMessages.gpt_preference_page_checkbox_enable_ai_label,
            AIUIMessages.gpt_preference_page_checkbox_enable_ai_tip,
            false,
            2);

        {
            Group authorizationGroup = UIUtils.createControlGroup(composite,
                AIUIMessages.gpt_preference_page_group_authorization,
                2,
                SWT.NONE,
                5
            );
            tokenText = UIUtils.createLabelText(authorizationGroup, AIUIMessages.gpt_preference_page_selector_token,
                "", SWT.BORDER | SWT.PASSWORD);
            tokenText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            Link link = UIUtils.createLink(
                authorizationGroup,
                NLS.bind(AIUIMessages.gpt_preference_page_token_info, API_KEY_URL),
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        UIUtils.openWebBrowser(API_KEY_URL);
                    }
                }
                );
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            link.setLayoutData(gd);
            authorizationGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }
        {
            Composite settingsPanel = UIUtils.createComposite(composite, 2);
            settingsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Group completionGroup = UIUtils.createControlGroup(settingsPanel,
                AIUIMessages.gpt_preference_page_completion_group,
                2,
                SWT.NONE,
                5
            );
            completionGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            includeSourceTextInCommentCheck = UIUtils.createCheckbox(
                completionGroup,
                AIUIMessages.gpt_preference_page_completion_include_source_label,
                AIUIMessages.gpt_preference_page_completion_include_source_tip,
                false,
                2);
            executeQueryImmediatelyCheck = UIUtils.createCheckbox(
                completionGroup,
                AIUIMessages.gpt_preference_page_completion_execute_immediately_label,
                AIUIMessages.gpt_preference_page_completion_execute_immediately_tip,
                false,
                2);

            createCompletionSettings(completionGroup, propertyChangeListener);

            createFormattingSettings(settingsPanel, propertyChangeListener);

            createProxySettings(composite, propertyChangeListener);

        }

        {
            expansionAdapter = new ExpansionAdapter() {
                @Override
                public void expansionStateChanged(ExpansionEvent e) {
                    scrolledComposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                    UIUtils.resizeShell(placeholder.getShell());
                }
            };
            {
                final Composite modelGroup = createExpandable(
                    composite,
                    AIUIMessages.gpt_preference_page_group_model,
                    expansionAdapter
                );
                modelCombo = UIUtils.createLabelCombo(modelGroup,
                    AIUIMessages.gpt_preference_page_combo_engine,
                    SWT.READ_ONLY
                );
                for (GPTModel model : GPTModel.values()) {
                    modelCombo.add(model.getName());
                }
                UIUtils.createInfoLabel(modelGroup,
                    AIUIMessages.gpt_preference_page_info_model,
                    GridData.FILL_HORIZONTAL,
                    2
                );
            }

            {
                final Composite modelAdvancedGroup = createExpandable(
                    composite,
                    AIUIMessages.gpt_preference_page_group_model_advanced, expansionAdapter
                );
                temperatureText = UIUtils.createLabelText(modelAdvancedGroup,
                    AIUIMessages.gpt_preference_page_text_temperature,
                    "0.0"
                );
                temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
                UIUtils.createInfoLabel(modelAdvancedGroup, "Lower temperatures give more precise results", GridData.FILL_HORIZONTAL, 2);

                temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));

                logQueryCheck = UIUtils.createCheckbox(
                    modelAdvancedGroup,
                    "Write GPT queries to debug log",
                    "Write GPT queries with metadata info in debug logs",
                    false,
                    2);

                createAdvancedSettings(modelAdvancedGroup, propertyChangeListener);
            }

        }
    }

    @NotNull
    protected final Composite createExpandable(
        @NotNull Composite composite,
        @NotNull String text,
        @Nullable ExpansionAdapter expansionAdapter
    ) {
        GridLayout layout = new GridLayout(2, false);

        final ExpandableComposite expandable = new ExpandableComposite(composite, SWT.CHECK);
        expandable.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        expandable.setText(text);

        final Composite modelGroup = new Composite(expandable, SWT.BORDER);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.widthHint = 5;
        modelGroup.setLayoutData(gridData);
        modelGroup.setLayout(layout);
        expandable.setClient(modelGroup);

        if (expansionAdapter != null) {
            expandable.addExpansionListener(expansionAdapter);
        }
        return modelGroup;
    }

    protected void createCompletionSettings(Group group, Runnable propertyChangeListener) {

    }

    protected void createFormattingSettings(Composite settingsPanel, Runnable propertyChangeListener) {
        UIUtils.createEmptyLabel(settingsPanel, 1, 1);

    }

    protected void createProxySettings(Composite settingsPanel, Runnable propertyChangeListener) {

    }

    protected void createAdvancedSettings(Composite group, Runnable propertyChangeListener) {

    }

    @Override
    public void loadSettings(@NotNull AISettings aiSettings) {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        AISettings settings = AISettings.getSettings();
        AIEngineSettings openAiSettings = settings.getEngineConfiguration(GPTConstants.OPENAI_ENGINE);

        enableAICheck.setSelection(!settings.isAiDisabled());

        includeSourceTextInCommentCheck.setSelection(store.getBoolean(AICompletionConstants.AI_INCLUDE_SOURCE_TEXT_IN_QUERY_COMMENT));
//        maxCompletionChoicesText.setText(store.getString(AICompletionConstants.AI_COMPLETION_MAX_CHOICES));
        executeQueryImmediatelyCheck.setSelection(store.getBoolean(AICompletionConstants.AI_COMPLETION_EXECUTE_IMMEDIATELY));

        modelCombo.select(GPTModel.getByName(store.getString(GPTConstants.GPT_MODEL)).ordinal());
        temperatureText.setText(String.valueOf(store.getDouble(GPTConstants.GPT_MODEL_TEMPERATURE)));
        logQueryCheck.setSelection(store.getBoolean(GPTConstants.GPT_LOG_QUERY));

        String secretValue = CommonUtils.toString(openAiSettings.getProperties().get(GPTConstants.GPT_API_TOKEN), null);
        if (secretValue == null) {
            secretValue = DBWorkbench.getPlatform().getPreferenceStore().getString(GPTConstants.GPT_API_TOKEN);
        }
        tokenText.setText(secretValue == null ? "" : secretValue);
    }

    @Override
    public void saveSettings(@NotNull AISettings settings) {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        settings.setAiDisabled(!enableAICheck.getSelection());

        AIEngineSettings engineConfiguration = settings.getEngineConfiguration(GPTConstants.OPENAI_ENGINE);
        engineConfiguration.setEngineEnabled(enableAICheck.getSelection());

        store.setValue(AICompletionConstants.AI_INCLUDE_SOURCE_TEXT_IN_QUERY_COMMENT, includeSourceTextInCommentCheck.getSelection());
        store.setValue(AICompletionConstants.AI_COMPLETION_EXECUTE_IMMEDIATELY, executeQueryImmediatelyCheck.getSelection());
//        store.setValue(AICompletionConstants.AI_COMPLETION_MAX_CHOICES, maxCompletionChoicesText.getText());

        store.setValue(GPTConstants.GPT_MODEL, modelCombo.getText());
        store.setValue(GPTConstants.GPT_MODEL_TEMPERATURE, temperatureText.getText());
        store.setValue(GPTConstants.GPT_LOG_QUERY, logQueryCheck.getSelection());

        if (!modelCombo.getText().equals(store.getString(GPTConstants.GPT_MODEL)) ||
            !tokenText.getText().equals(store.getString(GPTConstants.GPT_API_TOKEN))
        ) {
            GPTCompletionEngine.resetServices();
        }
        store.setToDefault(GPTConstants.GPT_API_TOKEN);
        engineConfiguration.getProperties().put(GPTConstants.GPT_API_TOKEN, tokenText.getText());
    }

    @Override
    public void resetSettings(@NotNull AISettings settings) {

    }

    @Override
    public boolean isComplete() {
        return !CommonUtils.isEmpty(tokenText.getText());
    }

    @NotNull
    public ExpansionAdapter getExpansionAdapter() {
        return expansionAdapter;
    }
}
