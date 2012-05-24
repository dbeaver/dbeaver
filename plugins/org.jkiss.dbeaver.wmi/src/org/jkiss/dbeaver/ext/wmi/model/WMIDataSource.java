/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.wmi.service.WMIService;

import java.util.Collection;
import java.util.Collections;

/**
 * WMIDataSource
 */
public class WMIDataSource implements DBPDataSource, IAdaptable//, DBSObjectContainer, DBSObjectSelector
{
    private DBSDataSourceContainer container;
    private WMINamespace rootNamespace;

    public WMIDataSource(DBSDataSourceContainer container)
        throws DBException
    {
        this.container = container;
    }

    @Override
    public DBSDataSourceContainer getContainer()
    {
        return container;
    }

    @Override
    public DBPDataSourceInfo getInfo()
    {
        return new WMIDataSourceInfo();
    }

    @Override
    public boolean isConnected()
    {
        return true;
    }

    @Override
    public DBCExecutionContext openContext(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String task)
    {
        return new WMIExecutionContext(monitor, purpose, task, this);
    }

    @Override
    public DBCExecutionContext openIsolatedContext(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String task)
    {
        // Open simple context.
        // Isolated connections doesn't make sense in WMI
        return openContext(monitor, purpose, task);
    }

    @Override
    public void invalidateConnection(DBRProgressMonitor monitor) throws DBException
    {
    }

    @Override
    public void initialize(DBRProgressMonitor monitor) throws DBException
    {
        final DBPConnectionInfo connectionInfo = container.getConnectionInfo();
        try {
            WMIService service = WMIService.connect(
                connectionInfo.getServerName(),
                connectionInfo.getHostName(),
                connectionInfo.getUserName(),
                connectionInfo.getUserPassword(),
                null,
                connectionInfo.getDatabaseName());
            this.rootNamespace = new WMINamespace(null, this, container.getConnectionInfo().getDatabaseName(), service);
        } catch (UnsatisfiedLinkError e) {
            throw new DBException("Can't link with WMI native library", e);
        } catch (Throwable e) {
            throw new DBException("Can't connect to WMI service", e);
        }
    }

    @Override
    public void close()
    {
        if (rootNamespace != null) {
            rootNamespace.close();
            if (rootNamespace.service != null) {
                rootNamespace.service.close();
            }
            rootNamespace = null;
        }
    }

    @Association
    public Collection<WMINamespace> getNamespaces()
    {
        return Collections.singletonList(rootNamespace);
    }

    public WMIService getService()
    {
        return rootNamespace.service;
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == DBSObjectContainer.class) {
            return rootNamespace;
        }
        return null;
    }
}
