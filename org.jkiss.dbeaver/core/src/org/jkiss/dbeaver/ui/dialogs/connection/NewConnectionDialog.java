/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.ui.IWorkbenchWindow;

/**
 * NewConnectionDialog
 */
public class NewConnectionDialog extends ConnectionDialog<NewConnectionDialog>
{
    public NewConnectionDialog(IWorkbenchWindow window)
    {
        super(window, new NewConnectionWizard(window));
    }

    protected NewConnectionWizard getWizard()
    {
        return (NewConnectionWizard) super.getWizard();
    }

}
