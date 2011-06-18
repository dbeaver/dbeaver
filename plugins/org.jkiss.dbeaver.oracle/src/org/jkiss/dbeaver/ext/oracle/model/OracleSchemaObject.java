/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityQualified;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

/**
 * Abstract oracle schema object
 */
public abstract class OracleSchemaObject implements DBSObject, DBSEntityQualified
{
    static final Log log = LogFactory.getLog(OracleSchemaObject.class);

    private final OracleSchema schema;
    private String name;
    private boolean persisted;

    protected OracleSchemaObject(
        OracleSchema schema,
        String name,
        boolean persisted)
    {
        this.schema = schema;
        this.name = name;

        this.persisted = persisted;
    }

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            schema,
            this);
    }

    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        return false;
    }

    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return schema;
    }

    public OracleDataSource getDataSource()
    {
        return schema.getDataSource();
    }

    public OracleSchema getSchema()
    {
        return schema;
    }

    @Property(name = "Name", viewable = true, editable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public boolean isPersisted()
    {
        return persisted;
    }
}
