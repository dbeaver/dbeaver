/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.DBException;
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
    private DBSDataSourceContainer container;
    private WMINamespace rootNamespace;

    public WMIDataSource(DBSDataSourceContainer container, WMIService service)
        throws DBException
    {
        this.container = container;
        this.rootNamespace = new WMINamespace(null, this, container.getConnectionInfo().getDatabaseName(), service);
    }

    public DBSDataSourceContainer getContainer()
    {
        return container;
    }

    public DBPDataSourceInfo getInfo()
    {
        return new WMIDataSourceInfo(rootNamespace.service);
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
    }

    public void close()
    {

    }

    @Association
    public Collection<WMINamespace> getNamespaces()
    {
        return Collections.singletonList(rootNamespace);
    }

}
