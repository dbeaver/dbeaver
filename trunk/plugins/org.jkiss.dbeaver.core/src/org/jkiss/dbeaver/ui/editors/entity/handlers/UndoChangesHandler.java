/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.entity.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;

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
        IWorkbenchWindow workbenchWindow = (IWorkbenchWindow) element.getServiceLocator().getService(IWorkbenchWindow.class);
        final IEditorPart activeEditor = workbenchWindow.getActivePage().getActiveEditor();
        if (activeEditor instanceof EntityEditor) {
            final DBECommandContext commandContext = ((EntityEditor) activeEditor).getCommandContext();
            String text = "Undo";
            if (commandContext.getUndoCommand() != null) {
                text += " " + commandContext.getUndoCommand().getTitle();
            }
            element.setText(text);
        }
    }

}