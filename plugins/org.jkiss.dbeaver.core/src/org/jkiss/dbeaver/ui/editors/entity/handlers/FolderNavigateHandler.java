/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.entity.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.FolderEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.Map;


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