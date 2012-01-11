/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
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
        this.getModel().addNode(this, false);
    }

    protected void dispose(boolean reflect)
    {
        this.getModel().removeNode(this, reflect);
/*
        if (this.dataSource.isConnected()) {
            try {
                this.dataSource.disconnect(this);
            }
            catch (DBException ex) {
                log.error("Error disconnecting datasource", ex);
            }
        }
*/
        this.dataSource = null;
        super.dispose(reflect);
    }

    public DataSourceDescriptor getObject()
    {
        return dataSource;
    }

    public Object getValueObject()
    {
        return dataSource == null ? null : dataSource.getDataSource();
    }

    public String getNodeName()
    {
        return dataSource == null ? "" : dataSource.getName();
    }

    public String getNodeDescription()
    {
        return dataSource == null ? "" : dataSource.getDescription();
    }

    @Override
    public String getNodePathName()
    {
        return getNodeName();
    }

    public boolean isLazyNode()
    {
        return super.isLazyNode();
    }

    public boolean isManagable()
    {
        return true;
    }

    public DBXTreeNode getMeta()
    {
        return treeRoot;
    }

    @Override
    protected void reloadObject(DBRProgressMonitor monitor, DBSObject object) {
        dataSource = (DataSourceDescriptor) object;
    }

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

    public Object getAdapter(Class adapter) {
        if (adapter == DBNDataSource.class) {
            return this;
        } else if (DBSDataSourceContainer.class.isAssignableFrom(adapter)) {
            return dataSource;
        }
        return null;
    }

    public DBSDataSourceContainer getDataSourceContainer()
    {
        return dataSource;
    }

    public boolean supportsRename()
    {
        return true;
    }

    public void rename(DBRProgressMonitor monitor, String newName)
    {
        dataSource.setName(newName);
        dataSource.getRegistry().updateDataSource(dataSource);
    }

}
