/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.sql;

import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

public class OpenSQLFileAction extends AbstractSQLAction
{

    public OpenSQLFileAction()
    {
        setId(ICommandIds.CMD_OPEN_FILE);
        setActionDefinitionId(ICommandIds.CMD_OPEN_FILE);
        setImageDescriptor(DBIcon.LOAD.getImageDescriptor());
        setText("Load from file ... ");
        setToolTipText("Load SQL script");
    }

    protected void execute(SQLEditor editor)
    {
        editor.loadFromExternalFile();
    }

}