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
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IResource;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourcePermission;
import org.jkiss.dbeaver.model.DBPHiddenObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeFolder;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSFolder;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Navigator helper functions
 */
public class DBNUtils {

    private static final Log log = Log.getLog(DBNUtils.class);

    public static DBNDatabaseNode getNodeByObject(DBSObject object) {
        return DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(object);
    }

    public static DBNDatabaseNode getNodeByObject(DBRProgressMonitor monitor, DBSObject object, boolean addFiltered) {
        return DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(monitor, object, addFiltered);
    }

    public static DBNDatabaseNode getChildFolder(DBRProgressMonitor monitor, DBNDatabaseNode node, Class<?> folderType) {
        try {
            for (DBNDatabaseNode childNode : node.getChildren(monitor)) {
                if (!(childNode instanceof DBNDatabaseFolder)) {
                    continue;
                }
                final DBXTreeFolder meta = ((DBNDatabaseFolder) childNode).getMeta();
                final Class<?> objectClass = meta.getSource().getObjectClass(meta.getType());
                if (objectClass != null && folderType.isAssignableFrom(objectClass)) {
                    return childNode;
                }
            }
        } catch (DBException e) {
            log.error("Error reading child folder", e);
        }
        return null;
    }

    public static DBNNode[] getNodeChildrenFiltered(DBRProgressMonitor monitor, DBNNode node, boolean forTree) throws DBException {
        DBNNode[] children = node.getChildren(monitor);
        if (children != null && children.length > 0) {
            children = filterNavigableChildren(children, forTree);
        }
        return children;
    }

    public static DBNNode[] filterNavigableChildren(DBNNode[] children, boolean forTree)
    {
        if (ArrayUtils.isEmpty(children)) {
            return children;
        }
        DBNNode[] result;
        if (forTree) {
            List<DBNNode> filtered = new ArrayList<>();
            for (int i = 0; i < children.length; i++) {
                DBNNode node = children[i];
                if (node instanceof DBPHiddenObject && ((DBPHiddenObject) node).isHidden()) {
                    continue;
                }
                if (node instanceof DBNDatabaseNode) {
                    DBNDatabaseNode dbNode = (DBNDatabaseNode) node;
                    if (dbNode.getMeta() != null && !dbNode.getMeta().isNavigable()) {
                        continue;
                    }
                }
                filtered.add(node);
            }
            result = filtered.toArray(new DBNNode[0]);
        } else {
            result = children;
        }
        sortNodes(result);
        return result;
    }

    private static void sortNodes(DBNNode[] children)
    {
        final DBPPreferenceStore prefStore = DBWorkbench.getPlatform().getPreferenceStore();

        // Sort children is we have this feature on in preferences
        // and if children are not folders
        if (children.length > 0) {
            if (prefStore.getBoolean(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY) || isMergedEntity(children[0])) {
                if (!(children[0] instanceof DBNContainer)) {
                    Arrays.sort(children, NodeNameComparator.INSTANCE);
                }
            }
        }

        if (children.length > 0 && prefStore.getBoolean(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST)) {
            Arrays.sort(children, NodeFolderComparator.INSTANCE);
        }
    }

    private static boolean isMergedEntity(DBNNode node) {
        return node instanceof DBNDatabaseNode &&
            ((DBNDatabaseNode) node).getObject() instanceof DBSEntity &&
            ((DBNDatabaseNode) node).getObject().getDataSource().getContainer().getNavigatorSettings().isMergeEntities();
    }

    public static boolean isDefaultElement(Object element)
    {
        if (element instanceof DBSWrapper) {
            DBSObject object = ((DBSWrapper) element).getObject();
            if (object != null) {
                // Get default context from default instance - not from active object
                DBCExecutionContext defaultContext = DBUtils.getDefaultContext(object.getDataSource(), false);
                if (defaultContext != null) {
                    DBCExecutionContextDefaults contextDefaults = defaultContext.getContextDefaults();
                    if (contextDefaults != null) {
                        return contextDefaults.getDefaultCatalog() == object || contextDefaults.getDefaultSchema() == object;
                    }
                }
            }
        } else if (element instanceof DBNProject) {
            if (((DBNProject)element).getProject() == DBWorkbench.getPlatform().getWorkspace().getActiveProject()) {
                return true;
            }
        }
        return false;
    }

    public static void refreshNavigatorResource(@NotNull IResource resource, Object source) {
        final DBNProject projectNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProjectNode(resource.getProject());
        if (projectNode != null) {
            final DBNResource fileNode = projectNode.findResource(resource);
            if (fileNode != null) {
                fileNode.refreshResourceState(source);
            }
        }
    }

    @NotNull
    public static String getLastNodePathSegment(@NotNull String path) {
        int divPos = path.lastIndexOf('/');
        return divPos == -1 ? path : path.substring(divPos + 1);
    }

    public static boolean isReadOnly(DBNNode node)
    {
        return node instanceof DBNDatabaseNode &&
            !(node instanceof DBNDataSource) &&
            !((DBNDatabaseNode) node).getDataSourceContainer().hasModifyPermission(DBPDataSourcePermission.PERMISSION_EDIT_METADATA);
    }

    private static class NodeNameComparator implements Comparator<DBNNode> {
        static NodeNameComparator INSTANCE = new NodeNameComparator();
        @Override
        public int compare(DBNNode node1, DBNNode node2) {
            return node1.getNodeName().compareToIgnoreCase(node2.getNodeName());
        }
    }

    private static class NodeFolderComparator implements Comparator<DBNNode> {
        static NodeFolderComparator INSTANCE = new NodeFolderComparator();
        @Override
        public int compare(DBNNode node1, DBNNode node2) {
            int first = node1 instanceof DBNLocalFolder || node1 instanceof DBSFolder ? -1 : 1;
            int second = node2 instanceof DBNLocalFolder || node2 instanceof DBSFolder ? -1 : 1;
            return first - second;
        }
    }
}
