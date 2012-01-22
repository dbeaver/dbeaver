/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumnKeyType;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * GenericTable
 */
public class GenericTableColumn extends JDBCTableColumn<GenericTable> implements DBSTableColumn, JDBCColumnKeyType
{
    private int radix;
    private String remarks;
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
        boolean notNull,
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
            precision,
            notNull);
        this.sourceType = sourceType;
        this.defaultValue = defaultValue;
        this.charLength = charLength;
        this.autoIncrement = autoIncrement;
        this.remarks = remarks;
        this.radix = radix;
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
    public boolean isSequence()
    {
        return autoIncrement;
    }

    public JDBCColumnKeyType getKeyType()
    {
        return this;
    }

    @Property(name = "Radix", viewable = false, order = 62)
    public int getRadix()
    {
        return radix;
    }

    public void setRadix(int radix)
    {
        this.radix = radix;
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

    public String getDescription()
    {
        return remarks;
    }

    @Override
    public String toString()
    {
        return getTable().getFullQualifiedName() + "." + getName();
    }

}
