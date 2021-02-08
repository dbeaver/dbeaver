/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * PostgreForeignServer
 */
public class PostgreForeignServer extends PostgreInformation implements PostgreScriptObject {

    private long oid;
    private String name;
    private String type;
    private String version;
    private String[] options;
    private long ownerId;
    private long dataWrapperId;
    private UserMappingCache userMappingCache = new UserMappingCache();

    public PostgreForeignServer(PostgreDatabase database) {
        super(database);
    }

    public PostgreForeignServer(PostgreDatabase database, ResultSet dbResult)
        throws SQLException
    {
        super(database);
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "srvname");
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "srvowner");
        this.dataWrapperId = JDBCUtils.safeGetLong(dbResult, "srvfdw");
        this.type = JDBCUtils.safeGetString(dbResult, "srvtype");
        this.version = JDBCUtils.safeGetString(dbResult, "srvversion");
        this.options = JDBCUtils.safeGetArray(dbResult, "srvoptions");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 3)
    public String getType() {
        return type;
    }

    @Property(viewable = true, order = 4)
    public String getVersion() {
        return version;
    }

    @Property(viewable = true, order = 5)
    public String[] getOptions() {
        return options;
    }

    @Override
    public long getObjectId() {
        return oid;
    }

    @Association
    public Collection<PostgreUserMapping> getUserMappings(DBRProgressMonitor monitor) throws DBException {
        return userMappingCache.getAllObjects(monitor, this);
    }

    public PostgreUserMapping getUserMapping(DBRProgressMonitor monitor, long oid) throws DBException {
        return PostgreUtils.getObjectById(monitor, userMappingCache, this, oid);
    }


    static class UserMappingCache extends JDBCObjectCache<PostgreForeignServer, PostgreUserMapping> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreForeignServer owner)
            throws SQLException
        {
            return session.prepareStatement(
                "select distinct " +
                "\nsrvname, " +
                "\ncase when rolname is null then 'public' else rolname end rolname, " +
                "\nsrvoptions,  " +
                "\numoptions  " +
                "\nfrom pg_user_mapping um  " +
                "\njoin pg_foreign_server fs on um.umserver = fs.OID  " +
                "\nleft join pg_authid pa on um.umuser = pa.OID " +
                "\nwhere fs.OID = " + owner.getObjectId() +
                "\nORDER BY srvname"
            );
        }

        @Override
        protected PostgreUserMapping fetchObject(@NotNull JDBCSession session, @NotNull PostgreForeignServer owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreUserMapping(owner, dbResult);
        }
    }

    @Property(viewable = false, order = 8)
    public PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return getDatabase().getRoleById(monitor, ownerId);
    }

    @Property(viewable = true, order = 10)
    public PostgreForeignDataWrapper getForeignDataWrapper(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, getDatabase().foreignDataWrapperCache, getDatabase(), dataWrapperId);
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return
            "-- Foreign server: " + getName() + "\n\n" +
                "-- DROP SERVER " + getName() + ";\n\n" +
                "CREATE SERVER " + getName() + "\n\t" +
                "FOREIGN DATA WRAPPER " + getForeignDataWrapper(monitor).getName() + "\n\t" +
                "OPTIONS " + PostgreUtils.getOptionsString(this.options);
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {

    }

}
