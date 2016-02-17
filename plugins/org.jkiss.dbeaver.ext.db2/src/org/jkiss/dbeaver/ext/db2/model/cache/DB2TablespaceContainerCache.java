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
package org.jkiss.dbeaver.ext.db2.model.cache;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2Tablespace;
import org.jkiss.dbeaver.ext.db2.model.DB2TablespaceContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

import java.sql.SQLException;

/**
 * Cache for DB2 Tablespaces Containers
 * 
 * @author Denis Forveille
 */
public class DB2TablespaceContainerCache extends JDBCObjectCache<DB2Tablespace, DB2TablespaceContainer> {

    private static final String SQL = "SELECT T.* FROM TABLE(SNAP_GET_CONTAINER('',-1)) AS T WHERE T.TBSP_ID= ? order by T.CONTAINER_ID WITH UR";

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DB2Tablespace ts) throws SQLException
    {
        final JDBCPreparedStatement dbStat = session.prepareStatement(SQL);
        dbStat.setInt(1, ts.getTbspaceId());
        return dbStat;
    }

    @Override
    protected DB2TablespaceContainer fetchObject(@NotNull JDBCSession session, @NotNull DB2Tablespace ts, @NotNull JDBCResultSet resultSet)
        throws SQLException, DBException
    {
        return new DB2TablespaceContainer(ts, resultSet);
    }
}