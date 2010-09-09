/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.action;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditor;

public class ERDUndoHandler extends AbstractHandler {
    public ERDUndoHandler() {
        System.out.println("asdf");
    }

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ERDEditor editor = (ERDEditor) HandlerUtil.getActiveEditor(event);
        editor.getCommandStack().undo();
        return null;
    }

}
