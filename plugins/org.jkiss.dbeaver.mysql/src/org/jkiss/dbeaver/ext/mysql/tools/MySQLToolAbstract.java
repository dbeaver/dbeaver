/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPTool;

/**
 * Abstract tool
 */
public abstract class MySQLToolAbstract implements DBPTool {

    public void execute(IWorkbenchWindow window, DBPObject object)
        throws DBException
    {

    }
}
