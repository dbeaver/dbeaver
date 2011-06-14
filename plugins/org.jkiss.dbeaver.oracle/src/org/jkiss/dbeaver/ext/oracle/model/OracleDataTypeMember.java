/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.OracleUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

/**
 * Oracle data type member
 */
public abstract class OracleDataTypeMember implements DBSObject {

    static final Log log = LogFactory.getLog(OracleDataTypeMember.class);

    private OracleDataType dataType;
    protected String name;
    protected int number;
    private boolean inherited;
    private boolean persisted;

    protected OracleDataTypeMember(OracleDataType dataType)
    {
        this.dataType = dataType;
        this.persisted = false;
    }

    protected OracleDataTypeMember(OracleDataType dataType, ResultSet dbResult)
    {
        this.dataType = dataType;
        this.inherited = JDBCUtils.safeGetBoolean(dbResult, "INHERITED", OracleConstants.YES);
        this.persisted = true;
    }

    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return dataType;
    }

    public OracleDataSource getDataSource()
    {
        return dataType.getDataSource();
    }

    public boolean isPersisted()
    {
        return persisted;
    }

    @Property(name = "Name", viewable = true, editable = true, valueTransformer = JDBCObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(name = "Number", viewable = true, order = 2)
    public int getNumber()
    {
        return number;
    }

    @Property(name = "Inherited", viewable = true, order = 20)
    public boolean isInherited()
    {
        return inherited;
    }
}
