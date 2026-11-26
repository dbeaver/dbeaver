/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorContentProvider;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilter;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeChildrenLoading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * SelectDataSourceDialog
 *
 * @author Serge Rider
 */
public class SelectDataSourceDialog extends AbstractPopupPanel {

    private static final String PARAM_SHOW_CONNECTED = "showConnected"; //$NON-NLS-1$
    private static final String PARAM_SHOW_ALL_PROJECTS = "showAllProjects"; //$NON-NLS-1$

    @Nullable
    private final DBPProject project;
    private final boolean allowAllProjects;
    private final boolean multiSelection;
    @Nullable
    private final Predicate<DBPProject> projectFilter;
    @Nullable
    private final Predicate<DBPDataSourceContainer> dataSourceFilter;
    private DBPDataSourceContainer dataSource;
    private List<DBPDataSourceContainer> selectedDataSources = List.of();

    private static final String DIALOG_ID = "DBeaver.SelectDataSourceDialog";//$NON-NLS-1$
    private boolean showConnected;
    private boolean showAllProjects;
    private DBNProjectDatabases projectNode;
    private DBNNode rootNode;

    public SelectDataSourceDialog(
        @NotNull Shell parentShell,
        @Nullable DBPProject project,
        @Nullable DBPDataSourceContainer selection
    ) {
        this(parentShell, project, selection, true);
    }

    public SelectDataSourceDialog(
        @NotNull Shell parentShell,
        @Nullable DBPProject project,
        @Nullable DBPDataSourceContainer selection,
        boolean allowAllProjects
    ) {
        this(parentShell, project, selection, allowAllProjects, false, null, null);
    }

    private SelectDataSourceDialog(
        @NotNull Shell parentShell,
        @Nullable DBPProject project,
        @Nullable DBPDataSourceContainer selection,
        boolean allowAllProjects,
        boolean multiSelection,
        @Nullable Predicate<DBPProject> projectFilter,
        @Nullable Predicate<DBPDataSourceContainer> dataSourceFilter
    ) {
        super(parentShell, UINavigatorMessages.dialog_select_datasource_title);
        this.project = project;
        this.dataSource = selection;
        this.allowAllProjects = allowAllProjects;
        this.multiSelection = multiSelection;
        this.projectFilter = projectFilter;
        this.dataSourceFilter = dataSourceFilter;
    }

    @NotNull
    public static SelectDataSourceDialog forMultipleConnections(
        @NotNull Shell parentShell,
        @NotNull Predicate<DBPProject> projectFilter,
        @NotNull Predicate<DBPDataSourceContainer> dataSourceFilter
    ) {
        return new SelectDataSourceDialog(parentShell, null, null, true, true, projectFilter, dataSourceFilter);
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent)
    {
        showConnected = !multiSelection && getDialogBoundsSettings().getBoolean(PARAM_SHOW_CONNECTED);
        showAllProjects = allowAllProjects && getDialogBoundsSettings().getBoolean(PARAM_SHOW_ALL_PROJECTS);

        Composite group = super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);

        rootNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot();
        projectNode = null;
        if (project != null) {
            DBNProject projectBaseNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProjectNode(project);
            if (projectBaseNode != null) {
                projectNode = projectBaseNode.getDatabases();
            }
        }

        INavigatorFilter dsFilter = new DatabaseNavigatorTreeFilter() {
            @Override
            public boolean filterFolders() {
                return true;
            }

            @Override
            public boolean isLeafObject(Object object) {
                return object instanceof DBNDataSource;
            }

            @Override
            public boolean filterObjectByPattern(Object object) {
                return object instanceof DBNDataSource;
            }

            @Override
            public boolean select(@NotNull Object element) {
                // Keep the loading placeholder visible so lazy projects stay expanded until their children arrive.
                return element instanceof TreeNodeChildrenLoading ||
                    element instanceof DBNProject ||
                    element instanceof DBNProjectDatabases ||
                    element instanceof DBNLocalFolder ||
                    element instanceof DBNDataSource;
            }
        };
        DatabaseNavigatorTree dataSourceTree = new DatabaseNavigatorTree(
            group,
            getTreeRootNode(),
            (multiSelection ? SWT.MULTI : SWT.SINGLE) | SWT.BORDER,
            false,
            dsFilter,
            UINavigatorMessages.filter_connection_name_placeholder)
        {
            @NotNull
            @Override
            protected DatabaseNavigatorContentProvider createContentProvider(boolean showRoot) {
                // Flatten the Connections container in this selector and map its children back to the project
                // so selection and reveal use the same hierarchy as the displayed tree.
                return new DatabaseNavigatorContentProvider(this, showRoot) {
                    @NotNull
                    @Override
                    public Object[] getChildren(@NotNull Object parent) {
                        Object[] children = super.getChildren(parent);
                        if (!(parent instanceof DBNProject)) {
                            return children;
                        }
                        List<Object> result = new ArrayList<>();
                        for (Object child : children) {
                            if (child instanceof DBNProjectDatabases) {
                                Collections.addAll(result, super.getChildren(child));
                            } else {
                                result.add(child);
                            }
                        }
                        return result.toArray();
                    }

                    @Nullable
                    @Override
                    public Object getParent(@NotNull Object child) {
                        Object parent = super.getParent(child);
                        return parent instanceof DBNProjectDatabases databases ? databases.getParentNode() : parent;
                    }
                };
            }

            @Override
            protected void onTreeRefresh() {
                DBNNode treeRootNode = getTreeRootNode();
                if (dataSource != null && projectNode != null) {
                    DBPDataSourceFolder dsFolder;
                    for (dsFolder = dataSource.getFolder(); dsFolder != null; dsFolder = dsFolder.getParent()) {
                        if (dsFolder.getParent() == null) {
                            break;
                        }
                    }
                    if (dsFolder != null) {
                        // Expand only current DS folder
                        DBNLocalFolder folderNode = projectNode.getFolderNode(dsFolder);
                        if (folderNode != null) {
                            expandFolders(this, folderNode);
                        }
                    }
                    return;
                }
                expandFolders(this, treeRootNode);
            }
        };
        gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 500;
        gd.minimumHeight = 100;
        gd.minimumWidth = 100;
        dataSourceTree.setLayoutData(gd);

        TreeViewer treeViewer = dataSourceTree.getViewer();

        Text descriptionText = new Text(group, SWT.READ_ONLY);
        descriptionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button showConnectedCheck = new Button(group, SWT.CHECK);
        showConnectedCheck.setText(UINavigatorMessages.label_show_connected);
        showConnectedCheck.setSelection(showConnected);
        showConnectedCheck.setVisible(!multiSelection);
        showConnectedCheck.setLayoutData(GridDataFactory.swtDefaults().exclude(multiSelection).create());
        showConnectedCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
                showConnected = showConnectedCheck.getSelection();
                treeViewer.getControl().setRedraw(false);
                try {
                    treeViewer.refresh();
                    if (showConnected) {
                        expandFolders(dataSourceTree, getTreeRootNode());
                    }
                } finally {
                    treeViewer.getControl().setRedraw(true);
                }
                getDialogBoundsSettings().put(PARAM_SHOW_CONNECTED, showConnected);
            }));
        final Button showAllProjectsCheck = new Button(group, SWT.CHECK);
        boolean showAllProjectsOption = project != null && allowAllProjects;
        showAllProjectsCheck.setLayoutData(GridDataFactory.swtDefaults().exclude(!showAllProjectsOption).create());
        showAllProjectsCheck.setVisible(showAllProjectsOption);
        showAllProjectsCheck.setText(UINavigatorMessages.label_show_all_projects);
        showAllProjectsCheck.setSelection(showAllProjects);
        showAllProjectsCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
                showAllProjects = showAllProjectsCheck.getSelection();
                treeViewer.getControl().setRedraw(false);
                try {
                    dataSourceTree.reloadTree(getTreeRootNode());
                    if (showAllProjects) {
                        expandFolders(dataSourceTree, getTreeRootNode());
                    }
                } finally {
                    treeViewer.getControl().setRedraw(true);
                }
                getDialogBoundsSettings().put(PARAM_SHOW_ALL_PROJECTS, showAllProjects);
            }));

        if (this.dataSource != null) {
            DBNDatabaseNode dsNode = DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(this.dataSource);
            if (dsNode != null) {
                treeViewer.setSelection(new StructuredSelection(dsNode), true);
            }
        }
        group.setTabList(new Control[] { dataSourceTree, showConnectedCheck, showAllProjectsCheck} );

        treeViewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                if (element instanceof DBNProject projectNode && projectFilter != null
                    && !projectFilter.test(projectNode.getProject())) {
                    return false;
                }
                if (element instanceof DBNDataSource connectionNode && dataSourceFilter != null
                    && !dataSourceFilter.test(connectionNode.getDataSourceContainer())) {
                    return false;
                }
                if (showConnected) {
                    if (element instanceof DBNDataSource) {
                        return ((DBNDataSource) element).getDataSource() != null;
                    }
                    if (element instanceof DBNLocalFolder) {
                        return ((DBNLocalFolder) element).hasConnected();
                    }
                }
                return element instanceof TreeNodeChildrenLoading || element instanceof DBNProject || element instanceof DBNProjectDatabases || element instanceof DBNLocalFolder || element instanceof DBNDataSource;
            }
        });
        treeViewer.addSelectionChangedListener(
            event -> {
                IStructuredSelection structSel = (IStructuredSelection) event.getSelection();
                List<DBPDataSourceContainer> selected = new ArrayList<>();
                for (Object element : structSel) {
                    if (element instanceof DBNDataSource dbnDataSource) {
                        selected.add(dbnDataSource.getObject());
                    }
                }
                selectedDataSources = selected;
                dataSource = selected.isEmpty() ? null : selected.getFirst();
                if (dataSource != null) {
                    enableButton(IDialogConstants.OK_ID, true);
                    String description = dataSource.getDescription();
                    if (description == null) {
                        description = dataSource.getName();
                    }
                    descriptionText.setText(description);
                } else {
                    enableButton(IDialogConstants.OK_ID, false);
                }
            }
        );
        treeViewer.addDoubleClickListener(event -> {
            if (isButtonEnabled(IDialogConstants.OK_ID)) {
                okPressed();
            }
        });
        Text filterControl = dataSourceTree.getFilterControl();
        UIUtils.asyncExec(() -> {
            Point treeSize = dataSourceTree.getViewer().getTree().computeSize(SWT.DEFAULT, SWT.DEFAULT);
            Point shellSize = getShell().getSize();
            if (treeSize.x >= shellSize.x) {
                Point shellCompSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
                getShell().setSize(shellCompSize.x, shellSize.y);
                getShell().layout(true);
            }
            if (filterControl != null) {
                filterControl.setFocus();
            }
            if (showConnected) {
                expandFolders(dataSourceTree, getTreeRootNode());
            }
        });

        closeOnFocusLost();

        return group;
    }

    private void expandFolders(@NotNull DatabaseNavigatorTree dataSourceTree, @NotNull DBNNode node) {
        if (node instanceof DBNLocalFolder || node instanceof DBNProjectDatabases || node instanceof DBNProject || node instanceof DBNRoot) {
            if (node instanceof DBNProject p && (!p.getProject().isOpen() || p.getProject() != project)) {
                // Expand only the current project automatically; let the user expand other projects.
                return;
            }
            if (!(node instanceof DBNProjectDatabases)) {
                dataSourceTree.getViewer().expandToLevel(node, 1);
            }
            DBNNode[] childNodes;
            try {
                childNodes = node.getChildren(new VoidProgressMonitor());
            } catch (DBException e) {
                return;
            }
            if (childNodes != null) {
                for (DBNNode childNode : childNodes) {
                    expandFolders(dataSourceTree, childNode);
                }
            }
        }
    }

    private DBNNode getTreeRootNode() {
        return showAllProjects || projectNode == null ? rootNode : projectNode;
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control ctl = super.createContents(parent);
        if (this.dataSource == null) {
            enableButton(IDialogConstants.OK_ID, false);
        }
        return ctl;
    }

    @NotNull
    protected Control createButtonBar(@NotNull Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        composite.setLayout(layout);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER);
        composite.setLayoutData(gd);
        composite.setFont(parent.getFont());

        // Add the buttons to the button bar.
        createButton(composite, IDialogConstants.OK_ID, UINavigatorMessages.dialog_select_datasource_button_select, true);
        if (!multiSelection) {
            createButton(composite, IDialogConstants.IGNORE_ID, UINavigatorMessages.dialog_select_datasource_button_none, false);
        }
//        if (!isModeless()) {
//            createButton(composite, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
//        }

        return composite;
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.IGNORE_ID) {
            dataSource = null;
            buttonId = IDialogConstants.OK_ID;
        }
        super.buttonPressed(buttonId);
    }

    public DBPDataSourceContainer getDataSource()
    {
        return dataSource;
    }

    @NotNull
    public List<DBPDataSourceContainer> getSelectedDataSources() {
        return List.copyOf(selectedDataSources);
    }

}
