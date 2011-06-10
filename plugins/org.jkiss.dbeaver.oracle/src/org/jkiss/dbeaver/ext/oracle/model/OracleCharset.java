/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * OracleCharset
 */
public class OracleCharset extends OracleInformation {

    private String name;
    private String description;
    private int maxLength;
    private List<OracleCollation> collations = new ArrayList<OracleCollation>();

    public OracleCharset(OracleDataSource dataSource, ResultSet dbResult)
        throws SQLException
    {
        super(dataSource);
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.name = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_CHARSET);
        this.description = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_DESCRIPTION);
        this.maxLength = JDBCUtils.safeGetInt(dbResult, OracleConstants.COL_MAX_LENGTH);
    }

    void addCollation(OracleCollation collation)
    {
        collations.add(collation);
    }

    @Property(name = "Charset", viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public List<OracleCollation> getCollations()
    {
        return collations;
    }

    @Property(name = "Default Collation", viewable = true, order = 2)
    public OracleCollation getDefaultCollation()
    {
        for (OracleCollation collation : collations) {
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

    @Property(name = "Description", viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

}
