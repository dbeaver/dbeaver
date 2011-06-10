/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumn;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumnType;

/**
 * OracleProcedureColumn
 */
public class OracleProcedureColumn extends JDBCColumn implements DBSProcedureColumn
{
    private OracleProcedure procedure;
    private DBSProcedureColumnType columnType;

    public OracleProcedureColumn(
        OracleProcedure procedure,
        String columnName,
        String typeName,
        int valueType,
        int ordinalPosition,
        long columnSize,
        int scale,
        int precision,
        int radix,
        boolean notNull,
        DBSProcedureColumnType columnType)
    {
        super(columnName,
            typeName,
            valueType,
            ordinalPosition,
            columnSize,
            scale,
            radix,
            precision,
            notNull);
        this.procedure = procedure;
        this.columnType = columnType;
    }

    public DBSObject getParentObject()
    {
        return getProcedure();
    }

    public OracleDataSource getDataSource()
    {
        return procedure.getDataSource();
    }

    public OracleProcedure getProcedure()
    {
        return procedure;
    }

    @Property(name = "Column Type", viewable = true, order = 10)
    public DBSProcedureColumnType getColumnType()
    {
        return columnType;
    }

}
