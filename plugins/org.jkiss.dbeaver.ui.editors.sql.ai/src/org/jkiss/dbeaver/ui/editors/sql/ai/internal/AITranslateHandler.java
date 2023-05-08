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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.AICompletionConstants;
import org.jkiss.dbeaver.model.ai.AIEngineRegistry;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionEngine;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionRequest;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionResponse;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionSettings;
import org.jkiss.dbeaver.model.ai.translator.DAIHistoryManager;
import org.jkiss.dbeaver.model.ai.translator.SimpleFilterManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
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

        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AICompletionConstants.AI_DISABLED)) {
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
            engine = AIEngineRegistry.getInstance().getCompletionEngine("openai");
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("AI error", "Cannot determine AI engine", e);
            return null;
        }

        if (!engine.isValidConfiguration()) {
            UIUtils.showPreferencesFor(editor.getSite().getShell(), null, AIPreferencePage.PAGE_ID);
        }
        if (!engine.isValidConfiguration()) {
            DBWorkbench.getPlatformUI().showError("Bad AI engine configuration", "You must specify OpenAI API token in preferences");
            return null;
        }
        DBCExecutionContext executionContext = editor.getExecutionContext();
        if (executionContext == null) {
            DBWorkbench.getPlatformUI().showError("No connection", "You must connect to the database before performing completion");
            return null;
        }

        DAICompletionSettings settings = new DAICompletionSettings(dataSourceContainer);

        // Show info transfer warning
        if (!settings.isMetaTransferConfirmed()) {
            if (UIUtils.confirmAction(editor.getSite().getShell(), "Transfer information to OpenAI",
                "In order to perform AI smart completion DBeaver needs to transfer\n" +
                    "your database metadata information (table and column names) to OpenAI API.\n" +
                    "Do you confirm it for connection '" + dataSourceContainer.getName() + "'?",
                DBIcon.AI))
            {
                settings.setMetaTransferConfirmed(true);
                settings.saveSettings();
            } else {
                return null;
            }
        }

        DAIHistoryManager historyManager = GeneralUtils.adapt(AISuggestionPopup.class, DAIHistoryManager.class);
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
            "ChatGPT smart completion",
            historyManager,
            lDataSource,
            executionContext,
            settings
        );
        if (aiCompletionPopup.open() == IDialogConstants.OK_ID) {
            DAICompletionRequest completionRequest = new DAICompletionRequest();
            completionRequest.setPromptText(aiCompletionPopup.getInputText());
            completionRequest.setScope(aiCompletionPopup.getScope());
            completionRequest.setCustomEntities(aiCompletionPopup.getCustomEntities());
            doAutoCompletion(executionContext, historyManager, lDataSource, editor, engine, completionRequest);
        }
        return null;
    }

    private void doAutoCompletion(
        DBCExecutionContext executionContext,
        DAIHistoryManager historyManager,
        DBSLogicalDataSource lDataSource,
        SQLEditor editor,
        DAICompletionEngine engine,
        DAICompletionRequest request
    ) {
        if (CommonUtils.isEmptyTrimmed(request.getPromptText())) {
            return;
        }

        List<DAICompletionResponse> completionResult = new ArrayList<>();
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    completionResult.addAll(
                        engine.performQueryCompletion(
                            monitor, lDataSource, executionContext, request, true, 100));
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
        String completion = response.getResultCompletion();
        if (CommonUtils.isEmptyTrimmed(completion)) {
            return;
        }

        // Save to history
        new AbstractJob("Save smart completion history") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    historyManager.saveTranslationHistory(
                        monitor,
                        lDataSource,
                        executionContext,
                        request.getPromptText(),
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
                if (query != null) {
                    offset = query.getOffset();
                    length = query.getLength();
                }
                document.replace(
                    offset,
                    length,
                    completion);
                editor.getSelectionProvider().setSelection(new TextSelection(offset + completion.length(), 0));
            } catch (BadLocationException e) {
                DBWorkbench.getPlatformUI().showError("Insert SQL", "Error inserting SQL completion in text editor", e);
            }
        }

        AIFeatures.SQL_AI_GENERATE_PROPOSALS.use(Map.of(
            "driver", lDataSource.getDataSourceContainer().getDriver().getPreconfiguredId(),
            "engine", engine.getEngineName(),
            "scope", request.getScope().name()

        ));
        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AICompletionConstants.AI_COMPLETION_EXECUTE_IMMEDIATELY)) {
            editor.processSQL(false, false);
        }
    }
}
