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

import java.sql.SQLException;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.DB2Index;
import org.jkiss.dbeaver.ext.db2.model.DB2IndexColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;

/**
 * Cache for DB2 Indexes at the Schema Level
 * 
 * @author Denis Forveille
 */
public final class DB2IndexCache extends JDBCStructCache<DB2Schema, DB2Index, DB2IndexColumn> {

    private static final String SQL_INDS_ALL = "SELECT * FROM SYSCAT.INDEXES WHERE INDSCHEMA = ? ORDER BY INDNAME WITH UR";
    private static final String SQL_COLS_IND = "SELECT * FROM SYSCAT.INDEXCOLUSE WHERE INDSCHEMA = ? AND INDNAME = ? ORDER BY COLSEQ WITH UR";

    public DB2IndexCache()
    {
        super("INDNAME");
    } 

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema) throws SQLException
    {
        JDBCPreparedStatement dbStat = session.prepareStatement(SQL_INDS_ALL);
        dbStat.setString(1, db2Schema.getName());
        return dbStat;
    }

    @Override
    protected DB2Index fetchObject(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema, @NotNull JDBCResultSet dbResult)
        throws SQLException, DBException
    {

        // Look for related table...or nickname...or MQT
        String tableOrNicknameSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABSCHEMA");
        String tableOrNicknameName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABNAME");
        DB2TableBase db2Table = DB2Utils.findTableBySchemaNameAndName(session.getProgressMonitor(), db2Schema.getDataSource(),
            tableOrNicknameSchemaName, tableOrNicknameName);
        if (db2Table == null) {
            db2Table = DB2Utils.findNicknameBySchemaNameAndName(session.getProgressMonitor(), db2Schema.getDataSource(),
                tableOrNicknameSchemaName, tableOrNicknameName);
        }
        if (db2Table == null) {
            db2Table = DB2Utils.findMaterializedQueryTableBySchemaNameAndName(session.getProgressMonitor(),
                db2Schema.getDataSource(), tableOrNicknameSchemaName, tableOrNicknameName);
        }

        return new DB2Index(session.getProgressMonitor(), db2Schema, db2Table, dbResult);
    }

    @Override
    protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema,
        @Nullable DB2Index forIndex) throws SQLException
    {
        JDBCPreparedStatement dbStat = session.prepareStatement(SQL_COLS_IND);
        dbStat.setString(1, forIndex.getContainer().getName());
        dbStat.setString(2, forIndex.getName());
        return dbStat;
    }

    @Override
    protected DB2IndexColumn fetchChild(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema, @NotNull DB2Index db2Index,
        @NotNull JDBCResultSet dbResult) throws SQLException, DBException
    {
        return new DB2IndexColumn(session.getProgressMonitor(), db2Index, dbResult);
    }

}
