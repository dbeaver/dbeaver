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
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.ai.client.GPTAPIClient;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.ai.popup.GPTSuggestionPopup;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class GPTExecuteHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        GPTSuggestionPopup gptSuggestionPopup = new GPTSuggestionPopup(
            HandlerUtil.getActiveShell(event),
            GPTMessages.gpt_dialog_title
        );
        if (gptSuggestionPopup.open() == IDialogConstants.OK_ID) {
            doAutoCompletion(editor, gptSuggestionPopup.getInputText());
        }
        return null;
    }

    private void doAutoCompletion(SQLEditor editor, String inputText) {
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
            if (executionContext.getDataSource() instanceof DBSObjectContainer) {
                object = ((DBSObjectContainer) executionContext.getDataSource());
            }
        }

        DBSObjectContainer finalObject = object;
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    Optional<String> completion = GPTAPIClient.requestCompletion(inputText, monitor, finalObject);

                    System.out.println(completion.get());
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Auto completion error", null, e.getTargetException());
        }
    }
}
