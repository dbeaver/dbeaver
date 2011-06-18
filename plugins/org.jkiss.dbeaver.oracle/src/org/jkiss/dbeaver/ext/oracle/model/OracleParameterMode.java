/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumnType;

/**
* Parameter/argument mode
*/
public enum OracleParameterMode {
    IN(DBSProcedureColumnType.IN),
    OUT(DBSProcedureColumnType.OUT),
    INOUT(DBSProcedureColumnType.INOUT);
    private final DBSProcedureColumnType columnType;

    OracleParameterMode(DBSProcedureColumnType columnType)
    {
        this.columnType = columnType;
    }

    public static OracleParameterMode getMode(String modeName)
    {
        if (CommonUtils.isEmpty(modeName)) {
            return null;
        } else if ("IN".equals(modeName)) {
            return IN;
        } else if ("OUT".equals(modeName)) {
            return OracleParameterMode.OUT;
        } else {
            return OracleParameterMode.INOUT;
        }
    }

    public DBSProcedureColumnType getColumnType()
    {
        return columnType;
    }
}
