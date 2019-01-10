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
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;

/**
 * Cache for DB2 Groups and Users
 * 
 * @author Denis Forveille
 */
public final class DB2GranteeCache extends JDBCObjectCache<DB2DataSource, DB2Grantee> {

    private DB2AuthIDType authIdType;
    private String authIdTypeName;

    public DB2GranteeCache(DB2AuthIDType authIdType)
    {
        this.authIdType = authIdType;
        this.authIdTypeName = authIdType.name();
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DB2DataSource db2DataSource) throws SQLException
    {
        JDBCPreparedStatement dbStat = session.prepareStatement(getSQLStatement(db2DataSource));
        dbStat.setString(1, authIdTypeName);
        dbStat.setString(2, authIdTypeName);
        dbStat.setString(3, authIdTypeName);
        dbStat.setString(4, authIdTypeName);
        dbStat.setString(5, authIdTypeName);
        dbStat.setString(6, authIdTypeName);
        dbStat.setString(7, authIdTypeName);
        dbStat.setString(8, authIdTypeName);
        dbStat.setString(9, authIdTypeName);
        dbStat.setString(10, authIdTypeName);
        if (db2DataSource.isAtLeastV9_5()) {
            dbStat.setString(11, authIdTypeName);
        }
        if (db2DataSource.isAtLeastV9_7()) {
            dbStat.setString(12, authIdTypeName);
        }
        return dbStat;
    }

    @Override
    protected DB2Grantee fetchObject(@NotNull JDBCSession session, @NotNull DB2DataSource db2DataSource, @NotNull JDBCResultSet resultSet) throws SQLException,
        DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();
        switch (authIdType) {
        case G:
            return new DB2Group(monitor, db2DataSource, resultSet);
        case U:
            return new DB2User(monitor, db2DataSource, resultSet);
        default:
            throw new DBException("Structural problem. " + authIdType + " type not implemented");
        }
    }

    // -------
    // Helpers
    // -------

    private String getSQLStatement(DB2DataSource db2DataSource)
    {

        // DF: could be put in a cache "per version"

        StringBuilder sb = new StringBuilder(1536);

        sb.append("SELECT DISTINCT GRANTEE");
        sb.append("  FROM SYSCAT.TABAUTH");
        sb.append(" WHERE GRANTEETYPE = ?"); // 1

        sb.append(" UNION ");

        sb.append("SELECT DISTINCT GRANTEE");
        sb.append("  FROM SYSCAT.INDEXAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 2

        sb.append(" UNION ");

        sb.append("SELECT DISTINCT GRANTEE");
        sb.append("  FROM SYSCAT.SEQUENCEAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 3

        sb.append(" UNION ");

        sb.append("SELECT DISTINCT GRANTEE");
        sb.append("  FROM SYSCAT.TBSPACEAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 4

        sb.append(" UNION ");

        sb.append("SELECT DISTINCT GRANTEE");
        sb.append("  FROM SYSCAT.SCHEMAAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 5

        sb.append(" UNION ");

        sb.append("SELECT DISTINCT GRANTEE");
        sb.append("  FROM SYSCAT.PACKAGEAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 6

        sb.append(" UNION ");

        sb.append("SELECT DISTINCT GRANTEE");
        sb.append("  FROM SYSCAT.COLAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 7

        sb.append(" UNION ");

        sb.append("SELECT DISTINCT GRANTEE");
        sb.append("  FROM SYSCAT.ROUTINEAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 8

        sb.append(" UNION ");

        sb.append("SELECT DISTINCT GRANTEE");
        sb.append("  FROM SYSCAT.DBAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 9

        sb.append(" UNION ");

        sb.append("SELECT DISTINCT GRANTEE");
        sb.append("  FROM SYSCAT.XSROBJECTAUTH");
        sb.append(" WHERE GRANTEETYPE = ?"); // 10

        if (db2DataSource.isAtLeastV9_5()) {
            sb.append(" UNION ");

            sb.append("SELECT DISTINCT GRANTEE");
            sb.append("  FROM SYSCAT.ROLEAUTH");
            sb.append(" WHERE GRANTEETYPE = ?"); // 11
        }

        if (db2DataSource.isAtLeastV9_7()) {
            sb.append(" UNION ");

            sb.append("SELECT DISTINCT GRANTEE");
            sb.append("  FROM SYSCAT.MODULEAUTH");
            sb.append(" WHERE GRANTEETYPE = ?"); // 12
        }

        sb.append(" ORDER BY GRANTEE");
        sb.append(" WITH UR");

        return sb.toString();
    }

}