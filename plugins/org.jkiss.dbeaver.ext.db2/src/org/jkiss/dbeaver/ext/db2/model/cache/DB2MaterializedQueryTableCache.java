/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.db2.editors.DB2ObjectType;
import org.jkiss.dbeaver.ext.db2.model.DB2MaterializedQueryTable;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;

import java.sql.SQLException;

/**
 * Cache for DB2 MQT
 * 
 * @author Denis Forveille
 */
public final class DB2MaterializedQueryTableCache
    extends JDBCStructLookupCache<DB2Schema, DB2MaterializedQueryTable, DB2TableColumn> {

    private static final String SQL_COLS_TAB = "SELECT * FROM SYSCAT.COLUMNS WHERE TABSCHEMA=? AND TABNAME = ? ORDER BY COLNO WITH UR";
    private static final String SQL_COLS_ALL = "SELECT * FROM SYSCAT.COLUMNS WHERE TABSCHEMA=? ORDER BY TABNAME, COLNO WITH UR";
    private static final String SQL_MQT;
    private static final String SQL_MQT_ALL;

    static {
        StringBuilder sb = new StringBuilder(256);
        sb.append("SELECT *");
        sb.append(" FROM SYSCAT.TABLES T");
        sb.append("    , SYSCAT.VIEWS V");
        sb.append(" WHERE V.VIEWSCHEMA = ?");
        sb.append("   AND T.TABSCHEMA = V.VIEWSCHEMA");
        sb.append("   AND T.TABNAME = V.VIEWNAME");
        sb.append("   AND T.TYPE IN ").append(DB2TableType.getInClause(DB2ObjectType.MQT));
        sb.append(" ORDER BY T.TABNAME");
        sb.append(" WITH UR");
        SQL_MQT_ALL = sb.toString();

        sb.setLength(0);

        sb.append("SELECT *");
        sb.append(" FROM SYSCAT.TABLES T");
        sb.append("    , SYSCAT.VIEWS V");
        sb.append(" WHERE V.VIEWSCHEMA = ?");
        sb.append("   AND V.VIEWNAME = ?");
        sb.append("   AND T.TABSCHEMA = V.VIEWSCHEMA");
        sb.append("   AND T.TABNAME = V.VIEWNAME");
        sb.append("   AND T.TYPE IN ").append(DB2TableType.getInClause(DB2ObjectType.MQT));
        sb.append(" WITH UR");
        SQL_MQT = sb.toString();
    }

    public DB2MaterializedQueryTableCache()
    {
        super("TABNAME");
    }

    @NotNull
    @Override
    public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema,
        DB2MaterializedQueryTable db2MaterializedQueryTable, String db2MaterializedQueryTableName) throws SQLException
    {
        if (db2MaterializedQueryTable != null || db2MaterializedQueryTableName != null) {
            final JDBCPreparedStatement dbStat = session.prepareStatement(SQL_MQT);
            dbStat.setString(1, db2Schema.getName());
            dbStat.setString(2,
                db2MaterializedQueryTable != null ? db2MaterializedQueryTable.getName() : db2MaterializedQueryTableName);
            return dbStat;
        } else {
            final JDBCPreparedStatement dbStat = session.prepareStatement(SQL_MQT_ALL);
            dbStat.setString(1, db2Schema.getName());
            return dbStat;
        }
    }

    @Override
    protected DB2MaterializedQueryTable fetchObject(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema,
        @NotNull JDBCResultSet dbResult) throws SQLException, DBException
    {
        return new DB2MaterializedQueryTable(session.getProgressMonitor(), db2Schema, dbResult);
    }

    @Override
    protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema,
        @Nullable DB2MaterializedQueryTable forMqt) throws SQLException
    {

        String sql;
        if (forMqt != null) {
            sql = SQL_COLS_TAB;
        } else {
            sql = SQL_COLS_ALL;
        }
        JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, db2Schema.getName());
        if (forMqt != null) {
            dbStat.setString(2, forMqt.getName());
        }
        return dbStat;
    }

    @Override
    protected DB2TableColumn fetchChild(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema,
        @NotNull DB2MaterializedQueryTable db2MQT, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
    {
        return new DB2TableColumn(session.getProgressMonitor(), db2MQT, dbResult);
    }

}
