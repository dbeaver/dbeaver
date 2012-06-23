/*
 *
 *  * Copyright (C) 2010-2012 Serge Rieder
 *  * serge@jkiss.org
 *  *
 *  * This library is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU Lesser General Public
 *  * License as published by the Free Software Foundation; either
 *  * version 2.1 of the License, or (at your option) any later version.
 *  *
 *  * This library is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this library; if not, write to the Free Software
 *  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jkiss.dbeaver.ext.mysql.model.session;

import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

/**
 * MySQL session
 */
public class MySQLSession implements DBAServerSession {
    private String pid;
    private String user;
    private String host;
    private String db;
    private String command;
    private String time;
    private String state;
    private String info;

    public MySQLSession(ResultSet dbResult) {
        this.pid = JDBCUtils.safeGetString(dbResult, "id");
        this.user = JDBCUtils.safeGetString(dbResult, "user");
        this.host = JDBCUtils.safeGetString(dbResult, "host");
        this.db = JDBCUtils.safeGetString(dbResult, "db");
        this.command = JDBCUtils.safeGetString(dbResult, "command");
        this.time = JDBCUtils.safeGetString(dbResult, "time");
        this.state = JDBCUtils.safeGetString(dbResult, "state");
        this.info = JDBCUtils.safeGetString(dbResult, "info");
    }

    @Property(name = "PID", viewable = true, order = 1, description = "Process ID")
    public String getPid()
    {
        return pid;
    }

    @Property(name = "User", viewable = true, order = 2, description = "Database user")
    public String getUser()
    {
        return user;
    }

    @Property(name = "Host", viewable = true, order = 3, description = "Remote host")
    public String getHost()
    {
        return host;
    }

    @Property(name = "Database", viewable = true, order = 4, description = "Database")
    public String getDb()
    {
        return db;
    }

    @Property(name = "Command", viewable = true, order = 5, description = "Current command")
    public String getCommand()
    {
        return command;
    }

    @Property(name = "Time", viewable = true, order = 6, description = "Command start time")
    public String getTime()
    {
        return time;
    }

    @Property(name = "State", viewable = true, order = 7, description = "State")
    public String getState()
    {
        return state;
    }

    @Override
    public String getActiveQuery()
    {
        return info;
    }

    @Override
    public String toString()
    {
        return pid + "@" + db;
    }
}
