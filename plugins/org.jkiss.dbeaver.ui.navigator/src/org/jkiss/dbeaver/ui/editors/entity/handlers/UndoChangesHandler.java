/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.Map;


public class UndoChangesHandler extends AbstractHandler implements IElementUpdater
{

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        EntityEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), EntityEditor.class);
        if (editor != null) {
            editor.undoChanges();
        }
        return null;
    }

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
            String text = WorkbenchMessages.Workbench_undo;
            if (commandContext != null && commandContext.getUndoCommand() != null) {
                text += " " + commandContext.getUndoCommand().getTitle();
            }
            element.setText(text);
        }
    }

}