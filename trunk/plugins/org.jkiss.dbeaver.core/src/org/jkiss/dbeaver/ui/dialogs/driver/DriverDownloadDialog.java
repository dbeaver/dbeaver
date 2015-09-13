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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDriverFile;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.registry.DriverFileDescriptor;
import org.jkiss.dbeaver.runtime.RunnableContextDelegate;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
        return ((DriverDownloadWizard)getWizard()).getDriver();
    }

    List<DriverFileDescriptor> getDriverFiles() {
        return ((DriverDownloadWizard)getWizard()).getFiles();
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
            String finishText;
            if (CommonUtils.isEmpty(getDriver().getDriverFileSources())) {
                finishText = "Download";
            } else {
                finishText = "Open Download Page";
            }
            Button button = super.createButton(parent, id, finishText, defaultButton);
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
        downloadLibraryFiles(new RunnableContextDelegate(this), getDriverFiles());
        doDownload = true;
        super.finishPressed();
    }

    public static boolean downloadDriverFiles(IWorkbenchWindow window, DriverDescriptor driver, List<DriverFileDescriptor> files) {
        DriverDownloadDialog dialog = new DriverDownloadDialog(window, driver, files);
        dialog.open();
        return dialog.doDownload;
    }

    private void downloadLibraryFiles(DBRRunnableContext runnableContext, final List<DriverFileDescriptor> files)
    {
        if (!getDriver().acceptDriverLicenses(runnableContext)) {
            return;
        }

        for (int i = 0, filesSize = files.size(); i < filesSize; ) {
            DriverFileDescriptor lib = files.get(i);
            int result = downloadLibraryFile(runnableContext, lib);
            switch (result) {
                case IDialogConstants.CANCEL_ID:
                case IDialogConstants.ABORT_ID:
                    return;
                case IDialogConstants.RETRY_ID:
                    continue;
                case IDialogConstants.OK_ID:
                case IDialogConstants.IGNORE_ID:
                    i++;
                    break;
            }
        }
    }

    private int downloadLibraryFile(DBRRunnableContext runnableContext, final DriverFileDescriptor file)
    {
        try {
            runnableContext.run(true, true, new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        file.downloadLibraryFile(monitor);
                    } catch (IOException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
            return IDialogConstants.OK_ID;
        } catch (InterruptedException e) {
            // User just canceled download
            return IDialogConstants.CANCEL_ID;
        } catch (InvocationTargetException e) {
            if (file.getType() == DBPDriverFile.FileType.license) {
                return IDialogConstants.OK_ID;
            }
            DownloadRetry retryConfirm = new DownloadRetry(file, e.getTargetException());
            UIUtils.runInUI(null, retryConfirm);
            return retryConfirm.result;
        }
    }

    private class DownloadRetry implements Runnable {
        private final DriverFileDescriptor file;
        private final Throwable error;
        private int result;

        public DownloadRetry(DriverFileDescriptor file, Throwable error)
        {
            this.file = file;
            this.error = error;
        }

        @Override
        public void run()
        {
            DownloadErrorDialog dialog = new DownloadErrorDialog(
                null,
                file.getPath(),
                "Driver file download failed.\nDo you want to retry?",
                error);
            result = dialog.open();
        }
    }

    public static class DownloadErrorDialog extends ErrorDialog {

        public DownloadErrorDialog(
            Shell parentShell,
            String dialogTitle,
            String message,
            Throwable error)
        {
            super(parentShell, dialogTitle, message,
                GeneralUtils.makeExceptionStatus(error),
                IStatus.INFO | IStatus.WARNING | IStatus.ERROR);
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(
                parent,
                IDialogConstants.ABORT_ID,
                IDialogConstants.ABORT_LABEL,
                true);
            createButton(
                parent,
                IDialogConstants.RETRY_ID,
                IDialogConstants.RETRY_LABEL,
                false);
            createButton(
                parent,
                IDialogConstants.IGNORE_ID,
                IDialogConstants.IGNORE_LABEL,
                false);
            createDetailsButton(parent);
        }

        @Override
        protected void buttonPressed(int buttonId) {
            if (buttonId == IDialogConstants.DETAILS_ID) {
                super.buttonPressed(buttonId);
            } else {
                setReturnCode(buttonId);
                close();
            }
        }
    }

}
