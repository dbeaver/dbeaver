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
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.DB2Index;
import org.jkiss.dbeaver.ext.db2.model.DB2IndexColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;

import java.sql.ResultSet;
import java.sql.SQLException;

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
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, DB2Schema db2Schema) throws SQLException
    {
        JDBCPreparedStatement dbStat = session.prepareStatement(SQL_INDS_ALL);
        dbStat.setString(1, db2Schema.getName());
        return dbStat;
    }

    @Override
    protected DB2Index fetchObject(JDBCSession session, DB2Schema db2Schema, ResultSet dbResult) throws SQLException,
        DBException
    {

        // Look for related table...or nickname
        String tableOrNicknameSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABSCHEMA");
        String tableOrNicknameName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABNAME");
        DB2Table db2Table = DB2Utils.findTableBySchemaNameAndName(session.getProgressMonitor(), db2Schema.getDataSource(),
            tableOrNicknameSchemaName, tableOrNicknameName);
        if (db2Table == null) {
            db2Table = DB2Utils.findNicknameBySchemaNameAndName(session.getProgressMonitor(), db2Schema.getDataSource(),
                tableOrNicknameSchemaName, tableOrNicknameName);
        }

        return new DB2Index(session.getProgressMonitor(), db2Schema, db2Table, dbResult);
    }

    @Override
    protected JDBCStatement prepareChildrenStatement(JDBCSession session, DB2Schema db2Schema, DB2Index forIndex)
        throws SQLException
    {
        JDBCPreparedStatement dbStat = session.prepareStatement(SQL_COLS_IND);
        dbStat.setString(1, forIndex.getContainer().getName());
        dbStat.setString(2, forIndex.getName());
        return dbStat;
    }

    @Override
    protected DB2IndexColumn fetchChild(JDBCSession session, DB2Schema db2Schema, DB2Index db2Index, ResultSet dbResult)
        throws SQLException, DBException
    {
        return new DB2IndexColumn(session.getProgressMonitor(), db2Index, dbResult);
    }

}
