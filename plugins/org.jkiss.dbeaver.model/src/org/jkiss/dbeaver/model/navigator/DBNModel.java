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
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIconComposite;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeFolder;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DBNModel.
 * Contains all objects which are shown in navigator tree.
 * Also ties DBSObjects to thee model (DBNNode).
 *
 * It's strongly recommended to not put the same DBSObject in tree model multiple times.
 * It will work but some actions will not work well
 * (e.g. TreeViewer sometimes update only first TreeItem corresponding to model certain model object).
 */
public class DBNModel implements IResourceChangeListener {
    private static final Log log = Log.getLog(DBNModel.class);

    private static class NodePath {
        DBNNode.NodePathType type;
        List<String> pathItems;

        public NodePath(DBNNode.NodePathType type, List<String> pathItems) {
            this.type = type;
            this.pathItems = pathItems;
        }

        public String first() {
            return pathItems.isEmpty() ? null : pathItems.get(0);
        }
    }

    private final DBPPlatform platform;
    private DBNRoot root;
    private final List<INavigatorListener> listeners = new ArrayList<>();
    private transient INavigatorListener[] listenersCopy = null;
    private final Map<DBSObject, Object> nodeMap = new HashMap<>();

    public DBNModel(DBPPlatform platform) {
        this.platform = platform;
    }

    public DBPPlatform getPlatform() {
        return platform;
    }

    public void initialize()
    {
        if (this.root != null) {
            throw new IllegalStateException("Can't initialize navigator model more than once");
        }
        this.root = new DBNRoot(this);

        // Add all existing projects to root node
        for (IProject project : platform.getLiveProjects()) {
            root.addProject(project, false);
        }

        platform.getWorkspace().addResourceChangeListener(this);
    }

    public void dispose()
    {
        platform.getWorkspace().removeResourceChangeListener(this);
        this.root.dispose(false);
        synchronized (nodeMap) {
            this.nodeMap.clear();
        }
        synchronized (this.listeners) {
            if (!listeners.isEmpty()) {
                for (INavigatorListener listener : listeners) {
                    log.warn("Listener '" + listener + "' is not unregistered from DBM model");
                }
            }
            this.listeners.clear();
            this.listenersCopy = null;
        }
        this.root = null;
    }

    public DBNRoot getRoot()
    {
        return root;
    }

    @Nullable
    public DBNDatabaseNode findNode(DBSObject object)
    {
        if (object instanceof DBNDatabaseNode) {
            return (DBNDatabaseNode)object;
        } else {
            return this.getNodeByObject(object);
        }
    }

    @Nullable
    public DBNDatabaseNode getNodeByObject(DBSObject object)
    {
        if (object instanceof DBNDatabaseNode) {
            return (DBNDatabaseNode)object;
        }
        Object obj;
        synchronized (nodeMap) {
            obj = nodeMap.get(object);
        }
        if (obj == null) {
            return null;
        } else if (obj instanceof DBNDatabaseNode) {
            return (DBNDatabaseNode)obj;
        } else if (obj instanceof List) {
            @SuppressWarnings("unchecked")
            List<DBNDatabaseNode> nodeList = (List<DBNDatabaseNode>) obj;
            if (nodeList.isEmpty()) {
                return null;
            }
            if (nodeList.size() > 1) {
                for (DBNDatabaseNode node : nodeList) {
                    if (node instanceof DBNDatabaseItem && !((DBNDatabaseItem)node).getMeta().isVirtual()) {
                        return node;
                    }
                }
            }
            // Get just first one
            return nodeList.get(0);
        } else {
            // Never be here
           throw new IllegalStateException();
        }
/*
        if (node == null) {
            log.warn("Can't find tree node for object " + object.getName() + " (" + object.getClass().getName() + ")");
        }
        return node;
*/
    }

    @Nullable
    public DBNDatabaseNode getNodeByObject(DBRProgressMonitor monitor, DBSObject object, boolean addFiltered)
    {
        DBNDatabaseNode node = getNodeByObject(object);
        if (node != null) {
            return node;
        }
        DBSObject[] path = DBUtils.getObjectPath(object, true);
        for (int i = 0; i < path.length - 1; i++) {
            DBSObject item = path[i];
            DBSObject nextItem = path[i + 1];
            node = getNodeByObject(item);
            if (node == null) {
                return null;
            }
            try {
                cacheNodeChildren(monitor, node, nextItem, addFiltered);
            } catch (DBException e) {
                log.error(e.getMessage());
                return null;
            }
        }
        return getNodeByObject(object);
    }

    @NotNull
    private NodePath getNodePath(@NotNull String path) {
        DBNNode.NodePathType nodeType = DBNNode.NodePathType.database;
        for (DBNNode.NodePathType type : DBNNode.NodePathType.values()) {
            final String prefix = type.getPrefix();
            if (path.startsWith(prefix)) {
                path = path.substring(prefix.length());
                nodeType = type;
                break;
            }
        }
        return new NodePath(nodeType, CommonUtils.splitString(path, '/'));
    }

    @Nullable
    public DBNDataSource getDataSourceByPath(String path) throws DBException
    {
        String dsId = getNodePath(path).first();
        for (DBNProject projectNode : getRoot().getProjects()) {
            DBNDataSource dataSource = projectNode.getDatabases().getDataSource(dsId);
            if (dataSource != null) {
                return dataSource;
            }
        }
        return null;
    }

    /**
     * Find node by path.
     * Deprecated - use getNodeByPath with project parameter
     * @throws DBException
     */
    @Nullable
    public DBNNode getNodeByPath(@NotNull DBRProgressMonitor monitor, @NotNull String path) throws DBException {
        final NodePath nodePath = getNodePath(path);
        if (nodePath.type == DBNNode.NodePathType.database) {
            for (DBNProject projectNode : getRoot().getProjects()) {
                DBNDataSource curNode = projectNode.getDatabases().getDataSource(nodePath.first());
                if (curNode != null) {
                    return findNodeByPath(monitor, nodePath, curNode, 1);
                }
            }
        } else {
            for (DBNProject projectNode : getRoot().getProjects()) {
                if (projectNode.getName().equals(nodePath.first())) {
                    return findNodeByPath(monitor, nodePath,
                        nodePath.type == DBNNode.NodePathType.folder ? projectNode.getDatabases() : projectNode, 1);
                }
            }
        }
        return null;
    }

    @Nullable
    public DBNNode getNodeByPath(@NotNull DBRProgressMonitor monitor, @NotNull IProject project, @NotNull String path) throws DBException
    {
        DBNProject projectNode = getRoot().getProject(project);
        if (projectNode == null) {
            log.debug("Project node not found");
            return null;
        }
        final NodePath nodePath = getNodePath(path);
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
        return findNodeByPath(monitor, nodePath, curNode, 1);
    }

    public DBNResource getNodeByResource(IResource resource) {
        final IProject project = resource.getProject();
        if (project == null) {
            return null;
        }
        final DBNProject projectNode = getRoot().getProject(project);
        if (projectNode == null) {
            return null;
        }
        List<IResource> path = new ArrayList<>();
        for (IResource parent = resource; parent != null && parent != project; parent = parent.getParent()) {
            path.add(0, parent);
        }
        DBNResource curResNode = projectNode;
        for (IResource res : path) {
            curResNode = curResNode.getChild(res);
            if (curResNode == null) {
                return null;
            }
        }
        return curResNode;
    }

    private DBNNode findNodeByPath(DBRProgressMonitor monitor, NodePath nodePath, DBNNode curNode, int firstItem) throws DBException {
        for (int i = firstItem, itemsSize = nodePath.pathItems.size(); i < itemsSize; i++) {
            String item = nodePath.pathItems.get(i);
            DBNNode[] children = curNode.getChildren(monitor);
            DBNNode nextChild = null;
            if (children != null && children.length > 0) {
                for (DBNNode child : children) {
                    if (nodePath.type == DBNNode.NodePathType.resource) {
                        if (child instanceof DBNResource && ((DBNResource) child).getResource().getName().equals(item)) {
                            nextChild = child;
                        }
                    } else if (nodePath.type == DBNNode.NodePathType.folder) {
                        if (child instanceof DBNLocalFolder && child.getName().equals(item)) {
                            nextChild = child;
                        }
                    } else {
                        if (child instanceof DBNDatabaseFolder) {
                            DBXTreeFolder meta = ((DBNDatabaseFolder) child).getMeta();
                            if (meta != null && !CommonUtils.isEmpty(meta.getType()) && meta.getType().equals(item)) {
                                nextChild = child;
                            }
                        }
                        if (child.getNodeName().equals(item)) {
                            nextChild = child;
                        }
                    }
                    if (nextChild != null) {
                        if (i < itemsSize - 1) {
                            nextChild = findNodeByPath(monitor, nodePath, nextChild, i + 1);
                            if (nextChild != null) {
                                return nextChild;
                            }
                            continue;
                        }
                        break;
                    }
                }
            }
            curNode = nextChild;
            if (curNode == null) {
                break;
            }
        }
        return curNode;
    }

    private boolean cacheNodeChildren(DBRProgressMonitor monitor, DBNDatabaseNode node, DBSObject objectToCache, boolean addFiltered) throws DBException
    {
        DBNDatabaseNode[] children = node.getChildren(monitor);
        boolean cached = false;
        if (!ArrayUtils.isEmpty(children)) {
            for (DBNDatabaseNode child : children) {
                if (child instanceof DBNDatabaseFolder) {
                    Class<?> itemsClass = ((DBNDatabaseFolder) child).getChildrenClass();
                    if (itemsClass != null && itemsClass.isAssignableFrom(objectToCache.getClass())) {
                        cached = cacheNodeChildren(monitor, child, objectToCache, addFiltered);
                        if (cached) {
                            break;
                        }
                    }
                }
            }
        }
        if (!cached && addFiltered && node.isFiltered()) {
            // It seems this object was filtered out
            // As it was requested explicitly - let's add new node
            node.addChildItem(objectToCache);
            return true;
        }
        return false;
    }

    @Nullable
    public DBNDatabaseNode getParentNode(DBSObject object)
    {
        DBNDatabaseNode node = getNodeByObject(object);
        if (node != null) {
            if (node.getParentNode() instanceof DBNDatabaseNode) {
                return (DBNDatabaseNode) node.getParentNode();
            } else {
                log.error("Object's " + object.getName() + "' parent node is not a database node: " + node.getParentNode());
                return null;
            }
        }
        DBSObject[] path = DBUtils.getObjectPath(object, false);
        for (int i = 0; i < path.length; i++) {
            DBSObject item = path[i];
            node = getNodeByObject(item);
            if (node == null) {
                // Parent node read
                return null;
            }
            DBNDatabaseNode[] children = node.getChildNodes();
            if (ArrayUtils.isEmpty(children)) {
                // Parent node is not read
                return null;
            }

            if (item == object.getParentObject()) {
                // Try to find parent node withing children
                for (DBNDatabaseNode child : children) {
                    if (child instanceof DBNDatabaseFolder) {
                        Class<?> itemsClass = ((DBNDatabaseFolder) child).getChildrenClass();
                        if (itemsClass != null && itemsClass.isAssignableFrom(object.getClass())) {
                            return child;
                        }
                    }
                }
                // It is actual parent and there are no folders
                return node;
            }
        }
        // Not found
        return null;
    }

    void addNode(DBNDatabaseNode node)
    {
        addNode(node, false);
    }

    void addNode(DBNDatabaseNode node, boolean reflect)
    {
        synchronized (nodeMap) {
            Object obj = nodeMap.get(node.getObject());
            if (obj == null) {
                // New node
                nodeMap.put(node.getObject(), node);
            } else if (obj instanceof DBNNode) {
                // Second node - make a list
                List<DBNNode> nodeList = new ArrayList<>(2);
                nodeList.add((DBNNode)obj);
                nodeList.add(node);
                nodeMap.put(node.getObject(), nodeList);
            } else if (obj instanceof List) {
                // Multiple nodes
                @SuppressWarnings("unchecked")
                List<DBNNode> nodeList = (List<DBNNode>) obj;
                nodeList.add(node);
            }
        }
        if (reflect) {
            this.fireNodeEvent(new DBNEvent(this, DBNEvent.Action.ADD, DBNEvent.NodeChange.LOAD, node));
        }
    }

    void removeNode(DBNDatabaseNode node, boolean reflect)
    {
        boolean badNode = false;
        synchronized (nodeMap) {
            Object obj = nodeMap.get(node.getObject());
            if (obj == null) {
                // No found
                badNode = true;
            } else if (obj instanceof DBNNode) {
                // Just remove it
                if (nodeMap.remove(node.getObject()) != node) {
                    badNode = true;
                }
            } else if (obj instanceof List) {
                // Multiple nodes
                @SuppressWarnings("unchecked")
                List<DBNNode> nodeList = (List<DBNNode>) obj;
                if (!nodeList.remove(node)) {
                    badNode = true;
                }
                if (nodeList.isEmpty()) {
                    nodeMap.remove(node.getObject());
                }
            }
        }
        if (badNode) {
            log.warn("Remove unregistered meta node object " + node.getNodeName());
        } else {
            if (reflect) {
                this.fireNodeEvent(new DBNEvent(this, DBNEvent.Action.REMOVE, DBNEvent.NodeChange.UNLOAD, node));
            }
        }
    }

    public void addListener(INavigatorListener listener)
    {
        synchronized (this.listeners) {
            if (this.listeners.contains(listener)) {
                log.warn("Listener " + listener + " already registered in model");
            } else {
                this.listeners.add(listener);
            }
            this.listenersCopy = this.listeners.toArray(new INavigatorListener[this.listeners.size()]);
        }
    }

    public void removeListener(INavigatorListener listener)
    {
        synchronized (this.listeners) {
            if (!this.listeners.remove(listener)) {
                log.warn("Listener " + listener + " wasn't registered in model");
            }
            this.listenersCopy = this.listeners.toArray(new INavigatorListener[this.listeners.size()]);
        }
    }

    void fireNodeUpdate(Object source, DBNNode node, DBNEvent.NodeChange nodeChange)
    {
        this.fireNodeEvent(new DBNEvent(source, DBNEvent.Action.UPDATE, nodeChange, node));
    }

    void fireNodeEvent(final DBNEvent event)
    {
        if (platform.isShuttingDown()) {
            return;
        }
        final INavigatorListener[] listenersCopy;
        synchronized (this.listeners) {
            if (listeners.isEmpty()) {
                return;
            }
            listenersCopy = this.listenersCopy;
        }
        if (listenersCopy.length == 0) {
            return;
        }
        // Notify listeners in detached job
        new Job("Notify node '" + event.getNode().getName() + "' changes") {
            {
                setSystem(true);
            }
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                for (INavigatorListener listener :  listenersCopy) {
                    listener.nodeChanged(event);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event)
    {
        if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
            IResourceDelta delta = event.getDelta();
            //IResource resource = delta.getResource();
            for (IResourceDelta childDelta : delta.getAffectedChildren()) {
                if (childDelta.getResource() instanceof IProject) {
                    IProject project = (IProject) childDelta.getResource();
                    DBNProject projectNode = getRoot().getProject(project);
                    if (projectNode == null) {
                        if (childDelta.getKind() == IResourceDelta.ADDED) {
                            // New projectNode
                            getRoot().addProject(project, true);

                            if (platform.getProjectManager().getActiveProject() == null) {
                                platform.getProjectManager().setActiveProject(project);
                            }
                        } else {
                            // Project not found - report an error
                            log.error("Project '" + childDelta.getResource().getName() + "' not found in navigator");
                        }
                    } else {
                        if (childDelta.getKind() == IResourceDelta.REMOVED) {
                            // Project deleted
                            getRoot().removeProject(project);
                            if (project == platform.getProjectManager().getActiveProject()) {
                                platform.getProjectManager().setActiveProject(null);
                            }
                        } else {
                            if (childDelta.getFlags() == IResourceDelta.OPEN) {
                                if (projectNode.getProject().isOpen()) {
                                    projectNode.openProject();
                                } else {
                                    // Project was closed - do nothing.
                                }
                            } else {
                                // Some resource changed within the projectNode
                                // Let it handle this event itself
                                projectNode.handleResourceChange(childDelta);
                            }
                        }
                    }
                }
            }
        }
    }

    public static synchronized DBPImage getStateOverlayImage(DBPImage image, DBSObjectState state)
    {
        if (state == null) {
            // Empty state
            return image;
        }
        final DBPImage overlayImage = state.getOverlayImage();
        if (overlayImage == null) {
            // No overlay
            return image;
        }
        if (image instanceof DBIconComposite) {
            ((DBIconComposite) image).setBottomRight(overlayImage);
            return image;
        }
        return new DBIconComposite(image, false, null, null, null, overlayImage);
    }

    public static void updateConfigAndRefreshDatabases(DBNNode node)
    {
        for (DBNNode parentNode = node; parentNode != null; parentNode = parentNode.getParentNode()) {
            if (parentNode instanceof DBNProjectDatabases) {
                DBNProjectDatabases projectDatabases = (DBNProjectDatabases) parentNode;
                projectDatabases.getDataSourceRegistry().flushConfig();
                projectDatabases.refreshChildren();
                break;
            }
        }
    }

    public void ensureProjectLoaded(IProject project) {
        DBNProject projectNode = getRoot().getProject(project);
        if (projectNode != null) {
            projectNode.getDatabases();
        }
    }
}
