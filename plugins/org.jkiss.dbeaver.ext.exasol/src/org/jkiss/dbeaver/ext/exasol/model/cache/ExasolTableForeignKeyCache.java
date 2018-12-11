/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCStatementImpl;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.List;

/**
 * @author Karl
 */
public final class ExasolTableForeignKeyCache
    extends JDBCCompositeCache<ExasolSchema, ExasolTable, ExasolTableForeignKey, ExasolTableKeyColumn> {

    private static final String SQL_FK_TAB =
        "select\r\n" +
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
        "select\r\n" +
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
            "        ORDER BY 1,2,3 c\r\n" +
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

    @SuppressWarnings("rawtypes")
	@NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, ExasolSchema exasolSchema, ExasolTable forTable)
        throws SQLException {
        String sql;
        if (forTable != null) {
            sql = String.format(SQL_FK_TAB,ExasolUtils.quoteString(exasolSchema.getName()),ExasolUtils.quoteString(forTable.getName()),ExasolUtils.quoteString(exasolSchema.getName()),ExasolUtils.quoteString(forTable.getName()),ExasolUtils.quoteString(exasolSchema.getName()),ExasolUtils.quoteString(forTable.getName()));
        } else {
            sql = String.format(SQL_FK_ALL,ExasolUtils.quoteString(exasolSchema.getName()),ExasolUtils.quoteString(exasolSchema.getName()),ExasolUtils.quoteString(exasolSchema.getName()));
        }
        JDBCStatement dbStat = session.createStatement();
        
        ((JDBCStatementImpl) dbStat).setQueryString(sql);
        
        return dbStat;

    }

    @Nullable
    @Override
    protected ExasolTableForeignKey fetchObject(JDBCSession session, ExasolSchema ExasolSchema, ExasolTable ExasolTable,
                                                String constName, JDBCResultSet dbResult) throws SQLException, DBException {
        return new ExasolTableForeignKey(session.getProgressMonitor(), ExasolTable, dbResult);
    }

    @Nullable
    @Override
    protected ExasolTableKeyColumn[] fetchObjectRow(JDBCSession session, ExasolTable ExasolTable, ExasolTableForeignKey object,
                                                    JDBCResultSet dbResult) throws SQLException, DBException {

        String colName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
        ExasolTableColumn tableColumn = ExasolTable.getAttribute(session.getProgressMonitor(), colName);
        if (tableColumn == null) {
            log.info("ExasolTableForeignKeyCache : Column '" + colName + "' not found in table '" + ExasolTable.getFullyQualifiedName(DBPEvaluationContext.UI)
                + "' ??");
            return null;
        } else {
            return new ExasolTableKeyColumn[]{
                new ExasolTableKeyColumn(object, tableColumn, JDBCUtils.safeGetInt(dbResult, "ORDINAL_POSITION"))
            };
        }
    }

    @Override
    protected void cacheChildren(DBRProgressMonitor monitor, ExasolTableForeignKey constraint, List<ExasolTableKeyColumn> rows) {
        constraint.setColumns(rows);
    }


}
