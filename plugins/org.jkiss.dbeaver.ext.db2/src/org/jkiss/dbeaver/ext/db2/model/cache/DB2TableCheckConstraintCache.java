/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableCheckConstraint;
import org.jkiss.dbeaver.ext.db2.model.DB2TableCheckConstraintColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableCheckConstraintColUsage;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.List;

/**
 * Cache for DB2 Table Check Constraints
 * 
 * @author Denis Forveille
 */
public final class DB2TableCheckConstraintCache extends
    JDBCCompositeCache<DB2Schema, DB2Table, DB2TableCheckConstraint, DB2TableCheckConstraintColumn> {

    private static final String SQL_CK_TAB;
    private static final String SQL_CK_ALL;

    static {
        StringBuilder sb = new StringBuilder(512);
        sb.append(" SELECT C.*");
        sb.append("      , CK.COLNAME");
        sb.append("      , CK.USAGE");
        sb.append("   FROM SYSCAT.CHECKS C");
        sb.append("       ,SYSCAT.COLCHECKS  CK");
        sb.append("  WHERE C.TABSCHEMA = ?");
        sb.append("    AND C.TABNAME = ?");
        sb.append("    AND CK.CONSTNAME = C.CONSTNAME");
        sb.append("    AND CK.TABSCHEMA = C.TABSCHEMA");
        sb.append("    AND CK.TABNAME   = C.TABNAME");
        sb.append("  ORDER BY CK.COLNAME");
        sb.append(" WITH UR");
        SQL_CK_TAB = sb.toString();

        sb.setLength(0);

        sb.append(" SELECT C.*");
        sb.append("      , CK.COLNAME");
        sb.append("      , CK.USAGE");
        sb.append("   FROM SYSCAT.CHECKS C");
        sb.append("       ,SYSCAT.COLCHECKS  CK");
        sb.append("  WHERE C.TABSCHEMA = ?");
        sb.append("    AND CK.CONSTNAME = C.CONSTNAME");
        sb.append("    AND CK.TABSCHEMA = C.TABSCHEMA");
        sb.append("    AND CK.TABNAME   = C.TABNAME");
        sb.append("  ORDER BY CK.COLNAME");
        sb.append(" WITH UR");
        SQL_CK_ALL = sb.toString();
    }

    public DB2TableCheckConstraintCache(DB2TableCache tableCache)
    {
        super(tableCache, DB2Table.class, "TABNAME", "CONSTNAME");
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, DB2Schema db2Schema, DB2Table forTable)
        throws SQLException
    {
        String sql;
        if (forTable != null) {
            sql = SQL_CK_TAB;
        } else {
            sql = SQL_CK_ALL;
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
    protected DB2TableCheckConstraint fetchObject(JDBCSession session, DB2Schema db2Schema, DB2Table db2Table,
        String indexName, JDBCResultSet dbResult) throws SQLException, DBException
    {

        return new DB2TableCheckConstraint(session.getProgressMonitor(), db2Table, dbResult);
    }

    @Nullable
    @Override
    protected DB2TableCheckConstraintColumn[] fetchObjectRow(JDBCSession session, DB2Table db2Table,
                                                             DB2TableCheckConstraint object, JDBCResultSet dbResult) throws SQLException, DBException
    {

        String colName = JDBCUtils.safeGetString(dbResult, "COLNAME");
        DB2TableColumn tableColumn = db2Table.getAttribute(session.getProgressMonitor(), colName);
        DB2TableCheckConstraintColUsage usage = CommonUtils.valueOf(DB2TableCheckConstraintColUsage.class,
            JDBCUtils.safeGetString(dbResult, "USAGE"));
        if (tableColumn == null) {
            log.debug("Column '" + colName + "' not found in table '" + db2Table.getFullyQualifiedName(DBPEvaluationContext.UI) + "' ??");
            return null;
        } else {
            return new DB2TableCheckConstraintColumn[] { new DB2TableCheckConstraintColumn(object, tableColumn, usage) };
        }
    }

    @Override
    protected void cacheChildren(DBRProgressMonitor monitor, DB2TableCheckConstraint constraint, List<DB2TableCheckConstraintColumn> rows)
    {
        constraint.setColumns(rows);
    }
}
