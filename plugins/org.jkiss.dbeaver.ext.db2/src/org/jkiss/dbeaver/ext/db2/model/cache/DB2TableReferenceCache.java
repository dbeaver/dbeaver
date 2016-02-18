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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2TableKeyColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2TableReference;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.List;

/**
 * Cache for DB2 Table Forign Keys (Reverse)
 * 
 * @author Denis Forveille
 */
public final class DB2TableReferenceCache extends JDBCCompositeCache<DB2Schema, DB2Table, DB2TableReference, DB2TableKeyColumn> {

    private static final String SQL_REF_TAB;
    private static final String SQL_REF_ALL;

    static {
        StringBuilder sb = new StringBuilder(256);
        sb.append(" SELECT R.*");
        sb.append("      , KCU.COLNAME");
        sb.append("      , KCU.COLSEQ");
        sb.append("   FROM SYSCAT.REFERENCES R");
        sb.append("       ,SYSCAT.KEYCOLUSE KCU");
        sb.append("  WHERE R.REFTABSCHEMA = ?");
        sb.append("    AND R.REFTABNAME = ?");
        sb.append("    AND KCU.CONSTNAME = R.REFKEYNAME");
        sb.append("    AND KCU.TABSCHEMA = R.REFTABSCHEMA");
        sb.append("    AND KCU.TABNAME   = R.REFTABNAME");
        sb.append("  ORDER BY R.REFKEYNAME");
        sb.append("         , KCU.COLSEQ");
        sb.append(" WITH UR");
        SQL_REF_TAB = sb.toString();

        sb.setLength(0);

        sb.append(" SELECT R.*");
        sb.append("      , KCU.COLNAME");
        sb.append("      , KCU.COLSEQ");
        sb.append("   FROM SYSCAT.REFERENCES R");
        sb.append("       ,SYSCAT.KEYCOLUSE KCU");
        sb.append("  WHERE R.REFTABSCHEMA = ?");
        sb.append("    AND KCU.CONSTNAME = R.REFKEYNAME");
        sb.append("    AND KCU.TABSCHEMA = R.REFTABSCHEMA");
        sb.append("    AND KCU.TABNAME   = R.REFTABNAME");
        sb.append("  ORDER BY R.REFKEYNAME");
        sb.append("         , KCU.COLSEQ");
        sb.append(" WITH UR");
        SQL_REF_ALL = sb.toString();
    }

    public DB2TableReferenceCache(DB2TableCache tableCache)
    {
        super(tableCache, DB2Table.class, "REFTABNAME", "CONSTNAME");
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, DB2Schema db2Schema, DB2Table forTable)
        throws SQLException
    {
        String sql;
        if (forTable != null) {
            sql = SQL_REF_TAB;
        } else {
            sql = SQL_REF_ALL;
        }
        JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, db2Schema.getName());
        if (forTable != null) {
            dbStat.setString(2, forTable.getName());
        }
        return dbStat;
    }

    @Nullable
    @Override
    protected DB2TableReference fetchObject(JDBCSession session, DB2Schema db2Schema, DB2Table db2Table, String constName,
        JDBCResultSet dbResult) throws SQLException, DBException
    {
        return new DB2TableReference(session.getProgressMonitor(), db2Table, dbResult);
    }

    @Nullable
    @Override
    protected DB2TableKeyColumn[] fetchObjectRow(JDBCSession session, DB2Table db2Table,
                                                 DB2TableReference db2TableReference, JDBCResultSet dbResult) throws SQLException, DBException
    {

        String colName = JDBCUtils.safeGetString(dbResult, "COLNAME");
        DB2TableColumn tableColumn = db2Table.getAttribute(session.getProgressMonitor(), colName);
        if (tableColumn == null) {
            log.debug("DB2TableReferenceCache : Column '" + colName + "' not found in table '" + db2Table.getName() + "' ??");
            return null;
        } else {
            return new DB2TableKeyColumn[]  {
                new DB2TableKeyColumn(db2TableReference, tableColumn, JDBCUtils.safeGetInt(dbResult, "COLSEQ"))
            };
        }
    }

    @Override
    protected void cacheChildren(DBRProgressMonitor monitor, DB2TableReference constraint, List<DB2TableKeyColumn> rows)
    {
        constraint.setColumns(rows);
    }
}
