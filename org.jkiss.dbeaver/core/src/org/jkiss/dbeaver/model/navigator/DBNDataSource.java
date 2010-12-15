/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.DBPDeletableObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceConnectHandler;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceDisconnectHandler;

/**
 * DBNDataSource
 */
public class DBNDataSource extends DBNTreeNode implements DBPDeletableObject, IAdaptable, IDataSourceContainerProvider
{
    private DataSourceDescriptor dataSource;
    private DBXTreeNode treeRoot;

    public DBNDataSource(DBNRoot parentNode, DataSourceDescriptor dataSource)
    {
        super(parentNode);
        this.dataSource = dataSource;
        this.treeRoot = dataSource.getDriver().getProviderDescriptor().getTreeDescriptor();
        this.getModel().addNode(this, true);
    }

    protected void dispose()
    {
        this.getModel().removeNode(this, true);
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
        super.dispose();
    }

    public DataSourceDescriptor getObject()
    {
        return dataSource;
    }

    public Object getValueObject()
    {
        return dataSource.getDataSource();
    }

    public String getNodeName()
    {
        return dataSource.getName();
    }

    public String getNodeDescription()
    {
        return dataSource.getDescription();
    }

    public String getDefaultCommandId()
    {
        return ICommandIds.CMD_EDIT_CONNECTION;
    }

    public boolean isLazyNode()
    {
        return false;
    }

    public DBXTreeNode getMeta()
    {
        return treeRoot;
    }

    @Override
    protected void reloadObject(DBRProgressMonitor monitor, DBSObject object) {
        dataSource = (DataSourceDescriptor) object;
    }

    protected boolean initializeNode(DBRProgressMonitor monitor)
        throws DBException
    {
        if (!dataSource.isConnected()) {
            DataSourceConnectHandler.execute(dataSource);
            //dataSource.connect(monitor);
        }
        return dataSource.isConnected();
    }

    public void deleteObject(final IWorkbenchWindow workbenchWindow) {
        if (dataSource.isConnected()) {
            DataSourceDisconnectHandler.execute(dataSource);
        }
        DataSourceRegistry.getDefault().removeDataSource(dataSource);
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

}
