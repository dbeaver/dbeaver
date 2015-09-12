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
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.utils.CommonUtils;

/**
 * DriverDownloadDialog
 */
public class DriverDownloadDialog extends WizardDialog
{
    public static final int EDIT_DRIVER_BUTTON_ID = 2000;

    private final DriverDescriptor driver;

    public DriverDownloadDialog(IWorkbenchWindow window, DriverDescriptor driver)
    {
        super(window.getShell(), new DriverDownloadWizard(driver));
        this.driver = driver;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, EDIT_DRIVER_BUTTON_ID, "Edit Driver", false);

        super.createButtonsForButtonBar(parent);

        Button finishButton = getButton(IDialogConstants.FINISH_ID);
        if (CommonUtils.isEmpty(driver.getDriverFileSources())) {
            finishButton.setText("Download");
        } else {
            finishButton.setText("Open Download Page");
        }
    }
}
