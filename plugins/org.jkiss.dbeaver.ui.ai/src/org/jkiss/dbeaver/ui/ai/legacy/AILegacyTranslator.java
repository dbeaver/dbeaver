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
package org.jkiss.dbeaver.ui.ai.legacy;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.AIAssistant;
import org.jkiss.dbeaver.model.ai.AICompletionSettings;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AITranslateRequest;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.registry.AIAssistantRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsRegistry;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.qm.QMTranslationHistoryItem;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.AIUIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIFeatures;
import org.jkiss.dbeaver.ui.ai.preferences.AIPreferencePageMain;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class AILegacyTranslator {

    public void performAiTranslation(ExecutionEvent event) {
        // CE legacy popup
        AIFeatures.SQL_AI_POPUP.use();

        if (AISettingsRegistry.getInstance().getSettings().isAiDisabled()) {
            return;
        }
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        if (editor == null) {
            return;
        }
        DBPDataSourceContainer dataSourceContainer = editor.getDataSourceContainer();
        if (dataSourceContainer == null) {
            DBWorkbench.getPlatformUI().showError("No datasource", "Connection must be associated with the SQL script");
            return;
        }
        DBCExecutionContext executionContext = editor.getExecutionContext();
        if (executionContext == null) {
            DBWorkbench.getPlatformUI().showError("No connection", "You must connect to the database before performing completion");
            return;
        }

        try {
            AIAssistant aiAssistant = AIAssistantRegistry.getInstance().createAssistant(dataSourceContainer.getProject().getWorkspace());
            if (!aiAssistant.hasValidConfiguration()) {
                UIUtils.showPreferencesFor(
                    editor.getSite().getShell(),
                    AISettingsRegistry.getInstance().getSettings(),
                    AIPreferencePageMain.PAGE_ID
                );
                return;
            }

            AICompletionSettings settings = new AICompletionSettings(dataSourceContainer);

            // Show info transfer warning
            if (!AIUIUtils.confirmMetaTransfer(settings)) {
                return;
            }

            DBSLogicalDataSource lDataSource = DBSLogicalDataSource.createLogicalDataSource(dataSourceContainer, executionContext);

            AISuggestionPopup aiCompletionPopup = new AISuggestionPopup(
                HandlerUtil.getActiveShell(event),
                "AI smart completion",
                lDataSource,
                executionContext,
                settings
            );
            if (aiCompletionPopup.open() == IDialogConstants.OK_ID) {
                doAutoCompletion(executionContext, lDataSource, editor, aiCompletionPopup);
            }
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("AI error", "Cannot determine AI engine", e);
        }
    }

    private void doAutoCompletion(
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSLogicalDataSource dataSource,
        @NotNull SQLEditor editor,
        @NotNull AISuggestionPopup popup
    ) {
        String userInput = popup.getInputText();

        try {
            String sql = translateUserInputIntoSql(
                userInput,
                dataSource,
                executionContext,
                popup
            );

            if (sql == null || sql.isEmpty()) {
                DBWorkbench.getPlatformUI().showError("AI error", "No smart completions returned");
                return;
            }

            InMemoryHistoryManager.saveTranslationHistory(dataSource, new QMTranslationHistoryItem(userInput, sql));

            insertSqlCompletion(editor, sql);
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Auto completion error", null, e.getTargetException());
            return;
        }

        AIFeatures.SQL_AI_GENERATE_PROPOSALS.use(Map.of(
            "driver", dataSource.getDataSourceContainer().getDriver().getPreconfiguredId(),
            "scope", popup.getScope().name()
        ));

        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AIConstants.AI_COMPLETION_EXECUTE_IMMEDIATELY)) {
            editor.processSQL(false, false);
        }
    }

    @Nullable
    private String translateUserInputIntoSql(
        @NotNull String userInput,
        @NotNull DBSLogicalDataSource dataSource,
        @NotNull DBCExecutionContext executionContext,
        @NotNull AISuggestionPopup popup
    ) throws InvocationTargetException {
        if (CommonUtils.isEmptyTrimmed(userInput)) {
            return null;
        }

        AtomicReference<String> sql = new AtomicReference<>();
        UIUtils.runInProgressDialog(monitor -> {
            try {
                final AIDatabaseContext context = new AIDatabaseContext.Builder(dataSource)
                    .setScope(popup.getScope())
                    .setCustomEntities(popup.getCustomEntities(monitor))
                    .setExecutionContext(executionContext)
                    .build();

                AITranslateRequest daiTranslateRequest = new AITranslateRequest(userInput, context);
                DBPWorkspace workspace = executionContext.getDataSource().getContainer().getProject().getWorkspace();
                AIAssistant aiAssistant = AIAssistantRegistry.getInstance().createAssistant(workspace);
                sql.set(aiAssistant.translateTextToSql(monitor, daiTranslateRequest));
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        });

        return sql.get();
    }

    private void insertSqlCompletion(
        SQLEditor editor,
        String completion
    ) {
        ISelection selection = editor.getSelectionProvider().getSelection();
        IDocument document = editor.getDocument();
        if (document != null && selection instanceof TextSelection) {
            try {
                int offset = ((TextSelection) selection).getOffset();
                int length = ((TextSelection) selection).getLength();
                SQLScriptElement query = editor.extractQueryAtPos(offset);
                String text = completion;
                if (query != null) {
                    offset = query.getOffset();
                    length = query.getLength();
                    // Trim trailing semicolon if needed
                    if (length > 0 && !query.getText().endsWith(";") && !text.isEmpty()) {
                        if (text.charAt(text.length() - 1) == ';') {
                            text = text.substring(0, text.length() - 1);
                        }
                    }
                }
                document.replace(offset, length, text);
                editor.getSelectionProvider().setSelection(new TextSelection(offset + text.length(), 0));
            } catch (BadLocationException e) {
                DBWorkbench.getPlatformUI().showError("Insert SQL", "Error inserting SQL completion in text editor", e);
            }
        }
    }
}
