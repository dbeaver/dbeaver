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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.RunnableContextDelegate;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

class DriverDownloadAutoPage extends DriverDownloadPage {

    private Tree filesTree;

    DriverDownloadAutoPage() {
        super("Automatic download", "Download driver files", null);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        DriverDownloadWizard wizard = getWizard();
        final DriverDescriptor driver = wizard.getDriver();

        setMessage("Download " + driver.getFullName() + " driver files");
        initializeDialogUnits(parent);

        Composite composite = UIUtils.createPlaceholder(parent, 1);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        if (!wizard.isForceDownload()) {
            Label infoText = new Label(composite, SWT.NONE);
            infoText.setText(driver.getFullName() + " driver files are missing.\nDBeaver can download these files automatically.\n\n");
            infoText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        {
            Group filesGroup = UIUtils.createControlGroup(composite, "Files required by driver", 1, -1, -1);
            filesGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
            filesTree = new Tree(filesGroup, SWT.BORDER | SWT.FULL_SELECTION);
            filesTree.setHeaderVisible(true);
            filesTree.setLayoutData(new GridData(GridData.FILL_BOTH));
            UIUtils.createTreeColumn(filesTree, SWT.LEFT, "File");
            UIUtils.createTreeColumn(filesTree, SWT.LEFT, "Version");
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
        final Map<String, List<DBPDriverLibrary>> depMap = new LinkedHashMap<>();
        try {
            new RunnableContextDelegate(getContainer()).run(true, true, new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Resolve dependencies", 100);
                    try {
                        DBPDriverDependencies dependencies = getWizard().getDriver().resolveDependencies(monitor);

                        for (DBPDriverLibrary library : getWizard().getFiles()) {
                            resolveDependencies(monitor, library, null, depMap);
                        }
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        monitor.done();
                    }
                }
            });
        } catch (InterruptedException e) {
            // User just canceled download
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(null, "Resolve libraries", "Error resolving driver libraries", e.getTargetException());
        }

        final Set<String> addedDeps = new HashSet<>();
        int totalItems = 0;
        for (DBPDriverLibrary file : getWizard().getFiles()) {
            TreeItem item = new TreeItem(filesTree, SWT.NONE);
            item.setImage(DBeaverIcons.getImage(file.getIcon()));
            item.setText(0, file.getDisplayName());
            item.setText(1, CommonUtils.notEmpty(file.getVersion()));
            totalItems++;
            if (addDependencies(item, file, depMap, addedDeps)) {
                item.setExpanded(true);
                totalItems += item.getItemCount();
            }
        }
        UIUtils.packColumns(filesTree);
        if (totalItems > 20) {
            totalItems = 20;
        }

        Shell shell = getContainer().getShell();
        shell.setSize(shell.getSize().x, shell.getSize().y + filesTree.getItemHeight() * totalItems);
        shell.layout();
    }

    private void resolveDependencies(DBRProgressMonitor monitor, DBPDriverLibrary library, DBPDriverLibrary ownerLibrary, Map<String, List<DBPDriverLibrary>> depMap) throws IOException {
        String libraryPath = library.getPath();
        List<DBPDriverLibrary> deps = depMap.get(libraryPath);
        if (deps != null) {
            return;
        }

        deps = new ArrayList<>();
        depMap.put(libraryPath, deps);

        Collection<? extends DBPDriverLibrary> dependencies = library.getDependencies(monitor, ownerLibrary);
        if (dependencies != null && !dependencies.isEmpty()) {
            for (DBPDriverLibrary dep : dependencies) {
                deps.add(dep);
                resolveDependencies(monitor, dep, library, depMap);
            }
        }
    }

    private boolean addDependencies(TreeItem parent, DBPDriverLibrary library, Map<String, List<DBPDriverLibrary>> depMap, Set<String> addedDeps) {
        Collection<? extends DBPDriverLibrary> dependencies = depMap.get(library.getPath());
        if (dependencies != null && !dependencies.isEmpty()) {
            Map<DBPDriverLibrary, TreeItem> itemMap = new HashMap<>();
            for (DBPDriverLibrary dep : dependencies) {
                TreeItem item = new TreeItem(parent, SWT.NONE);
                item.setImage(DBeaverIcons.getImage(dep.getIcon()));
                item.setText(0, dep.getDisplayName());
                item.setText(1, CommonUtils.notEmpty(dep.getVersion()));

                if (addedDeps.contains(dep.getPath())) {
                    item.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
                }
                itemMap.put(dep, item);
            }
            for (DBPDriverLibrary dep : dependencies) {
                TreeItem item = itemMap.get(dep);
                if (!addedDeps.contains(dep.getPath())) {
                    addedDeps.add(dep.getPath());
                    if (addDependencies(item, dep, depMap, addedDeps)) {
                        //item.setExpanded(true);
                    }
                }
            }

            return true;
        }
        return false;
    }

    @Override
    void performFinish() {
        downloadLibraryFiles(new RunnableContextDelegate(getContainer()), getWizard().getFiles());
    }

    private void downloadLibraryFiles(DBRRunnableContext runnableContext, final List<? extends DBPDriverLibrary> files)
    {
        if (!getWizard().getDriver().acceptDriverLicenses(runnableContext)) {
            return;
        }

        for (int i = 0, filesSize = files.size(); i < filesSize; ) {
            DBPDriverLibrary lib = files.get(i);
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

    private int downloadLibraryFile(DBRRunnableContext runnableContext, final DBPDriverLibrary file)
    {
        try {
            runnableContext.run(true, true, new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        file.downloadLibraryFile(monitor, getWizard().isUpdateVersion());
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
            if (file.getType() == DBPDriverLibrary.FileType.license) {
                return IDialogConstants.OK_ID;
            }
            DownloadRetry retryConfirm = new DownloadRetry(file, e.getTargetException());
            UIUtils.runInUI(null, retryConfirm);
            return retryConfirm.result;
        }
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
