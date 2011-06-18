/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

/**
 * Abstract oracle object
 */
public abstract class OracleObject extends OracleSchemaObject
{
    private long id;

    protected OracleObject(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "OBJECT_NAME"), true);
        this.id = JDBCUtils.safeGetLong(dbResult, "OBJECT_ID");
    }

    @Property(name = "ID", order = 2)
    public long getId()
    {
        return id;
    }

}
