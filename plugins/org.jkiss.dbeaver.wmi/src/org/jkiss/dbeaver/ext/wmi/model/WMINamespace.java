/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.utils.CommonUtils;
import org.jkiss.wmi.service.WMIConstants;
import org.jkiss.wmi.service.WMIException;
import org.jkiss.wmi.service.WMIObject;
import org.jkiss.wmi.service.WMIService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * WMI Namespace
 */
public class WMINamespace extends WMIContainer implements DBPCloseableObject {

    private String name;
    private volatile List<WMINamespace> namespaces;
    private volatile List<WMIClass> classes;
    protected WMIService service;

    public WMINamespace(WMIContainer parent, String name)
    {
        super(parent);
        this.name = name;
    }

    public WMINamespace(String name)
    {
        super(null);
        this.name = name;
    }

    public WMIService getService() throws WMIException
    {
        if (service == null) {
            this.service = getDataSource().getService().openNamespace(this.name);
        }
        return service;
    }

    @Property(name = "Name", viewable = true)
    public String getName()
    {
        return name;
    }

    public Collection<WMINamespace> getNamespaces(DBRProgressMonitor monitor)
        throws DBException
    {
        if (namespaces == null) {
            synchronized (this) {
                if (namespaces == null) {
                    // Namespaces are not yet loaded - it means we are in datasource object
                    namespaces = loadNamespaces(monitor);
                }
            }
        }
        return namespaces;
    }

    List<WMINamespace> loadNamespaces(DBRProgressMonitor monitor)
        throws DBException
    {
        WMIObjectCollectorSink sink = new WMIObjectCollectorSink(monitor);
        try {
            try {
                WMIService.initializeThread();
                getService().enumInstances("__NAMESPACE", sink, WMIConstants.WBEM_FLAG_SEND_STATUS);
                sink.waitForFinish();
                List<WMINamespace> children = new ArrayList<WMINamespace>();
                for (WMIObject object : sink.getObjectList()) {
                    String nsName = CommonUtils.toString(object.getValue("Name"));
                    children.add(new WMINamespace(this, nsName));
                    object.release();
                }
                DBUtils.orderObjects(children);
                return children;
            } finally {
                WMIService.unInitializeThread();
            }
        } catch (WMIException e) {
            throw new DBException("Can't enum namespaces", e);
        }
    }

    public Collection<WMIClass> getClasses(DBRProgressMonitor monitor)
        throws DBException
    {
        if (classes == null) {
            synchronized (this) {
                if (classes == null) {
                    // Class are not yet loaded - it means we are in datasource object
                    classes = loadClasses(monitor);
                }
            }
        }
        return classes;
    }

    List<WMIClass> loadClasses(DBRProgressMonitor monitor)
        throws DBException
    {
        boolean showSystemObjects = getDataSource().getContainer().isShowSystemObjects();

        WMIObjectCollectorSink sink = new WMIObjectCollectorSink(monitor);
        try {
            try {
                WMIService.initializeThread();
                getService().enumClasses(null, sink, WMIConstants.WBEM_FLAG_SEND_STATUS | WMIConstants.WBEM_FLAG_DEEP);
                sink.waitForFinish();
                List<WMIClass> children = new ArrayList<WMIClass>();
                for (WMIObject object : sink.getObjectList()) {
                    WMIClass wmiClass = new WMIClass(this, object);
                    if (!showSystemObjects && wmiClass.getName().startsWith("__")) {
                        wmiClass.close();
                        continue;
                    }
                    children.add(wmiClass);
                }
                DBUtils.orderObjects(children);
                return children;
            } finally {
                WMIService.unInitializeThread();
            }
        } catch (WMIException e) {
            throw new DBException("Can't enum classes", e);
        }
    }

    public Collection<? extends DBSEntity> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        List<WMIContainer> children = new ArrayList<WMIContainer>();
        children.addAll(getNamespaces(monitor));
        children.addAll(getClasses(monitor));
        return children;
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
