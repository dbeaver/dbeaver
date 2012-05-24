/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSProcedure;

/**
 * AbstractProcedure
 */
public abstract class AbstractProcedure<
    DATASOURCE extends DBPDataSource,
    CONTAINER extends DBSObjectContainer>
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

    @Override
    public CONTAINER getContainer()
    {
        return container;
    }

    @Override
    @Property(name = "Procedure Name", viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String tableName)
    {
        this.name = tableName;
    }

    @Override
    @Property(name = "Procedure Description", viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    @Override
    public DATASOURCE getDataSource()
    {
        return (DATASOURCE) container.getDataSource();
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }

    @Override
    public DBSObject getParentObject()
    {
        return container;
    }

}
