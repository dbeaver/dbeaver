/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model.session;

import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

/**
* Session
*/
public class OracleServerSession implements DBAServerSession {
    private String sid;
    private String serial;
    private String user;
    private String schema;
    private String state;
    private String sql;
    private String event;
    private String remoteHost;
    private String remoteUser;
    private String remoteProgram;

    public OracleServerSession(ResultSet dbResult)
    {
        this.sid = JDBCUtils.safeGetString(dbResult, "SID");
        this.serial = JDBCUtils.safeGetString(dbResult, "SERIAL#");
        this.user = JDBCUtils.safeGetString(dbResult, "USERNAME");
        this.schema = JDBCUtils.safeGetString(dbResult, "SCHEMANAME");
        this.state = JDBCUtils.safeGetString(dbResult, "STATE");
        this.sql = JDBCUtils.safeGetString(dbResult, "SQL_TEXT");
        this.event = JDBCUtils.safeGetString(dbResult, "EVENT");
        this.remoteHost = JDBCUtils.safeGetString(dbResult, "MACHINE");
        this.remoteUser = JDBCUtils.safeGetString(dbResult, "OSUSER");
        this.remoteProgram = JDBCUtils.safeGetString(dbResult, "PROGRAM");
    }

    @Property(name = "SID", viewable = true, order = 1, description = "Session identifier")
    public String getSid()
    {
        return sid;
    }

    public String getSerial()
    {
        return serial;
    }

    @Property(name = "User", viewable = true, order = 2, description = "Oracle user")
    public String getUser()
    {
        return user;
    }

    @Property(name = "Schema", viewable = true, order = 3, description = "Schema user name")
    public String getSchema()
    {
        return schema;
    }

    @Property(name = "State", viewable = true, order = 4, description = "Wait state")
    public String getState()
    {
        return state;
    }

    @Property(name = "Event", viewable = true, order = 5, description = "Resource or event for which the session is waiting")
    public String getEvent()
    {
        return event;
    }

    @Property(name = "Remote Host", viewable = true, order = 6, description = "Operating system machine name")
    public String getRemoteHost()
    {
        return remoteHost;
    }

    @Property(name = "Remote User", viewable = true, order = 7, description = "Operating system client user name")
    public String getRemoteUser()
    {
        return remoteUser;
    }
    @Property(name = "Remote Program", viewable = true, order = 8, description = "Operating system program name")
    public String getRemoteProgram()
    {
        return remoteProgram;
    }

    public String getActiveQuery()
    {
        return sql;
    }

    @Override
    public String toString()
    {
        return sid + " - " + event;
    }
}
