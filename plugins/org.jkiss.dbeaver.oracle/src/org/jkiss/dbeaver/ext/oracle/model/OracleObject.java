/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityQualified;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

/**
 * Abstract object
 */
public abstract class OracleObject implements DBSObject, DBSEntityQualified
{
    //static final Log log = LogFactory.getLog(OracleProcedure.class);

    private final OracleSchema schema;
    private String name;
    private long id;
    private boolean persisted;

    protected OracleObject(
        OracleSchema schema,
        ResultSet dbResult)
    {
        this.schema = schema;
        this.name = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
        this.id = JDBCUtils.safeGetLong(dbResult, "OBJECT_ID");

        this.persisted = true;
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

    @Property(name = "ID", order = 2)
    public long getId()
    {
        return id;
    }

    public boolean isPersisted()
    {
        return persisted;
    }
}
