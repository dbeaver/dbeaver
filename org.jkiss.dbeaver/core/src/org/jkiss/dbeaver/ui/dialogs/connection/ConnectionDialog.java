/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;

/**
 * NewConnectionDialog
 */
public class ConnectionDialog extends ActiveWizardDialog
{

    public ConnectionDialog(IWorkbenchWindow window, ConnectionWizard wizard)
    {
        super(window.getShell(), wizard);

        getWizard().init(window.getWorkbench(), null);
    }

    protected ConnectionWizard getWizard()
    {
        return (ConnectionWizard) super.getWizard();
    }

}
