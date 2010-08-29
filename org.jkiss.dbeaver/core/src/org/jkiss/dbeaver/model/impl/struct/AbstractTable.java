/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTable;

import java.util.Collection;

/**
 * AbstractTable
 */
public abstract class AbstractTable<
    DATASOURCE extends DBPDataSource,
    CONTAINER extends DBSEntityContainer>
    implements DBSTable, DBSEntityContainer
{
    private CONTAINER container;
    private String tableName;
    private String tableType;
    private String description;

    protected AbstractTable(CONTAINER container)
    {
        this.container = container;
    }

    protected AbstractTable(CONTAINER container, String tableName, String tableType, String description)
    {
        this(container);
        this.tableName = tableName;
        this.tableType = tableType;
        this.description = description;
    }

    public CONTAINER getContainer()
    {
        return container;
    }

    @Property(name = "Table Name", order = 1)
    public String getName()
    {
        return tableName;
    }

    protected void setName(String tableName)
    {
        this.tableName = tableName;
    }

    @Property(name = "Table Type", viewable = true, order = 2)
    public String getTableType()
    {
        return tableType;
    }

    protected void setTableType(String tableType)
    {
        this.tableType = tableType;
    }

    @Property(name = "Table Description", viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    protected void setDescription(String description)
    {
        this.description = description;
    }

    @SuppressWarnings("unchecked")
    public DATASOURCE getDataSource()
    {
        return (DATASOURCE) container.getDataSource();
    }

    public DBSObject getParentObject()
    {
        return container;
    }

    public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getColumns(monitor);
    }

    public DBSObject getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return getColumn(monitor, childName);
    }

    public String toString()
    {
        return getFullQualifiedName();
    }

}
