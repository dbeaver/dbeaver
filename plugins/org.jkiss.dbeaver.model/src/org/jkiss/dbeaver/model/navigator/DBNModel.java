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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIconComposite;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.auth.SMSessionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DBNModel.
 * Contains all objects which are shown in navigator tree.
 * Also ties DBSObjects to thee model (DBNNode).
 *
 * It's strongly recommended to not put the same DBSObject in tree model multiple times.
 * It will work but some actions will not work well
 * (e.g. TreeViewer sometimes update only first TreeItem corresponding to model certain model object).
 */
public class DBNModel {

    //TODO: create real resource root node
    //this 'node' exist to avoid collision between resource folders and other root nodes
    //example: you can create 'datasources' folder, and nodeUri will be the same as for DBNProjectDatabases
    public static final String FAKE_RESOURCE_ROOT_NODE = "resources";
    private static final Log log = Log.getLog(DBNModel.class);

    public static class NodePath {
        DBNNode.NodePathType type;
        List<String> pathItems;
        final boolean legacyFormat;

        NodePath(DBNNode.NodePathType type, List<String> pathItems) {
            this.type = type;
            this.pathItems = pathItems;
            this.legacyFormat = type != DBNNode.NodePathType.node;
        }

        public String first() {
            return pathItems.isEmpty() ? null : pathItems.get(0);
        }

        @Override
        public String toString() {
            return type.getPrefix() + pathItems.toString();
        }
    }

    private final DBPPlatform platform;
    private final List<? extends DBPProject> modelProjects;
    private DBNRoot root;
    private final List<INavigatorListener> listeners = new ArrayList<>();
    private transient INavigatorListener[] listenersCopy = null;
    private final transient List<DBNEvent> eventCache = new ArrayList<>();
    private final Map<DBSObject, Object> nodeMap = new HashMap<>();
    private final List<Function<DBNNode, Boolean>> nodeFilters = new ArrayList<>();

    private SMSessionContext modelAuthContext;

    /**
     * Creates navigator model.
     *
     * @param modelProjects Model projects. If null then this is global navigator model. Otherwise it points to a session-like object.
     */
    public DBNModel(DBPPlatform platform, @Nullable List<? extends DBPProject> modelProjects) {
        this.platform = platform;
        this.modelProjects = modelProjects;
    }

    public DBPPlatform getPlatform() {
        return platform;
    }

    @Nullable
    public List<? extends DBPProject> getModelProjects() {
        return modelProjects;
    }

    public SMSessionContext getModelAuthContext() {
        return modelAuthContext;
    }

    public void setModelAuthContext(SMSessionContext modelAuthContext) {
        this.modelAuthContext = modelAuthContext;
    }

    public boolean isGlobal() {
        return modelProjects == null;
    }

    public void initialize()
    {
        if (this.root != null) {
            throw new IllegalStateException("Can't initialize navigator model more than once");
        }
        this.root = new DBNRoot(this);

        if (isGlobal()) {
            new EventProcessingJob().schedule();
        }
    }

    public void dispose() {

        if (root != null) {
            this.root.dispose(false);
            synchronized (nodeMap) {
                this.nodeMap.clear();
            }
            this.root = null;
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
    }

    public DBNRoot getRoot()
    {
        return root;
    }

    protected DBNProject createProjectNode(DBNRoot parent, DBPProject project) {
        return new DBNProject(parent, project);
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
        object = DBUtils.getPublicObjectContainer(object);

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
                    if (node instanceof DBNDatabaseItem && !node.getMeta().isVirtual()) {
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
        if (object instanceof DBSEntity) {
            try {
                object = DBVUtils.getRealEntity(monitor, (DBSEntity)object);
            } catch (DBException e) {
                log.debug("Error dereferencing virtual entity", e);
            }
        }
        if (object == null) {
            return null;
        }
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
    private static NodePath getNodePath(@NotNull String path) {
        DBNNode.NodePathType nodeType = DBNNode.NodePathType.other;
        for (DBNNode.NodePathType type : DBNNode.NodePathType.values()) {
            final String prefix = type.getPrefix();
            if (path.startsWith(prefix)) {
                path = path.substring(prefix.length());
                nodeType = type;
                break;
            }
        }
        final List<String> items = CommonUtils.splitString(path, '/');
        items.replaceAll(DBNUtils::decodeNodePath);
        return new NodePath(nodeType, items);
    }

    @Nullable
    public DBNDataSource getDataSourceByPath(@NotNull DBPProject project, @NotNull String path) {
        DBNProject projectNode = getRoot().getProjectNode(project);
        if (projectNode == null) {
            return null;
        }
        NodePath nodePath = getNodePath(path);
        if (nodePath.legacyFormat) {
            return projectNode.getDatabases().getDataSource(nodePath.first());
        }
        DBNProjectDatabases databaseRootNode = projectNode.getDatabases();
        int rootDbNodeIndex = nodePath.pathItems.indexOf(databaseRootNode.getNodeId());
        if (rootDbNodeIndex < 0) {
            return null;
        }
        Map<String, DBNDataSource> datasourceById = databaseRootNode.getDataSources()
            .stream()
            .collect(Collectors.toMap(DBNDataSource::getNodeId, Function.identity()));
        for (int i = rootDbNodeIndex + 1; i < nodePath.pathItems.size(); i++) {
            // it can be folder name or datasource id
            String potentialDatasourceId = nodePath.pathItems.get(i);
            if (datasourceById.containsKey(potentialDatasourceId)) {
                return datasourceById.get(potentialDatasourceId);
            }
        }

        return null;
    }

    /**
     * Find node by path.
     * Deprecated - use getNodeByPath with project parameter
     */
    @Nullable
    public DBNNode getNodeByPath(@NotNull DBRProgressMonitor monitor, @NotNull String path) throws DBException {
        final NodePath nodePath = getNodePath(path);
        if (nodePath.legacyFormat) {
            return DBNLegacyUtils.legacyGetNodeByPath(monitor, this, nodePath);
        }
        return findNodeByPath(monitor, getRoot(), nodePath, 0);
    }

    @Nullable
    public DBNNode getNodeByPath(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPProject project,
        @NotNull String path
    ) throws DBException {
        DBNProject projectNode = getRoot().getProjectNode(project);
        if (projectNode == null) {
            log.debug("Project node not found");
            return null;
        }
        NodePath nodePath = getNodePath(path);
       
        if (nodePath.legacyFormat) {
            return DBNLegacyUtils.legacyGetNodeByPath(monitor, projectNode, nodePath);
        }
        String projectNodePath = projectNode.getNodeUri();
        if (!path.startsWith(projectNodePath)) {
            throw new DBException("Node from another project");
        }
        int nodeLevel = getNodePath(projectNodePath).pathItems.size();
        return findNodeByPath(monitor, projectNode, nodePath, nodeLevel);
    }

    private DBNNode findNodeByPath(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBNNode currentNode,
        @NotNull NodePath nodePath,
        int currentLevel
    ) throws DBException {
        if (CommonUtils.isEmpty(nodePath.pathItems)) {
            return currentNode;
        }
        if (currentLevel >= nodePath.pathItems.size()) {
            throw new DBException("Node level " + currentLevel + " out of range: " + (nodePath.pathItems.size() - 1));
        }
        String expectedNodePathName = nodePath.pathItems.get(currentLevel);
        //skip fake root resource node
        //1 because project node is 0, fake resource node must be 1 in the path
        if (currentLevel == 1 && FAKE_RESOURCE_ROOT_NODE.equals(expectedNodePathName)) {
            currentLevel++;
            expectedNodePathName = nodePath.pathItems.get(currentLevel);
        }
        DBNNode[] children = currentNode.getChildren(monitor);
        if (children == null) {
            return null;
        }

        DBNNode detectedNode = null;
        for (DBNNode child : children) {
            if (child.getNodeId().equals(expectedNodePathName)) {
                detectedNode = child;
                break;
            }
        }

        if (detectedNode == null) {
            log.debug("Node '" + expectedNodePathName + "' not found in parent node '"
                + currentNode.getNodeUri() + "'." + "\nAllowed children: " + Arrays.toString(children));
            return null;
        }

        if (currentLevel == nodePath.pathItems.size() - 1) {
            return detectedNode;
        } else {
            return findNodeByPath(monitor, detectedNode, nodePath, currentLevel + 1);
        }
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
                } else if (child.getObject() == objectToCache) {
                    cached = true;
                    break;
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
        DBSObject parentObject = object.getParentObject();
        if ((object instanceof DBSTablePartition part) && part.needFullPath()) {
            if (part.isSubPartition()) {
                parentObject = part.getPartitionParent();
            } else {
                parentObject = part.getParentTable();
            }
        }
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

            if (item == DBUtils.getPublicObjectContainer(parentObject)) {
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
            log.warn("Remove unregistered meta node object " + node.getNodeDisplayName());
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
            this.listenersCopy = this.listeners.toArray(new INavigatorListener[0]);
        }
    }

    public void removeListener(INavigatorListener listener)
    {
        synchronized (this.listeners) {
            if (!this.listeners.remove(listener)) {
                log.warn("Listener " + listener + " wasn't registered in model");
            }
            this.listenersCopy = this.listeners.toArray(new INavigatorListener[0]);
        }
    }

    public void fireNodeUpdate(Object source, DBNNode node, DBNEvent.NodeChange nodeChange)
    {
        this.fireNodeEvent(new DBNEvent(source, DBNEvent.Action.UPDATE, nodeChange, node));
    }

    public void fireNodeEvent(final DBNEvent event)
    {
        if (!isGlobal() || platform.isShuttingDown()) {
            return;
        }
        synchronized (eventCache) {
            eventCache.add(event);
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
            if (parentNode instanceof DBNProjectDatabases projectDatabases) {
                projectDatabases.getDataSourceRegistry().flushConfig();
                projectDatabases.refreshChildren();
                break;
            }
        }
    }

    public void ensureProjectLoaded(DBPProject project) {
        DBNProject projectNode = getRoot().getProjectNode(project);
        if (projectNode != null) {
            projectNode.getDatabases();
        }
    }

    public void addFilter(Function<DBNNode, Boolean> filter) {
        nodeFilters.add(filter);
    }

    boolean isNodeVisible(DBNNode node) {
        if (!nodeFilters.isEmpty()) {
            for (Function<DBNNode, Boolean> f : nodeFilters) {
                if (!f.apply(node)) {
                    return false;
                }
            }
        }
        return true;
    }

    private class EventProcessingJob extends Job {

        EventProcessingJob() {
            super("Navigator notifier");
            setSystem(true);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            Thread.currentThread().setName("Database navigator events processor");
            while (!platform.isShuttingDown()) {
                RuntimeUtils.pause(100);
                final INavigatorListener[] realListeners;
                synchronized (listeners) {
                    realListeners = listenersCopy;
                }
                if (realListeners == null || realListeners.length == 0) {
                    continue;
                }
                final DBNEvent[] realEvents;
                synchronized (eventCache) {
                    if (eventCache.isEmpty()) {
                        continue;
                    }
                    realEvents = eventCache.toArray(new DBNEvent[0]);
                    eventCache.clear();
                }

                try {
                    DBWorkbench.getPlatformUI().executeWithProgress(() -> {
                        for (int i = 0; i < realEvents.length; i++) {
                            for (INavigatorListener listener : listenersCopy) {
                                listener.nodeChanged(realEvents[i]);
                            }
                        }
                    });
                } catch (Exception e) {
                    log.error(e);
                }
            }
            return Status.OK_STATUS;
        }
    }

}
