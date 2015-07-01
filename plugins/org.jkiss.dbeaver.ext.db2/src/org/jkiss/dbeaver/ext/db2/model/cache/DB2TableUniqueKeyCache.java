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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2TableKeyColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2TableUniqueKey;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2ConstraintType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Cache for DB2 Table Unique Keys
 * 
 * @author Denis Forveille
 */
public final class DB2TableUniqueKeyCache extends JDBCCompositeCache<DB2Schema, DB2Table, DB2TableUniqueKey, DB2TableKeyColumn> {

    private static final String SQL_UK_TAB;
    private static final String SQL_UK_ALL;

    static {
        StringBuilder sb = new StringBuilder(512);
        sb.append(" SELECT C.*");
        sb.append("      , KCU.COLNAME");
        sb.append("      , KCU.COLSEQ");
        sb.append("   FROM SYSCAT.TABCONST C");
        sb.append("       ,SYSCAT.KEYCOLUSE KCU");
        sb.append("  WHERE C.TABSCHEMA = ?");
        sb.append("    AND C.TABNAME = ?");
        sb.append("    AND C.TYPE IN ('P','U')");
        sb.append("    AND KCU.CONSTNAME = C.CONSTNAME");
        sb.append("    AND KCU.TABSCHEMA = C.TABSCHEMA");
        sb.append("    AND KCU.TABNAME   = C.TABNAME");
        sb.append("  ORDER BY C.CONSTNAME");
        sb.append("         , KCU.COLSEQ");
        sb.append(" WITH UR");
        SQL_UK_TAB = sb.toString();

        sb.setLength(0);

        sb.append(" SELECT C.*");
        sb.append("      , KCU.COLNAME");
        sb.append("      , KCU.COLSEQ");
        sb.append("   FROM SYSCAT.TABCONST C");
        sb.append("       ,SYSCAT.KEYCOLUSE KCU");
        sb.append("  WHERE C.TABSCHEMA = ?");
        sb.append("    AND C.TYPE IN ('P','U')");
        sb.append("    AND KCU.CONSTNAME = C.CONSTNAME");
        sb.append("    AND KCU.TABSCHEMA = C.TABSCHEMA");
        sb.append("    AND KCU.TABNAME   = C.TABNAME");
        sb.append("  ORDER BY C.CONSTNAME");
        sb.append("         , KCU.COLSEQ");
        sb.append(" WITH UR");
        SQL_UK_ALL = sb.toString();
    }

    public DB2TableUniqueKeyCache(DB2TableCache tableCache)
    {
        super(tableCache, DB2Table.class, "TABNAME", "CONSTNAME");
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, DB2Schema db2Schema, DB2Table forTable)
        throws SQLException
    {
        String sql;
        if (forTable != null) {
            sql = SQL_UK_TAB;
        } else {
            sql = SQL_UK_ALL;
        }
        JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, db2Schema.getName());
        if (forTable != null) {
            dbStat.setString(2, forTable.getName());
        }
        return dbStat;
    }

    @Override
    protected DB2TableUniqueKey fetchObject(JDBCSession session, DB2Schema db2Schema, DB2Table db2Table, String indexName,
        ResultSet dbResult) throws SQLException, DBException
    {

        DBSEntityConstraintType type = DB2ConstraintType.getConstraintType(JDBCUtils.safeGetString(dbResult, "TYPE"));
        return new DB2TableUniqueKey(session.getProgressMonitor(), db2Table, dbResult, type);
    }

    @Override
    protected DB2TableKeyColumn fetchObjectRow(JDBCSession session, DB2Table db2Table, DB2TableUniqueKey object,
        ResultSet dbResult) throws SQLException, DBException
    {

        String colName = JDBCUtils.safeGetString(dbResult, "COLNAME");
        DB2TableColumn tableColumn = db2Table.getAttribute(session.getProgressMonitor(), colName);
        if (tableColumn == null) {
            log.debug("Column '" + colName + "' not found in table '" + db2Table.getFullQualifiedName() + "' ??");
            return null;
        } else {
            return new DB2TableKeyColumn(object, tableColumn, JDBCUtils.safeGetInt(dbResult, "COLSEQ"));
        }
    }

    @Override
    protected void cacheChildren(DB2TableUniqueKey constraint, List<DB2TableKeyColumn> rows)
    {
        constraint.setColumns(rows);
    }
}
