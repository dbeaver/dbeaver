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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.WebUtils;
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

class DriverDependenciesTree {

    public static final String NETWORK_TEST_URL = "https://repo1.maven.org";
    private DBRRunnableContext runnableContext;
    private DBPDriver driver;
    private Collection<? extends DBPDriverLibrary> libraries;
    private final DBPDriverDependencies dependencies;
    private boolean editable;

    private Tree filesTree;
    private TreeEditor treeEditor;
    private Font boldFont;

    public DriverDependenciesTree(Composite parent, DBRRunnableContext runnableContext, DBPDriverDependencies dependencies, DBPDriver driver, Collection<? extends DBPDriverLibrary> libraries, boolean editable) {
        this.runnableContext = runnableContext;
        this.driver = driver;
        this.libraries = libraries;
        this.dependencies = dependencies;
        this.editable = editable;

        filesTree = new Tree(parent, SWT.BORDER | SWT.FULL_SELECTION);
        filesTree.setHeaderVisible(true);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = filesTree.getHeaderHeight() + filesTree.getItemHeight() * 3;
        filesTree.setLayoutData(gd);
        UIUtils.createTreeColumn(filesTree, SWT.LEFT, "File");
        UIUtils.createTreeColumn(filesTree, SWT.LEFT, "Version");
        UIUtils.createTreeColumn(filesTree, SWT.LEFT, "Description");

        if (editable) {
            boldFont = UIUtils.makeBoldFont(filesTree.getFont());

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

            filesTree.addDisposeListener(e -> UIUtils.dispose(boldFont));
        }
    }

    public Tree getTree() {
        return filesTree;
    }

    public DBPDriver getDriver() {
        return driver;
    }

    public Collection<? extends DBPDriverLibrary> getLibraries() {
        return libraries;
    }

    public boolean loadLibDependencies() throws DBException {
        boolean resolved = false;
        try {
            runnableContext.run(true, true, monitor -> {
                monitor.beginTask("Resolve dependencies", 100);
                try {
                    dependencies.resolveDependencies(monitor);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            });
            resolved = true;
        } catch (InterruptedException e) {
            // User just canceled download
        } catch (InvocationTargetException e) {
            throw new DBException("Error resolving dependencies", e.getTargetException());
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
            if (editable) {
                item.setFont(1, boldFont);
            }
            totalItems++;
            if (addDependencies(item, node)) {
                item.setExpanded(true);
                totalItems += item.getItemCount();
            }
        }
        UIUtils.packColumns(filesTree);

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
        return resolved;
    }

    public boolean handleDownloadError(DBException e) {
        try {
            checkNetworkAccessible();
        } catch (DBException dbException) {
            DBWorkbench.getPlatformUI().showError("Download error",
                "Network error", dbException);
            return false;
        }
        DBWorkbench.getPlatformUI().showError("Resolve driver files", "Error downloading driver libraries", e);
        return true;
    }

    private void checkNetworkAccessible() throws DBException {
        try {
            WebUtils.openConnection(NETWORK_TEST_URL, GeneralUtils.getProductTitle());
        } catch (IOException e) {
            throw new DBException("Network unavailable:\n" + e.getClass().getName() + ":" + e.getMessage());
        }
    }

    public void resizeTree() {
        Shell shell = filesTree.getShell();
        Point curSize = shell.getSize();
        int itemHeight = filesTree.getItemHeight();
        shell.setSize(curSize.x, Math.min(
            (int)(UIUtils.getActiveWorkbenchWindow().getShell().getSize().y * 0.66),
            shell.computeSize(SWT.DEFAULT, SWT.DEFAULT).y) + itemHeight * 2);
        shell.layout();
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
                    item.setForeground(filesTree.getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
                } else {
                    addDependencies(item, dep);
                }
            }
            return true;
        }
        return false;
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
            runnableContext.run(true, true, monitor -> {
                try {
                    allVersions.addAll(
                        dependencyNode.library.getAvailableVersions(monitor));
                } catch (IOException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Versions", "Error reading versions", e.getTargetException());
            return;
        } catch (InterruptedException e) {
            return;
        }
        final String currentVersion = dependencyNode.library.getVersion();
        if (currentVersion != null && !allVersions.contains(currentVersion)) {
            allVersions.add(currentVersion);
        }

        final CCombo editor = new CCombo(filesTree, SWT.DROP_DOWN | SWT.READ_ONLY);
        editor.setVisibleItemCount(15);
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
            editor.setText(currentVersion);
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

    // This may be overrided
    protected void setLibraryVersion(DBPDriverLibrary library, String version) {

    }

}
