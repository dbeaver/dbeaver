/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumnKeyType;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

import java.util.List;

/**
 * GenericTable
 */
public class GenericTableColumn extends JDBCTableColumn<GenericTable> implements DBSTableColumn, JDBCColumnKeyType
{
    private String defaultValue;
    private int sourceType;
    private long charLength;
    private boolean autoIncrement;

    public GenericTableColumn(GenericTable table)
    {
        super(table, false);
    }

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
        super(table,
            true,
            columnName,
            typeName,
            valueType,
            ordinalPosition,
            columnSize,
            scale,
            radix,
            precision,
            nullable,
            remarks);
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
        return getTable().getDataSource();
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

    @Property(name = "Auto Increment", viewable = true, order = 51)
    public boolean isAutoIncrement()
    {
        return autoIncrement;
    }

    public JDBCColumnKeyType getKeyType()
    {
        return this;
    }

    @Property(name = "Key", viewable = true, order = 80)
    public boolean isInUniqueKey()
    {
        final List<GenericPrimaryKey> uniqueKeysCache = getTable().getUniqueKeysCache();
        if (!CommonUtils.isEmpty(uniqueKeysCache)) {
            for (GenericPrimaryKey key : uniqueKeysCache) {
                if (key.hasColumn(this)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isInReferenceKey()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return getTable().getFullQualifiedName() + "." + getName();
    }

}
