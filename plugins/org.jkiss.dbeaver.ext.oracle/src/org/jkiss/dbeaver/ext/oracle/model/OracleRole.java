/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * OracleRole
 */
public class OracleRole extends OracleGrantee implements DBARole
{
    private static final Log log = Log.getLog(OracleRole.class);

    private String name;
    private String authentication;
    private final UserCache userCache = new UserCache();

    public OracleRole(OracleDataSource dataSource, ResultSet resultSet) {
        super(dataSource);
        this.name = JDBCUtils.safeGetString(resultSet, "ROLE");
        this.authentication = JDBCUtils.safeGetStringTrimmed(resultSet, "PASSWORD_REQUIRED");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 3)
    public String getAuthentication()
    {
        return authentication;
    }

    @Association
    public Collection<OraclePrivUser> getUserPrivs(DBRProgressMonitor monitor) throws DBException
    {
        return userCache.getAllObjects(monitor, this);
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        userCache.clearCache();
        return super.refreshObject(monitor);
    }

    static class UserCache extends JDBCObjectCache<OracleRole, OraclePrivUser> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleRole owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM DBA_ROLE_PRIVS WHERE GRANTED_ROLE=? ORDER BY GRANTEE");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OraclePrivUser fetchObject(@NotNull JDBCSession session, @NotNull OracleRole owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new OraclePrivUser(owner, resultSet);
        }
    }

}
