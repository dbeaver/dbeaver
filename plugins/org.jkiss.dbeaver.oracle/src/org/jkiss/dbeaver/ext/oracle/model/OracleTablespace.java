/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

/**
 * Oracle tablespace
 */
public class OracleTablespace extends OracleGlobalObject {

    private String name;

    protected OracleTablespace(OracleDataSource dataSource, ResultSet dbResult)
    {
        super(dataSource, dbResult != null);
        this.name = JDBCUtils.safeGetString(dbResult, "TABLESPACE_NAME");
    }

    @Property(name = "Name", viewable = true, editable = true, order = 1)
    public String getName()
    {
        return name;
    }
}
