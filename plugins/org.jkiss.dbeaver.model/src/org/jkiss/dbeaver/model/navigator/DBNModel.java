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
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIconComposite;
import org.jkiss.dbeaver.model.DBPApplication;
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

    private final DBPApplication application;
    private DBNRoot root;
    private final List<INavigatorListener> listeners = new ArrayList<>();
    private transient INavigatorListener[] listenersCopy = null;
    private final Map<DBSObject, Object> nodeMap = new HashMap<>();

    public DBNModel(DBPApplication application) {
        this.application = application;
    }

    public DBPApplication getApplication() {
        return application;
    }

    public void initialize()
    {
        if (this.root != null) {
            throw new IllegalStateException("Can't initialize navigator model more than once");
        }
        this.root = new DBNRoot(this);

        // Add all existing projects to root node
        for (IProject project : application.getLiveProjects()) {
            root.addProject(project, false);
        }

        application.getWorkspace().addResourceChangeListener(this);
    }

    public void dispose()
    {
        application.getWorkspace().removeResourceChangeListener(this);
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
                log.warn("Can't find tree node for object " + item.getName() + " (" + item.getClass().getName() + ")");
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

    @Nullable
    public DBNDataSource getDataSourceByPath(String path) throws DBException
    {
        DBNProject project = getRoot().getProject(application.getProjectManager().getActiveProject());
        if (project == null) {
            log.debug("Project node not found");
            return null;
        }
        return project.getDatabases().getDataSource(CommonUtils.splitString(path, '/').get(0));
    }

    @Nullable
    public DBNNode getNodeByPath(DBRProgressMonitor monitor, String path) throws DBException
    {
        DBNProject project = getRoot().getProject(application.getProjectManager().getActiveProject());
        if (project == null) {
            log.debug("Project node not found");
            return null;
        }
        List<String> items = CommonUtils.splitString(path, '/');
        DBNNode curNode = project.getDatabases().getDataSource(items.get(0));
        if (curNode == null) {
            return null;
        }
        return findNodeByPath(monitor, items, curNode, 1);
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

    private DBNNode findNodeByPath(DBRProgressMonitor monitor, List<String> items, DBNNode curNode, int firstItem) throws DBException {
        for (int i = firstItem, itemsSize = items.size(); i < itemsSize; i++) {
            String item = items.get(i);
            DBNNode[] children = curNode.getChildren(monitor);
            DBNNode nextChild = null;
            if (children != null && children.length > 0) {
                for (DBNNode child : children) {
                    if (child instanceof DBNDatabaseFolder) {
                        DBXTreeFolder meta = ((DBNDatabaseFolder) child).getMeta();
                        if (meta != null && !CommonUtils.isEmpty(meta.getType()) && meta.getType().equals(item)) {
                            nextChild = child;
                        }
                    }
                    if (child.getNodeName().equals(item)) {
                        nextChild = child;
                    }
                    if (nextChild != null) {
                        if (i < itemsSize - 1) {
                            nextChild = findNodeByPath(monitor, items, nextChild, i + 1);
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

                            if (application.getProjectManager().getActiveProject() == null) {
                                application.getProjectManager().setActiveProject(project);
                            }
                        } else {
                            // Project not found - report an error
                            log.error("Project '" + childDelta.getResource().getName() + "' not found in navigator");
                        }
                    } else {
                        if (childDelta.getKind() == IResourceDelta.REMOVED) {
                            // Project deleted
                            getRoot().removeProject(project);
                            if (project == application.getProjectManager().getActiveProject()) {
                                application.getProjectManager().setActiveProject(null);
                            }
                        } else {
                            if (childDelta.getFlags() == IResourceDelta.OPEN) {
                                projectNode.openProject();
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
        for (DBNNode parentNode = node.getParentNode(); parentNode != null; parentNode = parentNode.getParentNode()) {
            if (parentNode instanceof DBNProjectDatabases) {
                DBNProjectDatabases projectDatabases = (DBNProjectDatabases) parentNode;
                projectDatabases.getDataSourceRegistry().flushConfig();
                projectDatabases.refreshChildren();
                break;
            }
        }
    }

}
