/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSSchema;
import org.jkiss.utils.CommonUtils;
import org.jkiss.wmi.service.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * WMI Namespace
 */
public class WMINamespace extends WMIContainer implements WMIClassContainer, DBSSchema, DBPCloseableObject {

    protected WMIDataSource dataSource;
    private String name;
    protected WMIService service;
    private volatile List<WMINamespace> namespaces;
    private volatile List<WMIClass> classes;
    private volatile List<WMIClass> allClasses;

    public WMINamespace(WMINamespace parent, WMIDataSource dataSource, String name, WMIService service)
    {
        super(parent);
        this.dataSource = dataSource;
        this.name = name;
        this.service = service;
    }

    @Override
    public DBSObject getParentObject()
    {
        return parent != null ? parent : dataSource.getContainer();
    }

    @Override
    public WMIDataSource getDataSource()
    {
        return dataSource;
    }

    public WMIService getService() throws WMIException
    {
        if (service == null) {
            this.service = parent.getService().openNamespace(this.name);
        }
        return service;
    }

    @Property(name = "Name", viewable = true, order = 1)
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
        try {
            WMIObjectCollectorSink sink = new WMIObjectCollectorSink(monitor, getService());
            try {
                WMIService.initializeThread();
                getService().enumInstances("__NAMESPACE", sink, WMIConstants.WBEM_FLAG_SHALLOW);
                sink.waitForFinish();
                List<WMINamespace> children = new ArrayList<WMINamespace>();
                for (WMIObject object : sink.getObjectList()) {
                    String nsName = CommonUtils.toString(object.getValue("Name"));
                    children.add(new WMINamespace(this, dataSource, nsName, null));
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

    public boolean hasClasses()
    {
        return true;
    }

    public Collection<WMIClass> getClasses(DBRProgressMonitor monitor)
        throws DBException
    {
        if (classes == null) {
            synchronized (this) {
                if (classes == null) {
                    // Class are not yet loaded - it means we are in datasource object
                    loadClasses(monitor);
                }
            }
        }
        return classes;
    }

    public Collection<WMIClass> getAllClasses(DBRProgressMonitor monitor)
        throws DBException
    {
        getClasses(monitor);
        return allClasses;
    }

    void loadClasses(DBRProgressMonitor monitor)
        throws DBException
    {
        boolean showSystemObjects = getDataSource().getContainer().isShowSystemObjects();

        try {
            WMIObjectCollectorSink sink = new WMIObjectCollectorSink(monitor, getService());
            try {
                WMIService.initializeThread();
                getService().enumClasses(null, sink, WMIConstants.WBEM_FLAG_DEEP);
                sink.waitForFinish();
                List<WMIClass> children = new ArrayList<WMIClass>();
                List<WMIClass> rootClasses = new ArrayList<WMIClass>();
                for (WMIObject object : sink.getObjectList()) {
                    WMIClass superClass = null;
                    String superClassName = (String)object.getValue(WMIConstants.CLASS_PROP_SUPER_CLASS);
                    if (superClassName != null) {
                        for (WMIClass c : children) {
                            if (c.getName().equals(superClassName)) {
                                superClass = c;
                                break;
                            }
                        }
                        if (superClass == null) {
                            log.warn("Super class '" + superClassName + "' not found");
                        }
                    }
                    WMIClass wmiClass = new WMIClass(this, superClass, object);
                    children.add(wmiClass);
                    if (superClass == null) {
                        rootClasses.add(wmiClass);
                    } else {
                        superClass.addSubClass(wmiClass);
                    }
                }

                // filter out system classes
                if (!showSystemObjects) {
                    for (Iterator<WMIClass> iter = children.iterator(); iter.hasNext(); ) {
                        WMIClass wmiClass = iter.next();
                        if (wmiClass.isSystem()) {
                            iter.remove();
                        }
                    }
                }

                DBUtils.orderObjects(rootClasses);
                DBUtils.orderObjects(children);

                this.classes = rootClasses;
                this.allClasses = children;
            } finally {
                WMIService.unInitializeThread();
            }
        } catch (WMIException e) {
            throw new DBException("Can't enum classes", e);
        }
    }

    public Collection<? extends WMIContainer> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        List<WMIContainer> children = new ArrayList<WMIContainer>();
        children.addAll(getNamespaces(monitor));
        children.addAll(getAllClasses(monitor));
        return children;
    }

    public WMIContainer getChild(DBRProgressMonitor monitor, String childName) throws DBException
    {
        return DBUtils.findObject(getChildren(monitor), childName);
    }

    public Class<? extends WMIContainer> getChildType(DBRProgressMonitor monitor) throws DBException
    {
        return WMIContainer.class;
    }

    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException
    {
        getNamespaces(monitor);
        getClasses(monitor);
    }

    public void close()
    {
        if (!CommonUtils.isEmpty(namespaces)) {
            for (WMINamespace namespace : namespaces) {
                namespace.close();
            }
            namespaces.clear();
        }
        if (!CommonUtils.isEmpty(allClasses)) {
            for (WMIClass wmiClass : allClasses) {
                wmiClass.close();
            }
            allClasses.clear();
            classes.clear();
        }
        if (parent != null && service != null) {
            service.close();
            service = null;
        }
    }

    @Override
    protected WMIQualifiedObject getQualifiedObject()
    {
        return null;
    }
}
