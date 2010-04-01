package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.meta.AbstractColumn;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

/**
 * GenericTable
 */
public class GenericTableColumn extends AbstractColumn<GenericDataSource> implements DBSTableColumn<GenericDataSource>
{
    private GenericTable table;
    private String defaultValue;
    private int sourceType;
    private int charLength;

    public GenericTableColumn(
        GenericTable table,
        String columnName,
        DBSDataType dataType, int valueType, int sourceType, int ordinalPosition,
        int columnSize,
        int charLength,
        int scale,
        int precision,
        int radix,
        boolean nullable,
        String remarks,
        String defaultValue
    )
    {
        super(columnName,
            dataType,
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

    public void setOrdinalPosition(int ordinalPosition)
    {
        this.ordinalPosition = ordinalPosition;
    }

    public int getSourceType()
    {
        return sourceType;
    }

    public int getCharLength()
    {
        return charLength;
    }

    public boolean refreshObject()
        throws DBException
    {
        return false;
    }

}
