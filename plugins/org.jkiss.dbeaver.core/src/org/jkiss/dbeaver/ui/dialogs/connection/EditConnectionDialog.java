/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.dialogs.MultiPageWizardDialog;

/**
 * CreateConnectionDialog
 */
public class EditConnectionDialog extends MultiPageWizardDialog
{

    public static final int TEST_BUTTON_ID = 2000;
    private Button testButton;

    public EditConnectionDialog(IWorkbenchWindow window, ConnectionWizard wizard)
    {
        super(window, wizard);
    }

    @Override
    public ConnectionWizard getWizard()
    {
        return (ConnectionWizard)super.getWizard();
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control contents = super.createContents(parent);
        DataSourceDescriptor activeDataSource = getWizard().getActiveDataSource();
        getShell().setText("Connection '" + activeDataSource.getName() + "' configuration");
        getShell().setImage(DBeaverIcons.getImage(activeDataSource.getObjectImage()));
        return contents;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        testButton = createButton(parent, TEST_BUTTON_ID, CoreMessages.dialog_connection_button_test, false);
        testButton.setEnabled(false);
        //testButton.moveAbove(getButton(IDialogConstants.CANCEL_ID));
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
        if (testButton != null) {
            ConnectionPageSettings settings = getWizard().getPageSettings();
            testButton.setEnabled(settings != null && settings.isPageComplete());
        }
        super.updateButtons();
    }

    private void testConnection()
    {
        getWizard().testConnection();
    }

}
