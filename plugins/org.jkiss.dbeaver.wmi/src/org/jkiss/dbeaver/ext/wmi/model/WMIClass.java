/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDColumnValue;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;
import org.jkiss.wmi.service.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * WMI class
 */
public class WMIClass extends WMIContainer
    implements WMIClassContainer, DBSTable, DBPCloseableObject, DBSDataContainer
{
    private WMIClass superClass;
    private WMIObject classObject;
    private String name;
    private List<WMIClass> subClasses = null;
    private List<WMIClassProperty> properties = null;

    public WMIClass(WMIContainer parent, WMIClass superClass, WMIObject classObject)
    {
        super(parent);
        this.superClass = superClass;
        this.classObject = classObject;
    }

    @Property(name = "Super Class", viewable = true, order = 10)
    public WMIClass getSuperClass()
    {
        return superClass;
    }

    public WMINamespace getNamespace()
    {
        return (WMINamespace) parent;
    }

    public WMIObject getClassObject()
    {
        return classObject;
    }

    public List<WMIClass> getSubClasses()
    {
        return subClasses;
    }

    public boolean hasClasses()
    {
        return !CommonUtils.isEmpty(subClasses);
    }

    public Collection<WMIClass> getClasses(DBRProgressMonitor monitor) throws DBException
    {
        return subClasses;
    }

    void addSubClass(WMIClass wmiClass)
    {
        if (subClasses == null) {
            subClasses = new ArrayList<WMIClass>();
        }
        subClasses.add(wmiClass);
    }

    @Property(name = "Name", viewable = true, order = 1)
    public String getName()
    {
        if (name == null && classObject != null) {
            try {
                name = CommonUtils.toString(
                    classObject.getValue(WMIConstants.CLASS_PROP_CLASS_NAME));
            } catch (WMIException e) {
                log.error(e);
                return e.getMessage();
            }
        }
        return name;
    }

    public String getFullQualifiedName()
    {
        try {
            return CommonUtils.toString(
                classObject.getValue(WMIConstants.CLASS_PROP_PATH));
        } catch (WMIException e) {
            log.error(e);
            return e.getMessage();
        }
    }

    public boolean isSystem()
    {
        return getName().startsWith("__");
    }

    public boolean isView()
    {
        return false;
    }

    public DBSEntityContainer getContainer()
    {
        return getNamespace();
    }

    public List<WMIClassProperty> getColumns(DBRProgressMonitor monitor) throws DBException
    {
        if (properties == null) {
            readProperties(monitor);
        }
        return properties;
    }

    public WMIClassProperty getColumn(DBRProgressMonitor monitor, String columnName) throws DBException
    {
        if (properties == null) {
            readProperties(monitor);
        }
        return DBUtils.findObject(properties, columnName);
    }

    private synchronized void readProperties(DBRProgressMonitor monitor) throws DBException
    {
        if (properties != null) {
            return;
        }
        try {
            properties = new ArrayList<WMIClassProperty>();
            for (WMIObjectProperty prop : classObject.getProperties()) {
                if (monitor.isCanceled()) {
                    break;
                }
                if (!prop.isSystem()) {
                    properties.add(new WMIClassProperty(this, prop));
                }
            }

        } catch (WMIException e) {
            throw new DBException(e);
        }
    }

    public List<? extends DBSIndex> getIndexes(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    public List<? extends DBSConstraint> getConstraints(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    public List<? extends DBSForeignKey> getForeignKeys(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    public List<? extends DBSForeignKey> getReferences(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    public void close()
    {
        if (classObject != null) {
            classObject.release();
            classObject = null;
        }
    }

    @Override
    public String toString()
    {
        if (classObject == null) {
            return super.toString();
        }
        return getName();
    }

    ///////////////////////////////////////////////////////////////////////
    // Data container

    public int getSupportedFeatures()
    {
        return DATA_SELECT;
    }

    public long readData(DBCExecutionContext context, DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows) throws DBException
    {
        try {
            WMIObjectCollectorSink sink = new WMIObjectCollectorSink(
                context.getProgressMonitor(),
                getNamespace().getService(),
                firstRow, maxRows);
            try {
                WMIService.initializeThread();
                getNamespace().getService().enumInstances(
                    getName(),
                    sink,
                    WMIConstants.WBEM_FLAG_SHALLOW);
                sink.waitForFinish();
                WMIResultSet resultSet = new WMIResultSet(context, this, sink.getObjectList());
                long resultCount = 0;
                try {
                    dataReceiver.fetchStart(context, resultSet);
                    while (resultSet.nextRow()) {
                        resultCount++;
                        dataReceiver.fetchRow(context, resultSet);
                    }
                } finally {
                    try {
                        dataReceiver.fetchEnd(context);
                    } catch (DBCException e) {
                        log.error("Error while finishing result set fetch", e); //$NON-NLS-1$
                    }
                    resultSet.close();
                }
                return resultCount;
            } finally {
                WMIService.unInitializeThread();
            }
        } catch (WMIException e) {
            throw new DBException("Can't enum instances", e);
        }
    }

    public long readDataCount(DBCExecutionContext context, DBDDataFilter dataFilter) throws DBException
    {
        throw new DBException("Not implemented");
    }

    public long insertData(DBCExecutionContext context, List<DBDColumnValue> columns, DBDDataReceiver keysReceiver) throws DBException
    {
        throw new DBException("Not implemented");
    }

    public long updateData(DBCExecutionContext context, List<DBDColumnValue> keyColumns, List<DBDColumnValue> updateColumns, DBDDataReceiver keysReceiver) throws DBException
    {
        throw new DBException("Not implemented");
    }

    public long deleteData(DBCExecutionContext context, List<DBDColumnValue> keyColumns) throws DBException
    {
        throw new DBException("Not implemented");
    }

}
