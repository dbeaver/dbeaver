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

    @Property(viewable = true, order = 1)
    public String getPid()
    {
        return pid;
    }

    @Property(viewable = true, order = 2)
    public String getUser()
    {
        return user;
    }

    @Property(viewable = true, order = 3)
    public String getHost()
    {
        return host;
    }

    @Property(viewable = true, order = 4)
    public String getDb()
    {
        return db;
    }

    @Property(viewable = true, order = 5)
    public String getCommand()
    {
        return command;
    }

    @Property(viewable = true, order = 6)
    public String getTime()
    {
        return time;
    }

    @Property(viewable = true, order = 7)
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
