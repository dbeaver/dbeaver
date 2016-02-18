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

import java.sql.SQLException;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.DB2Index;
import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

/**
 * Cache for DB2 Indexes for a given Table
 * 
 * @author Denis Forveille
 */
public class DB2TableIndexCache extends JDBCObjectCache<DB2TableBase, DB2Index> {

    private static final String SQL_INDS_TAB = "SELECT * FROM SYSCAT.INDEXES WHERE TABSCHEMA = ? AND TABNAME = ? ORDER BY INDNAME WITH UR";

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DB2TableBase db2Table)
        throws SQLException
    {
        final JDBCPreparedStatement dbStat = session.prepareStatement(SQL_INDS_TAB);
        dbStat.setString(1, db2Table.getSchema().getName());
        dbStat.setString(2, db2Table.getName());
        return dbStat;
    }
 
    @Override
    protected DB2Index fetchObject(@NotNull JDBCSession session, @NotNull DB2TableBase db2Table, @NotNull JDBCResultSet dbResult)
        throws SQLException, DBException
    {

        // Lookup for indexes in right cache..
        String indexSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "INDSCHEMA");
        String indexName = JDBCUtils.safeGetStringTrimmed(dbResult, "INDNAME");

        return DB2Utils.findIndexBySchemaNameAndName(session.getProgressMonitor(), db2Table.getDataSource(), indexSchemaName,
            indexName);
    }
}
