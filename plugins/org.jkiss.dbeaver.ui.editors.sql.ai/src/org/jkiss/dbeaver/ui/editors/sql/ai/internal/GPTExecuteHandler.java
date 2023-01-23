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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.GPTPreferences;
import org.jkiss.dbeaver.model.ai.client.GPTClient;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.ai.popup.GPTSuggestionPopup;
import org.jkiss.dbeaver.ui.editors.sql.ai.preferences.GPTPreferencePage;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;

public class GPTExecuteHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        DBPDataSourceContainer dataSourceContainer = editor.getDataSourceContainer();
        if (dataSourceContainer == null) {
            DBWorkbench.getPlatformUI().showError("No datasource", "Connection must be associated with the SQL script");
            return null;
        }
        GPTSuggestionPopup gptSuggestionPopup = new GPTSuggestionPopup(
            HandlerUtil.getActiveShell(event),
            "GPT-3 smart completion",
            dataSourceContainer
        );
        if (gptSuggestionPopup.open() == IDialogConstants.OK_ID) {
            doAutoCompletion(editor, gptSuggestionPopup.getInputText());
        }
        return null;
    }

    private void doAutoCompletion(SQLEditor editor, String inputText) {
        if (CommonUtils.isEmptyTrimmed(inputText)) {
            return;
        }

        if (!GPTClient.isValidConfiguration()) {
            UIUtils.showPreferencesFor(editor.getSite().getShell(), null, GPTPreferencePage.PAGE_ID);
        }
        if (!GPTClient.isValidConfiguration()) {
            DBWorkbench.getPlatformUI().showError("Bad GPT configuration", "You must specify OpenAI API token in preferences");
            return;
        }

        DBCExecutionContext executionContext = editor.getExecutionContext();
        DBSObjectContainer object;
        if (executionContext == null || executionContext.getContextDefaults() == null) {
            object = null;
        } else {
            if (executionContext.getContextDefaults().getDefaultSchema() == null) {
                object = executionContext.getContextDefaults().getDefaultCatalog();
            } else {
                object = executionContext.getContextDefaults().getDefaultSchema();
            }
            if (object == null) {
                object = ((DBSObjectContainer) executionContext.getDataSource());
            }
        }

        String[] completionResult = new String[1];
        DBSObjectContainer finalObject = object;
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    completionResult[0] = GPTClient.requestCompletion(inputText, monitor, finalObject, executionContext);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Auto completion error", null, e.getTargetException());
            return;
        }

        if (CommonUtils.isEmptyTrimmed(completionResult[0])) {
            return;
        }

        ISelection selection = editor.getSelectionProvider().getSelection();
        IDocument document = editor.getDocument();
        if (document != null && selection instanceof TextSelection) {
            try {
                int offset = ((TextSelection) selection).getOffset();
                int length = ((TextSelection) selection).getLength();
                String completion = completionResult[0];
                document.replace(
                    offset,
                    length,
                    completion);
                editor.getSelectionProvider().setSelection(new TextSelection(offset + completion.length(), 0));
            } catch (BadLocationException e) {
                DBWorkbench.getPlatformUI().showError("Insert SQL", "Error inserting SQL completion in text editor", e);
            }
        }

        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(GPTPreferences.GPT_EXECUTE_IMMEDIATELY)) {
            editor.processSQL(false, false);
        }
    }
}
