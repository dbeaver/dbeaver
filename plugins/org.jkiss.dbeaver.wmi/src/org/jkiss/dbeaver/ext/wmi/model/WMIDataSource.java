/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.wmi.service.WMIService;

import java.util.Collection;
import java.util.Collections;

/**
 * WMIDataSource
 */
public class WMIDataSource implements DBPDataSource//, DBSEntitySelector
{
    static final Log log = LogFactory.getLog(WMIDataSource.class);

    private DBSDataSourceContainer container;
    private WMINamespace rootNamespace;

    public WMIDataSource(DBSDataSourceContainer container)
        throws DBException
    {
        this.container = container;
    }

    public DBSDataSourceContainer getContainer()
    {
        return container;
    }

    public DBPDataSourceInfo getInfo()
    {
        return new WMIDataSourceInfo();
    }

    public boolean isConnected()
    {
        return true;
    }

    public DBCExecutionContext openContext(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String task)
    {
        return new WMIExecutionContext(monitor, purpose, task, this);
    }

    public DBCExecutionContext openIsolatedContext(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String task)
    {
        // Open simple context.
        // Isolated connections doesn't make sense in WMI
        return openContext(monitor, purpose, task);
    }

    public void invalidateConnection(DBRProgressMonitor monitor) throws DBException
    {
    }

    public void initialize(DBRProgressMonitor monitor) throws DBException
    {
        final DBPConnectionInfo connectionInfo = container.getConnectionInfo();
        try {
            WMIService service = WMIService.connect(
                log,
                connectionInfo.getServerName(),
                connectionInfo.getHostName(),
                connectionInfo.getUserName(),
                connectionInfo.getUserPassword(),
                null,
                connectionInfo.getDatabaseName());
            this.rootNamespace = new WMINamespace(null, this, container.getConnectionInfo().getDatabaseName(), service);
        } catch (Throwable e) {
            throw new DBException("Can't connect to WMI service", e);
        }
    }

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

}
