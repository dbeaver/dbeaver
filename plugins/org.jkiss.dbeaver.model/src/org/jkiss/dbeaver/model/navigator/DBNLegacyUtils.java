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
package org.jkiss.dbeaver.model.navigator;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeFolder;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;

/*
   Legacy methods for working with nodes,
   exists for backward compatibility with the legacy node path format
 */
public class DBNLegacyUtils {
    private static final Log log = Log.getLog(DBNLegacyUtils.class);

    /**
     * @deprecated used for backwards compatibility only
     */
    @Deprecated
    public static DBNNode legacyGetNodeByPath(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBNProject projectNode,
        @NotNull DBNModel.NodePath nodePath
    ) throws DBException {
        DBNNode curNode;
        switch (nodePath.type) {
            case database:
                var dsId = DBNModel.getDataSourceIdFromNodePath(nodePath);
                curNode = projectNode.getDatabases().getDataSource(dsId);
                break;
            case folder:
                curNode = projectNode.getDatabases();
                break;
            default:
                curNode = projectNode;
                break;
        }
        if (curNode == null) {
            return null;
        }
        return legacyFindNodeByPath(monitor, nodePath, curNode, 1);
    }

    /**
     * @deprecated used for backwards compatibility only
     */
    @Deprecated
    public static DBNNode legacyGetNodeByPath(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBNModel model,
        @NotNull DBNModel.NodePath nodePath
    ) throws DBException {
        DBNRoot root = model.getRoot();
        if (nodePath.type == DBNNode.NodePathType.database) {
            boolean hasLazyProjects = false;
            for (DBNProject projectNode : root.getProjects()) {
                if (!projectNode.getProject().isRegistryLoaded()) {
                    hasLazyProjects = true;
                    continue;
                }
                DBNDataSource curNode = projectNode.getDatabases().getDataSource(nodePath.first());
                if (curNode != null) {
                    return legacyFindNodeByPath(monitor, nodePath, curNode, 1);
                }
            }
            if (hasLazyProjects) {
                // No try to search in uninitialized proejcts
                for (DBNProject projectNode : root.getProjects()) {
                    if (!projectNode.getProject().isRegistryLoaded()) {
                        DBNDataSource curNode = projectNode.getDatabases().getDataSource(nodePath.first());
                        if (curNode != null) {
                            return legacyFindNodeByPath(monitor, nodePath, curNode, 1);
                        }
                    }
                }
            }
        } else if (nodePath.type == DBNNode.NodePathType.ext) {
            // works for rm resources, because their parent DBNRoot
            var node = legacyFindNodeByPath(monitor, nodePath, root, 0);
            if (node != null) {
                return node;
            }
            // works for cloud explorer
            DBNProject[] projects = root.getProjects();
            if (ArrayUtils.isEmpty(projects)) {
                throw new DBException("No projects in workspace");
            }
            if (projects.length > 1) {
                boolean multiNode = Arrays.stream(projects).anyMatch(pr -> pr.getProject().isVirtual());
                if (!multiNode) {
                    throw new DBException("Multi-project workspace. Extension nodes not supported");
                }
            }
            return legacyFindNodeByPath(monitor, nodePath,
                projects[0], 0);
        } else if (nodePath.type == DBNNode.NodePathType.other) {
            return legacyFindNodeByPath(monitor, nodePath, root, 0);
        } else {
            for (DBNProject projectNode : root.getProjects()) {
                if (projectNode.getName().equals(nodePath.first())) {
                    return legacyFindNodeByPath(monitor, nodePath,
                        nodePath.type == DBNNode.NodePathType.folder ? projectNode.getDatabases() : projectNode, 1);
                }
            }
        }
        return null;
    }

    private static DBNNode legacyFindNodeByPath(
        DBRProgressMonitor monitor,
        DBNModel.NodePath nodePath,
        DBNNode curNode,
        int firstItem
    ) throws DBException {
        //log.debug("findNodeByPath '" + nodePath + "' in '" + curNode.getNodeItemPath() + "'/" + firstItem);

        for (int i = firstItem, itemsSize = nodePath.pathItems.size(); i < itemsSize; i++) {
            String item = nodePath.pathItems.get(i).replace(DBNModel.SLASH_ESCAPE_TOKEN, "/");

            DBNNode[] children = curNode.getChildren(monitor);
            DBNNode nextChild = null;
            if (children != null && children.length > 0) {
                for (DBNNode child : children) {
                    if (nodePath.type == DBNNode.NodePathType.resource) {
                        if (child instanceof DBNResource && ((DBNResource) child).getResource().getName().equals(item)) {
                            nextChild = child;
                        } else if (child instanceof DBNProjectDatabases && child.getName().equals(item)) {
                            nextChild = child;
                        }
                    } else if (nodePath.type == DBNNode.NodePathType.folder) {
                        if (child instanceof DBNLocalFolder && child.getName().equals(item)) {
                            nextChild = child;
                        }
                    } else {
                        if (child instanceof DBNDatabaseFolder) {
                            DBXTreeFolder meta = ((DBNDatabaseFolder) child).getMeta();
                            if (meta != null) {
                                String idOrType = meta.getIdOrType();
                                if (!CommonUtils.isEmpty(idOrType) && idOrType.equals(item)) {
                                    nextChild = child;
                                }
                            }
                        }
                        if (child.getName().equals(item)) {
                            nextChild = child;
                        }
                    }
                    if (nextChild != null) {
                        if (i < itemsSize - 1) {
                            nextChild = legacyFindNodeByPath(monitor, nodePath, nextChild, i + 1);
                            if (nextChild != null) {
                                return nextChild;
                            }
                            continue;
                        }
                        break;
                    }
                }
            }
            if (nextChild == null) {
                log.debug("Legacy node '" + item + "' not found in parent node '" + curNode.getNodeFullPath() + "'."
                    + "Full legacy path [" + nodePath + "]."
                    + "\nAllowed children: " + Arrays.toString(children));
            }
            curNode = nextChild;
            if (curNode == null) {
                break;
            }
        }
        return curNode;
    }

}
