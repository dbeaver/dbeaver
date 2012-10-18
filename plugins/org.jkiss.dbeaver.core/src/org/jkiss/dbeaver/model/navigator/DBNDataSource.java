/*
 * Copyright (C) 2010-2012 Serge Rieder
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
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceConnectHandler;

/**
 * DBNDataSource
 */
public class DBNDataSource extends DBNDatabaseNode implements IAdaptable, IDataSourceContainerProvider
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
    protected void dispose(boolean reflect)
    {
        DBNModel.getInstance().removeNode(this, reflect);

//        if (this.dataSource.isConnected()) {
//            DataSourceDisconnectHandler.execute(this.dataSource, null);
//        }

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
    public boolean isLazyNode()
    {
        return super.isLazyNode();
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
    public boolean initializeNode(DBRProgressMonitor monitor, Runnable onFinish)
    {
        if (!dataSource.isConnected()) {
            DataSourceConnectHandler.execute(monitor, dataSource, onFinish);
            //dataSource.connect(monitor);
        } else {
            if (onFinish != null) {
                onFinish.run();
            }
        }
        return dataSource.isConnected();
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
        dataSource.setName(newName);
        dataSource.getRegistry().updateDataSource(dataSource);
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
        dataSource.getRegistry().fireDataSourceEvent(
            DBPEvent.Action.OBJECT_UPDATE,
            dataSource,
            true);
    }
}
