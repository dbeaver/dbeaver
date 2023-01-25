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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.GPTPreferences;
import org.jkiss.dbeaver.model.ai.client.GPTClient;
import org.jkiss.dbeaver.model.ai.internal.GPTModel;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.ai.internal.GPTMessages;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.util.Locale;

public class GPTPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {
    private static final Log log = Log.getLog(GPTPreferencePage.class);
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.gpt";
    private static final String API_KEY_URL = "https://beta.openai.com/account/api-keys";

    private Text tokenText;

    private Combo modelCombo;
    private Text temperatureText;
    private Text maxTokensText;
    private Button executeQueryImmediately;
    private Button logQueryCheck;
    private Text maxTablesText;

    @Override
    protected void performDefaults() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        modelCombo.select(GPTModel.getByName(store.getString(GPTPreferences.GPT_MODEL)).ordinal());
        temperatureText.setText(String.valueOf(store.getDouble(GPTPreferences.GPT_MODEL_TEMPERATURE)));
        maxTokensText.setText(String.valueOf(store.getInt(GPTPreferences.GPT_MODEL_MAX_TOKENS)));
        executeQueryImmediately.setSelection(store.getBoolean(GPTPreferences.GPT_EXECUTE_IMMEDIATELY));
        logQueryCheck.setSelection(store.getBoolean(GPTPreferences.GPT_LOG_QUERY));
        maxTablesText.setText(String.valueOf(store.getInt(GPTPreferences.GPT_MAX_TABLES)));

        String secretValue = DBWorkbench.getPlatform().getPreferenceStore()
            .getString(GPTPreferences.GPT_API_TOKEN);
        tokenText.setText(secretValue == null ? "" : secretValue);
    }

    @Override
    public boolean performOk() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        store.setValue(GPTPreferences.GPT_MODEL, modelCombo.getText());
        store.setValue(GPTPreferences.GPT_MODEL_TEMPERATURE, temperatureText.getText());
        store.setValue(GPTPreferences.GPT_MODEL_MAX_TOKENS, maxTokensText.getText());
        store.setValue(GPTPreferences.GPT_EXECUTE_IMMEDIATELY, executeQueryImmediately.getSelection());
        store.setValue(GPTPreferences.GPT_LOG_QUERY, logQueryCheck.getSelection());
        store.setValue(GPTPreferences.GPT_MAX_TABLES, maxTablesText.getText());

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
        Composite checkboxComposite = UIUtils.createPlaceholder(placeholder, 2);
        checkboxComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        placeholder.setLayoutData(new GridData(GridData.FILL_BOTH));
        {
            Group authorizationGroup = UIUtils.createControlGroup(placeholder,
                GPTMessages.gpt_preference_page_group_authorization,
                2,
                SWT.NONE,
                5
            );
            tokenText = UIUtils.createLabelText(authorizationGroup, GPTMessages.gpt_preference_page_selector_token,
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
            Group modelGroup = UIUtils.createControlGroup(placeholder,
                GPTMessages.gpt_preference_page_group_model,
                2,
                SWT.NONE,
                5
            );
            modelGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            modelCombo = UIUtils.createLabelCombo(modelGroup,
                GPTMessages.gpt_preference_page_combo_engine,
                SWT.READ_ONLY
            );
            for (GPTModel model : GPTModel.values()) {
                modelCombo.add(model.getName());
            }
            UIUtils.createInfoLabel(modelGroup, "code-davinci model suits the best for SQL code completion", GridData.FILL_HORIZONTAL, 2);
            {
                Group modelAdvancedGroup = UIUtils.createControlGroup(placeholder,
                    GPTMessages.gpt_preference_page_group_model_advanced,
                    2,
                    SWT.NONE,
                    5
                );

                temperatureText = UIUtils.createLabelText(modelAdvancedGroup,
                    GPTMessages.gpt_preference_page_text_temperature,
                    "0.0"
                );
                temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
                UIUtils.createInfoLabel(modelAdvancedGroup, "Lower temperatures give more precise results", GridData.FILL_HORIZONTAL, 2);

                maxTokensText = UIUtils.createLabelText(modelAdvancedGroup,
                    GPTMessages.gpt_preference_page_text_max_tokens,
                    "250"
                );
                maxTokensText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));

                temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
                modelAdvancedGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                maxTablesText = UIUtils.createLabelText(modelAdvancedGroup, GPTMessages.gpt_preference_page_text_max_tables, null);
                executeQueryImmediately = UIUtils.createCheckbox(
                    modelAdvancedGroup,
                    "Execute SQL immediately",
                    "Try to execute translated SQL immediately after completion",
                    false,
                    2);
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
