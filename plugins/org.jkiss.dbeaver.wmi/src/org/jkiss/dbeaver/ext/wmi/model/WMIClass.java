/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.ext.wmi.Activator;
import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDColumnValue;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.OverlayImageDescriptor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.wmi.service.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * WMI class
 */
public class WMIClass extends WMIContainer
    implements DBSEntity, DBPCloseableObject, DBPQualifiedObject, DBSDataContainer, IObjectImageProvider
{
    private static Image IMG_CLASS;
    private static Image IMG_CLASS_ABSTRACT;
    private static Image IMG_CLASS_FINAL;
    private static Image IMG_CLASS_ABSTRACT_FINAL;
    private static Image IMG_ASSOCIATION;
    private static Image IMG_ASSOCIATION_ABSTRACT;

    static {
        IMG_CLASS = Activator.getImageDescriptor("icons/class.png").createImage();
        ImageData baseData = IMG_CLASS.getImageData();
        OverlayImageDescriptor ovrDescriptor = new OverlayImageDescriptor(baseData);
        ovrDescriptor.setTopRight(new ImageDescriptor[]{
            Activator.getImageDescriptor("icons/ovr_abstract.png")});
        IMG_CLASS_ABSTRACT = ovrDescriptor.createImage();
        ovrDescriptor.setBottomRight(new ImageDescriptor[]{
            Activator.getImageDescriptor("icons/ovr_final.png")});
        IMG_CLASS_ABSTRACT_FINAL = ovrDescriptor.createImage();
        ovrDescriptor.setTopRight(null);
        IMG_CLASS_FINAL = ovrDescriptor.createImage();

        IMG_ASSOCIATION = Activator.getImageDescriptor("icons/association.png").createImage();
        baseData = IMG_ASSOCIATION.getImageData();
        ovrDescriptor = new OverlayImageDescriptor(baseData);
        ovrDescriptor.setTopRight(new ImageDescriptor[]{
            Activator.getImageDescriptor("icons/ovr_abstract.png")});
        IMG_ASSOCIATION_ABSTRACT = ovrDescriptor.createImage();
    }

    private WMIClass superClass;
    private WMIObject classObject;
    private String name;
    private List<WMIClass> subClasses = null;
    private List<WMIClassAttribute> attributes = null;
    private List<WMIClassReference> references = null;
    private List<WMIClassMethod> methods = null;

    public WMIClass(WMINamespace parent, WMIClass superClass, WMIObject classObject)
    {
        super(parent);
        this.superClass = superClass;
        this.classObject = classObject;
    }

    public boolean isAbstract() throws WMIException
    {
        return classObject != null && Boolean.TRUE.equals(
            classObject.getQualifier(WMIConstants.Q_Abstract));
    }

    public boolean isAssociation() throws WMIException
    {
        return classObject != null && Boolean.TRUE.equals(
            classObject.getQualifier(WMIConstants.Q_Association));
    }

    public boolean isFinal() throws WMIException
    {
        return classObject != null && Boolean.TRUE.equals(
            classObject.getQualifier(WMIConstants.Q_Terminal));
    }

    @Property(name = "Super Class", viewable = true, order = 10)
    public WMIClass getSuperClass()
    {
        return superClass;
    }

    public WMINamespace getNamespace()
    {
        return parent;
    }

    public WMIObject getClassObject()
    {
        return classObject;
    }

    @Association
    public List<WMIClass> getSubClasses()
    {
        return subClasses;
    }

    @Association
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
        if (name == null) {
            name = "?" + hashCode();
        }
        return name;
    }

    public String getFullQualifiedName()
    {
        if (classObject == null) {
            return getName();
        }
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

    public Collection<WMIClassAttribute> getAttributes(DBRProgressMonitor monitor) throws DBException
    {
        if (attributes == null) {
            readAttributes(monitor);
        }
        return attributes;
    }

    @Override
    public Collection<? extends DBSEntityConstraint> getConstraints(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    public WMIClassAttribute getAttribute(DBRProgressMonitor monitor, String name) throws DBException
    {
        return DBUtils.findObject(getAttributes(monitor), name);
    }

    @Association
    public Collection<WMIClassAttribute> getAllAttributes(DBRProgressMonitor monitor) throws DBException
    {
        if (superClass == null) {
            return getAttributes(monitor);
        } else {
            List<WMIClassAttribute> allAttrs = new ArrayList<WMIClassAttribute>();
            for (WMIClass c = this; c != null; c = c.superClass) {
                for (WMIClassAttribute attr : c.getAttributes(monitor)) {
                    boolean overridden = false;
                    for (WMIClassAttribute a : allAttrs) {
                        if (attr.getName().equals(a.getName())) {
                            overridden = true;
                            break;
                        }
                    }
                    if (!overridden) {
                        allAttrs.add(attr);
                    }
                }
            }
            return allAttrs;
        }
    }

    private synchronized void readAttributes(DBRProgressMonitor monitor) throws DBException
    {
        if (attributes != null) {
            return;
        }
        try {
            attributes = new ArrayList<WMIClassAttribute>();
            for (WMIObjectAttribute prop : classObject.getAttributes(WMIConstants.WBEM_FLAG_CLASS_LOCAL_AND_OVERRIDES)) {
                if (monitor.isCanceled()) {
                    break;
                }
                if (prop.getType() == WMIConstants.CIM_REFERENCE) {
                    Object refClassPath = prop.getQualifier(WMIConstants.Q_CIMTYPE);
                    if (refClassPath == null) {
                        log.warn("No " + WMIConstants.Q_CIMTYPE + " qualifier for reference property");
                        continue;
                    }
                    String refClassName = refClassPath.toString();
                    if (!refClassName.startsWith("ref:")) {
                        log.warn("Invalid class reference qualifier: " + refClassName);
                        continue;
                    }
                    refClassName = refClassName.substring(4);
                    WMIClass refClass = getNamespace().getClass(monitor, refClassName);
                    if (refClass == null) {
                        log.warn("Referenced class '" + refClassName + "' not found in '" + getNamespace().getName() + "'");
                        continue;
                    }
                    if (references == null) {
                        references = new ArrayList<WMIClassReference>();
                    }
                    references.add(new WMIClassReference(this, prop, refClass));
                } /*else*/ if (!prop.isSystem()) {
                    attributes.add(new WMIClassAttribute(this, prop));
                }
            }

        } catch (WMIException e) {
            throw new DBException(e);
        }
    }

    @Association
    public List<WMIClassMethod> getMethods(DBRProgressMonitor monitor) throws DBException
    {
        if (methods == null) {
            readMethods(monitor);
        }
        return methods;
    }

    public WMIClassMethod getMethod(DBRProgressMonitor monitor, String methodName) throws DBException
    {
        if (methods == null) {
            readMethods(monitor);
        }
        return DBUtils.findObject(methods, methodName);
    }

    private synchronized void readMethods(DBRProgressMonitor monitor) throws DBException
    {
        if (methods != null) {
            return;
        }
        try {
            methods = new ArrayList<WMIClassMethod>();
            for (WMIObjectMethod prop : classObject.getMethods(WMIConstants.WBEM_FLAG_LOCAL_ONLY)) {
                if (monitor.isCanceled()) {
                    break;
                }
                methods.add(new WMIClassMethod(this, prop));
            }

        } catch (WMIException e) {
            throw new DBException(e);
        }
    }

    public List<? extends DBSEntityAssociation> getAssociations(DBRProgressMonitor monitor) throws DBException
    {
        // Read attributes and references
        getAttributes(monitor);
        if (superClass == null && CommonUtils.isEmpty(references)) {
            return null;
        }
        List<DBSEntityAssociation> associations = new ArrayList<DBSEntityAssociation>();
        if (superClass != null) {
            associations.add(new WMIClassInheritance(superClass, this));
        }
        if (references != null) {
            associations.addAll(references);
        }
        return associations;
    }

    public List<? extends DBSEntityAssociation> getReferences(DBRProgressMonitor monitor) throws DBException
    {
        if (subClasses == null) {
            return null;
        }
        List<WMIClassInheritance> subList = new ArrayList<WMIClassInheritance>();
        for (WMIClass ss : subClasses) {
            subList.add(new WMIClassInheritance(this, ss));
        }
        return subList;
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

    public Image getObjectImage()
    {
        try {
            if (isAssociation()) {
                return isAbstract() ? IMG_ASSOCIATION : IMG_ASSOCIATION_ABSTRACT;
            } else if (isAbstract()) {
                return isFinal() ? IMG_CLASS_ABSTRACT_FINAL : IMG_CLASS_ABSTRACT;
            } else if (isFinal()) {
                return IMG_CLASS_FINAL;
            }
        } catch (WMIException e) {
            log.warn(e);
        }
        return IMG_CLASS;
    }

    @Override
    protected WMIQualifiedObject getQualifiedObject()
    {
        return classObject;
    }

}
