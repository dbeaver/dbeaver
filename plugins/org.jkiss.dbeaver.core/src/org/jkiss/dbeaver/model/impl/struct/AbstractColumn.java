/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSColumnDefinition;

/**
 * AbstractColumn
 */
public abstract class AbstractColumn implements DBSColumnDefinition
{
    protected String name;
    protected int valueType;
    protected long maxLength;
    protected boolean nullable;
    protected int scale;
    protected int precision;
    protected int radix;
    protected String description;
    protected String typeName;
    protected int ordinalPosition;

    protected AbstractColumn()
    {
    }

    protected AbstractColumn(
        String name,
        String typeName,
        int valueType,
        int ordinalPosition,
        long maxLength,
        int scale,
        int radix,
        int precision,
        boolean nullable,
        String description)
    {
        this.name = name;
        this.valueType = valueType;
        this.maxLength = maxLength;
        this.scale = scale;
        this.radix = radix;
        this.precision = precision;
        this.nullable = nullable;
        this.description = description;
        this.typeName = typeName;
        this.ordinalPosition = ordinalPosition;
    }

    @Property(name = "Column Name", viewable = true, order = 10)
    public String getName()
    {
        return name;
    }

    protected void setName(String columnName)
    {
        this.name = columnName;
    }

    @Property(name = "Data Type", viewable = true, order = 20)
    public String getTypeName()
    {
        return typeName;
    }

    protected void setTypeName(String typeName)
    {
        this.typeName = typeName;
    }

    @Property(name = "Ordinal Position", viewable = true, order = 30)
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    protected void setOrdinalPosition(int ordinalPosition)
    {
        this.ordinalPosition = ordinalPosition;
    }

    public int getValueType()
    {
        return valueType;
    }

    public void setValueType(int valueType)
    {
        this.valueType = valueType;
    }

    @Property(name = "Length", viewable = true, order = 40)
    public long getMaxLength()
    {
        return maxLength;
    }

    public void setMaxLength(long maxLength)
    {
        this.maxLength = maxLength;
    }

    @Property(name = "Nullable", viewable = true, order = 50)
    public boolean isNullable()
    {
        return nullable;
    }

    public void setNullable(boolean nullable)
    {
        this.nullable = nullable;
    }

    @Property(name = "Scale", viewable = false, order = 60)
    public int getScale()
    {
        return scale;
    }

    public void setScale(int scale)
    {
        this.scale = scale;
    }

    @Property(name = "Precision", viewable = false, order = 70)
    public int getPrecision()
    {
        return precision;
    }

    public void setPrecision(int precision)
    {
        this.precision = precision;
    }

    //@Property(name = "Radix", viewable = true, order = 8)
    public int getRadix()
    {
        return radix;
    }

    public void setRadix(int radix)
    {
        this.radix = radix;
    }

//    @Property(name = "Description", viewable = true, order = 1000)
    public String getDescription()
    {
        return description;
    }

    public boolean isPersisted()
    {
        return true;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

}
