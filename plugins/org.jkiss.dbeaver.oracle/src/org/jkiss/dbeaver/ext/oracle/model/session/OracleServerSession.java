/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.oracle.model.session;

import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.sql.Timestamp;

/**
* Session
*/
public class OracleServerSession implements DBAServerSession {
    private String sid;
    private String serial;
    private String user;
    private String schema;
    private String status;
    private String state;
    private String sql;
    private String event;
    private Timestamp logonTime;
    private String serviceName;

    private String remoteHost;
    private String remoteUser;
    private String remoteProgram;

    public OracleServerSession(ResultSet dbResult)
    {
        this.sid = JDBCUtils.safeGetString(dbResult, "SID");
        this.serial = JDBCUtils.safeGetString(dbResult, "SERIAL#");
        this.user = JDBCUtils.safeGetString(dbResult, "USERNAME");
        this.schema = JDBCUtils.safeGetString(dbResult, "SCHEMANAME");
        this.status = JDBCUtils.safeGetString(dbResult, "STATUS");
        this.state = JDBCUtils.safeGetString(dbResult, "STATE");
        this.sql = JDBCUtils.safeGetString(dbResult, "SQL_TEXT");
        this.event = JDBCUtils.safeGetString(dbResult, "EVENT");
        this.logonTime = JDBCUtils.safeGetTimestamp(dbResult, "LOGON_TIME");
        this.serviceName = JDBCUtils.safeGetString(dbResult, "SERVICE_NAME");

        this.remoteHost = JDBCUtils.safeGetString(dbResult, "MACHINE");
        this.remoteUser = JDBCUtils.safeGetString(dbResult, "OSUSER");
        this.remoteProgram = JDBCUtils.safeGetString(dbResult, "PROGRAM");
    }

    @Property(category = "Session", viewable = true, order = 1)
    public String getSid()
    {
        return sid;
    }

    public String getSerial()
    {
        return serial;
    }

    @Property(category = "Session", viewable = true, order = 2)
    public String getUser()
    {
        return user;
    }

    @Property(category = "Session", viewable = true, order = 3)
    public String getSchema()
    {
        return schema;
    }

    @Property(category = "Session", viewable = true, order = 4)
    public String getStatus()
    {
        return status;
    }

    @Property(category = "Session", viewable = true, order = 5)
    public String getState()
    {
        return state;
    }

    @Property(category = "Session", viewable = true, order = 6)
    public String getEvent()
    {
        return event;
    }

    @Property(category = "Session", order = 7)
    public Timestamp getLogonTime()
    {
        return logonTime;
    }

    @Property(category = "Session", order = 8)
    public String getServiceName()
    {
        return serviceName;
    }

    @Property(category = "SQL", order = 50)
    public String getSql()
    {
        return sql;
    }

    @Property(category = "Process", viewable = true, order = 30)
    public String getRemoteHost()
    {
        return remoteHost;
    }

    @Property(category = "Process", viewable = true, order = 31)
    public String getRemoteUser()
    {
        return remoteUser;
    }
    @Property(category = "Process", viewable = true, order = 32)
    public String getRemoteProgram()
    {
        return remoteProgram;
    }

    @Override
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
