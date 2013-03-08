/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProjectDatabases;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.views.navigator.database.load.TreeLoadNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * BrowseObjectDialog
 *
 * @author Serge Rieder
 */
public class BrowseObjectDialog extends Dialog {

    private String title;
    private DBNNode rootNode;
    private boolean singleSelection;
    private Class[] allowedTypes;
    private List<DBNNode> selectedObjects = new ArrayList<DBNNode>();
    private DatabaseNavigatorTree navigatorTree;

    private BrowseObjectDialog(
        Shell parentShell,
        String title,
        DBNNode rootNode,
        boolean singleSelection,
        Class[] allowedTypes)
    {
        super(parentShell);
        this.title = title;
        this.rootNode = rootNode;
        this.singleSelection = singleSelection;
        this.allowedTypes = allowedTypes;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(title);

        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);

        navigatorTree = new DatabaseNavigatorTree(group, rootNode, (singleSelection ? SWT.SINGLE : SWT.MULTI) | SWT.BORDER);
        gd = new GridData(GridData.FILL_BOTH);
        navigatorTree.setLayoutData(gd);

        navigatorTree.getViewer().addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                if (element instanceof TreeLoadNode) {
                    return true;
                }
                if (element instanceof DBNNode) {
                    if (element instanceof DBNDatabaseFolder) {
                        DBNDatabaseFolder folder = (DBNDatabaseFolder) element;
                        Class<? extends DBSObject> folderItemsClass = folder.getChildrenClass();
                        return folderItemsClass != null && DBSObjectContainer.class.isAssignableFrom(folderItemsClass);
                    }
                    if (element instanceof DBNProjectDatabases || element instanceof DBNDataSource || (element instanceof DBSWrapper && ((DBSWrapper) element).getObject() instanceof DBSObjectContainer)) {
                        return true;
                    }
                }
                return false;
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

    @Override
    protected void okPressed()
    {
        IStructuredSelection selection = (IStructuredSelection) navigatorTree.getViewer().getSelection();
        super.okPressed();
    }

    public List<DBNNode> getSelectedObjects()
    {
        return selectedObjects;
    }

    public static List<DBNNode> selectObjects(Shell parentShell, String title, DBNNode rootNode, Class ... allowedTypes)
    {
        BrowseObjectDialog scDialog = new BrowseObjectDialog(parentShell, title, rootNode, false, allowedTypes);
        if (scDialog.open() == IDialogConstants.OK_ID) {
            return scDialog.getSelectedObjects();
        } else {
            return null;
        }
    }

    public static DBNNode selectObject(Shell parentShell, String title, DBNNode rootNode, Class ... allowedTypes)
    {
        BrowseObjectDialog scDialog = new BrowseObjectDialog(parentShell, title, rootNode, true, allowedTypes);
        if (scDialog.open() == IDialogConstants.OK_ID) {
            List<DBNNode> result = scDialog.getSelectedObjects();
            return result.isEmpty() ? null : result.get(0);
        } else {
            return null;
        }
    }

}
