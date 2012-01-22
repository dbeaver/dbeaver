/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSIndexType;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * JDBC abstract index
 */
public abstract class JDBCTableIndex<TABLE extends JDBCTable>
    extends AbstractTableIndex
    implements DBPSaveableObject
{
    private final TABLE table;
    protected String name;
    protected DBSIndexType indexType;
    private boolean persisted;

    protected JDBCTableIndex(TABLE table, String name, DBSIndexType indexType, boolean persisted) {
        this.table = table;
        this.name = name;
        this.indexType = indexType;
        this.persisted = persisted;
    }

    protected JDBCTableIndex(JDBCTableIndex<TABLE> source)
    {
        this.table = source.table;
        this.name = source.name;
        this.indexType = source.indexType;
        this.persisted = source.persisted;
    }

    public DBSObject getParentObject()
    {
        return table;
    }

    @Property(name = "Index Name", viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
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

    @Property(name = "Index Type", viewable = true, order = 3)
    public DBSIndexType getIndexType()
    {
        return this.indexType;
    }

    public boolean isPersisted()
    {
        return persisted;
    }

    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }

}
