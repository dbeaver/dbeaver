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

    private Text tokenText;

    private Combo modelCombo;
    private Text temperatureText;
    private Text maxTokensText;

    @Override
    protected void performDefaults() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        modelCombo.select(GPTModel.getByName(store.getString(GPTPreferences.GPT_MODEL)).ordinal());
        temperatureText.setText(String.valueOf(store.getDouble(GPTPreferences.GPT_MODEL_TEMPERATURE)));
        maxTokensText.setText(String.valueOf(store.getInt(GPTPreferences.GPT_MODEL_MAX_TOKENS)));

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
        if (!tokenText.getText().equals(store.getString(GPTPreferences.GPT_API_TOKEN))) {
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

                maxTokensText = UIUtils.createLabelText(modelAdvancedGroup,
                    GPTMessages.gpt_preference_page_text_max_tokens,
                    "250"
                );
                maxTokensText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));

                temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
                modelAdvancedGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            }
        }

        performDefaults();

        return placeholder;
    }

    @Override
    public void init(IWorkbench workbench) {

    }
}
