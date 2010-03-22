package org.jkiss.dbeaver.model.impl.meta;

import org.jkiss.dbeaver.model.struct.DBSColumnDefinition;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * AbstractColumn
 */
public abstract class AbstractColumn<DATASOURCE extends DBPDataSource> implements DBSColumnDefinition<DATASOURCE>
{
    private String name;
    private int valueType;
    private int maxLength;
    private boolean nullable;
    private int scale;
    private int precision;
    private int radix;
    private String description;
    protected DBSDataType dataType;
    protected int ordinalPosition;

    protected AbstractColumn()
    {
    }

    protected AbstractColumn(
        String name,
        DBSDataType dataType,
        int valueType,
        int ordinalPosition,
        int maxLength,
        int scale,
        int radix,
        int precision,
        boolean nullable,
        String description
    )
    {
        this.name = name;
        this.valueType = valueType;
        this.maxLength = maxLength;
        this.scale = scale;
        this.radix = radix;
        this.precision = precision;
        this.nullable = nullable;
        this.description = description;
        this.dataType = dataType;
        this.ordinalPosition = ordinalPosition;
    }

    @Property(name = "Column Name", order = 1)
    public String getName()
    {
        return name;
    }

    protected void setName(String columnName)
    {
        this.name = columnName;
    }

    @Property(name = "Data Type", viewable = true, order = 2)
    public DBSDataType getDataType()
    {
        return dataType;
    }

    protected void setDataType(DBSDataType dataType)
    {
        this.dataType = dataType;
    }

    @Property(name = "Ordinal Position", viewable = true, order = 5)
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

    @Property(name = "Length", viewable = true, order = 3)
    public int getMaxLength()
    {
        return maxLength;
    }

    public void setMaxLength(int maxLength)
    {
        this.maxLength = maxLength;
    }

    @Property(name = "Nullable", viewable = true, order = 4)
    public boolean isNullable()
    {
        return nullable;
    }

    public void setNullable(boolean nullable)
    {
        this.nullable = nullable;
    }

    @Property(name = "Scale", viewable = true, order = 6)
    public int getScale()
    {
        return scale;
    }

    public void setScale(int scale)
    {
        this.scale = scale;
    }

    @Property(name = "Precision", viewable = true, order = 7)
    public int getPrecision()
    {
        return precision;
    }

    public void setPrecision(int precision)
    {
        this.precision = precision;
    }

    @Property(name = "Radix", viewable = true, order = 8)
    public int getRadix()
    {
        return radix;
    }

    public void setRadix(int radix)
    {
        this.radix = radix;
    }

    @Property(name = "Description", viewable = true, order = 9)
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

}
