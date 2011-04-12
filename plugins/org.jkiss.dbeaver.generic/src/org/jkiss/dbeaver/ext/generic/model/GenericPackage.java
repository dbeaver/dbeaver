/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityQualified;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GenericPackage
 */
public class GenericPackage extends GenericEntityContainer implements DBSEntityQualified, GenericStoredCode
{

    private GenericEntityContainer container;
    private String packageName;
    private List<GenericProcedure> procedures = new ArrayList<GenericProcedure>();
    private boolean nameFromCatalog;

    public GenericPackage(
        GenericEntityContainer container,
        String packageName,
        boolean nameFromCatalog)
    {
        super(container.getDataSource());
        this.container = container;
        this.packageName = packageName;
        this.nameFromCatalog = nameFromCatalog;
    }

    @Property(name = "Package", viewable = true, order = 1)
    public String getName()
    {
        return packageName;
    }

    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return container;
    }

    @Property(name = "Catalog", viewable = true, order = 3)
    public GenericCatalog getCatalog()
    {
        return container.getCatalog();
    }

    @Property(name = "Schema", viewable = true, order = 4)
    public GenericSchema getSchema()
    {
        return container.getSchema();
    }

    @Override
    public DBSObject getObject()
    {
        return this;
    }

    public List<GenericProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return procedures;
    }

    public List<GenericProcedure> getProcedures(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObjects(procedures, name);
    }

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getCatalog(),
            getSchema(),
            this);
    }

    public Collection<? extends DBSEntity> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        return procedures;
    }

    public DBSEntity getChild(DBRProgressMonitor monitor, String childName) throws DBException
    {
        return DBUtils.findObject(procedures, childName);
    }

    public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor) throws DBException
    {
        return GenericProcedure.class;
    }

    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException
    {
    }

    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        procedures.clear();
        return false;
    }

    public boolean isNameFromCatalog()
    {
        return nameFromCatalog;
    }

    void addProcedure(GenericProcedure procedure)
    {
        procedures.add(procedure);
    }

    void orderProcedures()
    {
        DBUtils.orderObjects(procedures);
    }
}
