/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.registry.DriverFileDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;

import java.util.List;

/**
 * DriverDownloadDialog
 */
public class DriverDownloadDialog extends WizardDialog
{
    public static final int EDIT_DRIVER_BUTTON_ID = 2000;

    private boolean doDownload = false;

    DriverDownloadDialog(IWorkbenchWindow window, DriverDescriptor driver, List<DriverFileDescriptor> files)
    {
        super(window.getShell(), new DriverDownloadWizard(driver, files));
    }

    DriverDescriptor getDriver() {
        return getWizard().getDriver();
    }

    @Override
    public DriverDownloadWizard getWizard() {
        return (DriverDownloadWizard)super.getWizard();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, EDIT_DRIVER_BUTTON_ID, "Edit Driver", false);

        super.createButtonsForButtonBar(parent);
        parent.layout();
    }

    @Override
    protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        if (id == IDialogConstants.FINISH_ID) {
            Button button = super.createButton(parent, id, getWizard().getFinishText(), defaultButton);
            button.setImage(DBeaverIcons.getImage(UIIcon.BROWSER));
            setButtonLayoutData(button);
            return button;
        }
        return super.createButton(parent, id, label, defaultButton);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == EDIT_DRIVER_BUTTON_ID) {
            cancelPressed();
            DriverEditDialog dialog = new DriverEditDialog(DBeaverUI.getActiveWorkbenchShell(), getDriver());
            dialog.open();
        }
        super.buttonPressed(buttonId);
    }

    @Override
    protected void finishPressed() {
        getButton(EDIT_DRIVER_BUTTON_ID).setEnabled(false);
        doDownload = true;
        super.finishPressed();
    }

    public static boolean downloadDriverFiles(IWorkbenchWindow window, DriverDescriptor driver, List<DriverFileDescriptor> files) {
        DriverDownloadDialog dialog = new DriverDownloadDialog(window, driver, files);
        dialog.open();
        return dialog.doDownload;
    }

}
