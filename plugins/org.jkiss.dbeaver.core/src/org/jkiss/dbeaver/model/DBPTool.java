/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;

/**
 * Database utility
 */
public interface DBPTool {

    void execute(IWorkbenchWindow window, DBPObject object)
        throws DBException;

}
