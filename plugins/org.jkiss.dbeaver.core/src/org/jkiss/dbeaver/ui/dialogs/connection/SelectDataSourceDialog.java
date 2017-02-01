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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;

import java.util.List;

/**
 * SelectDataSourceDialog
 *
 * @author Serge Rider
 */
public class SelectDataSourceDialog extends Dialog {

    @Nullable
    private final IProject project;
    private DBPDataSourceContainer dataSource = null;

    private static final String DIALOG_ID = "DBeaver.SelectDataSourceDialog";//$NON-NLS-1$

    private SelectDataSourceDialog(@NotNull Shell parentShell, @Nullable IProject project)
    {
        super(parentShell);
        this.project = project;
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

        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);

        DBeaverCore core = DBeaverCore.getInstance();
        DBNNode rootNode = null;
        if (project != null) {
            DBNProject projectNode = core.getNavigatorModel().getRoot().getProject(project);
            if (projectNode != null) {
                rootNode = projectNode.getDatabases();
            }
        }
        if (rootNode == null) {
            rootNode = core.getNavigatorModel().getRoot();
        }

        DatabaseNavigatorTree dataSourceTree = new DatabaseNavigatorTree(group, rootNode, SWT.SINGLE | SWT.BORDER, false);
        dataSourceTree.setLayoutData(new GridData(GridData.FILL_BOTH));

        final Text descriptionText = new Text(group, SWT.READ_ONLY);
        descriptionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        dataSourceTree.getViewer().addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                return element instanceof DBNProject || element instanceof DBNProjectDatabases || element instanceof DBNLocalFolder || element instanceof DBNDataSource;
            }
        });
        dataSourceTree.getViewer().addSelectionChangedListener(
            new ISelectionChangedListener() {
                @Override
                public void selectionChanged(SelectionChangedEvent event)
                {
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
            }
        );
        dataSourceTree.getViewer().addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event)
            {
                if (getButton(IDialogConstants.OK_ID).isEnabled()) {
                    okPressed();
                }
            }
        });


        return group;
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control ctl = super.createContents(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
        return ctl;
    }

    public DBPDataSourceContainer getDataSource()
    {
        return dataSource;
    }

    public static DBPDataSourceContainer selectDataSource(@NotNull Shell parentShell, @Nullable IProject project)
    {
        List<DataSourceDescriptor> datasources = DataSourceRegistry.getAllDataSources();
        if (datasources.size() == 1) {
            return datasources.get(0);
        } else {
            SelectDataSourceDialog scDialog = new SelectDataSourceDialog(parentShell, project);
            if (scDialog.open() == IDialogConstants.OK_ID) {
                return scDialog.getDataSource();
            } else {
                return null;
            }
        }
    }

}
