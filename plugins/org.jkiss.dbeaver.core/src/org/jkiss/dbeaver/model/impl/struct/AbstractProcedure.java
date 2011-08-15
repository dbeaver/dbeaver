/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPSaveableObject;
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
    implements DBSProcedure, DBPSaveableObject
{
    protected CONTAINER container;
    protected String name;
    protected String description;
    protected boolean persisted;

    protected AbstractProcedure(CONTAINER container, boolean persisted)
    {
        this.container = container;
        this.persisted = persisted;
    }

    protected AbstractProcedure(CONTAINER container, boolean persisted, String name, String description)
    {
        this(container, persisted);
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

    public void setName(String tableName)
    {
        this.name = tableName;
    }

    @Property(name = "Procedure Description", viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public DATASOURCE getDataSource()
    {
        return (DATASOURCE) container.getDataSource();
    }

    public boolean isPersisted()
    {
        return persisted;
    }

    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
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
