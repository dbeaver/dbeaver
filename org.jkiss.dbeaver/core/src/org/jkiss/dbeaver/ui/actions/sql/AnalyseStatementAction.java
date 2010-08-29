/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.sql;

import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.DBIcon;

public class AnalyseStatementAction extends AbstractSQLAction
{

    public AnalyseStatementAction()
    {
        setImageDescriptor(DBIcon.SQL_ANALYSE.getImageDescriptor());
        setText("Analyse statement");
    }

    protected void execute(SQLEditor editor)
    {

    }

}