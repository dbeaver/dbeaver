/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.sql;

import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;


public class ExecuteScriptAction extends AbstractSQLAction
{

    public ExecuteScriptAction()
    {
        setId(ICommandIds.CMD_EXECUTE_SCRIPT);
        setActionDefinitionId(ICommandIds.CMD_EXECUTE_SCRIPT);
        setImageDescriptor(DBIcon.SQL_SCRIPT_EXECUTE.getImageDescriptor());
        setText("Execute Script");
        setToolTipText("Execute Script");
    }

    protected void execute(SQLEditor editor)
    {
        editor.processSQL(true);
    }

}