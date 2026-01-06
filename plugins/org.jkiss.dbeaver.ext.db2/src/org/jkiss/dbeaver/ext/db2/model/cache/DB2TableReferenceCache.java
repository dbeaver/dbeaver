/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
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

    public static final String SQL_REF_TAB;
    public static final String SQL_REF_ALL;

    static {
        SQL_REF_TAB = " SELECT R.*" +
            "      , KCU.COLNAME" +
            "      , KCU.COLSEQ" +
            "   FROM SYSCAT.REFERENCES R" +
            "       ,SYSCAT.KEYCOLUSE KCU" +
            "  WHERE R.REFTABSCHEMA = ?" +
            "    AND R.REFTABNAME = ?" +
            "    AND KCU.CONSTNAME = R.REFKEYNAME" +
            "    AND KCU.TABSCHEMA = R.REFTABSCHEMA" +
            "    AND KCU.TABNAME   = R.REFTABNAME" +
            "  ORDER BY R.REFKEYNAME" +
            "         , KCU.COLSEQ" +
            " WITH UR";

        SQL_REF_ALL = " SELECT R.*" +
            "      , KCU.COLNAME" +
            "      , KCU.COLSEQ" +
            "   FROM SYSCAT.REFERENCES R" +
            "       ,SYSCAT.KEYCOLUSE KCU" +
            "  WHERE R.REFTABSCHEMA = ?" +
            "    AND KCU.CONSTNAME = R.REFKEYNAME" +
            "    AND KCU.TABSCHEMA = R.REFTABSCHEMA" +
            "    AND KCU.TABNAME   = R.REFTABNAME" +
            "  ORDER BY R.REFKEYNAME" +
            "         , KCU.COLSEQ" +
            " WITH UR";
    }

    public DB2TableReferenceCache(DB2TableCache tableCache)
    {
        super(tableCache, DB2Table.class, "REFTABNAME", "CONSTNAME");
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema, @Nullable DB2Table forTable)
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
    protected DB2TableReference fetchObject(
        @NotNull JDBCSession session, @NotNull DB2Schema db2Schema, @NotNull DB2Table db2Table, @NotNull String constName,
        @NotNull JDBCResultSet dbResult) throws SQLException, DBException
    {
        String ownerSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABSCHEMA");
        String ownerTableName = JDBCUtils.safeGetString(dbResult, "TABNAME");
        DB2Table ownerTable = DB2Utils.findTableBySchemaNameAndName(
            session.getProgressMonitor(), db2Schema.getDataSource(), ownerSchemaName, ownerTableName);
        if (ownerTable == null) {
            log.error("Cannot find reference owner table " + ownerSchemaName + "." + ownerTableName);
            return null;
        }

        return new DB2TableReference(session.getProgressMonitor(), ownerTable, dbResult);
    }

    @Nullable
    @Override
    protected DB2TableKeyColumn[] fetchObjectRow(
        @NotNull JDBCSession session, @NotNull DB2Table db2Table,
        @NotNull DB2TableReference db2TableReference, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
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
    protected void cacheChildren(@NotNull DBRProgressMonitor monitor, @NotNull DB2TableReference constraint, @NotNull List<DB2TableKeyColumn> rows)
    {
        constraint.setAttributeReferences(rows);
    }
}
