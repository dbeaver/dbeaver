/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model.security;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

import java.sql.SQLException;

/**
 * Cache for DB2 Roles for a given Grantee
 * 
 * @author Denis Forveille
 */
public class DB2GranteeRoleCache extends JDBCObjectCache<DB2Grantee, DB2RoleAuth> {

    private static final String SQL = "SELECT * FROM SYSCAT.ROLEAUTH WHERE GRANTEETYPE = ? AND GRANTEE = ? ORDER BY ROLENAME WITH UR";

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DB2Grantee db2Grantee) throws SQLException
    {
        final JDBCPreparedStatement dbStat = session.prepareStatement(SQL);
        dbStat.setString(1, db2Grantee.getType().name());
        dbStat.setString(2, db2Grantee.getName());
        return dbStat;
    }

    @Override
    protected DB2RoleAuth fetchObject(@NotNull JDBCSession session, @NotNull DB2Grantee db2Grantee, @NotNull JDBCResultSet dbResult) throws SQLException,
        DBException
    {
        // Lookup for the role in DS Cache
        String db2RoleName = JDBCUtils.safeGetStringTrimmed(dbResult, "ROLENAME");
        DB2Role db2Role = db2Grantee.getDataSource().getRole(session.getProgressMonitor(), db2RoleName);

        return new DB2RoleAuth(db2Role, dbResult);
    }
}
