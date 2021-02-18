/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.db2.model.cache;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
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
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;

import java.sql.SQLException;

/**
 * Cache for DB2 Indexes at the Schema Level
 * 
 * @author Denis Forveille
 */
public final class DB2IndexCache extends JDBCStructLookupCache<DB2Schema, DB2Index, DB2IndexColumn> {

    private static final Log    log          = Log.getLog(DB2IndexCache.class);

    private static final String SQL_COLS_IND = "SELECT * FROM SYSCAT.INDEXCOLUSE WHERE INDSCHEMA = ? AND INDNAME = ? ORDER BY COLSEQ WITH UR";
    private static final String SQL_IND;
    private static final String SQL_IND_ALL;

    static {
        StringBuilder sb = new StringBuilder(128);
        sb.append("SELECT *");
        sb.append("  FROM SYSCAT.INDEXES");
        sb.append(" WHERE INDSCHEMA = ?");
        sb.append(" ORDER BY INDNAME");
        sb.append(" WITH UR");
        SQL_IND_ALL = sb.toString();

        sb.setLength(0);

        sb.append("SELECT *");
        sb.append("  FROM SYSCAT.INDEXES");
        sb.append(" WHERE INDSCHEMA = ?");
        sb.append("   AND INDNAME = ?");
        sb.append(" WITH UR");
        SQL_IND = sb.toString();
    }

    public DB2IndexCache()
    {
        super("INDNAME");
    }

    @NotNull
    @Override
    public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema, DB2Index db2Index, String db2IndexName)
        throws SQLException
    {
        if (db2Index != null || db2IndexName != null) {
            final JDBCPreparedStatement dbStat = session.prepareStatement(SQL_IND);
            dbStat.setString(1, db2Schema.getName());
            dbStat.setString(2, db2Index != null ? db2Index.getName() : db2IndexName);
            return dbStat;
        } else {
            final JDBCPreparedStatement dbStat = session.prepareStatement(SQL_IND_ALL);
            dbStat.setString(1, db2Schema.getName());
            return dbStat;
        }
    }

    @Override
    protected DB2Index fetchObject(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema, @NotNull JDBCResultSet dbResult)
        throws SQLException, DBException
    {

        // Look for related table...or nickname...or MQT
        String tableOrNicknameSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABSCHEMA");
        String tableOrNicknameName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABNAME");

        DB2Schema objectSchema = db2Schema.getDataSource().getSchema(session.getProgressMonitor(), tableOrNicknameSchemaName);
        if (objectSchema == null) {
            log.error("Schema '" + tableOrNicknameSchemaName + "' not found");
            return null;
        }
        // FIXME: here we cache all tables to avoid spam in table lookup
        // FIXME: because we always read all indexes. Make index cache lookup cache
        objectSchema.getTables(session.getProgressMonitor());
        DB2TableBase db2Table = objectSchema.getTable(session.getProgressMonitor(), tableOrNicknameName);
        if (db2Table == null) {
            db2Table = DB2Utils.findNicknameBySchemaNameAndName(session.getProgressMonitor(), db2Schema.getDataSource(),
                tableOrNicknameSchemaName, tableOrNicknameName);
        }
        if (db2Table == null) {
            db2Table = DB2Utils.findMaterializedQueryTableBySchemaNameAndName(session.getProgressMonitor(),
                db2Schema.getDataSource(), tableOrNicknameSchemaName, tableOrNicknameName);
        }
        if (db2Table == null) {
            log.error("Object '" + tableOrNicknameName + "' not found in schema '" + tableOrNicknameSchemaName + "'");
            return null;
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
