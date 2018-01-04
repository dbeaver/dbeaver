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
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilter;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeLoadNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * BrowseObjectDialog
 *
 * @author Serge Rider
 */
public class BrowseObjectDialog extends Dialog {

    private String title;
    private DBNNode rootNode;
    private DBNNode selectedNode;
    private boolean singleSelection;
    private Class<?>[] allowedTypes;
    private Class<?>[] resultTypes;
    private List<DBNNode> selectedObjects = new ArrayList<>();
    private DatabaseNavigatorTree navigatorTree;

    private BrowseObjectDialog(
        Shell parentShell,
        String title,
        DBNNode rootNode,
        DBNNode selectedNode,
        boolean singleSelection,
        Class<?>[] allowedTypes,
        Class<?>[] resultTypes)
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

        navigatorTree = new DatabaseNavigatorTree(group, rootNode, (singleSelection ? SWT.SINGLE : SWT.MULTI) | SWT.BORDER, false, new DatabaseNavigatorTreeFilter());
        gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 500;
        gd.heightHint = 500;
        navigatorTree.setLayoutData(gd);

        final TreeViewer treeViewer = navigatorTree.getViewer();
        treeViewer.addFilter(new ViewerFilter() {
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
                    if (element instanceof DBNProject || element instanceof DBNProjectDatabases ||
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
            treeViewer.setSelection(new StructuredSelection(selectedNode));
            selectedObjects.add(selectedNode);
        }
        treeViewer.addSelectionChangedListener(event -> {
            selectedObjects.clear();
            IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
            for (Iterator iter = selection.iterator(); iter.hasNext(); ) {
                DBNNode node = (DBNNode) iter.next();
                if (node instanceof DBSWrapper) {
                    DBSObject object = DBUtils.getAdapter(DBSObject.class, ((DBSWrapper) node).getObject());
                    if (object != null) {
                        if (!matchesType(object.getClass(), true)) {
                            selectedObjects.clear();
                            break;
                        }
                        selectedObjects.add(node);
                    }
                }
            }
            getButton(IDialogConstants.OK_ID).setEnabled(!selectedObjects.isEmpty());
        });
        treeViewer.addDoubleClickListener(event -> okPressed());
        treeViewer.getTree().setFocus();

        return group;
    }

    private boolean matchesType(Class<?> nodeType, boolean result)
    {
        for (Class<?> ot : result ? resultTypes : allowedTypes) {
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

    public static DBNNode selectObject(Shell parentShell, String title, DBNNode rootNode, DBNNode selectedNode, Class<?>[] allowedTypes, Class<?>[] resultTypes)
    {
        BrowseObjectDialog scDialog = new BrowseObjectDialog(parentShell, title, rootNode, selectedNode, true, allowedTypes, resultTypes);
        if (scDialog.open() == IDialogConstants.OK_ID) {
            List<DBNNode> result = scDialog.getSelectedObjects();
            return result.isEmpty() ? null : result.get(0);
        } else {
            return null;
        }
    }

    public static List<DBNNode> selectObjects(Shell parentShell, String title, DBNNode rootNode, DBNNode selectedNode, Class<?>[] allowedTypes, Class<?>[] resultTypes)
    {
        BrowseObjectDialog scDialog = new BrowseObjectDialog(parentShell, title, rootNode, selectedNode, false, allowedTypes, resultTypes);
        if (scDialog.open() == IDialogConstants.OK_ID) {
            return scDialog.getSelectedObjects();
        } else {
            return null;
        }
    }

}
