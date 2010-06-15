package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSSchema;

/**
 * GenericSchema
 */
public class GenericSchema extends GenericStructureContainer implements DBSSchema
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

}
