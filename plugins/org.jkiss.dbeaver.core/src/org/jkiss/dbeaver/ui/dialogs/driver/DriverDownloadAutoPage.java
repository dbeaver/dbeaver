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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.RunnableContextDelegate;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

class DriverDownloadAutoPage extends DriverDownloadPage {

    private DriverDependenciesTree depsTree;

    DriverDownloadAutoPage() {
        super("Automatic download", "Download driver files", null);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        final DriverDownloadWizard wizard = getWizard();
        final DriverDescriptor driver = wizard.getDriver();

        setMessage("Download " + driver.getFullName() + " driver files");
        initializeDialogUnits(parent);

        Composite composite = UIUtils.createPlaceholder(parent, 1);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        if (!wizard.isForceDownload()) {
            Composite infoGroup = UIUtils.createPlaceholder(composite, 2, 5);
            infoGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            Label infoText = new Label(infoGroup, SWT.NONE);
            infoText.setText(driver.getFullName() + " driver files are missing.\nDBeaver can download these files automatically.\n\n");
            infoText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            final Button forceCheckbox = UIUtils.createCheckbox(infoGroup, "Force download / overwrite", wizard.isForceDownload());
            forceCheckbox.setToolTipText("Force files download. Will download files even if they are already on the disk");
            forceCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING));
            forceCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    wizard.setForceDownload(forceCheckbox.getSelection());
                }
            });
        }

        {
            Group filesGroup = UIUtils.createControlGroup(composite, "Files required by driver", 1, -1, -1);
            filesGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

            depsTree = new DriverDependenciesTree(
                filesGroup,
                new RunnableContextDelegate(getContainer()),
                getWizard().getDependencies(),
                driver,
                driver.getDriverLibraries(),
                true)
            {
                protected void setLibraryVersion(final DBPDriverLibrary library, final String version) {
                    String curVersion = library.getVersion();
                    if (CommonUtils.equalObjects(curVersion, version)) {
                        return;
                    }
                    library.setPreferredVersion(version);
                    resolveLibraries();
                }

            };
            new Label(filesGroup, SWT.NONE).setText("You can change driver version by clicking on version column.\nThen you can choose one of the available versions.");
        }

        if (!wizard.isForceDownload()) {
            Label infoText = new Label(composite, SWT.NONE);
            infoText.setText("\nOr you can obtain driver files by yourself and add them in driver editor.");
            infoText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        createLinksPanel(composite);

        setControl(composite);
    }


    @Override
    void resolveLibraries() {
        if (!depsTree.resolveLibraries()) {
            setErrorMessage("Can't resolve libraries. Check your network settings");
        }
        depsTree.resizeTree();
    }

    @Override
    public boolean isPageComplete() {
        return true;
    }

    @Override
    boolean performFinish() {
        try {
            getContainer().run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    downloadLibraryFiles(new DefaultProgressMonitor(monitor));
                }
            });
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(getShell(), "Driver download", "Error downloading driver files", e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
        }
        return true;
    }

    private void downloadLibraryFiles(DBRProgressMonitor monitor) throws InterruptedException {
        if (!getWizard().getDriver().acceptDriverLicenses()) {
            return;
        }

        List<DBPDriverDependencies.DependencyNode> nodes = getWizard().getDependencies().getLibraryList();
        for (int i = 0, filesSize = nodes.size(); i < filesSize; ) {
            DBPDriverLibrary lib = nodes.get(i).library;
            int result = IDialogConstants.OK_ID;
            try {
                lib.downloadLibraryFile(monitor, getWizard().isForceDownload(), "Download " + (i + 1) + "/" + filesSize);
            } catch (IOException e) {
                if (lib.getType() == DBPDriverLibrary.FileType.license) {
                    result = IDialogConstants.OK_ID;
                } else {
                    DownloadRetry retryConfirm = new DownloadRetry(lib, e);
                    DBeaverUI.syncExec(retryConfirm);
                    result = retryConfirm.result;
                }
            }
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

        getWizard().getDriver().setModified(true);
        DataSourceProviderRegistry.getInstance().saveDrivers();
    }

    private class DownloadRetry implements Runnable {
        private final DBPDriverLibrary file;
        private final Throwable error;
        private int result;

        public DownloadRetry(DBPDriverLibrary file, Throwable error)
        {
            this.file = file;
            this.error = error;
        }

        @Override
        public void run()
        {
            DownloadErrorDialog dialog = new DownloadErrorDialog(
                null,
                file.getDisplayName(),
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
