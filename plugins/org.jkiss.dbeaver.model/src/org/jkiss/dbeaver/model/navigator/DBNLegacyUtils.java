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
package org.jkiss.dbeaver.model.navigator;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.fs.DBNFileSystems;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeFolder;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

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
                curNode = projectNode.getDatabases().getDataSource(nodePath.first());
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

    @Deprecated
    public static DBNNode legacyGetNodeByPath(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBNModel model,
        @NotNull DBNModel.NodePath nodePath
    ) throws DBException {
        if (nodePath.type == DBNNode.NodePathType.database) {
            boolean hasLazyProjects = false;
            for (DBNProject projectNode : model.getRoot().getProjects()) {
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
                // No try to search in uninitialized projects
                for (DBNProject projectNode : model.getRoot().getProjects()) {
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
            var node = legacyFindNodeByPath(monitor, nodePath, model.getRoot(), 0);
            if (node != null) {
                return node;
            }
            // works for cloud explorer
            DBNProject[] projects = model.getRoot().getProjects();
            if (ArrayUtils.isEmpty(projects)) {
                throw new DBException("No projects in workspace");
            }
            var projectId = nodePath.first();
            DBNProject parentProjectNode = projectId == null
                ? null
                : Arrays.stream(projects)
                .filter(dbnProject -> dbnProject.getProject().getId().equals(projectId))
                .findFirst()
                .orElse(null);
            int firstItem = 0;
            //backward compatibility
            if (parentProjectNode == null) {
                parentProjectNode = projects[0];
            } else {
                // cause projectId included in the path
                firstItem = 1;
            }
            return legacyFindNodeByPath(monitor, nodePath, parentProjectNode, firstItem);
        } else if (nodePath.type == DBNNode.NodePathType.other) {
            return legacyFindNodeByPath(monitor, nodePath, model.getRoot(), 0);
        } else {
            for (DBNProject projectNode : model.getRoot().getProjects()) {
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

        final List<String> pathItems = nodePath.pathItems;
        for (int i = firstItem, itemsSize = pathItems.size(); i < itemsSize; i++) {
            String item = pathItems.get(i);
            if (nodePath.type == DBNNode.NodePathType.ext && curNode instanceof DBNProject pn) {
                // Trigger project to load extra nodes
                pn.getExtraNode(DBNFileSystems.class);
            }
            DBNNode[] children = curNode.getChildren(monitor);
            DBNNode nextChild = null;
            if (children != null && children.length > 0) {
                for (DBNNode child : children) {
                    if (nodeMatchesPath(nodePath, child, item)) {
                        nextChild = child;
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
                log.debug("Node '" + item + "' not found in parent node '" + curNode.getNodeItemPath() + "'." +
                    "\nAllowed children: " + Arrays.toString(children));
            }
            if (nextChild != null) {
                curNode = nextChild;
            }
        }

        if (!pathItems.isEmpty()) {
            String lastItemName = pathItems.get(pathItems.size() - 1);
            if (!nodeMatchesPath(nodePath, curNode, lastItemName)) {
                // Tail node doesn't match tail node from the desired path
                return null;
            }
        }
        return curNode;
    }

    private static boolean nodeMatchesPath(
        @NotNull DBNModel.NodePath path,
        @NotNull DBNNode child,
        @NotNull String item
    ) {
        if (path.type == DBNNode.NodePathType.resource) {
            if (child instanceof DBNProject && ((DBNProject) child).getProject().getId().equals(item) ||
                child instanceof DBNProjectDatabases && child.getName().equals(item)) {
                return true;
            }
            Path filePath = child.getAdapter(Path.class);
            return filePath != null && filePath.getFileName().toString().equals(item);
        } else if (path.type == DBNNode.NodePathType.folder) {
            return child instanceof DBNLocalFolder && child.getName().equals(item);
        } else if (child instanceof DBNDataSource) {
            return ((DBNDataSource) child).getDataSourceContainer().getId().equals(item);
        } else if (child instanceof DBNDatabaseFolder) {
            DBXTreeFolder meta = ((DBNDatabaseFolder) child).getMeta();
            if (meta != null) {
                String idOrType = meta.getIdOrType();
                if (!CommonUtils.isEmpty(idOrType) && idOrType.equals(item)) {
                    return true;
                }
            }
        }

        return child.getName().equals(item);
    }

}
