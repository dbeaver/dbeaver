/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.common;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.actions.ActionDelegate;
import org.jkiss.dbeaver.core.DBeaverVersionChecker;


public class CheckForUpdateAction extends ActionDelegate {

    public void run(IAction action)
    {
        new DBeaverVersionChecker(true).schedule();
    }

}