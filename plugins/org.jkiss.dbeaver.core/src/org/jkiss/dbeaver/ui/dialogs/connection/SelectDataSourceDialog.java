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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;

import java.util.List;

/**
 * SelectDataSourceDialog
 *
 * @author Serge Rieder
 */
public class SelectDataSourceDialog extends Dialog {

    private DBSDataSourceContainer dataSource = null;

    private static final String DIALOG_ID = "DBeaver.SelectDataSourceDialog";//$NON-NLS-1$

    private SelectDataSourceDialog(Shell parentShell)
    {
        super(parentShell);
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
        DBNProject rootNode = core.getNavigatorModel().getRoot().getProject(core.getProjectRegistry().getActiveProject());

        DatabaseNavigatorTree dataSourceTree = new DatabaseNavigatorTree(group, rootNode.getDatabases(), SWT.SINGLE, false);
        dataSourceTree.setLayoutData(new GridData(GridData.FILL_BOTH));
        dataSourceTree.getViewer().addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                return element instanceof DBNLocalFolder || element instanceof DBNDataSource;
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

    public DBSDataSourceContainer getDataSource()
    {
        return dataSource;
    }

    public static DBSDataSourceContainer selectDataSource(Shell parentShell)
    {
        DataSourceRegistry activeDataSourceRegistry = DBeaverCore.getInstance().getProjectRegistry().getActiveDataSourceRegistry();
		if (activeDataSourceRegistry == null) {
            UIUtils.showMessageBox(parentShell, CoreMessages.dialog_select_datasource_error_title, CoreMessages.dialog_select_datasource_error_message, SWT.ICON_ERROR);
            return null;
		}
        List<DataSourceDescriptor> datasources = activeDataSourceRegistry.getDataSources();
        if (datasources.isEmpty()) {
            UIUtils.showMessageBox(parentShell, CoreMessages.dialog_select_datasource_error_title, CoreMessages.dialog_select_datasource_error_message, SWT.ICON_ERROR);
            return null;
        } else if (datasources.size() == 1) {
            return datasources.get(0);
        } else {
            SelectDataSourceDialog scDialog = new SelectDataSourceDialog(parentShell);
            if (scDialog.open() == IDialogConstants.OK_ID) {
                return scDialog.getDataSource();
            } else {
                return null;
            }
        }
    }

}
