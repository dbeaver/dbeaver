/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.wmi.service.WMIService;

import java.util.Collection;

/**
 * WMI Namespace
 */
public class WMINamespace extends WMIContainer implements DBPCloseableObject {

    private WMIService service;
    private String name;

    public WMINamespace(WMIContainer parent, String name)
    {
        super(parent);
        this.name = name;
    }

    public WMINamespace(WMIContainer parent, WMIService service)
    {
        super(parent);
        this.service = service;
    }

    public WMIService getService()
    {
        return service;
    }

    public String getName()
    {
        return name;
    }

    public Collection<? extends DBSEntity> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor) throws DBException
    {
        return WMIContainer.class;
    }

    public void close()
    {
        if (service != null) {
            service.close();
            service = null;
        }
    }
}
