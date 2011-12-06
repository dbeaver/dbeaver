/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.tools;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPTool;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dialogs.tools.ToolWizardDialog;

/**
 * Database import
 */
public class OracleToolScript implements DBPTool
{
    public void execute(IWorkbenchWindow window, DBPObject object) throws DBException
    {
        if (object instanceof DBSObject && ((DBSObject) object).getDataSource() instanceof OracleDataSource) {
            ToolWizardDialog dialog = new ToolWizardDialog(
                window,
                new OracleScriptExecuteWizard((OracleDataSource)((DBSObject) object).getDataSource()));
            dialog.open();
        }
    }
}
