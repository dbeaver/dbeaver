/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQLCharset
 */
public class MySQLCharset extends MySQLInformation {

    private String name;
    private String description;
    private int maxLength;
    private List<MySQLCollation> collations = new ArrayList<MySQLCollation>();

    public MySQLCharset(MySQLDataSource dataSource, ResultSet dbResult)
        throws SQLException
    {
        super(dataSource);
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.name = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_CHARSET);
        this.description = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DESCRIPTION);
        this.maxLength = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_MAX_LENGTH);
    }

    void addCollation(MySQLCollation collation)
    {
        collations.add(collation);
    }

    @Override
    @Property(name = "Charset", viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public List<MySQLCollation> getCollations()
    {
        return collations;
    }

    @Property(name = "Default Collation", viewable = true, order = 2)
    public MySQLCollation getDefaultCollation()
    {
        for (MySQLCollation collation : collations) {
            if (collation.isDefault()) {
                return collation;
            }
        }
        return null;
    }

    @Property(name = "Max length", viewable = true, order = 3)
    public int getMaxLength()
    {
        return maxLength;
    }

    @Override
    @Property(name = "Description", viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

}
