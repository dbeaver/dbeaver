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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;

public class SQLEditorHandlerResultTabsNavigate extends AbstractHandler //implements IElementUpdater
{
    static protected final Log log = Log.getLog(SQLEditorHandlerResultTabsNavigate.class);

    private static final String COMMAND_PREVIOUS_TAB = "org.eclipse.ui.navigate.previousTab";
    private static final String COMMAND_NEXT_TAB = "org.eclipse.ui.navigate.nextTab";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        if (editor != null) {
            String actionId = event.getCommand().getId();
            CTabFolder resultTabs = editor.getResultTabsContainer();
            int idx = resultTabs.getSelectionIndex(), newIdx = idx;
            
            switch (actionId) {
                case COMMAND_PREVIOUS_TAB: {
                    newIdx = idx > 0 ? idx - 1 : resultTabs.getItemCount() - 1;
                    break;
                }
                case COMMAND_NEXT_TAB: {
                    newIdx = idx < resultTabs.getItemCount() - 1 ? idx + 1 : 0;
                    break;
                }
            }
            
            editor.setResultTabSelection(resultTabs.getItem(newIdx));

        }
        return null;
    }
}
