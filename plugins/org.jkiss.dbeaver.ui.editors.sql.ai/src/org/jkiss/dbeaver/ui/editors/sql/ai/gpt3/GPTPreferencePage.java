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
package org.jkiss.dbeaver.ui.editors.sql.ai.gpt3;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AICompletionConstants;
import org.jkiss.dbeaver.model.ai.gpt3.GPTClient;
import org.jkiss.dbeaver.model.ai.gpt3.GPTModel;
import org.jkiss.dbeaver.model.ai.gpt3.GPTPreferences;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.util.Locale;

public class GPTPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {
    private static final Log log = Log.getLog(GPTPreferencePage.class);
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.gpt";
    private static final String API_KEY_URL = "https://beta.openai.com/account/api-keys";

    private Button enableAICheck;

    private Button includeSourceTextInCommentCheck;
//    private Text maxCompletionChoicesText;
    private Button executeQueryImmediatelyCheck;

    private Text tokenText;

    private Combo modelCombo;
    private Text temperatureText;
    private Button logQueryCheck;

    @Override
    protected void performDefaults() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        enableAICheck.setSelection(!store.getBoolean(AICompletionConstants.AI_DISABLED));

        includeSourceTextInCommentCheck.setSelection(store.getBoolean(AICompletionConstants.AI_INCLUDE_SOURCE_TEXT_IN_QUERY_COMMENT));
//        maxCompletionChoicesText.setText(store.getString(AICompletionConstants.AI_COMPLETION_MAX_CHOICES));
        executeQueryImmediatelyCheck.setSelection(store.getBoolean(AICompletionConstants.AI_COMPLETION_EXECUTE_IMMEDIATELY));

        modelCombo.select(GPTModel.getByName(store.getString(GPTPreferences.GPT_MODEL)).ordinal());
        temperatureText.setText(String.valueOf(store.getDouble(GPTPreferences.GPT_MODEL_TEMPERATURE)));
        logQueryCheck.setSelection(store.getBoolean(GPTPreferences.GPT_LOG_QUERY));

        String secretValue = DBWorkbench.getPlatform().getPreferenceStore()
            .getString(GPTPreferences.GPT_API_TOKEN);
        tokenText.setText(secretValue == null ? "" : secretValue);
    }

    @Override
    public boolean performOk() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        store.setValue(AICompletionConstants.AI_DISABLED, !enableAICheck.getSelection());

        store.setValue(AICompletionConstants.AI_INCLUDE_SOURCE_TEXT_IN_QUERY_COMMENT, includeSourceTextInCommentCheck.getSelection());
        store.setValue(AICompletionConstants.AI_COMPLETION_EXECUTE_IMMEDIATELY, executeQueryImmediatelyCheck.getSelection());
//        store.setValue(AICompletionConstants.AI_COMPLETION_MAX_CHOICES, maxCompletionChoicesText.getText());

        store.setValue(GPTPreferences.GPT_MODEL, modelCombo.getText());
        store.setValue(GPTPreferences.GPT_MODEL_TEMPERATURE, temperatureText.getText());
        store.setValue(GPTPreferences.GPT_LOG_QUERY, logQueryCheck.getSelection());

        if (!modelCombo.getText().equals(store.getString(GPTPreferences.GPT_MODEL)) ||
            !tokenText.getText().equals(store.getString(GPTPreferences.GPT_API_TOKEN))
        ) {
            GPTClient.resetServices();
        }
        store.setValue(GPTPreferences.GPT_API_TOKEN, tokenText.getText());
        return true;
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        placeholder.setLayoutData(new GridData(GridData.FILL_BOTH));

        enableAICheck = UIUtils.createCheckbox(
            placeholder,
            "Enable smart completion",
            "Enable AI smart completion. If you don't want to see it in SQL editor then you can disable this feature.",
            false,
            2);

        {
            Group authorizationGroup = UIUtils.createControlGroup(placeholder,
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
                "Copy-paste API token from <a>" + API_KEY_URL + "</a>",
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        UIUtils.openWebBrowser(API_KEY_URL);
                    }
                });
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            link.setLayoutData(gd);
            authorizationGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }
        {
            Group completionGroup = UIUtils.createControlGroup(placeholder,
                "Completion",
                2,
                SWT.NONE,
                5
            );
            completionGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            includeSourceTextInCommentCheck = UIUtils.createCheckbox(
                completionGroup,
                "Include source in query comment",
                "Add your human language text in query comment",
                false,
                2);
            executeQueryImmediatelyCheck = UIUtils.createCheckbox(
                completionGroup,
                "Execute SQL immediately",
                "Try to execute translated SQL immediately after completion",
                false,
                2);
//            maxCompletionChoicesText = UIUtils.createLabelText(
//                completionGroup, "Completion choices number", null);
//            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
//            gd.widthHint = UIUtils.getFontHeight(maxCompletionChoicesText) * 5;
//            maxCompletionChoicesText.setLayoutData(gd);
        }
        {
            Group modelGroup = UIUtils.createControlGroup(placeholder,
                AIUIMessages.gpt_preference_page_group_model,
                2,
                SWT.NONE,
                5
            );
            modelGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            modelCombo = UIUtils.createLabelCombo(modelGroup,
                AIUIMessages.gpt_preference_page_combo_engine,
                SWT.READ_ONLY
            );
            for (GPTModel model : GPTModel.values()) {
                modelCombo.add(model.getName());
            }
            UIUtils.createInfoLabel(modelGroup, "code-davinci model suits the best for SQL code completion", GridData.FILL_HORIZONTAL, 2);
            {
                Group modelAdvancedGroup = UIUtils.createControlGroup(placeholder,
                    AIUIMessages.gpt_preference_page_group_model_advanced,
                    2,
                    SWT.NONE,
                    5
                );

                temperatureText = UIUtils.createLabelText(modelAdvancedGroup,
                    AIUIMessages.gpt_preference_page_text_temperature,
                    "0.0"
                );
                temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
                UIUtils.createInfoLabel(modelAdvancedGroup, "Lower temperatures give more precise results", GridData.FILL_HORIZONTAL, 2);

                temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
                modelAdvancedGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                logQueryCheck = UIUtils.createCheckbox(
                    modelAdvancedGroup,
                    "Write GPT queries to debug log",
                    "Write GPT queries with metadata info in debug logs",
                    false,
                    2);
            }
        }

        performDefaults();

        return placeholder;
    }

    @Override
    public void init(IWorkbench workbench) {

    }
}
