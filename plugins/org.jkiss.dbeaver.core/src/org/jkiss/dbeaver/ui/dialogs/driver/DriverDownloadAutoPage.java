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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDependencies;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.runtime.RunnableContextDelegate;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class DriverDownloadAutoPage extends DriverDownloadPage {

    private Tree filesTree;
    private Font boldFont;
    private TreeEditor treeEditor;

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
            UIUtils.createTreeColumn(filesTree, SWT.LEFT, "Description");

            treeEditor = new TreeEditor(filesTree);
            treeEditor.horizontalAlignment = SWT.RIGHT;
            treeEditor.verticalAlignment = SWT.CENTER;
            treeEditor.grabHorizontal = true;
            treeEditor.minimumWidth = 50;

            filesTree.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseUp(MouseEvent e)
                {
                    TreeItem item = filesTree.getItem(new Point(e.x, e.y));
                    if (item != null && item.getData() instanceof DBPDriverDependencies.DependencyNode) {
                        if (UIUtils.getColumnAtPos(item, e.x, e.y) == 1) {
                            showVersionEditor(item);
                            return;
                        }
                    }
                    disposeOldEditor();
                }
            });
        }

        if (!wizard.isForceDownload()) {
            Label infoText = new Label(composite, SWT.NONE);
            infoText.setText("\nOr you can obtain driver files by yourself and add them in driver editor.");
            infoText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        createLinksPanel(composite);

        boldFont = UIUtils.makeBoldFont(filesTree.getFont());
        filesTree.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                UIUtils.dispose(boldFont);
            }
        });

        setControl(composite);
    }

    private void disposeOldEditor()
    {
        if (treeEditor.getEditor() != null) {
            treeEditor.getEditor().dispose();
        }
        Control oldEditor = treeEditor.getEditor();
        if (oldEditor != null) oldEditor.dispose();
    }

    private void showVersionEditor(final TreeItem item) {
        disposeOldEditor();
        final DBPDriverDependencies.DependencyNode dependencyNode = (DBPDriverDependencies.DependencyNode) item.getData();
        if (dependencyNode == null || dependencyNode.library == null || !dependencyNode.library.isDownloadable()) {
            return;
        }
        final List<String> allVersions = new ArrayList<>();
        try {
            getContainer().run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        allVersions.addAll(
                            dependencyNode.library.getAvailableVersions(new DefaultProgressMonitor(monitor)));
                    } catch (IOException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(getShell(), "Versions", "Error reading versions", e.getTargetException());
            return;
        } catch (InterruptedException e) {
            return;
        }
        final String currentVersion = dependencyNode.library.getVersion();
        if (currentVersion != null && !allVersions.contains(currentVersion)) {
            allVersions.add(currentVersion);
        }

        final Combo editor = new Combo(filesTree, SWT.DROP_DOWN | SWT.READ_ONLY);
        int versionIndex = -1;
        for (int i = 0; i < allVersions.size(); i++) {
            String version = allVersions.get(i);
            editor.add(version);
            if (version.equals(currentVersion)) {
                versionIndex = i;
            }
        }
        if (versionIndex >= 0) {
            editor.select(versionIndex);
        }
        editor.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String newVersion = editor.getItem(editor.getSelectionIndex());
                disposeOldEditor();
                setLibraryVersion(dependencyNode.library, newVersion);
            }
        });

        treeEditor.setEditor(editor, item, 1);
        editor.setListVisible(true);
    }

    private boolean setLibraryVersion(final DBPDriverLibrary library, final String version) {
        String curVersion = library.getVersion();
        if (CommonUtils.equalObjects(curVersion, version)) {
            return false;
        }
        try {
            getContainer().run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        DBPDriverLibrary newVersion = library.createVersion(new DefaultProgressMonitor(monitor), version);
                        DriverDescriptor driver = getWizard().getDriver();
                        driver.removeDriverLibrary(library);
                        driver.addDriverLibrary(newVersion);
                        getWizard().getDependencies().changeLibrary(library, newVersion);
                    } catch (IOException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
            resolveLibraries();
            return true;
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(getShell(), "Version change", "Error changing library version", e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
        }
        return false;
    }

    @Override
    void resolveLibraries() {
        final DriverDependencies dependencies = getWizard().getDependencies();
        try {
            new RunnableContextDelegate(getContainer()).run(true, true, new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Resolve dependencies", 100);
                    try {
                        dependencies.resolveDependencies(monitor);
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

        filesTree.removeAll();
        int totalItems = 1;
        for (DBPDriverDependencies.DependencyNode node : dependencies.getLibraryMap()) {
            DBPDriverLibrary library = node.library;
            TreeItem item = new TreeItem(filesTree, SWT.NONE);
            item.setData(node);
            item.setImage(DBeaverIcons.getImage(library.getIcon()));
            item.setText(0, library.getDisplayName());
            item.setText(1, CommonUtils.notEmpty(library.getVersion()));
            item.setText(2, CommonUtils.notEmpty(library.getDescription()));
            item.setFont(1, boldFont);
            totalItems++;
            if (addDependencies(item, node)) {
                item.setExpanded(true);
                totalItems += item.getItemCount();
            }
        }
        UIUtils.packColumns(filesTree);

        Shell shell = getContainer().getShell();
        Point curSize = shell.getSize();
        int itemHeight = filesTree.getItemHeight();
        shell.setSize(curSize.x, Math.min(
            (int)(DBeaverUI.getActiveWorkbenchWindow().getShell().getSize().y * 0.66),
            shell.computeSize(SWT.DEFAULT, SWT.DEFAULT).y) + itemHeight);
        shell.layout();

        // Check missing files
        int missingFiles = 0;
        for (DBPDriverDependencies.DependencyNode node : dependencies.getLibraryList()) {
            File localFile = node.library.getLocalFile();
            if (localFile == null || !localFile.exists()) {
                missingFiles++;
            }
        }
        if (missingFiles == 0) {
//            UIUtils.showMessageBox(getShell(), "Driver Download", "All driver files are present", SWT.ICON_INFORMATION);
//            ((DriverDownloadDialog)getWizard().getContainer()).closeWizard();
        }
    }

    private boolean addDependencies(TreeItem parent, DBPDriverDependencies.DependencyNode node) {
        Collection<DBPDriverDependencies.DependencyNode> dependencies = node.dependencies;
        if (dependencies != null && !dependencies.isEmpty()) {
            for (DBPDriverDependencies.DependencyNode dep : dependencies) {
                TreeItem item = new TreeItem(parent, SWT.NONE);
                //item.setData(dep);
                item.setImage(DBeaverIcons.getImage(dep.library.getIcon()));
                item.setText(0, dep.library.getDisplayName());
                item.setText(1, CommonUtils.notEmpty(dep.library.getVersion()));
                item.setText(2, CommonUtils.notEmpty(dep.library.getDescription()));

                if (dep.duplicate) {
                    item.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
                } else {
                    addDependencies(item, dep);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isPageComplete() {
        return true;
    }

    @Override
    void performFinish() {
        downloadLibraryFiles(new RunnableContextDelegate(getContainer()));
    }

    private void downloadLibraryFiles(DBRRunnableContext runnableContext)
    {
        if (!getWizard().getDriver().acceptDriverLicenses(runnableContext)) {
            return;
        }

        try {
            runnableContext.run(true, true, new LibraryDownloader());
            getWizard().getDriver().setModified(true);
            DataSourceProviderRegistry.getInstance().saveDrivers();
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(getShell(), "Driver download", "Error downloading driver files", e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
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

    private class LibraryDownloader implements DBRRunnableWithProgress {
        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            List<DBPDriverDependencies.DependencyNode> nodes = getWizard().getDependencies().getLibraryList();
            for (int i = 0, filesSize = nodes.size(); i < filesSize; ) {
                DBPDriverLibrary lib = nodes.get(i).library;
                int result = IDialogConstants.OK_ID;
                try {
                    lib.downloadLibraryFile(monitor, false, "Download " + (i + 1) + "/" + filesSize);
                } catch (IOException e) {
                    if (lib.getType() == DBPDriverLibrary.FileType.license) {
                        result = IDialogConstants.OK_ID;
                    } else {
                        DownloadRetry retryConfirm = new DownloadRetry(lib, e);
                        UIUtils.runInUI(null, retryConfirm);
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
        }
    }
}
