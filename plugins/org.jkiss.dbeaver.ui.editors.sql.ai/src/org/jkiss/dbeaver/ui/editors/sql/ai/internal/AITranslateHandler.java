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
package org.jkiss.dbeaver.ui.editors.sql.ai.internal;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionContext;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionSettings;
import org.jkiss.dbeaver.model.ai.completion.DAITranslateRequest;
import org.jkiss.dbeaver.model.ai.utils.InMemoryHistoryManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.qm.QMTranslationHistoryManager;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.ai.AIUIUtils;
import org.jkiss.dbeaver.ui.editors.sql.ai.popup.AISuggestionPopup;
import org.jkiss.dbeaver.ui.editors.sql.ai.preferences.AIPreferencePage;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class AITranslateHandler extends AbstractHandler {

    public AITranslateHandler() throws DBException {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        AIFeatures.SQL_AI_POPUP.use();

        if (AISettingsRegistry.getInstance().getSettings().isAiDisabled()) {
            return null;
        }
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);

        DBPDataSourceContainer dataSourceContainer = editor.getDataSourceContainer();
        if (dataSourceContainer == null) {
            DBWorkbench.getPlatformUI().showError("No datasource", "Connection must be associated with the SQL script");
            return null;
        }

        AIAssistant aiAssistant = AIAssistantRegistry.getInstance().getAssistant();

        try {
            if (!aiAssistant.hasValidConfiguration()) {
                UIUtils.showPreferencesFor(editor.getSite().getShell(), null, AIPreferencePage.PAGE_ID);
                return null;
            }
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("AI error", "Cannot determine AI engine", e);
            return null;
        }

        DBCExecutionContext executionContext = editor.getExecutionContext();
        if (executionContext == null) {
            DBWorkbench.getPlatformUI().showError("No connection", "You must connect to the database before performing completion");
            return null;
        }

        DAICompletionSettings settings = new DAICompletionSettings(dataSourceContainer);

        // Show info transfer warning
        if (!AIUIUtils.confirmMetaTransfer(settings, dataSourceContainer)) {
            return null;
        }

        QMTranslationHistoryManager historyManager = GeneralUtils.adapt(AISuggestionPopup.class, QMTranslationHistoryManager.class);
        if (historyManager == null) {
            historyManager = new InMemoryHistoryManager();
        }
        DBSLogicalDataSource lDataSource = new DBSLogicalDataSource(dataSourceContainer, "AI logical wrapper", null);
        DBCExecutionContextDefaults<?,?> contextDefaults = executionContext.getContextDefaults();
        if (contextDefaults != null) {
            if (contextDefaults.getDefaultCatalog() != null) {
                lDataSource.setCurrentCatalog(contextDefaults.getDefaultCatalog().getName());
            }
            if (contextDefaults.getDefaultSchema() != null) {
                lDataSource.setCurrentSchema(contextDefaults.getDefaultSchema().getName());
            }
        }

        AISuggestionPopup aiCompletionPopup = new AISuggestionPopup(
            HandlerUtil.getActiveShell(event),
            "AI smart completion",
            historyManager,
            lDataSource,
            executionContext,
            settings
        );
        if (aiCompletionPopup.open() == IDialogConstants.OK_ID) {
            try {
                if (!aiAssistant.hasValidConfiguration()) {
                    DBWorkbench.getPlatformUI()
                        .showError("Bad AI engine configuration", "You must specify OpenAI API token in preferences");
                    return null;
                }
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("AI error", "Cannot determine AI engine", e);
                return null;
            }

            doAutoCompletion(executionContext, historyManager, lDataSource, editor, aiCompletionPopup);
        }
        return null;
    }

    private void doAutoCompletion(
        DBCExecutionContext executionContext,
        QMTranslationHistoryManager historyManager,
        DBSLogicalDataSource dataSource,
        SQLEditor editor,
        @NotNull AISuggestionPopup popup
    ) {
        String userInput = popup.getInputText();

        try {
            String sql = translateUserInputIntoSql(
                userInput,
                executionContext,
                dataSource,
                popup
            );

            if (sql == null || sql.isEmpty()) {
                DBWorkbench.getPlatformUI().showError("AI error", "No smart completions returned");
                return;
            }

            saveToHistory(historyManager, dataSource, executionContext, userInput, sql);
            insertSqlCompletion(editor, sql);
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Auto completion error", null, e.getTargetException());
            return;
        }

        AIFeatures.SQL_AI_GENERATE_PROPOSALS.use(Map.of(
            "driver", dataSource.getDataSourceContainer().getDriver().getPreconfiguredId(),
            "scope", popup.getScope().name()
        ));

        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AICompletionConstants.AI_COMPLETION_EXECUTE_IMMEDIATELY)) {
            editor.processSQL(false, false);
        }
    }

    @Nullable
    private String translateUserInputIntoSql(
        String userInput,
        DBCExecutionContext executionContext,
        DBSLogicalDataSource dataSource,
        @NotNull AISuggestionPopup popup
    ) throws InvocationTargetException {
        if (CommonUtils.isEmptyTrimmed(userInput)) {
            return null;
        }

        AtomicReference<String> sql = new AtomicReference<>();
        UIUtils.runInProgressDialog(monitor -> {
            try {
                final DAICompletionContext context = new DAICompletionContext.Builder()
                    .setScope(popup.getScope())
                    .setCustomEntities(popup.getCustomEntities(monitor))
                    .setDataSource(dataSource)
                    .setExecutionContext(executionContext)
                    .build();

                DAITranslateRequest daiTranslateRequest = new DAITranslateRequest(userInput, context);
                AIAssistant aiAssistant = AIAssistantRegistry.getInstance().getAssistant();
                sql.set(aiAssistant.translateTextToSql(monitor, daiTranslateRequest));
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        });

        return sql.get();
    }

    private void saveToHistory(
        QMTranslationHistoryManager historyManager,
        DBSLogicalDataSource dataSource,
        DBCExecutionContext executionContext,
        String userInput,
        String completion
    ) {
        new AbstractJob("Save smart completion history") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    historyManager.saveTranslationHistory(
                        monitor,
                        dataSource,
                        executionContext,
                        userInput,
                        completion
                    );
                } catch (DBException e) {
                    return GeneralUtils.makeExceptionStatus(e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
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
