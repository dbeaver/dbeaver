/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.db2.model.fed;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.DB2Object;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;

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

    @NotNull
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

    @Property(viewable = true, editable = false, length = PropertyLength.MULTILINE, order = 5)
    public String getRemarks()
    {
        return remarks;
    }

}
