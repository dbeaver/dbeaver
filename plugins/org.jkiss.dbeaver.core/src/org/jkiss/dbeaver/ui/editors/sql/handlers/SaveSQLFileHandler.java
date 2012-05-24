/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

public class SaveSQLFileHandler extends BaseSQLEditorHandler
{

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        SQLEditorBase editor = getActiveControl(event);
        if (editor == null) {
            return null;
        }
        editor.saveToExternalFile();
        return null;
    }

}