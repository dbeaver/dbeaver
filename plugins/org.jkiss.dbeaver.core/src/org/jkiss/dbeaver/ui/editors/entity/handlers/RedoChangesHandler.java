/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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


public class RedoChangesHandler extends AbstractHandler implements IElementUpdater
{

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        EntityEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), EntityEditor.class);
        if (editor != null) {
            editor.redoChanges();
        }
        return null;
    }

    public void updateElement(UIElement element, Map parameters)
    {
        IWorkbenchWindow workbenchWindow = (IWorkbenchWindow) element.getServiceLocator().getService(IWorkbenchWindow.class);
        final IEditorPart activeEditor = workbenchWindow.getActivePage().getActiveEditor();
        if (activeEditor instanceof EntityEditor) {
            final DBECommandContext commandContext = ((EntityEditor) activeEditor).getCommandContext();
            String text = "Redo";
            if (commandContext.getRedoCommand() != null) {
                text += " " + commandContext.getRedoCommand().getTitle();
            }
            element.setText(text);
        }
    }
}