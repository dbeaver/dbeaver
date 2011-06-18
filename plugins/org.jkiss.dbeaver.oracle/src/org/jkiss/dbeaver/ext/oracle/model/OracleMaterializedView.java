/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;

/**
 * Oracle materialized view
 */
public class OracleMaterializedView extends OracleTablePhysical
{

    public OracleMaterializedView(OracleSchema schema)
    {
        super(schema);
    }

    public OracleMaterializedView(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult);
    }

    public boolean isView()
    {
        return false;
    }

    public String getDDL(DBRProgressMonitor monitor)
        throws DBException
    {
        return "";
    }

}
