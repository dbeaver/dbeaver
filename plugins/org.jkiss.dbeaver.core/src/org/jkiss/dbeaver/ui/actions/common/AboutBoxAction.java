/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.common;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionDelegate;
import org.jkiss.dbeaver.ui.dialogs.misc.AboutBoxDialog;


public class AboutBoxAction extends ActionDelegate
{

    private IWorkbenchWindow window;

    public AboutBoxAction(IWorkbenchWindow window) {
        this.window = window;
    }

    @Override
    public void run(IAction action)
    {
        AboutBoxDialog dialog = new AboutBoxDialog(window.getShell());
        dialog.open();
    }

}