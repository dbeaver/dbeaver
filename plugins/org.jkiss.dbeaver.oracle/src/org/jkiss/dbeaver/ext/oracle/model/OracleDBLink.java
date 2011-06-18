/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.util.Date;

/**
 * DB Link
 */
public class OracleDBLink extends OracleSchemaObject {

    private String userName;
    private String host;
    private Date created;

    protected OracleDBLink(DBRProgressMonitor progressMonitor, OracleSchema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "DB_LINK"), true);
        this.userName = JDBCUtils.safeGetString(dbResult, "USERNAME");
        this.host = JDBCUtils.safeGetString(dbResult, "HOST");
        this.created = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");

    }

    @Property(name = "User Name", viewable = true, editable = true, order = 2)
    public String getUserName()
    {
        return userName;
    }

    @Property(name = "Host", viewable = true, editable = true, order = 3)
    public String getHost()
    {
        return host;
    }

    @Property(name = "Created", viewable = true, order = 4)
    public Date getCreated()
    {
        return created;
    }
}
