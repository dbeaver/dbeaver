/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * OracleCollation
 */
public class OracleCollation extends OracleInformation {

    private OracleCharset charset;
    private int id;
    private String name;
    private boolean isDefault;
    private boolean isCompiled;
    private int sortLength;

    public OracleCollation(OracleCharset charset, ResultSet dbResult)
        throws SQLException
    {
        super(charset.getDataSource());
        this.charset = charset;
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.name = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_COLLATION);
        this.id = JDBCUtils.safeGetInt(dbResult, OracleConstants.COL_ID);
        this.isDefault = "Yes".equalsIgnoreCase(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_DEFAULT));
        this.isCompiled = "Yes".equalsIgnoreCase(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_COMPILED));
        this.sortLength = JDBCUtils.safeGetInt(dbResult, OracleConstants.COL_SORT_LENGTH);
    }

    @Property(name = "Charset", viewable = true, order = 1)
    public OracleCharset getCharset()
    {
        return charset;
    }

    @Property(name = "Collation", viewable = true, order = 2)
    public String getName()
    {
        return name;
    }

    @Property(name = "Id", viewable = true, order = 3)
    public int getId()
    {
        return id;
    }

    @Property(name = "Default", viewable = true, order = 4)
    public boolean isDefault()
    {
        return isDefault;
    }

    @Property(name = "Compiled", viewable = true, order = 5)
    public boolean isCompiled()
    {
        return isCompiled;
    }

    @Property(name = "Sort length", viewable = true, order = 6)
    public int getSortLength()
    {
        return sortLength;
    }

    public String getDescription()
    {
        return null;
    }

    @Override
    public DBSObject getParentObject()
    {
        return charset;
    }
}
