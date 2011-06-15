/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumn;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumnType;

/**
 * MySQLProcedureColumn
 */
public class MySQLProcedureColumn extends JDBCColumn implements DBSProcedureColumn
{
    private MySQLProcedure procedure;
    private DBSProcedureColumnType columnType;

    public MySQLProcedureColumn(
        MySQLProcedure procedure,
        String columnName,
        String typeName,
        int valueType,
        int ordinalPosition,
        long columnSize,
        int scale,
        int precision,
        boolean notNull,
        DBSProcedureColumnType columnType)
    {
        super(columnName,
            typeName,
            valueType,
            ordinalPosition,
            columnSize,
            scale,
            precision,
            notNull);
        this.procedure = procedure;
        this.columnType = columnType;
    }

    public DBSObject getParentObject()
    {
        return getProcedure();
    }

    public MySQLDataSource getDataSource()
    {
        return procedure.getDataSource();
    }

    public MySQLProcedure getProcedure()
    {
        return procedure;
    }

    @Property(name = "Column Type", viewable = true, order = 10)
    public DBSProcedureColumnType getColumnType()
    {
        return columnType;
    }

}
