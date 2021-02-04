/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;
import org.jkiss.wmi.service.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * WMI Namespace
 */
public class WMINamespace extends WMIContainer implements DBSObjectContainer, DBPCloseableObject {

    private static final Log log = Log.getLog(WMINamespace.class);

    protected WMIDataSource dataSource;
    private String name;
    protected WMIService service;
    private volatile List<WMINamespace> namespaces;
    private volatile List<WMIClass> rooClasses;
    private volatile List<WMIClass> associations;
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

    @NotNull
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

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
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
            getService().enumInstances("__NAMESPACE", sink, WMIConstants.WBEM_FLAG_SHALLOW);
            sink.waitForFinish();
            List<WMINamespace> children = new ArrayList<>();
            for (WMIObject object : sink.getObjectList()) {
                String nsName = CommonUtils.toString(object.getValue("Name"));
                children.add(new WMINamespace(this, dataSource, nsName, null));
                object.release();
            }
            DBUtils.orderObjects(children);
            return children;
        } catch (WMIException e) {
            throw new DBException(e, getDataSource());
        }
    }

    @Association
    public Collection<WMIClass> getRootClasses(DBRProgressMonitor monitor)
        throws DBException
    {
        if (rooClasses == null) {
            synchronized (this) {
                if (rooClasses == null) {
                    // Class are not yet loaded - it means we are in datasource object
                    loadClasses(monitor);
                }
            }
        }
        return rooClasses;
    }

    @Association
    public Collection<WMIClass> getClasses(DBRProgressMonitor monitor)
        throws DBException
    {
        getRootClasses(monitor);
        return allClasses;
    }

    public WMIClass getClass(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObject(getClasses(monitor), name);
    }

    @Association
    public Collection<WMIClass> getAssociations(DBRProgressMonitor monitor)
        throws DBException
    {
        getRootClasses(monitor);
        return associations;
    }

    void loadClasses(DBRProgressMonitor monitor)
        throws DBException
    {
        boolean showSystemObjects = getDataSource().getContainer().getNavigatorSettings().isShowSystemObjects();

        try {
            WMIObjectCollectorSink sink = new WMIObjectCollectorSink(monitor, getService());

            getService().enumClasses(null, sink, WMIConstants.WBEM_FLAG_DEEP);
            sink.waitForFinish();
            List<WMIClass> allClasses = new ArrayList<>();
            List<WMIClass> allAssociations = new ArrayList<>();
            List<WMIClass> rootClasses = new ArrayList<>();
            for (WMIObject object : sink.getObjectList()) {
                WMIClass superClass = null;
                String superClassName = (String)object.getValue(WMIConstants.CLASS_PROP_SUPER_CLASS);
                if (superClassName != null) {
                    for (WMIClass c : allClasses) {
                        if (c.getName().equals(superClassName)) {
                            superClass = c;
                            break;
                        }
                    }
                    if (superClass == null) {
                        for (WMIClass c : allAssociations) {
                            if (c.getName().equals(superClassName)) {
                                superClass = c;
                                break;
                            }
                        }
                        if (superClass == null) {
                            log.warn("Super class '" + superClassName + "' not found");
                        }
                    }
                }
                WMIClass wmiClass = new WMIClass(this, superClass, object);
                if (wmiClass.isAssociation()) {
                    allAssociations.add(wmiClass);
                } else {
                    allClasses.add(wmiClass);
                    if (superClass == null) {
                        rootClasses.add(wmiClass);
                    }
                }
                if (superClass != null) {
                    superClass.addSubClass(wmiClass);
                }
            }

            // filter out system classes
            if (!showSystemObjects) {
                for (Iterator<WMIClass> iter = allClasses.iterator(); iter.hasNext(); ) {
                    WMIClass wmiClass = iter.next();
                    if (wmiClass.isSystem()) {
                        iter.remove();
                    }
                }
            }

            DBUtils.orderObjects(rootClasses);
            DBUtils.orderObjects(allClasses);
            DBUtils.orderObjects(allAssociations);

            this.rooClasses = rootClasses;
            this.allClasses = allClasses;
            this.associations = allAssociations;
        } catch (WMIException e) {
            throw new DBException(e, getDataSource());
        }
    }

    @Override
    public Collection<? extends WMIContainer> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        List<WMIContainer> children = new ArrayList<>();
        children.addAll(getNamespaces(monitor));
        children.addAll(getClasses(monitor));
        children.addAll(getAssociations(monitor));
        return children;
    }

    @Override
    public WMIContainer getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException
    {
        return DBUtils.findObject(getChildren(monitor), childName);
    }

    @NotNull
    @Override
    public Class<? extends WMIContainer> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException
    {
        return WMIContainer.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException
    {
        getNamespaces(monitor);
        getClasses(monitor);
    }

    @Override
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
            rooClasses.clear();
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
