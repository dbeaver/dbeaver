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
package org.jkiss.dbeaver.ext.mysql.model.session;

import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

/**
 * MySQL session
 */
public class MySQLSession implements DBAServerSession {
    private long pid;
    private String user;
    private String host;
    private String db;
    private String command;
    private long time;
    private String state;
    private String info;

    public MySQLSession(ResultSet dbResult) {
        this.pid = JDBCUtils.safeGetLong(dbResult, "id");
        this.user = JDBCUtils.safeGetString(dbResult, "user");
        this.host = JDBCUtils.safeGetString(dbResult, "host");
        this.db = JDBCUtils.safeGetString(dbResult, "db");
        this.command = JDBCUtils.safeGetString(dbResult, "command");
        this.time = JDBCUtils.safeGetLong(dbResult, "time");
        this.state = JDBCUtils.safeGetString(dbResult, "state");
        this.info = JDBCUtils.safeGetString(dbResult, "info");
    }

    @Property(viewable = true, order = 1)
    public long getPid()
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
    public long getTime()
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
