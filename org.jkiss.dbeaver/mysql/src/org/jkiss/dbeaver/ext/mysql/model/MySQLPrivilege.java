/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

/**
 * MySQLPrivilege
 */
public class MySQLPrivilege implements DBAPrivilege
{
    static final Log log = LogFactory.getLog(MySQLPrivilege.class);

    private MySQLDataSource dataSource;
    private String name;
    private String context;
    private String comment;
    
    public MySQLPrivilege(MySQLDataSource dataSource, ResultSet resultSet) {
        this.dataSource = dataSource;
        this.name = JDBCUtils.safeGetString(resultSet, "privilege");
        this.context = JDBCUtils.safeGetString(resultSet, "context");
        this.comment = JDBCUtils.safeGetString(resultSet, "comment");
    }

    @Property(name = "Privilege", viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Property(name = "Context", viewable = true, order = 2)
    public String getContext()
    {
        return context;
    }

    public String getObjectId() {
        return getName();
    }

    @Property(name = "Description", viewable = true, order = 100)
    public String getDescription() {
        return comment;
    }

    public DBSObject getParentObject() {
        return dataSource;
    }

    public JDBCDataSource getDataSource() {
        return dataSource;
    }

}