/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;

/**
 * JDBCDataType
 */
public class JDBCDataType implements DBSDataType
{
    private int type;
    private String name;
    private String remarks;
    private boolean isUnsigned;
    private boolean isSearchable;
    private int precision;
    private int minScale;
    private int maxScale;

    public JDBCDataType(
        int type,
        String name,
        String remarks,
        boolean unsigned,
        boolean searchable,
        int precision,
        int minScale,
        int maxScale)
    {
        this.type = type;
        this.name = name;
        this.remarks = remarks;
        isUnsigned = unsigned;
        isSearchable = searchable;
        this.precision = precision;
        this.minScale = minScale;
        this.maxScale = maxScale;
    }

    public int getTypeNumber()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return remarks;
    }

    public DBSDataKind getDataKind()
    {
        switch (type) {
            case java.sql.Types.BOOLEAN:
                return DBSDataKind.BOOLEAN;
            case java.sql.Types.CHAR:
            case java.sql.Types.VARCHAR:
            case java.sql.Types.LONGVARCHAR:
                return DBSDataKind.STRING;
            case java.sql.Types.BIGINT:
            case java.sql.Types.BIT:
            case java.sql.Types.DECIMAL:
            case java.sql.Types.DOUBLE:
            case java.sql.Types.FLOAT:
            case java.sql.Types.INTEGER:
            case java.sql.Types.NUMERIC:
            case java.sql.Types.REAL:
            case java.sql.Types.SMALLINT:
            case java.sql.Types.TINYINT:
                return DBSDataKind.NUMERIC;
            case java.sql.Types.DATE:
            case java.sql.Types.TIME:
            case java.sql.Types.TIMESTAMP:
                return DBSDataKind.DATETIME;
            case java.sql.Types.BLOB:
            case java.sql.Types.CLOB:
            case java.sql.Types.VARBINARY:
            case java.sql.Types.LONGVARBINARY:
                return DBSDataKind.LOB;
            case java.sql.Types.STRUCT:
                return DBSDataKind.STRUCT;
            case java.sql.Types.ARRAY:
                return DBSDataKind.ARRAY;
        }
        return null;
    }

    public boolean isUnsigned()
    {
        return isUnsigned;
    }

    public boolean isSearchable()
    {
        return isSearchable;
    }

    public int getPrecision()
    {
        return precision;
    }

    public int getMinScale()
    {
        return minScale;
    }

    public int getMaxScale()
    {
        return maxScale;
    }
    
    public String toString()
    {
        return name;
    }
}
