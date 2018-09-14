/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    public static final String CAT_SESSION = "Session";
    public static final String CAT_SQL = "SQL";
    public static final String CAT_PROCESS = "Process";

    private String sid;
    private String serial;
    private String user;
    private String schema;
    private String type;
    private String status;
    private String state;
    private String sql;
    private String event;
    private String elapsedTime;
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
        this.type = JDBCUtils.safeGetString(dbResult, "TYPE");
        this.status = JDBCUtils.safeGetString(dbResult, "STATUS");
        this.state = JDBCUtils.safeGetString(dbResult, "STATE");
        this.sql = JDBCUtils.safeGetString(dbResult, "SQL_FULLTEXT");
        this.event = JDBCUtils.safeGetString(dbResult, "EVENT");
        this.elapsedTime = JDBCUtils.safeGetString(dbResult, "LAST_CALL_ET");
        this.logonTime = JDBCUtils.safeGetTimestamp(dbResult, "LOGON_TIME");
        this.serviceName = JDBCUtils.safeGetString(dbResult, "SERVICE_NAME");

        this.remoteHost = JDBCUtils.safeGetString(dbResult, "MACHINE");
        this.remoteUser = JDBCUtils.safeGetString(dbResult, "OSUSER");
        this.remoteProgram = JDBCUtils.safeGetString(dbResult, "PROGRAM");
    }

    @Property(category = CAT_SESSION, viewable = true, order = 1)
    public String getSid()
    {
        return sid;
    }

    public String getSerial()
    {
        return serial;
    }

    @Property(category = CAT_SESSION, viewable = true, order = 2)
    public String getUser()
    {
        return user;
    }

    @Property(category = CAT_SESSION, viewable = true, order = 3)
    public String getSchema()
    {
        return schema;
    }

    @Property(category = CAT_SESSION, viewable = true, order = 4)
    public String getType()
    {
        return type;
    }

    @Property(category = CAT_SESSION, viewable = true, order = 5)
    public String getStatus()
    {
        return status;
    }

    @Property(category = CAT_SESSION, viewable = true, order = 6)
    public String getState()
    {
        return state;
    }

    @Property(category = CAT_SESSION, viewable = true, order = 7)
    public String getEvent()
    {
        return event;
    }

    @Property(category = CAT_SESSION, viewable = true, order = 8)
    public String getElapsedTime()
    {
        return elapsedTime;
    }

    @Property(category = CAT_SESSION, order = 9)
    public Timestamp getLogonTime()
    {
        return logonTime;
    }

    @Property(category = CAT_SESSION, order = 10)
    public String getServiceName()
    {
        return serviceName;
    }

    @Property(category = CAT_SQL, order = 50)
    public String getSql()
    {
        return sql;
    }

    @Property(category = CAT_PROCESS, viewable = true, order = 30)
    public String getRemoteHost()
    {
        return remoteHost;
    }

    @Property(category = CAT_PROCESS, viewable = true, order = 31)
    public String getRemoteUser()
    {
        return remoteUser;
    }
    @Property(category = CAT_PROCESS, viewable = true, order = 32)
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
