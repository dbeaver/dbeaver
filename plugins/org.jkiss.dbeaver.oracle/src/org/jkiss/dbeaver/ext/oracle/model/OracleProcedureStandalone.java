/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSProcedureType;

import java.sql.ResultSet;

/**
 * GenericProcedure
 */
public class OracleProcedureStandalone extends OracleProcedureBase
{

    private boolean valid;

    public OracleProcedureStandalone(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(
            schema,
            JDBCUtils.safeGetString(dbResult, "OBJECT_NAME"),
            DBSProcedureType.valueOf(JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE")));
        this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));
    }

    @Property(name = "Valid", viewable = true, order = 3)
    public boolean isValid()
    {
        return valid;
    }

    public DBSEntityContainer getContainer()
    {
        return getSchema();
    }

    public OracleSourceType getSourceType()
    {
        return getProcedureType() == DBSProcedureType.PROCEDURE ?
            OracleSourceType.PROCEDURE :
            OracleSourceType.FUNCTION;
    }

    @Override
    public int getOverloadNumber()
    {
        return 0;
    }
}
