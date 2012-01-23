/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSTable;

/**
 * AbstractTable
 */
public abstract class AbstractTable<
    DATASOURCE extends DBPDataSource,
    CONTAINER extends DBSObjectContainer>
    implements DBSTable
{
    private CONTAINER container;
    private String tableName;

    protected AbstractTable(CONTAINER container)
    {
        this.container = container;
    }

    protected AbstractTable(CONTAINER container, String tableName)
    {
        this(container);
        this.tableName = tableName;
    }

    @Override
    public DBSEntityType getEntityType()
    {
        return DBSEntityType.TABLE;
    }

    public CONTAINER getContainer()
    {
        return container;
    }

    @Property(name = "Table Name", viewable = true, editable = true, order = 1)
    public String getName()
    {
        return tableName;
    }

    public void setName(String tableName)
    {
        this.tableName = tableName;
    }

    @SuppressWarnings("unchecked")
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

/*
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

*/
    public String toString()
    {
        return getFullQualifiedName();
    }

}
