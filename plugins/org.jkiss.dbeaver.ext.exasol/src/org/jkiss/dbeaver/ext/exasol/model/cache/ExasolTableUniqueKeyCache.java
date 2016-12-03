/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model.cache;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

import java.sql.SQLException;
import java.util.List;

public final class ExasolTableUniqueKeyCache
    extends JDBCCompositeCache<ExasolSchema, ExasolTable, ExasolTableUniqueKey, ExasolTableKeyColumn> {

    private static final String SQL_UK_TAB =
        "	select\r\n" +
            "		*\r\n" +
            "	from\r\n" +
            "			EXA_ALL_CONSTRAINTS c\r\n" +
            "		inner join\r\n" +
            "			EXA_ALL_CONSTRAINT_COLUMNS\r\n" +
            "	using\r\n" +
            "			(\r\n" +
            "				CONSTRAINT_SCHEMA, CONSTRAINT_TABLE, CONSTRAINT_NAME, CONSTRAINT_OWNER, CONSTRAINT_TYPE\r\n" +
            "			)\r\n" +
            "	where\r\n" +
            "		CONSTRAINT_SCHEMA = ? and\r\n" +
            "		CONSTRAINT_TYPE = 'PRIMARY KEY' and\r\n" +
            "		CONSTRAINT_TABLE = ?\r\n" +
            "	order by\r\n" +
            "		ORDINAL_POSITION";
    private static final String SQL_UK_ALL =
        "	select\r\n" +
            "		*\r\n" +
            "	from\r\n" +
            "			EXA_ALL_CONSTRAINTS c\r\n" +
            "		inner join\r\n" +
            "			EXA_ALL_CONSTRAINT_COLUMNS\r\n" +
            "	using\r\n" +
            "			(\r\n" +
            "				CONSTRAINT_SCHEMA, CONSTRAINT_TABLE, CONSTRAINT_NAME, CONSTRAINT_OWNER, CONSTRAINT_TYPE\r\n" +
            "			)\r\n" +
            "	where\r\n" +
            "		CONSTRAINT_SCHEMA = ? and\r\n" +
            "		CONSTRAINT_TYPE = 'PRIMARY KEY' \r\n" +
            "	order by\r\n" +
            "		ORDINAL_POSITION";

    public ExasolTableUniqueKeyCache(ExasolTableCache tableCache) {
        super(tableCache, ExasolTable.class, "CONSTRAINT_TABLE", "CONSTRAINT_NAME");
    }


    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, ExasolSchema exasolSchema, ExasolTable forTable)
        throws SQLException {

        String sql;
        if (forTable != null) {
            sql = SQL_UK_TAB;
        } else {
            sql = SQL_UK_ALL;
        }

        JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, exasolSchema.getName());
        if (forTable != null)
            dbStat.setString(2, forTable.getName());

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
            log.debug("Column '" + columnName + "' not found in table '" + exasolTable.getFullyQualifiedName(DBPEvaluationContext.UI) + "' ??");
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
