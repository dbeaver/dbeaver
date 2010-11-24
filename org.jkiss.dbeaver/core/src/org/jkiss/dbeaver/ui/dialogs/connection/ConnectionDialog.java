/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.DBIcon;
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

    @Override
    protected Control createDialogArea(Composite parent)
    {
        DataSourceDescriptor ds = getWizard().getDataSourceDescriptor();
        if (ds != null) {
            getShell().setImage(ds.getDriver().getIcon());
        } else {
            getShell().setImage(DBIcon.GEN_DATABASE.getImage());
        }
        return super.createDialogArea(parent);
    }
}
