/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import java.sql.ResultSet;

/**
 * Oracle tablespace
 */
public class OracleTablespace extends OracleObject {

    protected OracleTablespace(OracleSchema schema, ResultSet dbResult)
    {
        super(schema, dbResult);
    }
}
