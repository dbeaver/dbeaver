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
package org.jkiss.dbeaver.ext.exasol.model.cache;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.*;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.List;

/**
 * @author Karl
 */
public final class ExasolTableForeignKeyCache
    extends JDBCCompositeCache<ExasolSchema, ExasolTable, ExasolTableForeignKey, ExasolTableForeignKeyColumn> {

    private static final String SQL_FK_TAB =
        "/*snapshot execution*/ select\r\n" +
            "		CONSTRAINT_NAME,CONSTRAINT_TABLE,CONSTRAINT_SCHEMA,constraint_owner,c.constraint_enabled,constraint_Type," +
            "cc.column_name,cc.ordinal_position,cc.referenced_schema,cc.referenced_table,cc.referenced_column" +
            "	from\r\n" +
            "		(SELECT * FROM 	EXA_ALL_CONSTRAINTS " +
            "			where\r\n" +
            "				CONSTRAINT_SCHEMA = '%s' and\r\n" +
            "				CONSTRAINT_TYPE = 'FOREIGN KEY' AND CONSTRAINT_TABLE = '%s' \r\n" +
            "        ORDER BY 1,2,3 \r\n" +
            "        )c\r\n" +
            "		inner join\r\n" +
            "		(SELECT * FROM EXA_ALL_CONSTRAINT_COLUMNS " +
            "			where\r\n" +
            "				CONSTRAINT_SCHEMA = '%s' and\r\n" +
            "				CONSTRAINT_TYPE = 'FOREIGN KEY' AND CONSTRAINT_TABLE = '%s' \r\n" +
            "        ORDER BY 1,2,3 \r\n" +
            " 		) cc\r\n" +
            "	using\r\n" +
            "			(\r\n" +
            "				CONSTRAINT_SCHEMA, CONSTRAINT_TABLE, CONSTRAINT_NAME, CONSTRAINT_OWNER, CONSTRAINT_TYPE\r\n" +
            "			)\r\n" +
            "	where\r\n" +
            "		CONSTRAINT_SCHEMA = '%s' and\r\n" +
            "		CONSTRAINT_TYPE = 'FOREIGN KEY' AND CONSTRAINT_TABLE = '%s' \r\n" +
            "	order by\r\n" +
            "		ORDINAL_POSITION";
    private static final String SQL_FK_ALL =
        "/*snapshot execution*/ select\r\n" +
            "		CONSTRAINT_NAME,CONSTRAINT_TABLE,CONSTRAINT_SCHEMA,constraint_owner,c.constraint_enabled,constraint_Type," +
            "cc.column_name,cc.ordinal_position,cc.referenced_schema,cc.referenced_table,cc.referenced_column" +
            "	from\r\n" +
            "		(SELECT * FROM 	EXA_ALL_CONSTRAINTS " +
            "			where\r\n" +
            "				CONSTRAINT_SCHEMA = '%s' and\r\n" +
            "				CONSTRAINT_TYPE = 'FOREIGN KEY'\r\n" +
            "        ORDER BY 1,2,3 \r\n" +
            "        )c\r\n" +
            "		inner join\r\n" +
            "		(SELECT * FROM EXA_ALL_CONSTRAINT_COLUMNS " +
            "			where\r\n" +
            "				CONSTRAINT_SCHEMA = '%s' and\r\n" +
            "				CONSTRAINT_TYPE = 'FOREIGN KEY'  \r\n" +
            "        ORDER BY 1,2,3 \r\n" +
            " 		) cc\r\n" +
            "	using\r\n" +
            "			(\r\n" +
            "				CONSTRAINT_SCHEMA, CONSTRAINT_TABLE, CONSTRAINT_NAME, CONSTRAINT_OWNER, CONSTRAINT_TYPE\r\n" +
            "			)\r\n" +
            "	where\r\n" +
            "		CONSTRAINT_SCHEMA = '%s' and\r\n" +
            "		CONSTRAINT_TYPE = 'FOREIGN KEY' \r\n" +
            "	order by\r\n" +
            "		ORDINAL_POSITION";

    public ExasolTableForeignKeyCache(ExasolTableCache tableCache) {
        super(tableCache, ExasolTable.class, "CONSTRAINT_TABLE", "CONSTRAINT_NAME");
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(
        @NotNull JDBCSession session,
        @NotNull ExasolSchema exasolSchema,
        @Nullable ExasolTable forTable
    ) throws SQLException {
        String sql;
        if (forTable != null) {
            sql = String.format(SQL_FK_TAB,
                ExasolUtils.quoteString(exasolSchema.getName()),
                ExasolUtils.quoteString(forTable.getName()),
                ExasolUtils.quoteString(exasolSchema.getName()),
                ExasolUtils.quoteString(forTable.getName()),
                ExasolUtils.quoteString(exasolSchema.getName()),
                ExasolUtils.quoteString(forTable.getName()));
        } else {
            sql = String.format(SQL_FK_ALL,
                ExasolUtils.quoteString(exasolSchema.getName()),
                ExasolUtils.quoteString(exasolSchema.getName()),
                ExasolUtils.quoteString(exasolSchema.getName()));
        }
        JDBCStatement dbStat = session.createStatement();
        
        dbStat.setQueryString(sql);
        
        return dbStat;

    }

    @NotNull
    @Override
    protected ExasolTableForeignKey fetchObject(
        @NotNull JDBCSession session,
        @NotNull ExasolSchema ExasolSchema,
        @NotNull ExasolTable ExasolTable,
        @NotNull String constName,
        @NotNull JDBCResultSet dbResult
    ) throws DBException {
        return new ExasolTableForeignKey(session.getProgressMonitor(), ExasolTable, dbResult);
    }

    @Nullable
    @Override
    protected ExasolTableForeignKeyColumn[] fetchObjectRow(
        @NotNull JDBCSession session,
        @NotNull ExasolTable table,
        @NotNull ExasolTableForeignKey object,
        @NotNull JDBCResultSet dbResult
    ) throws DBException {
        String colName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
        ExasolTableColumn tableColumn = table.getAttribute(session.getProgressMonitor(), colName);
        if (tableColumn == null) {
            log.error("ExasolTableForeignKeyCache : Column '" + colName + "' not found in table '" + table.getFullyQualifiedName(DBPEvaluationContext.UI) + "' ??");
            return null;
        }
        ExasolTable refTable = object.getReferencedConstraint() == null ? null : object.getReferencedConstraint().getTable();
        if (refTable == null) {
            log.error("ExasolTableForeignKeyCache : RefTable not found for FK '" + object.getFullyQualifiedName(DBPEvaluationContext.UI) + "' ??");
            return null;
        }
        String refColName = JDBCUtils.safeGetString(dbResult, "REFERENCED_COLUMN");
        ExasolTableColumn refColumn = refTable.getAttribute(session.getProgressMonitor(), refColName);
        if (refColumn == null) {
            log.error("ExasolTableForeignKeyCache : RefColumn '" + refColName + "' not found in table '" + table.getFullyQualifiedName(DBPEvaluationContext.UI) + "' ??");
            return null;
        }

        return new ExasolTableForeignKeyColumn[]{
            new ExasolTableForeignKeyColumn(
                object,
                tableColumn,
                refColumn,
                JDBCUtils.safeGetInt(dbResult, "ORDINAL_POSITION"))
        };
    }

    @Override
    protected void cacheChildren(@NotNull DBRProgressMonitor monitor, @NotNull ExasolTableForeignKey constraint, @NotNull List<ExasolTableForeignKeyColumn> rows) {
        constraint.setAttributeReferences(rows);
    }


}
