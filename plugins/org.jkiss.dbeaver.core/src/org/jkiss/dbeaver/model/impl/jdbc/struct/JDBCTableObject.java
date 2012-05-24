/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * JDBC abstract table object
 */
public abstract class JDBCTableObject<TABLE extends JDBCTable> implements DBSObject, DBPSaveableObject
{
    private final TABLE table;
    private String name;
    private boolean persisted;

    protected JDBCTableObject(TABLE table, String name, boolean persisted) {
        this.table = table;
        this.name = name;
        this.persisted = persisted;
    }

    protected JDBCTableObject(JDBCTableObject<TABLE> source)
    {
        this.table = source.table;
        this.name = source.name;
        this.persisted = source.persisted;
    }

    @Override
    public DBSObject getParentObject()
    {
        return table;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public void setName(String indexName)
    {
        this.name = indexName;
    }

    @Property(name = "Table", viewable = true, order = 2)
    public TABLE getTable()
    {
        return table;
    }

    @Override
    public String getDescription()
    {
        return null;
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

}
