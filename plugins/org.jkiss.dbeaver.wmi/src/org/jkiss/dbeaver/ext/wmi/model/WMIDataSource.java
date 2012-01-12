/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSEntitySelector;
import org.jkiss.utils.CommonUtils;
import org.jkiss.wmi.service.WMIConstants;
import org.jkiss.wmi.service.WMIException;
import org.jkiss.wmi.service.WMIObject;
import org.jkiss.wmi.service.WMIService;

import java.util.ArrayList;
import java.util.List;

/**
 * WMIDataSource
 */
public class WMIDataSource extends WMINamespace implements DBPDataSource//, DBSEntitySelector
{
    private DBSDataSourceContainer container;

    public WMIDataSource(DBSDataSourceContainer container, WMIService service)
        throws DBException
    {
        super(container.getName());
        this.container = container;
        this.service = service;
    }

    @Override
    public WMIService getService()
    {
        return service;
    }

    public DBSDataSourceContainer getContainer()
    {
        return container;
    }

    public DBPDataSourceInfo getInfo()
    {
        return new WMIDataSourceInfo(getService());
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

    @Override
    public void close()
    {
        super.close();
    }
}
