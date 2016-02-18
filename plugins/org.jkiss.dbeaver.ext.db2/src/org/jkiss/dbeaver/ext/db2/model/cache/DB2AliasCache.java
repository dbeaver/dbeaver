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
import org.jkiss.dbeaver.ext.db2.model.DB2Alias;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

import java.sql.SQLException;

/**
 * Cache for DB2 Aliases
 * 
 * @author Denis Forveille
 */
public final class DB2AliasCache extends JDBCObjectCache<DB2Schema, DB2Alias> {

    private static final String SQL_WITHOUT_MODULE_AND_SEQUALIAS;
    private static final String SQL_FULL;

    static {
        StringBuilder sb1 = new StringBuilder(1024);
        sb1.append(" SELECT 'TABLE' as TYPE ");
        sb1.append("      , TABNAME AS NAME");
        sb1.append("      , BASE_TABSCHEMA AS BASE_SCHEMA");
        sb1.append("      , BASE_TABNAME AS BASE_NAME");
        sb1.append("   FROM SYSCAT.TABLES");
        sb1.append("  WHERE TABSCHEMA = ?"); // 1
        sb1.append("    AND TYPE = '").append(DB2TableType.A.name()).append("'");

        StringBuilder sb2 = new StringBuilder(256);
        sb2.append(" UNION ALL");
        sb2.append(" SELECT 'SEQUENCE' as TYPE ");
        sb2.append("       , SEQNAME AS NAME");
        sb2.append("       , BASE_SEQSCHEMA AS BASE_SCHEMA");
        sb2.append("       , BASE_SEQNAME AS BASE_NAME");
        sb2.append("   FROM SYSCAT.SEQUENCES");
        sb2.append("  WHERE SEQSCHEMA = ?"); // 2
        sb2.append("    AND SEQTYPE = '").append(DB2TableType.A.name()).append("'");
        sb2.append(" UNION ALL");
        sb2.append(" SELECT 'MODULE' as TYPE ");
        sb2.append("       , MODULENAME AS NAME");
        sb2.append("       , BASE_MODULESCHEMA AS BASE_SCHEMA");
        sb2.append("       , BASE_MODULENAME AS BASE_NAME");
        sb2.append("   FROM SYSCAT.MODULES");
        sb2.append("  WHERE MODULESCHEMA = ?"); // 3
        sb2.append("    AND MODULETYPE = '").append(DB2TableType.A.name()).append("'");

        StringBuilder sb3 = new StringBuilder(64);
        sb3.append(" ORDER BY NAME");
        sb3.append("        , TYPE");
        sb3.append(" WITH UR");

        SQL_FULL = sb1.toString() + sb2.toString() + sb3.toString();
        SQL_WITHOUT_MODULE_AND_SEQUALIAS = sb1.toString() + sb3.toString();
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema) throws SQLException
    {
        DB2DataSource db2DataSource = db2Schema.getDataSource();
        String sql;
        if (db2DataSource.isAtLeastV9_7()) {
            sql = SQL_FULL;
        } else {
            sql = SQL_WITHOUT_MODULE_AND_SEQUALIAS;
        }
        JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, db2Schema.getName());
        if (db2DataSource.isAtLeastV9_7()) {
            dbStat.setString(2, db2Schema.getName());
            dbStat.setString(3, db2Schema.getName());
        }
        return dbStat;
    }

    protected DB2Alias fetchObject(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
    {
        return new DB2Alias(session.getProgressMonitor(), db2Schema, resultSet);
    }

}