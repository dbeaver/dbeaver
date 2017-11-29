/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.RunnableContextDelegate;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.UIConfirmation;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

class DriverDownloadAutoPage extends DriverDownloadPage {

    private DriverDependenciesTree depsTree;

    DriverDownloadAutoPage() {
        super(CoreMessages.dialog_driver_download_auto_page_auto_download, CoreMessages.dialog_driver_download_auto_page_download_driver_files, null);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        final DriverDownloadWizard wizard = getWizard();
        final DriverDescriptor driver = wizard.getDriver();

        setMessage(NLS.bind(CoreMessages.dialog_driver_download_auto_page_download_specific_driver_files, driver.getFullName()));
        initializeDialogUnits(parent);

        Composite composite = UIUtils.createPlaceholder(parent, 1);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        if (!wizard.isForceDownload()) {
            Composite infoGroup = UIUtils.createPlaceholder(composite, 2, 5);
            infoGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            Label infoText = new Label(infoGroup, SWT.NONE);
            infoText.setText(NLS.bind(CoreMessages.dialog_driver_download_auto_page_driver_file_missing_text, driver.getFullName()));
            infoText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            final Button forceCheckbox = UIUtils.createCheckbox(infoGroup, CoreMessages.dialog_driver_download_auto_page_force_download, wizard.isForceDownload());
            forceCheckbox.setToolTipText(CoreMessages.dialog_driver_download_auto_page_force_download_tooltip);
            forceCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING));
            forceCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    wizard.setForceDownload(forceCheckbox.getSelection());
                }
            });
        }

        {
            Group filesGroup = UIUtils.createControlGroup(composite, CoreMessages.dialog_driver_download_auto_page_required_files, 1, -1, -1);
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
            new Label(filesGroup, SWT.NONE).setText(CoreMessages.dialog_driver_download_auto_page_change_driver_version_text);
        }

        if (!wizard.isForceDownload()) {
            Label infoText = new Label(composite, SWT.NONE);
            infoText.setText(CoreMessages.dialog_driver_download_auto_page_obtain_driver_files_text);
            infoText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        createLinksPanel(composite);

        setControl(composite);
    }


    @Override
    void resolveLibraries() {
        if (!depsTree.resolveLibraries()) {
            setErrorMessage(CoreMessages.dialog_driver_download_auto_page_cannot_resolve_libraries_text);
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
            DBUserInterface.getInstance().showError(CoreMessages.dialog_driver_download_auto_page_driver_download_error, CoreMessages.dialog_driver_download_auto_page_driver_download_error_msg, e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
        }
        return true;
    }

    private void downloadLibraryFiles(final DBRProgressMonitor monitor) throws InterruptedException {
        if (!getWizard().getDriver().acceptDriverLicenses()) {
            return;
        }

        boolean processUnsecure = false;
        List<DBPDriverDependencies.DependencyNode> nodes = getWizard().getDependencies().getLibraryList();
        for (int i = 0, filesSize = nodes.size(); i < filesSize; ) {
            final DBPDriverLibrary lib = nodes.get(i).library;
            if (!processUnsecure && !lib.isSecureDownload(monitor)) {
                boolean process = new UIConfirmation() {
                    @Override
                    protected Boolean runTask() {
                        MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
                        messageBox.setText(CoreMessages.dialog_driver_download_auto_page_driver_security_warning);
                        messageBox.setMessage(NLS.bind(CoreMessages.dialog_driver_download_auto_page_driver_security_warning_msg,
                                lib.getDisplayName(), lib.getExternalURL(monitor)));
                        int response = messageBox.open();
                        return (response == SWT.YES);
                    }
                }.execute();
                if (process) {
                    processUnsecure = true;
                } else {
                    break;
                }
            }
            int result = IDialogConstants.OK_ID;
            try {
                lib.downloadLibraryFile(monitor, getWizard().isForceDownload(), NLS.bind(CoreMessages.dialog_driver_download_auto_page_download_rate, (i + 1), filesSize));
            } catch (final IOException e) {
                if (lib.getType() == DBPDriverLibrary.FileType.license) {
                    result = IDialogConstants.OK_ID;
                } else {
                    result = new UITask<Integer>() {
                        @Override
                        protected Integer runTask() {
                            DownloadErrorDialog dialog = new DownloadErrorDialog(
                                    null,
                                    lib.getDisplayName(),
                                    CoreMessages.dialog_driver_download_auto_page_download_failed_msg,
                                    e);
                            return dialog.open();
                        }
                    }.execute();
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
        //DataSourceProviderRegistry.getInstance().saveDrivers();
    }

    public static class DownloadErrorDialog extends ErrorDialog {

        DownloadErrorDialog(
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
