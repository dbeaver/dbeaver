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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeLoadNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * BrowseObjectDialog
 *
 * @author Serge Rieder
 */
public class BrowseObjectDialog extends Dialog {

    private String title;
    private DBNNode rootNode;
    private DBNNode selectedNode;
    private boolean singleSelection;
    private Class[] allowedTypes;
    private Class[] resultTypes;
    private List<DBNNode> selectedObjects = new ArrayList<>();
    private DatabaseNavigatorTree navigatorTree;

    private BrowseObjectDialog(
        Shell parentShell,
        String title,
        DBNNode rootNode,
        DBNNode selectedNode,
        boolean singleSelection,
        Class[] allowedTypes,
        Class[] resultTypes)
    {
        super(parentShell);
        this.title = title;
        this.rootNode = rootNode;
        this.selectedNode = selectedNode;
        this.singleSelection = singleSelection;
        this.allowedTypes = allowedTypes;
        this.resultTypes = resultTypes == null ? allowedTypes : resultTypes;
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
        gd.widthHint = 500;
        gd.heightHint = 500;
        navigatorTree.setLayoutData(gd);

        navigatorTree.getViewer().addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                if (element instanceof TreeLoadNode || element instanceof DBNLocalFolder) {
                    return true;
                }
                if (element instanceof DBNNode) {
                    if (element instanceof DBNDatabaseFolder) {
                        DBNDatabaseFolder folder = (DBNDatabaseFolder) element;
                        Class<? extends DBSObject> folderItemsClass = folder.getChildrenClass();
                        return folderItemsClass != null && matchesType(folderItemsClass, false);
                    }
                    if (element instanceof DBNProjectDatabases ||
                        element instanceof DBNDataSource ||
                        (element instanceof DBSWrapper && matchesType(((DBSWrapper) element).getObject().getClass(), false)))
                    {
                        return true;
                    }
                }
                return false;
            }
        });
        if (selectedNode != null) {
            navigatorTree.getViewer().setSelection(new StructuredSelection(selectedNode));
            selectedObjects.add(selectedNode);
        }
        navigatorTree.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                selectedObjects.clear();
                IStructuredSelection selection = (IStructuredSelection) navigatorTree.getViewer().getSelection();
                for (Iterator iter = selection.iterator(); iter.hasNext(); ) {
                    DBNNode node = (DBNNode) iter.next();
                    if (node instanceof DBSWrapper) {
                        DBSObject object = DBUtils.getAdapter(DBSObject.class, ((DBSWrapper) node).getObject());
                        if (object != null && matchesType(object.getClass(), true)) {
                            selectedObjects.add(node);
                        }
                    }
                }
                getButton(IDialogConstants.OK_ID).setEnabled(!selectedObjects.isEmpty());
            }
        });

        return group;
    }

    private boolean matchesType(Class<?> nodeType, boolean result)
    {
        for (Class ot : result ? resultTypes : allowedTypes) {
            if (ot.isAssignableFrom(nodeType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control contents = super.createContents(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(!selectedObjects.isEmpty());
        return contents;
    }

    @Override
    protected void okPressed()
    {
        super.okPressed();
    }

    public List<DBNNode> getSelectedObjects()
    {
        return selectedObjects;
    }

/*
    public static List<DBNNode> selectObjects(Shell parentShell, String title, DBNNode rootNode, DBNNode selectedNode, Class ... allowedTypes)
    {
        BrowseObjectDialog scDialog = new BrowseObjectDialog(parentShell, title, rootNode, selectedNode, false, allowedTypes);
        if (scDialog.open() == IDialogConstants.OK_ID) {
            return scDialog.getSelectedObjects();
        } else {
            return null;
        }
    }
*/

    public static DBNNode selectObject(Shell parentShell, String title, DBNNode rootNode, DBNNode selectedNode, Class[] allowedTypes, Class[] resultTypes)
    {
        BrowseObjectDialog scDialog = new BrowseObjectDialog(parentShell, title, rootNode, selectedNode, true, allowedTypes, resultTypes);
        if (scDialog.open() == IDialogConstants.OK_ID) {
            List<DBNNode> result = scDialog.getSelectedObjects();
            return result.isEmpty() ? null : result.get(0);
        } else {
            return null;
        }
    }

}
