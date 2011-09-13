/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.DBSObjectUnique;
import org.jkiss.dbeaver.model.struct.DBSProcedureType;

import java.sql.ResultSet;

/**
 * GenericProcedure
 */
public class OracleProcedurePackaged extends OracleProcedureBase<OraclePackage> implements DBSObjectUnique
{
    private Integer overload;

    public OracleProcedurePackaged(
        OraclePackage ownerPackage,
        ResultSet dbResult)
    {
        super(ownerPackage,
            JDBCUtils.safeGetString(dbResult, "PROCEDURE_NAME"),
            0l,
            DBSProcedureType.valueOf(JDBCUtils.safeGetString(dbResult, "PROCEDURE_TYPE")));
    }

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getSchema(),
            getParentObject(),
            this);
    }

    public OracleSchema getSchema()
    {
        return getParentObject().getSchema();
    }

    @Override
    public Integer getOverloadNumber()
    {
        return overload;
    }

    public void setOverload(int overload)
    {
        this.overload = overload;
    }

    public String getUniqueName()
    {
        return overload == null || overload <= 1 ? getName() : getName() + "#" + overload;
    }

}
