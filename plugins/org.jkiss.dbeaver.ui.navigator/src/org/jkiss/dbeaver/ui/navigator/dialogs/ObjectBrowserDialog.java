/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilter;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeSpecial;

import java.util.List;

/**
 * ObjectBrowserDialog
 *
 * @author Serge Rider
 */
public class ObjectBrowserDialog extends ObjectBrowserDialogBase {
    private Class<?>[] allowedTypes;
    private Class<?>[] resultTypes;
    private Class<?>[] leafTypes;

    private ObjectBrowserDialog(
        Shell parentShell,
        String title,
        DBNNode rootNode,
        DBNNode selectedNode,
        boolean singleSelection,
        Class<?>[] allowedTypes,
        Class<?>[] resultTypes,
        Class<?>[] leafTypes)
    {
        super(parentShell, title, rootNode, selectedNode, singleSelection);
        this.allowedTypes = allowedTypes;
        this.resultTypes = resultTypes == null ? allowedTypes : resultTypes;
        this.leafTypes = leafTypes;
    }

    @Override
    protected DatabaseNavigatorTreeFilter createNavigatorFilter() {
        return new DatabaseNavigatorTreeFilter() {
            @Override
            public boolean isLeafObject(Object object) {
                if (leafTypes != null && leafTypes.length > 0) {
                    if (object instanceof DBNDatabaseNode) {
                        DBNDatabaseNode node = (DBNDatabaseNode) object;
                        DBSObject dbObject = node.getObject();
                        DBXTreeNode meta = node.getMeta();
                        if (dbObject != null) {
                            for (Class<?> leafType : leafTypes) {
                                if (leafType.isAssignableFrom(dbObject.getClass())) {
                                    if (DBSObjectContainer.class.isAssignableFrom(leafType)) {
                                        return !DBNNode.nodeHasStructureContainers(node, meta); // Special case. Node has structure container inside if true (can be recursion)
                                    }
                                    return true;
                                }
                            }
                        }
                    }
                }
                return super.isLeafObject(object);
            }
        };
    }

    @Override
    protected ViewerFilter createViewerFilter() {
        return new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (isShowConnected()) {
                    if (element instanceof DBNDataSource) {
                        return ((DBNDataSource) element).getDataSource() != null;
                    }
                    if (element instanceof DBNLocalFolder) {
                        return ((DBNLocalFolder) element).hasConnected();
                    }
                }
                if (element instanceof TreeNodeSpecial || element instanceof DBNLocalFolder) {
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
                        (element instanceof DBSWrapper && matchesType(((DBSWrapper) element).getObject().getClass(), false))
                    ) {
                        return true;
                    }
                    if (!(element instanceof DBNDatabaseNode) && !((DBNNode) element).isPersisted()) {
                        // Show non-database nodes
                        return true;
                    }
                }
                return false;
            }
        };
    }

    @Override
    protected boolean matchesType(Object object, boolean result) {
        for (Class<?> ot : result ? resultTypes : allowedTypes) {
            if (ot.isAssignableFrom(object.getClass())) {
                return true;
            }
        }
        return false;
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

    public static DBNNode selectObject(Shell parentShell, String title, DBNNode rootNode, DBNNode selectedNode, Class<?>[] allowedTypes, Class<?>[] resultTypes, Class<?>[] leafTypes)
    {
        ObjectBrowserDialog scDialog = new ObjectBrowserDialog(parentShell, title, rootNode, selectedNode, true, allowedTypes, resultTypes, leafTypes);
        if (scDialog.open() == IDialogConstants.OK_ID) {
            List<DBNNode> result = scDialog.getSelectedObjects();
            return result.isEmpty() ? null : result.get(0);
        } else {
            return null;
        }
    }

    public static List<DBNNode> selectObjects(Shell parentShell, String title, DBNNode rootNode, DBNNode selectedNode, Class<?>[] allowedTypes, Class<?>[] resultTypes, Class<?>[] leafTypes)
    {
        ObjectBrowserDialog scDialog = new ObjectBrowserDialog(parentShell, title, rootNode, selectedNode, false, allowedTypes, resultTypes, leafTypes);
        if (scDialog.open() == IDialogConstants.OK_ID) {
            return scDialog.getSelectedObjects();
        } else {
            return null;
        }
    }

}
