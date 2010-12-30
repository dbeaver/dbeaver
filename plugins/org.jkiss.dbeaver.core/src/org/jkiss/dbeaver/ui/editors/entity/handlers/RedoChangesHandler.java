/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;


public class RedoChangesHandler extends AbstractHandler
{

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        EntityEditor editor = (EntityEditor)Platform.getAdapterManager().getAdapter(HandlerUtil.getActiveEditor(event), EntityEditor.class);
        if (editor != null) {
            editor.redoChanges();
        }
        return null;
    }

}