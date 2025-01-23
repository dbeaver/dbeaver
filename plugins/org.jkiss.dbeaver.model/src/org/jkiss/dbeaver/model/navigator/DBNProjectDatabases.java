/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.*;

/**
 * DBNProjectDatabases
 */
public class DBNProjectDatabases extends DBNNode implements DBNContainer, DBPEventListener
{
    private DBPDataSourceRegistry dataSourceRegistry;
    private final List<DBNDataSource> dataSources = new ArrayList<>();
    private volatile DBNNode[] children;
    private final IdentityHashMap<DBPDataSourceFolder, DBNLocalFolder> folderNodes = new IdentityHashMap<>();

    public DBNProjectDatabases(DBNProject parentNode, DBPDataSourceRegistry dataSourceRegistry)
    {
        super(parentNode);
        this.dataSourceRegistry = dataSourceRegistry;
        this.dataSourceRegistry.addDataSourceListener(this);

        List<? extends DBPDataSourceContainer> projectDataSources = this.dataSourceRegistry.getDataSources();
        for (DBPDataSourceContainer ds : projectDataSources) {
            addDataSource(ds, false, false);
        }
    }

    @Override
    public boolean isDisposed() {
        return dataSourceRegistry == null;
    }

    @Override
    protected void dispose(boolean reflect) {
        synchronized (dataSources) {
            for (DBNDataSource dataSource : dataSources) {
                DBNUtils.disposeNode(dataSource, reflect);
            }
            dataSources.clear();
        }
        folderNodes.clear();
        children = null;
        if (dataSourceRegistry != null) {
            dataSourceRegistry.removeDataSourceListener(this);
            dataSourceRegistry = null;
        }
        super.dispose(reflect);
    }

    @Override
    public String getNodeType() {
        return "datasources";
    }

    @NotNull
    @Override
    public String getNodeId() {
        return "datasources";
    }

    public DBPDataSourceRegistry getDataSourceRegistry()
    {
        return dataSourceRegistry;
    }

    @Override
    public Object getValueObject()
    {
        return dataSourceRegistry;
    }

    @Override
    public String getChildrenType()
    {
        return ModelMessages.model_navigator_Connection;
    }

    @Override
    public Class<DBPDataSourceContainer> getChildrenClass()
    {
        return DBPDataSourceContainer.class;
    }

    @NotNull
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return getNodeDisplayName();
    }

    @Override
    public String getNodeDisplayName()
    {
        return "Connections";
    }

    @Override
    public String getNodeDescription()
    {
        return getParentNode().getProject().getName() + ModelMessages.model_navigator__connections;
    }

    public DBNProject getParentNode() {
        return (DBNProject) super.getParentNode();
    }

    @Override
    public DBPImage getNodeIcon()
    {
        return DBIcon.TREE_DATABASE_CATEGORY;
    }

    @Override
    public boolean allowsChildren() {
        synchronized (dataSources) {
            return !dataSources.isEmpty() || !dataSourceRegistry.getRootFolders().isEmpty();
        }
    }

    @Override
    public DBNNode[] getChildren(@NotNull DBRProgressMonitor monitor)
    {
        if (children == null && !monitor.isForceCacheUsage()) {
            List<DBNNode> childNodes = new ArrayList<>();
            // Add root folders
            for (DBPDataSourceFolder folder : dataSourceRegistry.getAllFolders()) {
                DBNLocalFolder folderNode = folderNodes.get(folder);
                if (folderNode == null) {
                    folderNode = new DBNLocalFolder(this, folder);
                    folderNodes.put(folder, folderNode);
                }
                if (folder.getParent() == null) {
                    childNodes.add(folderNode);
                }
            }
            synchronized (dataSources) {
                // Add only visible root datasources
                for (DBNDataSource dataSource : dataSources) {
                    if (dataSource == null ||
                        dataSource.getDataSourceContainer().isHidden() ||
                        dataSource.getDataSourceContainer().getFolder() != null) {
                        continue;
                    }
                    childNodes.add(dataSource);
                }
            }
            sortNodes(childNodes);
            this.children = childNodes.toArray(new DBNNode[0]);
        }
        return children;
    }

    @Override
    public boolean supportsDrop(DBNNode otherNode) {
        return otherNode == null
            || otherNode instanceof DBNDataSource
            || otherNode instanceof DBNLocalFolder && ((DBNLocalFolder) otherNode).getDataSourceRegistry() == dataSourceRegistry;
    }

    @Override
    public void dropNodes(DBRProgressMonitor monitor, Collection<DBNNode> nodes) throws DBException {
        moveNodesToFolder(nodes, null);
    }

    public void moveNodesToFolder(Collection<DBNNode> nodes, DBPDataSourceFolder toFolder) throws DBException {
        Set<DBPDataSourceRegistry> registryToRefresh = new LinkedHashSet<>();
        for (DBNNode node : nodes) {
            if (node instanceof DBNDataSource dataSource) {
                dataSource.moveToFolder(dataSource.getOwnerProject(), toFolder);
                DBPDataSourceContainer oldContainer = dataSource.getDataSourceContainer();
                registryToRefresh.add(oldContainer.getRegistry());
                if (oldContainer.getRegistry() == dataSourceRegistry) {
                    // the same registry
                    continue;
                }
                // Set folder to null to avoid its creation during copy
                DBPDataSourceFolder srcFolder = oldContainer.getFolder();
                oldContainer.setFolder(null);
                DBPDataSourceContainer newContainer = oldContainer.createCopy(dataSourceRegistry);
                oldContainer.setFolder(srcFolder);

                newContainer.setFolder(toFolder);
                oldContainer.getRegistry().removeDataSource(oldContainer);

                dataSourceRegistry.addDataSource(newContainer);
                registryToRefresh.add(dataSourceRegistry);
            } else if (node instanceof DBNLocalFolder folder) {
                folder.getFolder().setParent(toFolder);
                registryToRefresh.add(folder.getDataSourceRegistry());
            }
        }

        for (DBPDataSourceRegistry registy : registryToRefresh) {
            registy.flushConfig();
        }

        refreshChildren();
    }

    public void refreshChildren()
    {
        this.children = null;
        getModel().fireNodeUpdate(this, this, DBNEvent.NodeChange.STRUCT_REFRESH);
    }

    @Override
    public boolean allowsOpen() {
        return true;
    }

    @Deprecated
    @Override
    public String getNodeItemPath() {
        return getParentNode().getNodeItemPath() + "/" + getNodeDisplayName();
    }

    public DBNLocalFolder getFolderNode(DBPDataSourceFolder folder)
    {
        synchronized (folderNodes) {
            DBNLocalFolder folderNode = folderNodes.get(folder);
            if (folderNode == null) {
                //log.warn("Folder node '" + folder.getFolderPath() + "' not found");
                folderNode = new DBNLocalFolder(this, folder);
                folderNodes.put(folder, folderNode);
            }
            return folderNode;
        }
    }

    public List<DBNDataSource> getDataSources() {
        return dataSources;
    }

    public DBNDataSource getDataSource(String id) {
        synchronized (dataSources) {
            for (DBNDataSource dataSource : dataSources) {
                if (dataSource.getDataSourceContainer().getId().equals(id)) {
                    return dataSource;
                }
            }
        }
        return null;
    }

    public DBNDataSource getDataSource(DBPDataSourceContainer ds) {
        synchronized (dataSources) {
            for (DBNDataSource dataSource : dataSources) {
                if (dataSource.getDataSourceContainer() == ds) {
                    return dataSource;
                }
            }
        }
        return null;
    }

    private DBNDataSource addDataSource(@NotNull DBPDataSourceContainer descriptor, boolean reflect, boolean reveal)
    {
        if (descriptor.isTemplate()) {
            // Skip templates
            return null;
        }
        DBNDataSource newNode = new DBNDataSource(this, descriptor);
        if (!getModel().isNodeVisible(newNode)) {
            return null;
        }
        synchronized (dataSources) {
            if (dataSources.stream().anyMatch(node -> node.getDataSourceContainer() == descriptor)) {
                // current node already contains provided data source
                return null;
            }
            dataSources.add(newNode);
        }

        DBPDataSourceFolder dsFolder = descriptor.getFolder();
        if (dsFolder != null) {
            // Add folder node to cache
            getFolderNode(dsFolder);
        }

        children = null;
        if (reflect) {
            getModel().fireNodeEvent(new DBNEvent(
                this,
                DBNEvent.Action.ADD,
                reveal ? DBNEvent.NodeChange.SELECT : DBNEvent.NodeChange.REFRESH,
                newNode));
        }
        return newNode;
    }

    void removeDataSource(DBPDataSourceContainer descriptor) {
        DBNDataSource removedNode = null;
        synchronized (dataSources) {
            for (Iterator<DBNDataSource> iter = dataSources.iterator(); iter.hasNext(); ) {
                DBNDataSource dataSource = iter.next();
                if (dataSource.getObject() == descriptor) {
                    iter.remove();
                    removedNode = dataSource;
                    break;
                }
            }
        }
        if (removedNode != null) {
            children = null;
            DBNUtils.disposeNode(removedNode, true);
            refreshChildren();
        }
    }

    @Override
    public void handleDataSourceEvent(DBPEvent event)
    {
        DBNModel model = getModel();
        switch (event.getAction()) {
            case OBJECT_ADD:
                if (event.getObject() instanceof DBPDataSourceContainer container) {
                    addDataSource(container, true, event.getEnabled() != null && event.getEnabled());
                } else if (model.getNodeByObject(event.getObject()) == null) {
                    DBNDatabaseNode parentNode = null;
                    {
                        Object containerNode = event.getOptions().get(DBEObjectManager.OPTION_CONTAINER);
                        if (containerNode instanceof DBNDatabaseFolder && event.getObject().getClass().getName().equals(((DBNDatabaseFolder) containerNode).getMeta().getType())) {
                            // Use container node only if it a folder with exact object type
                            // Otherwise it may be a wrong node (e.g. grand-parent node)
                            parentNode = (DBNDatabaseNode) containerNode;
                        }
                    }
                    if (parentNode == null) {
                        parentNode = model.getParentNode(event.getObject());
                    }
                    boolean parentFound = (parentNode != null);
                    if (parentNode == null) {
                        // Not yet loaded. Parent node might be a folder (like Tables)
                        parentNode = model.getParentNode(event.getObject());
                        parentFound = false;
                    }
                    if (parentNode != null) {
                        if (parentNode.getChildNodes() == null && parentNode.hasChildren(false)) {
                            final DBNDatabaseNode nodeToLoad = parentNode;
                            // We have to load children here
                            final AbstractJob loaderJob = new AbstractJob("Load sibling nodes of new database object") {
                                {
                                    setUser(true);
                                }
                                @Override
                                protected IStatus run(DBRProgressMonitor monitor) {
                                    try {
                                        nodeToLoad.getChildren(monitor);
                                    } catch (Exception e) {
                                        return GeneralUtils.makeExceptionStatus(e);
                                    }
                                    return Status.OK_STATUS;
                                }
                            };
                            loaderJob.schedule();
                            try {
                                loaderJob.join();
                            } catch (InterruptedException e) {
                                // That's ok
                            }
                        }
                        if (!parentFound) {
                            // Second try
                            parentNode = model.getParentNode(event.getObject());
                        }
                        if (parentNode != null && parentNode.getChildNodes() != null && !parentNode.hasChildItem(event.getObject())) {
                            // Add only if object wasn't yet added (e.g. by create new object command)
                            parentNode.addChildItem(event.getObject());
                        }
                    }
                }
                break;
            case OBJECT_REMOVE:
                if (event.getObject() instanceof DBPDataSourceContainer) {
                    removeDataSource((DBPDataSourceContainer) event.getObject());
                } else {
                    final DBNDatabaseNode node = model.getNodeByObject(event.getObject());
                    if (node != null && node.getParentNode() instanceof DBNDatabaseNode) {
                        ((DBNDatabaseNode)node.getParentNode()).removeChildItem(event.getObject());
                    }
                }
                break;
            case BEFORE_CONNECT:
            case AFTER_CONNECT: {
                DBNDatabaseNode dbmNode = model.getNodeByObject(event.getObject());
                if (dbmNode != null) {
                    model.fireNodeUpdate(
                        event,
                        dbmNode,
                        event.getAction() == DBPEvent.Action.BEFORE_CONNECT ?
                            DBNEvent.NodeChange.BEFORE_LOAD : DBNEvent.NodeChange.AFTER_LOAD);
                }
                break;
                }
            case OBJECT_UPDATE:
            case OBJECT_SELECT:
            {
                DBNDatabaseNode dbmNode = model.getNodeByObject(event.getObject());
                if (dbmNode != null) {
                    DBNEvent.NodeChange nodeChange;
                    Boolean enabled = event.getEnabled();
                    Object source = event;
                    if (event.getAction() == DBPEvent.Action.OBJECT_SELECT) {
                        nodeChange = DBNEvent.NodeChange.REFRESH;
                        if (enabled != null && enabled) {
                            source = DBNEvent.FORCE_REFRESH;
                        }
                    } else {
                        if (enabled != null) {
                            if (enabled) {
                                nodeChange = DBNEvent.NodeChange.LOAD;
                            } else {
                                nodeChange = DBNEvent.NodeChange.UNLOAD;
                            }
                        } else {
                            nodeChange = DBNEvent.NodeChange.REFRESH;
                        }
                        if (event.getData() == DBPEvent.REORDER) {
                            dbmNode.updateChildrenOrder(false);
                        }
                    }
                    model.fireNodeUpdate(
                        source,
                        dbmNode,
                        nodeChange);

                    if (event.getObject() instanceof DBPDataSourceContainer) {
                        if (enabled != null) {
                            if (!enabled) {
                                // Clear disabled node
                                dbmNode.clearNode(false);
                            }
                        } else {
                            if (event.getAction() == DBPEvent.Action.OBJECT_UPDATE) {
                                // Force reorder.
                                children = null;
                                getModel().fireNodeEvent(new DBNEvent(this, DBNEvent.Action.UPDATE, this));
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    @Override
    public boolean hasChildren(boolean navigableOnly) {
        return true;
    }

}
