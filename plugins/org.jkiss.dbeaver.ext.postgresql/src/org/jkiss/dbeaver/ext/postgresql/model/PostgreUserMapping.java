/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * PostgreForeignServer
 */
public class PostgreUserMapping extends PostgreInformation implements PostgreScriptObject {

    private long oid;
    private PostgreForeignServer foreignServer;
    private String name;
    private String[] serverOptions;
    private String[] userMappingOptions;

    public PostgreUserMapping(PostgreForeignServer foreignServer, ResultSet dbResult)
        throws SQLException
        {
            super(foreignServer.getDatabase());
            this.foreignServer = foreignServer;
            this.loadInfo(dbResult);
        }


    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "rolname");
        this.serverOptions = JDBCUtils.safeGetArray(dbResult, "srvoptions");
        this.userMappingOptions = JDBCUtils.safeGetArray(dbResult, "umoptions");
    }

    @Override
    public long getObjectId() {
        return oid;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 3)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 4)
    public String[] getServerOptions() {
        return serverOptions;
    }

    @Property(viewable = true, order = 5)
    public String[] getUserMappingOptions() {
        return userMappingOptions;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return
            "-- User Mapping: " + getName() + "\n\n" +
                "-- DROP USER MAPPING FOR " + getName() + " SERVER " + foreignServer.getName() + ";\n\n" +
                "CREATE USER MAPPING" + "\n\t" + 
                   "FOR " + getName() + "\n\t" +
                   "SERVER " + foreignServer.getName() + "\n\t" +
                   "OPTIONS " + PostgreUtils.getOptionsString(this.userMappingOptions);
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {

    }
}
