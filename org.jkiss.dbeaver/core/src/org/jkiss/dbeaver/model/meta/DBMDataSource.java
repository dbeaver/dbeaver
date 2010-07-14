/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.meta;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.ui.actions.EditConnectionAction;

/**
 * DBMDataSource
 */
public class DBMDataSource extends DBMTreeNode
{
    static final Log log = LogFactory.getLog(DBMDataSource.class);

    private DataSourceDescriptor dataSource;
    private DBXTreeNode treeRoot;

    public DBMDataSource(DBMRoot parentNode, DataSourceDescriptor dataSource)
    {
        super(parentNode);
        this.dataSource = dataSource;
        this.treeRoot = dataSource.getDriver().getProviderDescriptor().getTreeDescriptor();
        this.getModel().addNode(this);
    }

    protected void dispose()
    {
        this.getModel().removeNode(this);
        if (this.dataSource.isConnected()) {
            try {
                this.dataSource.disconnect(this);
            }
            catch (DBException ex) {
                log.error("Error disconnecting datasource", ex);
            }
        }
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

    public DBMNode refreshNode(DBRProgressMonitor monitor)
        throws DBException
    {
        if (dataSource.isConnected()) {
            dataSource.getDataSource().refreshDataSource(monitor);
        }
        this.clearChildren();
        return this;
    }

    public Class<EditConnectionAction> getDefaultAction()
    {
        return EditConnectionAction.class;
    }

    public boolean isLazyNode()
    {
        return false;
    }

    public DBXTreeNode getMeta()
    {
        return treeRoot;
    }

    protected boolean initializeNode(DBRProgressMonitor monitor)
        throws DBException
    {
        if (!dataSource.isConnected()) {
            dataSource.connect(this);
        }
        return dataSource.isConnected();
    }

}
