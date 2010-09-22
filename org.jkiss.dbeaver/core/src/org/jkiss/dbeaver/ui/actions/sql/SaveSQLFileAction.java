/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.sql;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

public class SaveSQLFileAction extends AbstractSQLAction
{

    public SaveSQLFileAction()
    {
        setId(ICommandIds.CMD_SAVE_FILE);
        setActionDefinitionId(ICommandIds.CMD_SAVE_FILE);
        setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ETOOL_SAVEAS_EDIT));
        setText("Save to file ... ");
        setToolTipText("Save SQL script");
    }

    protected void execute(SQLEditor editor)
    {
        editor.saveToExternalFile();
    }

}