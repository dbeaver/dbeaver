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
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Cache for DB2 Tables
 * 
 * @author Denis Forveille
 */
public final class DB2TableCache extends JDBCStructCache<DB2Schema, DB2Table, DB2TableColumn> {

    private static final String SQL_TABS;
    private static final String SQL_COLS_TAB = "SELECT * FROM SYSCAT.COLUMNS WHERE TABSCHEMA = ? AND TABNAME = ? ORDER BY COLNO WITH UR";
    private static final String SQL_COLS_ALL = "SELECT * FROM SYSCAT.COLUMNS WHERE TABSCHEMA = ? ORDER BY TABNAME, COLNO WITH UR";

    static {
        StringBuilder sb = new StringBuilder(512);
        sb.append("SELECT *");
        sb.append(" FROM SYSCAT.TABLES");
        sb.append(" WHERE TABSCHEMA = ?");
        sb.append("   AND TYPE IN (");
        sb.append("                  '" + DB2TableType.H.name() + "'");
        sb.append("                 ,'" + DB2TableType.L.name() + "'");
        sb.append("                 ,'" + DB2TableType.T.name() + "'");
        sb.append("                 ,'" + DB2TableType.U.name() + "'");
        sb.append("                 ,'" + DB2TableType.G.name() + "'");
        sb.append("                 )");
        sb.append(" ORDER BY TABNAME");
        sb.append(" WITH UR");

        SQL_TABS = sb.toString();
    }

    public DB2TableCache()
    {
        super("TABNAME");
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, DB2Schema db2Schema) throws SQLException
    {
        final JDBCPreparedStatement dbStat = session.prepareStatement(SQL_TABS);
        dbStat.setString(1, db2Schema.getName());
        return dbStat;
    }

    @Override
    protected DB2Table fetchObject(JDBCSession session, DB2Schema db2Schema, ResultSet dbResult) throws SQLException,
        DBException
    {
        return new DB2Table(session.getProgressMonitor(), db2Schema, dbResult);
    }

    @Override
    protected JDBCStatement prepareChildrenStatement(JDBCSession session, DB2Schema db2Schema, DB2Table forTable)
        throws SQLException
    {

        String sql;
        if (forTable != null) {
            sql = SQL_COLS_TAB;
        } else {
            sql = SQL_COLS_ALL;
        }
        JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, db2Schema.getName());
        if (forTable != null) {
            dbStat.setString(2, forTable.getName());
        }
        return dbStat;
    }

    @Override
    protected DB2TableColumn fetchChild(JDBCSession session, DB2Schema db2Schema, DB2Table db2Table, ResultSet dbResult)
        throws SQLException, DBException
    {
        return new DB2TableColumn(session.getProgressMonitor(), db2Table, dbResult);
    }

}
