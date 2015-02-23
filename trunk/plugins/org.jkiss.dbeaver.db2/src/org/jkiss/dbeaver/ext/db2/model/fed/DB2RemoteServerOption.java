/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.model.fed;

import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.DB2Object;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * DB2 Federated Remote Server Option
 * 
 * @author Denis Forveille
 */
public class DB2RemoteServerOption extends DB2Object<DB2RemoteServer> {

    private final DB2RemoteServer remoteServer;

    private Timestamp createTime;
    private String setting;
    private String serverOptionKey;
    private String remarks;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2RemoteServerOption(DB2RemoteServer remoteServer, ResultSet dbResult)
    {
        super(remoteServer, JDBCUtils.safeGetString(dbResult, "OPTION"), true);

        this.remoteServer = remoteServer;

        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.setting = JDBCUtils.safeGetString(dbResult, "SETTING");
        this.serverOptionKey = JDBCUtils.safeGetString(dbResult, "SERVEROPTIONKEY");
        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");
    }

    public DB2RemoteServer getRemoteServer()
    {
        return remoteServer;
    }

    // -----------------
    // Properties
    // -----------------

    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, editable = false, order = 2)
    public String getSetting()
    {
        return setting;
    }

    @Property(viewable = true, editable = false, order = 3)
    public String getServerOptionKey()
    {
        return serverOptionKey;
    }

    @Property(viewable = true, editable = false, order = 4, category = DB2Constants.CAT_DATETIME)
    public Timestamp getCreateTime()
    {
        return createTime;
    }

    @Property(viewable = true, editable = false, order = 5)
    public String getRemarks()
    {
        return remarks;
    }

}
