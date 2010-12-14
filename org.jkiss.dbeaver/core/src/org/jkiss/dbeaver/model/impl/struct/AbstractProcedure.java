/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSProcedure;

/**
 * AbstractProcedure
 */
public abstract class AbstractProcedure<
    DATASOURCE extends DBPDataSource,
    CONTAINER extends DBSEntityContainer>
    implements DBSProcedure
{
    private CONTAINER container;
    private String name;
    private String description;

    protected AbstractProcedure(CONTAINER container)
    {
        this.container = container;
    }

    protected AbstractProcedure(CONTAINER container, String name, String description)
    {
        this(container);
        this.name = name;
        this.description = description;
    }

    public CONTAINER getContainer()
    {
        return container;
    }

    @Property(name = "Procedure Name", viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    protected void setName(String tableName)
    {
        this.name = tableName;
    }

    public String getObjectId() {
        return getParentObject().getObjectId() + getName();
    }

    @Property(name = "Procedure Description", viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    protected void setDescription(String description)
    {
        this.description = description;
    }

    public DATASOURCE getDataSource()
    {
        return (DATASOURCE) container.getDataSource();
    }

    public boolean isPersisted()
    {
        return true;
    }

    public DBSObject getParentObject()
    {
        return container;
    }

    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        return false;
    }
}
