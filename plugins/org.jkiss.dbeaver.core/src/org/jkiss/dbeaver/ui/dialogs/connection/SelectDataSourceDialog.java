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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;

/**
 * SelectDataSourceDialog
 *
 * @author Serge Rider
 */
public class SelectDataSourceDialog extends Dialog {

    private static final String PARAM_SHOW_CONNECTED = "showConnected";
    private static final String PARAM_SHOW_ALL_PROJECTS = "showAllProjects";

    @Nullable
    private final IProject project;
    private DBPDataSourceContainer dataSource = null;

    private static final String DIALOG_ID = "DBeaver.SelectDataSourceDialog";//$NON-NLS-1$
    private boolean showConnected;
    private boolean showAllProjects;
    private DBNNode projectNode;
    private DBNNode rootNode;

    public SelectDataSourceDialog(@NotNull Shell parentShell, @Nullable IProject project, DBPDataSourceContainer selection)
    {
        super(parentShell);
        this.project = project;
        this.dataSource = selection;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(CoreMessages.dialog_select_datasource_title);

        showConnected = getDialogBoundsSettings().getBoolean(PARAM_SHOW_CONNECTED);
        showAllProjects = getDialogBoundsSettings().getBoolean(PARAM_SHOW_ALL_PROJECTS);

        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);

        DBeaverCore core = DBeaverCore.getInstance();
        rootNode = core.getNavigatorModel().getRoot();
        projectNode = null;
        if (project != null) {
            DBNProject projectBaseNode = core.getNavigatorModel().getRoot().getProject(project);
            if (projectBaseNode != null) {
                projectNode = projectBaseNode.getDatabases();
            }
        }

        IFilter dsFilter = element -> element instanceof DBNProject || element instanceof DBNProjectDatabases || element instanceof DBNLocalFolder;
        DatabaseNavigatorTree dataSourceTree = new DatabaseNavigatorTree(group, getTreeRootNode(), SWT.SINGLE | SWT.BORDER, false, dsFilter);
        gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 500;
        gd.minimumHeight = 100;
        gd.minimumWidth = 100;
        dataSourceTree.setLayoutData(gd);

        final Text descriptionText = new Text(group, SWT.READ_ONLY);
        descriptionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        final Button showConnectedCheck = new Button(group, SWT.CHECK);
        showConnectedCheck.setText("Show connected databases only");
        showConnectedCheck.setSelection(showConnected);
        showConnectedCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                showConnected = showConnectedCheck.getSelection();
                dataSourceTree.getViewer().getControl().setRedraw(false);
                try {
                    dataSourceTree.getViewer().refresh();
                    if (showConnected) {
                        dataSourceTree.getViewer().expandAll();
                    }
                } finally {
                    dataSourceTree.getViewer().getControl().setRedraw(true);
                }
                getDialogBoundsSettings().put(PARAM_SHOW_CONNECTED, showConnected);
            }
        });
        final Button showAllProjectsCheck = new Button(group, SWT.CHECK);
        showAllProjectsCheck.setText("Show all projects");
        showAllProjectsCheck.setSelection(showAllProjects);
        showAllProjectsCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                showAllProjects = showAllProjectsCheck.getSelection();
                dataSourceTree.getViewer().getControl().setRedraw(false);
                try {
                    dataSourceTree.reloadTree(getTreeRootNode());
                    if (showAllProjects) {
                        dataSourceTree.getViewer().expandToLevel(3);
                    }
                } finally {
                    dataSourceTree.getViewer().getControl().setRedraw(true);
                }
                getDialogBoundsSettings().put(PARAM_SHOW_ALL_PROJECTS, showAllProjects);
            }
        });

        if (this.dataSource != null) {
            DBNDatabaseNode dsNode = core.getNavigatorModel().getNodeByObject(this.dataSource);
            if (dsNode != null) {
                dataSourceTree.getViewer().setSelection(new StructuredSelection(dsNode), true);
            }
        }

        dataSourceTree.getViewer().addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                if (showConnected) {
                    if (element instanceof DBNDataSource) {
                        return ((DBNDataSource) element).getDataSource() != null;
                    }
                    if (element instanceof DBNLocalFolder) {
                        return ((DBNLocalFolder) element).hasConnected();
                    }
                }
                return element instanceof DBNProject || element instanceof DBNProjectDatabases || element instanceof DBNLocalFolder || element instanceof DBNDataSource;
            }
        });
        dataSourceTree.getViewer().addSelectionChangedListener(
            event -> {
                IStructuredSelection structSel = (IStructuredSelection) event.getSelection();
                Object selNode = structSel.isEmpty() ? null : structSel.getFirstElement();
                if (selNode instanceof DBNDataSource) {
                    dataSource = ((DBNDataSource) selNode).getObject();
                    getButton(IDialogConstants.OK_ID).setEnabled(true);
                    String description = dataSource.getDescription();
                    if (description == null) {
                        description = dataSource.getName();
                    }
                    descriptionText.setText(description);
                } else {
                    dataSource = null;
                    getButton(IDialogConstants.OK_ID).setEnabled(false);
                }
            }
        );
        dataSourceTree.getViewer().addDoubleClickListener(event -> {
            if (getButton(IDialogConstants.OK_ID).isEnabled()) {
                okPressed();
            }
        });
        DBeaverUI.asyncExec(() -> {
            dataSourceTree.getViewer().getControl().setFocus();
            if (showConnected) {
                dataSourceTree.getViewer().expandAll();
            }
        });

        return group;
    }

    private DBNNode getTreeRootNode() {
        return showAllProjects || projectNode == null ? rootNode : projectNode;
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control ctl = super.createContents(parent);
        if (this.dataSource == null) {
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        }
        return ctl;
    }

    protected Control createButtonBar(Composite parent) {
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
        createButton(composite, IDialogConstants.OK_ID, "Select", true);
        createButton(composite, IDialogConstants.IGNORE_ID, "None", false);
        createButton(composite, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

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

}
