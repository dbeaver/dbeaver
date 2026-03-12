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
package org.jkiss.dbeaver.ui.ai.preferences;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

public class AIPreferencePageMain extends AbstractPrefPage implements IWorkbenchPreferencePage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.ai";

    private final AISettings settings;
    private Button enableAICheck;

    public AIPreferencePageMain() {
        this.settings = AISettingsManager.getInstance().getSettings();
    }

    @Override
    public void init(IWorkbench workbench) {

    }

    @Override
    protected void performDefaults() {
        if (!hasAccessToPage()) {
            return;
        }
        enableAICheck.setSelection(!this.settings.isAiDisabled());
    }

    @Override
    public boolean performOk() {
        if (!hasAccessToPage()) {
            return false;
        }
        this.settings.setAiDisabled(!enableAICheck.getSelection());

        AISettingsManager.getInstance().saveSettings(this.settings);

        return true;
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);

        Composite groupObjects = UIUtils.createTitledComposite(
            composite,
            "AI settings",
            2,
            GridData.HORIZONTAL_ALIGN_BEGINNING);

        enableAICheck = UIUtils.createCheckbox(
            groupObjects,
            AIUIMessages.gpt_preference_page_checkbox_enable_ai_label,
            AIUIMessages.gpt_preference_page_checkbox_enable_ai_tip,
            !this.settings.isAiDisabled(),
            2);

        Composite links = UIUtils.createTitledComposite(
            composite,
            "Configuration",
            1,
            GridData.HORIZONTAL_ALIGN_BEGINNING);

        // Link to secure storage config
        addLinkToSettings(links, AIPreferencePageEngines.PAGE_ID);
        addLinkToSettings(links, AIPreferencePageConfiguration.PAGE_ID);
        addLinkToSettings(links, AIPreferencePagePrompts.PAGE_ID);

        return composite;
    }

    private void addLinkToSettings(Composite composite, String pageID) {
        if (getContainer() instanceof IWorkbenchPreferenceContainer wpc) {
            UIUtils.createPreferenceLink(
                composite,
                "<a>''{0}''</a> " + CoreMessages.pref_page_ui_general_label_settings,
                pageID,
                wpc,
                null
            );
        }
    }

}
