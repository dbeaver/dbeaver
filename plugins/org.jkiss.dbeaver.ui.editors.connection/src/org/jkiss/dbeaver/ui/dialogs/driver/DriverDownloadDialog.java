/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;

/**
 * DriverDownloadDialog
 */
public class DriverDownloadDialog extends WizardDialog {

    public static final int EDIT_DRIVER_BUTTON_ID = 2000;

    public static final int MAX_WIDTH = 800;

    private boolean doDownload = false;

    DriverDownloadDialog(
        @NotNull Shell shell,
        @NotNull DBPDriver driver,
        @NotNull DBPDriverDependencies dependencies,
        boolean updateVersion,
        boolean forceDownload,
        boolean isShowExpanded
    ) {
        super(shell, new DriverDownloadWizard(driver, dependencies, updateVersion, forceDownload, isShowExpanded));
        getWizard().init(UIUtils.getActiveWorkbenchWindow().getWorkbench(), null);
        addPageChangedListener(event -> UIUtils.asyncExec(() -> getWizard().pageActivated(event.getSelectedPage())));
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
            Button button = super.createButton(parent, id, getWizard().getFinishText(), true);
            button.setImage(DBeaverIcons.getImage(UIIcon.BROWSER));
            setButtonLayoutData(button);
            button.setFocus();
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
        doDownload = true;
        super.finishPressed();
    }

    void closeWizard() {
        UIUtils.asyncExec(() -> buttonPressed(IDialogConstants.CANCEL_ID));
    }

    public static boolean downloadDriverFiles(
        @Nullable Shell shell,
        @NotNull DBPDriver driver,
        @NotNull DBPDriverDependencies dependencies,
        boolean isShowExpanded
    ) {
        return downloadDriverFiles(shell, driver, dependencies, false, isShowExpanded);
    }

    public static boolean downloadDriverFiles(
        @Nullable Shell shell,
        @NotNull DBPDriver driver,
        @NotNull DBPDriverDependencies dependencies,
        boolean forceDownload,
        boolean isShowExpanded
    ) {
        if (DBWorkbench.getPlatform().isShuttingDown()) {
            return false;
        }
        if (shell == null) {
            shell = Display.getCurrent().getActiveShell();
        }
        var dialog = new DriverDownloadDialog(shell, driver, dependencies, false, forceDownload, isShowExpanded);
        dialog.setMinimumPageSize(0, 0);
        dialog.open();
        return dialog.doDownload;
    }

    @Override
    protected Point getInitialSize() {
        return getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
    }
}
