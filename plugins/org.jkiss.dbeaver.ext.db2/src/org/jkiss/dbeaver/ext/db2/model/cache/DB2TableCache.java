/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.code.Nullable;
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
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema) throws SQLException
    {
        final JDBCPreparedStatement dbStat = session.prepareStatement(SQL_TABS);
        dbStat.setString(1, db2Schema.getName());
        return dbStat;
    }

    @Override
    protected DB2Table fetchObject(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema, @NotNull ResultSet dbResult) throws SQLException,
        DBException
    {
        return new DB2Table(session.getProgressMonitor(), db2Schema, dbResult);
    }

    @Override
    protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema, @Nullable DB2Table forTable)
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
    protected DB2TableColumn fetchChild(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema, @NotNull DB2Table db2Table, @NotNull ResultSet dbResult)
        throws SQLException, DBException
    {
        return new DB2TableColumn(session.getProgressMonitor(), db2Table, dbResult);
    }

}
