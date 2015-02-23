/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.navigator;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.registry.ProjectRegistry;
import org.jkiss.dbeaver.registry.tree.DBXTreeFolder;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.OverlayImageDescriptor;
import org.jkiss.dbeaver.ui.actions.GlobalPropertyTester;
import org.jkiss.utils.CommonUtils;

import java.util.*;

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
    static final Log log = Log.getLog(DBNModel.class);

    private static Map<Image, Map<DBSObjectState, Image>> overlayStateImageCache = new IdentityHashMap<Image, Map<DBSObjectState, Image>>();
    private static Map<Image, Image> overlayLockImageCache = new IdentityHashMap<Image, Image>();
    private static Map<Image, Image> overlayNetworkImageCache = new IdentityHashMap<Image, Image>();

    private DBNRoot root;
    private final List<IDBNListener> listeners = new ArrayList<IDBNListener>();
    private transient IDBNListener[] listenersCopy = null;
    private final Map<DBSObject, Object> nodeMap = new HashMap<DBSObject, Object>();
    private DBNAdapterFactory nodesAdapter;

    public DBNModel()
    {
    }

    public static DBNModel getInstance()
    {
        return DBeaverCore.getInstance().getNavigatorModel();
    }

    public void initialize()
    {
        if (this.root != null) {
            throw new IllegalStateException("Can't initialize navigator model more than once");
        }
        this.root = new DBNRoot();

        // Add all existing projects to root node
        final DBeaverCore core = DBeaverCore.getInstance();
        for (IProject project : core.getLiveProjects()) {
            root.addProject(project, false);
        }
/*
        for (DataSourceDescriptor dataSource : registry.getDataSources()) {
            root.addProject(dataSource);
        }
*/

        core.getWorkspace().addResourceChangeListener(this);

        nodesAdapter = new DBNAdapterFactory();
        IAdapterManager mgr = Platform.getAdapterManager();
        mgr.registerAdapters(nodesAdapter, DBNNode.class);
        mgr.registerAdapters(nodesAdapter, DBPObject.class);
        mgr.registerAdapters(nodesAdapter, DBPQualifiedObject.class);
    }

    public void dispose()
    {
        Platform.getAdapterManager().unregisterAdapters(nodesAdapter);
        DBeaverCore.getInstance().getWorkspace().removeResourceChangeListener(this);
        this.root.dispose(false);
        synchronized (nodeMap) {
            this.nodeMap.clear();
        }
        synchronized (this.listeners) {
            if (!listeners.isEmpty()) {
                for (IDBNListener listener : listeners) {
                    log.warn("Listener '" + listener + "' is not unregistered from DBM model");
                }
            }
            this.listeners.clear();
            this.listenersCopy = null;
        }
        this.root = null;

        synchronized (DBNModel.class) {
            // Clear images cache
            for (Map<DBSObjectState, Image> state : overlayStateImageCache.values()) {
                for (Image img : state.values()) {
                    img.dispose();
                }
            }
            overlayStateImageCache.clear();

            for (Image img : overlayLockImageCache.values()) {
                img.dispose();
            }
            overlayLockImageCache.clear();
            for (Image img : overlayNetworkImageCache.values()) {
                img.dispose();
            }
            overlayNetworkImageCache.clear();
        }
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
        List<DBSObject> path = new ArrayList<DBSObject>();
        for (DBSObject item = object; item != null; item = item.getParentObject()) {
            path.add(0, item);
        }
        for (int i = 0; i < path.size() - 1; i++) {
            DBSObject item = path.get(i);
            DBSObject nextItem = path.get(i + 1);
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
        DBNProject project = getRoot().getProject(getProjectRegistry().getActiveProject());
        if (project == null) {
            log.debug("Project node not found");
            return null;
        }
        return project.getDatabases().getDataSource(CommonUtils.splitString(path, '/').get(0));
    }

    @Nullable
    public DBNNode getNodeByPath(DBRProgressMonitor monitor, String path) throws DBException
    {
        DBNProject project = getRoot().getProject(getProjectRegistry().getActiveProject());
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

    private DBNNode findNodeByPath(DBRProgressMonitor monitor, List<String> items, DBNNode curNode, int firstItem) throws DBException {
        for (int i = firstItem, itemsSize = items.size(); i < itemsSize; i++) {
            String item = items.get(i);
            List<? extends DBNNode> children = curNode.getChildren(monitor);
            DBNNode nextChild = null;
            if (children != null && !children.isEmpty()) {
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
        List<? extends DBNDatabaseNode> children = node.getChildren(monitor);
        boolean cached = false;
        if (!CommonUtils.isEmpty(children)) {
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
        List<DBSObject> path = new ArrayList<DBSObject>();
        for (DBSObject item = object.getParentObject(); item != null; item = item.getParentObject()) {
            path.add(0, item);
        }
        for (int i = 0; i < path.size(); i++) {
            DBSObject item = path.get(i);
            node = getNodeByObject(item);
            if (node == null) {
                // Parent node read
                return null;
            }
            List<? extends DBNDatabaseNode> children = node.getChildNodes();
            if (CommonUtils.isEmpty(children)) {
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
                List<DBNNode> nodeList = new ArrayList<DBNNode>(2);
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

    public void addListener(IDBNListener listener)
    {
        synchronized (this.listeners) {
            if (this.listeners.contains(listener)) {
                log.warn("Listener " + listener + " already registered in model");
            } else {
                this.listeners.add(listener);
            }
            this.listenersCopy = this.listeners.toArray(new IDBNListener[this.listeners.size()]);
        }
    }

    public void removeListener(IDBNListener listener)
    {
        synchronized (this.listeners) {
            if (!this.listeners.remove(listener)) {
                log.warn("Listener " + listener + " wasn't registered in model");
            }
            this.listenersCopy = this.listeners.toArray(new IDBNListener[this.listeners.size()]);
        }
    }

    void fireNodeUpdate(Object source, DBNNode node, DBNEvent.NodeChange nodeChange)
    {
        this.fireNodeEvent(new DBNEvent(source, DBNEvent.Action.UPDATE, nodeChange, node));
    }

    void fireNodeEvent(final DBNEvent event)
    {
        IDBNListener[] listenersCopy;
        synchronized (this.listeners) {
            if (listeners.isEmpty()) {
                return;
            }
            listenersCopy = this.listenersCopy;
        }
        for (IDBNListener listener :  listenersCopy) {
            listener.nodeChanged(event);
        }
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

                            if (getProjectRegistry().getActiveProject() == null) {
                                getProjectRegistry().setActiveProject(project);
                            }
                            GlobalPropertyTester.firePropertyChange(GlobalPropertyTester.PROP_HAS_MULTI_PROJECTS);
                        } else {
                            // Project not found - report an error
                            log.error("Project '" + childDelta.getResource().getName() + "' not found in navigator");
                        }
                    } else {
                        if (childDelta.getKind() == IResourceDelta.REMOVED) {
                            // Project deleted
                            getRoot().removeProject(project);
                            if (project == getProjectRegistry().getActiveProject()) {
                                getProjectRegistry().setActiveProject(null);
                            }
                            GlobalPropertyTester.firePropertyChange(GlobalPropertyTester.PROP_HAS_MULTI_PROJECTS);
                        } else {
                            if (childDelta.getFlags() == IResourceDelta.OPEN) {
                                //projectNode.openProject();
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

    public static synchronized Image getStateOverlayImage(Image image, DBSObjectState state)
    {
        if (state == null) {
            // Empty state
            return image;
        }
        final ImageDescriptor overlayImage = state.getOverlayImage();
        if (overlayImage == null) {
            // No overlay
            return image;
        }
        Map<DBSObjectState, Image> stateOverlays = overlayStateImageCache.get(image);
        if (stateOverlays == null) {
            stateOverlays = new IdentityHashMap<DBSObjectState, Image>();
            overlayStateImageCache.put(image, stateOverlays);
        }
        Image result = stateOverlays.get(state);
        if (result == null) {
            OverlayImageDescriptor oid = new OverlayImageDescriptor(image.getImageData());
            oid.setBottomRight(new ImageDescriptor[] {overlayImage} );
            result = oid.createImage();
            stateOverlays.put(state, result);
        }
        return result;
    }

    public static synchronized Image getLockedOverlayImage(Image image)
    {
        final ImageDescriptor overlayImage = DBIcon.OVER_LOCK.getImageDescriptor();
        Image result = overlayLockImageCache.get(image);
        if (result == null) {
            OverlayImageDescriptor oid = new OverlayImageDescriptor(image.getImageData());
            oid.setBottomLeft(new ImageDescriptor[]{overlayImage});
            result = oid.createImage();
            overlayLockImageCache.put(image, result);
        }
        return result;
    }

    public static synchronized Image getNetworkOverlayImage(Image image)
    {
        final ImageDescriptor overlayImage = DBIcon.OVER_EXTERNAL.getImageDescriptor();
        Image result = overlayNetworkImageCache.get(image);
        if (result == null) {
            OverlayImageDescriptor oid = new OverlayImageDescriptor(image.getImageData());
            oid.setTopRight(new ImageDescriptor[]{overlayImage});
            result = oid.createImage();
            overlayNetworkImageCache.put(image, result);
        }
        return result;
    }

    private static ProjectRegistry getProjectRegistry() {
        return DBeaverCore.getInstance().getProjectRegistry();
    }

}
