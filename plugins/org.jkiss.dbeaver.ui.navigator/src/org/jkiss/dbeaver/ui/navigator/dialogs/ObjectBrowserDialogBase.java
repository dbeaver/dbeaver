/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNObjectNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilter;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilterObjectType;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeSpecial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ObjectBrowserDialog
 *
 * @author Serge Rider
 */
public abstract class ObjectBrowserDialogBase extends Dialog {
    /** Expands first level of each node */
    private static final int TREE_EXPANSION_DEPTH = 2;

    private final String title;
    private final DBNNode rootNode;
    private final DBNNode[] selectedNodes;
    private final boolean singleSelection;
    private final List<DBNNode> selectedObjects = new ArrayList<>();
    private TreeNodeSpecial specialNode;
    private DatabaseNavigatorTree navigatorTree;

    private static boolean showConnected;

    protected ObjectBrowserDialogBase(
        @NotNull Shell parentShell,
        @NotNull String title,
        @NotNull DBNNode rootNode,
        @NotNull List<? extends DBNNode> selectedNodes,
        boolean singleSelection
    ) {
        super(parentShell);
        this.title = title;
        this.rootNode = rootNode;
        this.selectedNodes = selectedNodes.toArray(DBNNode[]::new);
        this.singleSelection = singleSelection;
    }

    public static boolean isShowConnected() {
        return showConnected;
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

        DatabaseNavigatorTreeFilter navigatorFilter = createNavigatorFilter();
        navigatorTree = new DatabaseNavigatorTree(group, rootNode, (singleSelection ? SWT.SINGLE : SWT.MULTI) | SWT.BORDER, false, navigatorFilter);
        gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 500;
        gd.heightHint = 500;
        navigatorTree.setLayoutData(gd);

        navigatorTree.setFilterObjectType(DatabaseNavigatorTreeFilterObjectType.connection);

        final TreeViewer treeViewer = navigatorTree.getViewer();
        ViewerFilter viewerFilter = createViewerFilter();
        if (viewerFilter != null) {
            treeViewer.addFilter(viewerFilter);
        }
        if (selectedNodes.length > 0) {
            treeViewer.setSelection(new StructuredSelection(selectedNodes));
            Collections.addAll(selectedObjects, selectedNodes);

            for (DBNNode node : selectedNodes) {
                if (!(node instanceof DBNDataSource dataSource) || dataSource.getDataSourceContainer().isConnected()) {
                    treeViewer.expandToLevel(selectedNodes, 1);
                }
            }
        }
        treeViewer.addSelectionChangedListener(event -> {
            selectedObjects.clear();
            specialNode = null;
            IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
            for (Object node : selection) {
                if (node instanceof DBNNode) {
                    if (matchesResultNode((DBNNode)node)) {
                        selectedObjects.add((DBNNode) node);
                    } else {
                        selectedObjects.clear();
                    }
                } else if (node instanceof TreeNodeSpecial) {
                    specialNode = (TreeNodeSpecial) node;
                }
            }
            getButton(IDialogConstants.OK_ID).setEnabled(!selectedObjects.isEmpty());
        });
        treeViewer.addDoubleClickListener(event -> {
            if (!selectedObjects.isEmpty()) {
                okPressed();
            } else if (specialNode != null) {
                specialNode.handleDefaultAction(navigatorTree);
            }
        });
        treeViewer.getTree().setFocus();

        if (rootNode instanceof DBNContainer && ((DBNContainer) rootNode).getChildrenClass() == DBPDataSourceContainer.class) {
            final Button showConnectedCheck = new Button(group, SWT.CHECK);
            showConnectedCheck.setText(UINavigatorMessages.label_show_connected);
            showConnectedCheck.setSelection(showConnected);
            showConnectedCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    showConnected = showConnectedCheck.getSelection();
                    treeViewer.getControl().setRedraw(false);
                    try {
                        treeViewer.refresh();
                        if (showConnected) {
                            treeViewer.expandToLevel(TREE_EXPANSION_DEPTH, false);
                        }
                    } finally {
                        treeViewer.getControl().setRedraw(true);
                    }
                }
            });
        }

        return group;
    }

    protected boolean matchesResultNode(DBNNode node) {
        if (node instanceof DBSWrapper) {
            DBSObject object = ((DBSWrapper) node).getObject();
            if (object != null && matchesType(object, true)) {
                return true;
            }
            object = DBUtils.getAdapter(DBSObject.class, object);
            return object != null && matchesType(object, true);
        } else if (node instanceof DBNObjectNode) {
            return matchesType(((DBNObjectNode) node).getNodeObject(), true);
        } else {
            return matchesType(node, true);
        }
    }

    protected boolean matchesType(Object object, boolean result) {
        return true;
    }

    protected DatabaseNavigatorTreeFilter createNavigatorFilter() {
        return null;
    }

    protected ViewerFilter createViewerFilter() {
        return null;
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

}
