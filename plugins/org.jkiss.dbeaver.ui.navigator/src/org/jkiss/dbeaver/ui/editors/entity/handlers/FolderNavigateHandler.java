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
package org.jkiss.dbeaver.ui.editors.entity.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.editors.entity.FolderEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;


public class FolderNavigateHandler extends AbstractHandler //implements IElementUpdater
{

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        FolderEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), FolderEditor.class);
        if (editor != null) {
            String actionId = event.getCommand().getId();
            switch (actionId) {
                case IWorkbenchCommandConstants.NAVIGATE_BACKWARD_HISTORY: {
                    final int hp = editor.getHistoryPosition();
                    if (hp > 0) {
                        editor.navigateHistory(hp - 1);
                    }
                    break;
                }
                case IWorkbenchCommandConstants.NAVIGATE_FORWARD_HISTORY: {
                    final int hp = editor.getHistoryPosition();
                    if (hp < editor.getHistorySize() - 1) {
                        editor.navigateHistory(hp + 1);
                    }
                    break;
                }
            }

        }
        return null;
    }

/*
    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        IWorkbenchWindow workbenchWindow = element.getServiceLocator().getService(IWorkbenchWindow.class);
        if (workbenchWindow == null || workbenchWindow.getActivePage() == null) {
            return;
        }
        final IEditorPart activeEditor = workbenchWindow.getActivePage().getActiveEditor();
        if (activeEditor instanceof EntityEditor) {
            final DBECommandContext commandContext = ((EntityEditor) activeEditor).getCommandContext();
            String text = "Undo";
            if (commandContext != null && commandContext.getUndoCommand() != null) {
                text += " " + commandContext.getUndoCommand().getTitle();
            }
            element.setText(text);
        }
    }
*/

}