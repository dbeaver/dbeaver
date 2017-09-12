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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * DBNDataSource
 */
public class DBNDataSource extends DBNDatabaseNode implements DBNContainer, IAdaptable
{
    private final DBPDataSourceContainer dataSource;
    private DBXTreeNode treeRoot;

    public DBNDataSource(@NotNull DBNNode parentNode, @NotNull DBPDataSourceContainer dataSource)
    {
        super(parentNode);
        this.dataSource = dataSource;
        this.treeRoot = dataSource.getDriver().getNavigatorRoot();
        registerNode();
    }

    @Override
    public DBNNode getParentNode()
    {
        DBPDataSourceFolder folder = dataSource.getFolder();
        if (folder != null) {
            DBNLocalFolder localFolder = ((DBNProjectDatabases) super.getParentNode()).getFolderNode(folder);
            if (localFolder != null) {
                return localFolder;
            }
        }
        return super.getParentNode();
    }

    @Override
    protected void dispose(boolean reflect)
    {
        unregisterNode(reflect);

        super.dispose(reflect);
    }

    @Override
    public DBPDataSourceContainer getObject()
    {
        return dataSource;
    }

    @Override
    public Object getValueObject()
    {
        return dataSource.getDataSource();
    }

    @Override
    public String getChildrenType() {
        final List<DBXTreeNode> metaChildren = treeRoot.getChildren(this);
        if (CommonUtils.isEmpty(metaChildren) || metaChildren.size() > 1) {
            return "?";
        } else {
            return metaChildren.get(0).getChildrenType(getDataSource());
        }
    }

    @Override
    public Class<?> getChildrenClass() {
        final List<DBXTreeNode> metaChildren = treeRoot.getChildren(this);
        if (CommonUtils.isEmpty(metaChildren) || metaChildren.size() > 1) {
            return null;
        }
        DBXTreeNode childNode = metaChildren.get(0);
        if (childNode instanceof DBXTreeItem) {
            return getChildrenClass((DBXTreeItem) childNode);
        }
        return null;
    }

    @Override
    public String getNodeName()
    {
        return dataSource.getName();
    }

    @Override
    public String getNodeDescription()
    {
        return dataSource.getDescription();
    }

    @Override
    public String getNodeFullName()
    {
        return getNodeName();
    }

    @Override
    public boolean isManagable()
    {
        return true;
    }

    @Override
    public DBXTreeNode getMeta()
    {
        return treeRoot;
    }

    @Override
    protected void reloadObject(DBRProgressMonitor monitor, DBSObject object) {

    }

    @Override
    public boolean initializeNode(@Nullable DBRProgressMonitor monitor, DBRProgressListener onFinish)
    {
        if (!dataSource.isConnected()) {
            dataSource.initConnection(monitor, onFinish);
        } else {
            if (onFinish != null) {
                onFinish.onTaskFinished(Status.OK_STATUS);
            }
        }
        return dataSource.isConnected();
    }

    @Override
    public DBPImage getNodeIcon() {
        DBPImage image = super.getNodeIcon();
        boolean hasNetworkHandlers = hasNetworkHandlers();
        if (dataSource.isConnectionReadOnly() || hasNetworkHandlers) {
            if (image instanceof DBIconComposite) {
                ((DBIconComposite) image).setTopRight(hasNetworkHandlers ? DBIcon.OVER_EXTERNAL : null);
                ((DBIconComposite) image).setBottomLeft(dataSource.isConnectionReadOnly() ? DBIcon.OVER_LOCK : null);
            } else {
                image = new DBIconComposite(
                    image,
                    false,
                    null,
                    hasNetworkHandlers ? DBIcon.OVER_EXTERNAL : null,
                    dataSource.isConnectionReadOnly() ? DBIcon.OVER_LOCK : null,
                    null);
            }
        }
        return image;
    }

    public boolean hasNetworkHandlers() {
        for (DBWHandlerConfiguration handler : dataSource.getConnectionConfiguration().getDeclaredHandlers()) {
            if (handler.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBNDataSource.class) {
            return adapter.cast(this);
        } else if (DBPDataSourceContainer.class.isAssignableFrom(adapter)) {
            return adapter.cast(dataSource);
        }
        return null;
    }

    @Override
    @NotNull
    public DBPDataSourceContainer getDataSourceContainer()
    {
        return dataSource;
    }

    @Override
    public boolean supportsRename()
    {
        return true;
    }

    @Override
    public void rename(DBRProgressMonitor monitor, String newName)
    {
        dataSource.setName(newName);
        dataSource.persistConfiguration();
        dataSource.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, dataSource, null));
    }

    @Override
    protected void afterChildRead()
    {
        // Notify datasource listeners about state change.
        // We make this action here because we can't update state in
        // initializeNode if this action caused by readChildren.
        // Because readChildren executes in separate job - this job reused by
        // datasource connect job and it do not updates UI after connect because
        // we need to read datasource children immediately.
        // It breaks loading process. So we refresh datasource state manually
        // right after children nodes read
        dataSource.fireEvent(
            new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, dataSource, true));
    }

    @Override
    public boolean supportsDrop(DBNNode otherNode)
    {
        return otherNode == null || otherNode instanceof DBNDataSource;
    }

    @Override
    public void dropNodes(Collection<DBNNode> nodes) throws DBException
    {
        DBPDataSourceFolder folder = dataSource.getFolder();
        for (DBNNode node : nodes) {
            if (node instanceof DBNDataSource) {
                if (!((DBNDataSource) node).setFolder(folder)) {
                    return;
                }
            }
        }
        DBNModel.updateConfigAndRefreshDatabases(this);
    }

    public boolean setFolder(DBPDataSourceFolder folder)
    {
        final DBPDataSourceFolder oldFolder = dataSource.getFolder();
        if (oldFolder == folder) {
            return false;
        }
        dataSource.setFolder(folder);
        return true;
    }

    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) throws DBException
    {
        DBNNode node = super.refreshNode(monitor, source);
        if (node == this) {
            // Refresh succeeded. Let's fire event
            dataSource.fireEvent(
                new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, dataSource));
        }
        return node;
    }

    @Override
    public String toString() {
        return dataSource.toString();
    }
}
