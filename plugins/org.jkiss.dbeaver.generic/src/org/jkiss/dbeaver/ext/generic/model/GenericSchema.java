/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSSchema;

/**
 * GenericSchema
 */
public class GenericSchema extends GenericObjectContainer implements DBSSchema
{
    private GenericCatalog catalog;
    private String schemaName;

    public GenericSchema(GenericDataSource dataSource, String schemaName)
    {
        super(dataSource);
        this.schemaName = schemaName;
    }

    public GenericSchema(GenericCatalog catalog, String schemaName)
    {
        super(catalog.getDataSource());
        this.catalog = catalog;
        this.schemaName = schemaName;
    }

    @Override
    @Property(name = "Catalog", order = 2)
    public GenericCatalog getCatalog()
    {
        return catalog;
    }

    @Override
    public GenericSchema getSchema()
    {
        return this;
    }

    @Override
    public GenericSchema getObject()
    {
        return this;
    }

    @Override
    @Property(name = "Schema Name", viewable = true, order = 1)
    public String getName()
    {
        return schemaName;
    }

    @Override
    @Property(name = "Schema Description", viewable = true, order = 100)
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBSObject getParentObject()
    {
        return catalog != null ? catalog : getDataSource().getContainer();
    }

    @Override
    public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor)
        throws DBException
    {
        return GenericTable.class;
    }
}
