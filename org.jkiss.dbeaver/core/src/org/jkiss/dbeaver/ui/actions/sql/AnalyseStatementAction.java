/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.sql;

import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

public class AnalyseStatementAction extends AbstractSQLAction
{

    public AnalyseStatementAction()
    {
        setImageDescriptor(DBeaverActivator.getImageDescriptor("/icons/sql/sql_analyse.png"));
        setText("Analyse statement");
    }

    protected void execute(SQLEditor editor)
    {

    }

}