/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
import org.jkiss.dbeaver.ext.db2.model.DB2Alias;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Cache for DB2 Aliases
 * 
 * @author Denis Forveille
 */
public final class DB2AliasCache extends JDBCObjectCache<DB2Schema, DB2Alias> {

    private static String SQL_ALL;

    static {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(" SELECT 'TABLE' as TYPE ");
        sb.append("      , TABNAME AS NAME");
        sb.append("      , BASE_TABSCHEMA AS BASE_SCHEMA");
        sb.append("      , BASE_TABNAME AS BASE_NAME");
        sb.append("   FROM SYSCAT.TABLES");
        sb.append("  WHERE TABSCHEMA = ?"); // 1
        sb.append("    AND TYPE = 'A'");

        sb.append(" UNION ALL");

        sb.append(" SELECT 'SEQUENCE' as TYPE ");
        sb.append("       , SEQNAME AS NAME");
        sb.append("       , BASE_SEQSCHEMA AS BASE_SCHEMA");
        sb.append("       , BASE_SEQNAME AS BASE_NAME");
        sb.append("   FROM SYSCAT.SEQUENCES");
        sb.append("  WHERE SEQSCHEMA = ?"); // 2
        sb.append("    AND SEQTYPE = 'A'");

        sb.append(" UNION ALL");

        sb.append(" SELECT 'MODULE' as TYPE ");
        sb.append("       , MODULENAME AS NAME");
        sb.append("       , BASE_MODULESCHEMA AS BASE_SCHEMA");
        sb.append("       , BASE_MODULENAME AS BASE_NAME");
        sb.append("   FROM SYSCAT.MODULES");
        sb.append("  WHERE MODULESCHEMA = ?"); // 3
        sb.append("    AND MODULETYPE = 'A'");

        sb.append(" ORDER BY NAME");
        sb.append("        , TYPE");
        sb.append(" WITH UR");
        SQL_ALL = sb.toString();
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, DB2Schema db2Schema) throws SQLException
    {
        final JDBCPreparedStatement dbStat = context.prepareStatement(SQL_ALL);
        dbStat.setString(1, db2Schema.getName());
        dbStat.setString(2, db2Schema.getName());
        dbStat.setString(3, db2Schema.getName());
        return dbStat;
    }

    protected DB2Alias fetchObject(JDBCExecutionContext context, DB2Schema db2Schema, ResultSet resultSet) throws SQLException,
        DBException
    {
        return new DB2Alias(context.getProgressMonitor(), db2Schema, resultSet);
    }

}