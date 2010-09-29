/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.sql;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;


public class ExecuteScriptHandler extends AbstractHandler
{

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        SQLEditor editor = (SQLEditor)Platform.getAdapterManager().getAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        if (editor != null) {
            editor.processSQL(true);
        }
        return null;
    }
}