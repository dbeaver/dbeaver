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
package org.jkiss.dbeaver.ui.ai.format;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AISchemaGenerator;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;

public class AIChatConfigurator implements IObjectPropertyConfigurator<AISchemaGenerator, AISettings> {
    private Button useStreamModeCheck;
    private Button executeInNewConsoleCheck;
    private Button includeSourceTextInCommentCheck;

    @Override
    public void createControl(
        @NotNull Composite parent,
        AISchemaGenerator object,
        @NotNull Runnable propertyChangeListener
    ) {
        Composite composite = UIUtils.createComposite(parent, 1);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite chatGroup = UIUtils.createTitledComposite(
            composite,
            AIUIMessages.gpt_preference_page_chat_group,
            2,
            GridData.FILL_HORIZONTAL
        );
        chatGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL));
        createChatGroupSettings(chatGroup);

        Composite completionGroup = UIUtils.createTitledComposite(
            composite,
            AIUIMessages.gpt_preference_page_completion_group,
            2,
            GridData.FILL_HORIZONTAL
        );
        completionGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL));
        createCompletionGroupSettings(completionGroup);

        createStorageGroup(composite);
    }

    protected void createStorageGroup(@NotNull Composite composite) {
    }

    protected void createChatGroupSettings(@NotNull Composite chatGroup) {
        useStreamModeCheck = UIUtils.createCheckbox(
            chatGroup,
            AIUIMessages.gpt_preference_page_chat_use_stream_mode_label,
            AIUIMessages.gpt_preference_page_chat_use_stream_mode_tip,
            false,
            2);
        executeInNewConsoleCheck = UIUtils.createCheckbox(
            chatGroup,
            AIUIMessages.gpt_preference_page_chat_execute_in_new_console_label,
            AIUIMessages.gpt_preference_page_chat_execute_in_new_console_tip,
            false,
            2);
    }

    protected void createCompletionGroupSettings(@NotNull Composite completionGroup) {
        includeSourceTextInCommentCheck = UIUtils.createCheckbox(
            completionGroup,
            AIUIMessages.gpt_preference_page_completion_include_source_label,
            AIUIMessages.gpt_preference_page_completion_include_source_tip,
            false,
            2);
    }

    @Override
    public void loadSettings(@NotNull AISettings aiSettings) {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        useStreamModeCheck.setSelection(store.getBoolean(AIConstants.AI_USE_STREAM_MODE));
        executeInNewConsoleCheck.setSelection(store.getBoolean(AIConstants.AI_CHAT_EXECUTE_IN_NEW_CONSOLE));
        includeSourceTextInCommentCheck.setSelection(store.getBoolean(AIConstants.AI_INCLUDE_SOURCE_TEXT_IN_QUERY_COMMENT));
    }

    @Override
    public void saveSettings(@NotNull AISettings aiSettings) {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        store.setValue(AIConstants.AI_USE_STREAM_MODE, useStreamModeCheck.getSelection());
        store.setValue(AIConstants.AI_CHAT_EXECUTE_IN_NEW_CONSOLE, executeInNewConsoleCheck.getSelection());
        store.setValue(AIConstants.AI_INCLUDE_SOURCE_TEXT_IN_QUERY_COMMENT, includeSourceTextInCommentCheck.getSelection());
    }

    @Override
    public void resetSettings(@NotNull AISettings aiSettings) {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        store.setToDefault(AIConstants.AI_USE_STREAM_MODE);
        store.setToDefault(AIConstants.AI_CHAT_EXECUTE_IN_NEW_CONSOLE);
        store.setToDefault(AIConstants.AI_INCLUDE_SOURCE_TEXT_IN_QUERY_COMMENT);
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
