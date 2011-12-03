/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.common;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionDelegate;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.UIUtils;

public class EmergentExitAction extends ActionDelegate
{

    private IWorkbenchWindow window;

    public EmergentExitAction(IWorkbenchWindow window) {
        this.window = window;
    }

    public void run(IAction action)
    {
        if (UIUtils.confirmAction(
            window == null ? null : window.getShell(),
            CoreMessages.actions_menu_exit_emergency,
            CoreMessages.ui_actions_exit_emergency_question))
        {
            System.exit(1);
        }
    }

}