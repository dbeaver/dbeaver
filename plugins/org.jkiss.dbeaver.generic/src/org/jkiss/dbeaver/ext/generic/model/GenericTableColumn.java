/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

/**
 * GenericTable
 */
public class GenericTableColumn extends JDBCColumn implements DBSTableColumn
{
    private GenericTable table;
    private String defaultValue;
    private int sourceType;
    private long charLength;
    private boolean autoIncrement;

    public GenericTableColumn(
        GenericTable table,
        String columnName,
        String typeName,
        int valueType,
        int sourceType,
        int ordinalPosition,
        long columnSize,
        long charLength,
        int scale,
        int precision,
        int radix,
        boolean nullable,
        String remarks,
        String defaultValue,
        boolean autoIncrement)
    {
        super(columnName,
            typeName,
            valueType,
            ordinalPosition,
            columnSize,
            scale,
            radix,
            precision,
            nullable,
            remarks);
        this.table = table;
        this.sourceType = sourceType;
        this.defaultValue = defaultValue;
        this.charLength = charLength;
        this.autoIncrement = autoIncrement;
    }

    public DBSObject getParentObject()
    {
        return getTable();
    }

    public GenericDataSource getDataSource()
    {
        return table.getDataSource();
    }

    public GenericTable getTable()
    {
        return table;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public int getSourceType()
    {
        return sourceType;
    }

    public long getCharLength()
    {
        return charLength;
    }

    @Property(name = "Auto Increment", viewable = true, order = 10)
    public boolean isAutoIncrement()
    {
        return autoIncrement;
    }

    @Override
    public String toString()
    {
        return table.getFullQualifiedName() + "." + getName();
    }
}
