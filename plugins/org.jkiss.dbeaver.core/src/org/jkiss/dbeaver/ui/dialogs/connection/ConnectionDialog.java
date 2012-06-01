/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;

/**
 * NewConnectionDialog
 */
public class ConnectionDialog extends ActiveWizardDialog
{

    public static final int TEST_BUTTON_ID = 2000;
    private Button testButton;

    public ConnectionDialog(IWorkbenchWindow window, ConnectionWizard wizard)
    {
        super(window, wizard);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
//        DataSourceDescriptor ds = ((ConnectionWizard)getWizard()).getDataSourceDescriptor();
//        if (ds != null) {
//            getShell().setImage(ds.getDriver().getIcon());
//        } else {
//            getShell().setImage(DBIcon.GEN_DATABASE.getImage());
//        }
        return super.createDialogArea(parent);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        testButton = createButton(parent, TEST_BUTTON_ID, "Test Connection ...", false);
        testButton.setEnabled(false);
        testButton.moveAbove(getButton(IDialogConstants.BACK_ID));
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == TEST_BUTTON_ID) {
            testConnection();
            return;
        }
        super.buttonPressed(buttonId);
    }

    @Override
    public void updateButtons()
    {
        ConnectionWizard wizard = (ConnectionWizard) getWizard();
        ConnectionPageSettings settings = wizard.getPageSettings();
        testButton.setEnabled(settings != null && settings.isPageComplete());
        super.updateButtons();
    }

    private void testConnection()
    {
        ConnectionWizard wizard = (ConnectionWizard) getWizard();
        wizard.testConnection(wizard.getPageSettings().getConnectionInfo());
    }

}
