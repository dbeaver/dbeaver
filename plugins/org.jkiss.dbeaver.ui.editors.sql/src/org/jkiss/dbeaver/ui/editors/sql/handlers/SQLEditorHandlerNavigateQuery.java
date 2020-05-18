/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;

public class SQLEditorHandlerNavigateQuery extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
        if (!(activeEditor instanceof SQLEditorBase)) {
            return null;
        }
        SQLEditorBase editor = (SQLEditorBase)activeEditor;

        String actionId = event.getCommand().getId();

        SQLScriptElement nextQuery;
        switch (actionId) {
            case SQLEditorCommands.CMD_SQL_QUERY_NEXT:
                nextQuery = editor.extractNextQuery(true);
                break;
            case SQLEditorCommands.CMD_SQL_QUERY_PREV:
                nextQuery = editor.extractNextQuery(false);
                break;
            default:
                nextQuery = null;
                break;
        }
        if (nextQuery != null) {
            editor.selectAndReveal(nextQuery.getOffset(), nextQuery.getLength());
        }
        return null;
    }

}
