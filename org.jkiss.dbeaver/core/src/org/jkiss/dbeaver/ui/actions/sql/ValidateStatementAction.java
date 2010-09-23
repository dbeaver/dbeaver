/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.sql;

import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

public class ValidateStatementAction extends AbstractSQLAction
{

    public ValidateStatementAction()
    {
        setImageDescriptor(DBIcon.SQL_VALIDATE.getImageDescriptor());
        setText("Validate statement");
    }

    protected void execute(SQLEditor editor)
    {

    }

}