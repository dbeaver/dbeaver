package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.meta.AbstractColumn;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumn;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumnType;
import org.jkiss.dbeaver.model.anno.Property;

/**
 * GenericTable
 */
public class GenericProcedureColumn extends AbstractColumn implements DBSProcedureColumn
{
    private GenericProcedure procedure;
    private DBSProcedureColumnType columnType;

    public GenericProcedureColumn(
        GenericProcedure procedure,
        String columnName,
        DBSDataType dataType,
        int valueType,
        int ordinalPosition,
        int columnSize,
        int scale,
        int precision,
        int radix,
        boolean nullable,
        String remarks,
        DBSProcedureColumnType columnType)
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
        this.procedure = procedure;
        this.columnType = columnType;
    }

    public DBSObject getParentObject()
    {
        return getProcedure();
    }

    public GenericDataSource getDataSource()
    {
        return procedure.getDataSource();
    }

    public GenericProcedure getProcedure()
    {
        return procedure;
    }

    @Property(name = "Column Type", viewable = true, order = 10)
    public DBSProcedureColumnType getColumnType()
    {
        return columnType;
    }

    public boolean refreshObject()
        throws DBException
    {
        return false;
    }

}
