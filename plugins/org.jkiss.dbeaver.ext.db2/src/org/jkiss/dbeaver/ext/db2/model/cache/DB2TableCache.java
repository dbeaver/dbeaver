/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.editors.DB2ObjectType;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;

import java.sql.SQLException;

/**
 * Cache for DB2 Tables
 * 
 * @author Denis Forveille
 */
public final class DB2TableCache extends JDBCStructLookupCache<DB2Schema, DB2Table, DB2TableColumn> {

    private static final String SQL_COLS_TAB = "SELECT * FROM SYSCAT.COLUMNS WHERE TABSCHEMA = ? AND TABNAME = ? ORDER BY COLNO WITH UR";
    private static final String SQL_COLS_ALL = "SELECT * FROM SYSCAT.COLUMNS WHERE TABSCHEMA = ? ORDER BY TABNAME, COLNO WITH UR";
    private static final String SQL_TAB;
    private static final String SQL_TAB_ALL;

    static {
        StringBuilder sb = new StringBuilder(256);
        sb.append("SELECT *");
        sb.append("  FROM SYSCAT.TABLES");
        sb.append(" WHERE TABSCHEMA = ?");
        sb.append("   AND TYPE IN ").append(DB2TableType.getInClause(DB2ObjectType.TABLE));
        sb.append(" ORDER BY TABNAME");
        sb.append(" WITH UR");
        SQL_TAB_ALL = sb.toString();

        sb.setLength(0);

        sb.append("SELECT *");
        sb.append("  FROM SYSCAT.TABLES");
        sb.append(" WHERE TABSCHEMA = ?");
        sb.append("   AND TABNAME = ?");
        sb.append("   AND TYPE IN ").append(DB2TableType.getInClause(DB2ObjectType.TABLE));
        sb.append(" WITH UR");
        SQL_TAB = sb.toString();
    }

    public DB2TableCache()
    {
        super("TABNAME");
    }

    @NotNull
    @Override
    public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema,
        @Nullable DB2Table db2Table, @Nullable String db2TableName) throws SQLException
    {
        if (db2Table != null || db2TableName != null) {
            final JDBCPreparedStatement dbStat = session.prepareStatement(SQL_TAB);
            dbStat.setString(1, db2Schema.getName());
            dbStat.setString(2, db2Table != null ? db2Table.getName() : db2TableName);
            return dbStat;
        } else {
            final JDBCPreparedStatement dbStat = session.prepareStatement(SQL_TAB_ALL);
            dbStat.setString(1, db2Schema.getName());
            return dbStat;
        }
    }

    @Override
    protected DB2Table fetchObject(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema, @NotNull JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        return new DB2Table(session.getProgressMonitor(), db2Schema, dbResult);
    }

    @Override
    protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema,
        @Nullable DB2Table forTable) throws SQLException
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
    protected DB2TableColumn fetchChild(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema, @NotNull DB2Table db2Table,
        @NotNull JDBCResultSet dbResult) throws SQLException, DBException
    {
        return new DB2TableColumn(session.getProgressMonitor(), db2Table, dbResult);
    }

}
