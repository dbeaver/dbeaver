/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.completion.*;
import org.jkiss.dbeaver.model.ai.translator.SimpleFilterManager;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AITranslateHandler extends AbstractHandler {

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

        DAICompletionEngine engine;
        try {
            engine = AIEngineRegistry.getInstance().getCompletionEngine(AISettingsRegistry.getInstance().getSettings().getActiveEngine());
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("AI error", "Cannot determine AI engine", e);
            return null;
        }

        if (!engine.isValidConfiguration()) {
            UIUtils.showPreferencesFor(editor.getSite().getShell(), null, AIPreferencePage.PAGE_ID);
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
            historyManager = new SimpleFilterManager();
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
                engine = AIEngineRegistry.getInstance().getCompletionEngine(AISettingsRegistry.getInstance().getSettings().getActiveEngine());
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("AI error", "Cannot determine AI engine", e);
                return null;
            }
            if (!engine.isValidConfiguration()) {
                DBWorkbench.getPlatformUI().showError("Bad AI engine configuration", "You must specify OpenAI API token in preferences");
                return null;
            }

            doAutoCompletion(executionContext, historyManager, lDataSource, editor, engine, aiCompletionPopup);
        }
        return null;
    }

    private void doAutoCompletion(
        DBCExecutionContext executionContext,
        QMTranslationHistoryManager historyManager,
        DBSLogicalDataSource lDataSource,
        SQLEditor editor,
        @NotNull DAICompletionEngine<?> engine,
        @NotNull AISuggestionPopup popup
    ) {
        final DAICompletionMessage message = new DAICompletionMessage(
            DAICompletionMessage.Role.USER,
            popup.getInputText()
        );

        if (CommonUtils.isEmptyTrimmed(message.getContent())) {
            return;
        }

        List<DAICompletionResponse> completionResult = new ArrayList<>();
        try {
            UIUtils.runInProgressDialog(monitor -> {
                final DAICompletionContext context = new DAICompletionContext.Builder()
                    .setScope(popup.getScope())
                    .setCustomEntities(popup.getCustomEntities(monitor))
                    .setDataSource(lDataSource)
                    .setExecutionContext(executionContext)
                    .build();

                try {
                    completionResult.addAll(
                        engine.performQueryCompletion(
                            monitor,
                            context,
                            message,
                            AIFormatterRegistry.getInstance().getFormatter(AIConstants.CORE_FORMATTER)
                        ));
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Auto completion error", null, e.getTargetException());
            return;
        }
        if (completionResult.isEmpty()) {
            DBWorkbench.getPlatformUI().showError("AI error", "No smart completions returned");
            return;
        }

        DAICompletionResponse response = completionResult.get(0);
        MessageChunk[] messageChunks = AITextUtils.splitIntoChunks(editor.getSQLDialect(), CommonUtils.notEmpty(response.getResultCompletion()));

        if (messageChunks.length == 0) {
            return;
        }

        final String completion = AITextUtils.convertToSQL(message, messageChunks, executionContext.getDataSource());

        // Save to history
        new AbstractJob("Save smart completion history") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    historyManager.saveTranslationHistory(
                        monitor,
                        lDataSource,
                        executionContext,
                        message.getContent(),
                        completion);
                } catch (DBException e) {
                    return GeneralUtils.makeExceptionStatus(e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();

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

        AIFeatures.SQL_AI_GENERATE_PROPOSALS.use(Map.of(
            "driver", lDataSource.getDataSourceContainer().getDriver().getPreconfiguredId(),
            "engine", engine.getEngineName(),
            "scope", popup.getScope().name()
        ));

        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AICompletionConstants.AI_COMPLETION_EXECUTE_IMMEDIATELY)) {
            editor.processSQL(false, false);
        }
    }
}
