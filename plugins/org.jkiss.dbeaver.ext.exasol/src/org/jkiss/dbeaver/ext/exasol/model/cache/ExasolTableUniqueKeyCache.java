/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

import java.sql.SQLException;
import java.util.List;

public final class ExasolTableUniqueKeyCache
    extends JDBCCompositeCache<ExasolSchema, ExasolTable, ExasolTableUniqueKey, ExasolTableKeyColumn> {

    private static final String SQL_UK_TAB =
        "SELECT\r\n" + 
        "	C.CONSTRAINT_SCHEMA,\r\n" + 
        "	c.CONSTRAINT_TABLE,\r\n" + 
        "	c.CONSTRAINT_NAME,\r\n" + 
        "	c.CONSTRAINT_OWNER,\r\n" + 
        "	c.CONSTRAINT_TYPE,\r\n" + 
        "	c.CONSTRAINT_ENABLED,\r\n" + 
        "	cc.ORDINAL_POSITION,\r\n" + 
        "	cc.COLUMN_NAME,\r\n" + 
        "	cc.REFERENCED_COLUMN,\r\n" + 
        "	cc.REFERENCED_SCHEMA,\r\n" + 
        "	cc.REFERENCED_TABLE\r\n" + 
        "FROM\r\n" + 
        "	(SELECT * FROM SYS.EXA_ALL_CONSTRAINTS c" +
        "	WHERE\r\n" + 
        "		c.CONSTRAINT_SCHEMA = '%s'\r\n" + 
        "		AND c.CONSTRAINT_TYPE = 'PRIMARY KEY'\r\n" + 
        "		AND c.CONSTRAINT_TABLE = '%s' order by 1,2,3,4\r\n" + 
        "	) c\r\n" + 
        "INNER JOIN (SELECT * FROM SYS.EXA_ALL_CONSTRAINT_COLUMNS c " +
        "	WHERE\r\n" + 
        "		c.CONSTRAINT_SCHEMA = '%s'\r\n" + 
        "		AND c.CONSTRAINT_TYPE = 'PRIMARY KEY'\r\n" + 
        "		AND c.CONSTRAINT_TABLE = '%s' order by 1,2,3,4\r\n" + 
        "	)cc ON\r\n" + 
        "	(\r\n" + 
        "		c.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA\r\n" + 
        "		AND c.CONSTRAINT_TABLE = cc.CONSTRAINT_TABLE\r\n" + 
        "		AND c.CONSTRAINT_NAME = cc.CONSTRAINT_NAME\r\n" + 
        "		AND c.CONSTRAINT_OWNER = cc.CONSTRAINT_OWNER\r\n" + 
        "		AND c.CONSTRAINT_TYPE = cc.CONSTRAINT_TYPE\r\n" + 
        "	)\r\n" + 
        "ORDER BY\r\n" + 
        "	ORDINAL_POSITION";
    
    private static final String SQL_UK_ALL =
    		"SELECT\r\n" + 
    		"	C.CONSTRAINT_SCHEMA,\r\n" + 
    		"	c.CONSTRAINT_TABLE,\r\n" + 
    		"	c.CONSTRAINT_NAME,\r\n" + 
    		"	c.CONSTRAINT_OWNER,\r\n" + 
    		"	c.CONSTRAINT_TYPE,\r\n" + 
    		"	c.CONSTRAINT_ENABLED,\r\n" + 
    		"	cc.ORDINAL_POSITION,\r\n" + 
    		"	cc.COLUMN_NAME,\r\n" + 
    		"	cc.REFERENCED_COLUMN,\r\n" + 
    		"	cc.REFERENCED_SCHEMA,\r\n" + 
    		"	cc.REFERENCED_TABLE\r\n" + 
            "FROM\r\n" + 
            "	(SELECT * FROM SYS.EXA_ALL_CONSTRAINTS c" +
            "	WHERE\r\n" + 
            "		c.CONSTRAINT_SCHEMA = '%s'\r\n" + 
            "		AND c.CONSTRAINT_TYPE = 'PRIMARY KEY'\r\n" + 
            "		order by 1,2,3,4\r\n" + 
            "	) c\r\n" + 
            "INNER JOIN (SELECT * FROM SYS.EXA_ALL_CONSTRAINT_COLUMNS c " +
            "	WHERE\r\n" + 
            "		c.CONSTRAINT_SCHEMA = '%s'\r\n" + 
            "		AND c.CONSTRAINT_TYPE = 'PRIMARY KEY'\r\n" + 
            "		order by 1,2,3,4\r\n" + 
            "	)cc ON\r\n" + 
            "	(\r\n" + 
            "		c.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA\r\n" + 
            "		AND c.CONSTRAINT_TABLE = cc.CONSTRAINT_TABLE\r\n" + 
            "		AND c.CONSTRAINT_NAME = cc.CONSTRAINT_NAME\r\n" + 
            "		AND c.CONSTRAINT_OWNER = cc.CONSTRAINT_OWNER\r\n" + 
            "		AND c.CONSTRAINT_TYPE = cc.CONSTRAINT_TYPE\r\n" + 
            "	)\r\n" + 
            "ORDER BY\r\n" + 
            "	ORDINAL_POSITION";
    		;		

    public ExasolTableUniqueKeyCache(ExasolTableCache tableCache) {
        super(tableCache, ExasolTable.class, "CONSTRAINT_TABLE", "CONSTRAINT_NAME");
    }


    @SuppressWarnings("rawtypes")
	@NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, ExasolSchema exasolSchema, ExasolTable forTable)
        throws SQLException {

        String sql;
        if (forTable != null) {
            sql = String.format(SQL_UK_TAB,ExasolUtils.quoteString(exasolSchema.getName()), ExasolUtils.quoteString(forTable.getName()),ExasolUtils.quoteString(exasolSchema.getName()), ExasolUtils.quoteString(forTable.getName()));
        } else {
            sql = String.format(SQL_UK_ALL,ExasolUtils.quoteString(exasolSchema.getName()),ExasolUtils.quoteString(exasolSchema.getName()));
        }

        JDBCStatement dbStat = session.createStatement();
        
        ((JDBCStatementImpl) dbStat).setQueryString(sql);

        return dbStat;

    }

    @Nullable
    @Override
    protected ExasolTableUniqueKey fetchObject(JDBCSession session, ExasolSchema exasolSchema, ExasolTable exasolTable, String constName, JDBCResultSet dbResult) throws SQLException, DBException {
        //SQLs only return primary keys. no unique constraints in exasol
        DBSEntityConstraintType type = DBSEntityConstraintType.PRIMARY_KEY;
        return new ExasolTableUniqueKey(session.getProgressMonitor(), exasolTable, dbResult, type);
    }

    @Nullable
    @Override
    protected ExasolTableKeyColumn[] fetchObjectRow(JDBCSession session, ExasolTable exasolTable, ExasolTableUniqueKey object, JDBCResultSet dbResult) throws SQLException, DBException {
        String columnName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
        ExasolTableColumn tableColumn = exasolTable.getAttribute(session.getProgressMonitor(), columnName);
        if (tableColumn == null) {
            log.info("Column '" + columnName + "' not found in table '" + exasolTable.getFullyQualifiedName(DBPEvaluationContext.UI) + "' ??");
            return null;
        } else {
            /* verify that the column is not null -> even though it is not in the meta data
             * Exasol always verify not null for columns in a PK
             * this is necessary for the automatic unique identifiers detection to work
             */
            tableColumn.setRequired(true);
            return new ExasolTableKeyColumn[]{
                new ExasolTableKeyColumn(object, tableColumn, JDBCUtils.safeGetInteger(dbResult, "ORDINAL_POSITION"))
            };
        }

    }

    @Override
    protected void cacheChildren(DBRProgressMonitor monitor, ExasolTableUniqueKey constraint, List<ExasolTableKeyColumn> rows) {
        constraint.setColumns(rows);
    }


}
