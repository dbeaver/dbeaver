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
            "cc.column_name,cc.ordinal_position,cc.referenced_schema,cc.referenced_table,cc.referenced_column," +
            "PK.CONSTRAINT_NAME AS REF_PK_NAME\r\n" +
            "	from\r\n" +
            "			EXA_ALL_CONSTRAINTS c\r\n" +
            "		inner join\r\n" +
            "			EXA_ALL_CONSTRAINT_COLUMNS cc\r\n" +
            "	using\r\n" +
            "			(\r\n" +
            "				CONSTRAINT_SCHEMA, CONSTRAINT_TABLE, CONSTRAINT_NAME, CONSTRAINT_OWNER, CONSTRAINT_TYPE\r\n" +
            "			)\r\n" +
            "		inner join \r\n" +
            "			EXA_ALL_CONSTRAINTS pk\r\n" +
            "    on REFERENCED_SCHEMA = PK.CONSTRAINT_SCHEMA AND REFERENCED_TABLE = PK.CONSTRAINT_TABLE AND PK.CONSTRAINT_TYPE = 'PRIMARY KEY'\r\n" +
            "	where\r\n" +
            "		CONSTRAINT_SCHEMA = ? and\r\n" +
            "		CONSTRAINT_TYPE = 'FOREIGN KEY'AND CONSTRAINT_TABLE = ? \r\n" +
            "	order by\r\n" +
            "		ORDINAL_POSITION";
    private static final String SQL_FK_ALL =
        "select\r\n" +
            "		CONSTRAINT_NAME,CONSTRAINT_TABLE,CONSTRAINT_SCHEMA,constraint_owner,c.constraint_enabled,constraint_Type," +
            "cc.column_name,cc.ordinal_position,cc.referenced_schema,cc.referenced_table,cc.referenced_column," +
            "PK.CONSTRAINT_NAME AS REF_PK_NAME\r\n" +
            "	from\r\n" +
            "			EXA_ALL_CONSTRAINTS c\r\n" +
            "		inner join\r\n" +
            "			EXA_ALL_CONSTRAINT_COLUMNS cc\r\n" +
            "	using\r\n" +
            "			(\r\n" +
            "				CONSTRAINT_SCHEMA, CONSTRAINT_TABLE, CONSTRAINT_NAME, CONSTRAINT_OWNER, CONSTRAINT_TYPE\r\n" +
            "			)\r\n" +
            "		inner join \r\n" +
            "			EXA_ALL_CONSTRAINTS pk\r\n" +
            "    on REFERENCED_SCHEMA = PK.CONSTRAINT_SCHEMA AND REFERENCED_TABLE = PK.CONSTRAINT_TABLE AND PK.CONSTRAINT_TYPE = 'PRIMARY KEY'\r\n" +
            "	where\r\n" +
            "		CONSTRAINT_SCHEMA = ? and\r\n" +
            "		CONSTRAINT_TYPE = 'FOREIGN KEY' \r\n" +
            "	order by\r\n" +
            "		ORDINAL_POSITION";

    public ExasolTableForeignKeyCache(ExasolTableCache tableCache) {
        super(tableCache, ExasolTable.class, "CONSTRAINT_TABLE", "CONSTRAINT_NAME");
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, ExasolSchema exasolSchema, ExasolTable forTable)
        throws SQLException {
        String sql;
        if (forTable != null) {
            sql = SQL_FK_TAB;
        } else {
            sql = SQL_FK_ALL;
        }
        JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, exasolSchema.getName());
        if (forTable != null) {
            dbStat.setString(2, forTable.getName());
        }
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
            log.debug("ExasolTableForeignKeyCache : Column '" + colName + "' not found in table '" + ExasolTable.getFullyQualifiedName(DBPEvaluationContext.UI)
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
