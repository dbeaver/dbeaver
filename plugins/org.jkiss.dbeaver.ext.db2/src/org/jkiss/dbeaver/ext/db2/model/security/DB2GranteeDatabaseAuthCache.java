/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.db2.model.security;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

import java.sql.SQLException;

/**
 * Cache for Databases authorities for a given Grantee
 * 
 * @author Denis Forveille
 */
public class DB2GranteeDatabaseAuthCache extends JDBCObjectCache<DB2Grantee, DB2DatabaseAuth> {

    private static final String SQL = "SELECT * FROM SYSCAT.DBAUTH WHERE GRANTEETYPE = ? AND GRANTEE = ? WITH UR";

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DB2Grantee db2Grantee) throws SQLException
    {
        final JDBCPreparedStatement dbStat = session.prepareStatement(SQL);
        dbStat.setString(1, db2Grantee.getType().name());
        dbStat.setString(2, db2Grantee.getName());
        // dbStat.setMaxRows(1);
        // dbStat.setFetchSize(1); // Only 1 row per user
        return dbStat;
    }

    @Override
    protected DB2DatabaseAuth fetchObject(@NotNull JDBCSession session, @NotNull DB2Grantee db2Grantee, @NotNull JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        return new DB2DatabaseAuth(session.getProgressMonitor(), db2Grantee.getDataSource(), dbResult);
    }
}
