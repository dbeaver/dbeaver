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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Image;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.runtime.DBRProcessListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.ui.NavigatorUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceConnectHandler;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

/**
 * DBNDataSource
 */
public class DBNDataSource extends DBNDatabaseNode implements IAdaptable
{
    private DataSourceDescriptor dataSource;
    private DBXTreeNode treeRoot;

    public DBNDataSource(DBNNode parentNode, DataSourceDescriptor dataSource)
    {
        super(parentNode);
        this.dataSource = dataSource;
        this.treeRoot = dataSource.getDriver().getProviderDescriptor().getTreeDescriptor();
        DBNModel.getInstance().addNode(this, false);
    }

    @Override
    public DBNNode getParentNode()
    {
        String folderPath = dataSource == null ? null : dataSource.getFolderPath();
        if (!CommonUtils.isEmpty(folderPath)) {
            DBNLocalFolder localFolder = ((DBNProjectDatabases) super.getParentNode()).getLocalFolder(folderPath);
            if (localFolder != null) {
                return localFolder;
            }
        }
        return super.getParentNode();
    }

    @Override
    protected void dispose(boolean reflect)
    {
        DBNModel.getInstance().removeNode(this, reflect);

        this.dataSource = null;
        super.dispose(reflect);
    }

    @Override
    public DataSourceDescriptor getObject()
    {
        return dataSource;
    }

    @Override
    public Object getValueObject()
    {
        return dataSource == null ? null : dataSource.getDataSource();
    }

    @Override
    public String getNodeName()
    {
        return dataSource == null ? "" : dataSource.getName();
    }

    @Override
    public String getNodeDescription()
    {
        return dataSource == null ? "" : dataSource.getDescription();
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
        dataSource = (DataSourceDescriptor) object;
    }

    @Override
    public boolean initializeNode(DBRProgressMonitor monitor, DBRProcessListener onFinish)
    {
        if (dataSource == null) {
            return false;
        }
        if (!dataSource.isConnected()) {
            DataSourceConnectHandler.execute(monitor, dataSource, onFinish);
            //dataSource.connect(monitor);
        } else {
            if (onFinish != null) {
                onFinish.onProcessFinish(Status.OK_STATUS);
            }
        }
        return dataSource.isConnected();
    }

    @Override
    public Image getNodeIcon() {
        Image image = super.getNodeIcon();
        DataSourceDescriptor dataSource = getDataSourceContainer();
        if (dataSource.isConnectionReadOnly()) {
            image = DBNModel.getLockedOverlayImage(image);
        }
        if (dataSource.hasNetworkHandlers()) {
            image = DBNModel.getNetworkOverlayImage(image);
        }
        return image;
    }

    @Override
    public Object getAdapter(Class adapter) {
        if (adapter == DBNDataSource.class) {
            return this;
        } else if (DBSDataSourceContainer.class.isAssignableFrom(adapter)) {
            return dataSource;
        }
        return null;
    }

    @Override
    @NotNull
    public DataSourceDescriptor getDataSourceContainer()
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
        if (dataSource == null) {
            log.warn("Try to rename data source after dispose");
            return;
        }
        dataSource.setName(newName);
        dataSource.getRegistry().updateDataSource(dataSource);
    }

    @Override
    protected void afterChildRead()
    {
        if (dataSource == null) {
            log.warn("Try to update data source after dispose");
            return;
        }
        // Notify datasource listeners about state change.
        // We make this action here because we can't update state in
        // initializeNode if this action caused by readChildren.
        // Because readChildren executes in separate job - this job reused by
        // datasource connect job and it do not updates UI after connect because
        // we need to read datasource children immediately.
        // It breaks loading process. So we refresh datasource state manually
        // right after children nodes read
        dataSource.getRegistry().fireDataSourceEvent(
            DBPEvent.Action.OBJECT_UPDATE,
            dataSource,
            true);
    }

    @Override
    public boolean supportsDrop(DBNNode otherNode)
    {
        return otherNode == null || otherNode instanceof DBNDataSource;
    }

    @Override
    public void dropNodes(Collection<DBNNode> nodes) throws DBException
    {
        String folderPath = getDataSourceContainer().getFolderPath();
        for (DBNNode node : nodes) {
            if (node instanceof DBNDataSource) {
                ((DBNDataSource) node).setFolderPath(folderPath);
            }
        }
        NavigatorUtils.updateConfigAndRefreshDatabases(this);
    }

    public void setFolderPath(String folder)
    {
        getDataSourceContainer().setFolderPath(folder);
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
