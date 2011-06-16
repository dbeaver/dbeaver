/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import java.sql.ResultSet;

/**
 * GenericProcedure
 */
public class OraclePackage extends OracleObject implements OracleSourceObject
{

    public OraclePackage(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult);
    }

    public OracleSchema getSourceOwner()
    {
        return getSchema();
    }

    public OracleSourceType getSourceType()
    {
        return OracleSourceType.PACKAGE;
    }
}
