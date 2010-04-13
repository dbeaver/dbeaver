/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.sql;

import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

public class ExecuteStatementAction extends AbstractSQLAction
{

    public ExecuteStatementAction()
    {
        setId(ICommandIds.CMD_EXECUTE_STATEMENT);
        setActionDefinitionId(ICommandIds.CMD_EXECUTE_STATEMENT);
        setImageDescriptor(DBeaverActivator.getImageDescriptor("/icons/sql/run_sql.png"));
        setText("Execute Statement");
        setToolTipText("Execute SQL statement");
    }

    protected void execute(SQLEditor editor)
    {
        editor.processSQL(false);
    }

}