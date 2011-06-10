/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSCatalog;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSSchema;

/**
 * AbstractSchema
 */
public abstract class AbstractSchema<DATASOURCE extends DBPDataSource> implements DBSSchema
{
    private DATASOURCE dataSource;
    private String schemaName;

    protected AbstractSchema(DATASOURCE dataSource, String schemaName)
    {
        this.dataSource = dataSource;
        this.schemaName = schemaName;
    }

    public DATASOURCE getDataSource()
    {
        return dataSource;
    }

    public boolean isPersisted()
    {
        return true;
    }

    @Property(name = "Schema Name", viewable = true, order = 1)
    public String getName()
    {
        return schemaName;
    }

    public void setName(String catalogName)
    {
        this.schemaName = catalogName;
    }

    public DBSObject getParentObject()
    {
        final DBSCatalog catalog = getCatalog();
        return catalog != null ? catalog : getDataSource().getContainer();
    }

    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        return false;
    }

    @Override
    public String toString()
    {
        return getName() + " [" + getDataSource().getContainer().getName() + "]";
    }

}
