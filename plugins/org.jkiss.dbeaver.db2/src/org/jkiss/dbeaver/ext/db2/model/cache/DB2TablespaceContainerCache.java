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
package org.jkiss.dbeaver.ext.db2.model.cache;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2Tablespace;
import org.jkiss.dbeaver.ext.db2.model.DB2TablespaceContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Cache for DB2 Tablespaces Containers
 * 
 * @author Denis Forveille
 */
public class DB2TablespaceContainerCache extends JDBCObjectCache<DB2Tablespace, DB2TablespaceContainer> {

    private static final String SQL = "SELECT T.* FROM TABLE(SNAP_GET_CONTAINER('',-1)) AS T WHERE T.TBSP_ID= ? order by T.CONTAINER_ID WITH UR";

    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, DB2Tablespace ts) throws SQLException
    {
        final JDBCPreparedStatement dbStat = session.prepareStatement(SQL);
        dbStat.setInt(1, ts.getTbspaceId());
        return dbStat;
    }

    @Override
    protected DB2TablespaceContainer fetchObject(JDBCSession session, DB2Tablespace ts, ResultSet resultSet)
        throws SQLException, DBException
    {
        return new DB2TablespaceContainer(ts, resultSet);
    }
}