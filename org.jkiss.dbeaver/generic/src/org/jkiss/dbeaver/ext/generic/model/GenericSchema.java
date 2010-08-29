/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSSchema;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;

/**
 * GenericSchema
 */
public class GenericSchema extends GenericEntityContainer implements DBSSchema
{
    private GenericDataSource dataSource;
    private GenericCatalog catalog;
    private String schemaName;

    public GenericSchema(GenericDataSource dataSource, String schemaName)
    {
        this.dataSource = dataSource;
        this.schemaName = schemaName;
        this.initCache();
    }

    public GenericSchema(GenericCatalog catalog, String schemaName)
    {
        this.dataSource = catalog.getDataSource();
        this.catalog = catalog;
        this.schemaName = schemaName;
        this.initCache();
    }

    public GenericDataSource getDataSource()
    {
        return dataSource;
    }

    @Property(name = "Catalog", order = 2)
    public GenericCatalog getCatalog()
    {
        return catalog;
    }

    public GenericSchema getSchema()
    {
        return this;
    }

    public GenericSchema getObject()
    {
        return this;
    }

    @Property(name = "Schema Name", order = 1)
    public String getName()
    {
        return schemaName;
    }

    @Property(name = "Schema Description", viewable = true, order = 100)
    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return catalog != null ? catalog : getDataSource().getContainer();
    }

    public Class<? extends DBSObject> getChildType(DBRProgressMonitor monitor)
        throws DBException
    {
        return GenericTable.class;
    }
}
