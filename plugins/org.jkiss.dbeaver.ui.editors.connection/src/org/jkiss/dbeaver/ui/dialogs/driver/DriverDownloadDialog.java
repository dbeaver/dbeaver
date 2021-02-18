/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;

/**
 * DriverDownloadDialog
 */
public class DriverDownloadDialog extends WizardDialog
{
    private static final String DIALOG_ID = "DBeaver.DriverDownloadDialog";//$NON-NLS-1$
    public static final int EDIT_DRIVER_BUTTON_ID = 2000;

    private boolean doDownload = false;

    DriverDownloadDialog(Shell shell, DBPDriver driver, DBPDriverDependencies dependencies, boolean updateVersion, boolean forceDownload)
    {
        super(shell, new DriverDownloadWizard(driver, dependencies, updateVersion, forceDownload));
        getWizard().init(UIUtils.getActiveWorkbenchWindow().getWorkbench(), null);
        addPageChangedListener(event -> UIUtils.asyncExec(() -> getWizard().pageActivated(event.getSelectedPage())));
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    DBPDriver getDriver() {
        return getWizard().getDriver();
    }

    @Override
    public DriverDownloadWizard getWizard() {
        return (DriverDownloadWizard)super.getWizard();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Control dialogArea = super.createDialogArea(parent);
        if (getWizard().isForceDownload()) {
            UIUtils.asyncExec(() -> buttonPressed(IDialogConstants.FINISH_ID));
        }
        return dialogArea;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        DriverDownloadWizard wizard = getWizard();
        if (!wizard.isForceDownload() && DriverEditDialog.getDialogCount() == 0) {
            createButton(parent, EDIT_DRIVER_BUTTON_ID,
                wizard.isAutoDownloadWizard() ? UIConnectionMessages.dialog_driver_download_button_edit_dirver : UIConnectionMessages.dialog_driver_download_button_add_jars,
                false);
        }

        super.createButtonsForButtonBar(parent);
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
    public void buttonPressed(int buttonId) {
        if (buttonId == EDIT_DRIVER_BUTTON_ID) {
            cancelPressed();
            DriverEditDialog dialog = new DriverEditDialog(null, getDriver());
            dialog.open(!getWizard().isAutoDownloadWizard());
        }
        super.buttonPressed(buttonId);
    }

    @Override
    protected void finishPressed() {
//        Button editButton = getButton(EDIT_DRIVER_BUTTON_ID);
//        if (editButton != null) {
//            editButton.setEnabled(false);
//        }
        doDownload = true;
        super.finishPressed();
    }

    void closeWizard() {
        UIUtils.asyncExec(() -> buttonPressed(IDialogConstants.CANCEL_ID));
    }

    public static boolean downloadDriverFiles(Shell shell, DBPDriver driver, DBPDriverDependencies dependencies) {
        return downloadDriverFiles(shell, driver, dependencies, false);
    }

    public static boolean downloadDriverFiles(Shell shell, DBPDriver driver, DBPDriverDependencies dependencies, boolean forceDownload) {
        DriverDownloadDialog dialog = new DriverDownloadDialog(shell, driver, dependencies, false, forceDownload);
        dialog.setMinimumPageSize(100, 100);
        dialog.open();
        return dialog.doDownload;
    }

    public static boolean updateDriverFiles(Shell shell, DBPDriver driver, DBPDriverDependencies dependencies, boolean forceDownload) {
        DriverDownloadDialog dialog = new DriverDownloadDialog(shell, driver, dependencies, true, forceDownload);
        dialog.setMinimumPageSize(100, 100);
        dialog.open();
        return dialog.doDownload;
    }

}
