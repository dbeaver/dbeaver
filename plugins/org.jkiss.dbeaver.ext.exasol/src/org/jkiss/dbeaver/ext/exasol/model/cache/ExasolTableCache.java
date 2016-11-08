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
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableColumn;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;

import java.sql.SQLException;

/**
 * @author Karl
 */
public final class ExasolTableCache extends JDBCStructCache<ExasolSchema, ExasolTable, ExasolTableColumn> {

    private static final String SQL_TABS =
        "select * from ("
            + "select" +
            "	table_schema," +
            "	table_name," +
            "	table_owner," +
            "	table_has_distribution_key," +
            "	table_comment," +
            "	delete_percentage," +
            "	o.created," +
            "	o.last_commit," +
            "	s.raw_object_size," +
            "	s.mem_object_size," +
            "   s.object_type" +
            " from" +
            "		EXA_ALL_OBJECTS o" +
            "	inner join" +
            "		EXA_ALL_TABLES T" +
            "	on" +
            "		o.root_name = t.table_schema and" +
            "		t.table_name = o.object_name and" +
            "		o.object_type = 'TABLE'" +
            "	inner join " +
            "		EXA_ALL_OBJECT_SIZES s" +
            "	on" +
            "		o.root_name = s.root_name and" +
            "		o.object_name = s.object_name and" +
            "		o.object_type = s.object_type" +
            "   where o.root_name = ? and t.table_schema = ?" +
            " union all "
            + " select schema_name as table_schema,"
            + " object_name as table_name,"
            + " 'SYS' as table_owner,"
            + " false as table_has_distribution_key,"
            + " object_comment as table_comment,"
            + " 0 as delete_percentage,"
            + " cast( null as timestamp) as created,"
            + " cast( null as timestamp) as last_commit,"
            + " 0 as raw_object_size,"
            + " 0 as mem_object_size,"
            + " object_type"
            + " from SYS.EXA_SYSCAT WHERE object_type = 'TABLE' and schema_name = ?"
            + ") as o"
            + "	order by table_schema,o.table_name";
    // TODO: change to "$ODBCJDBC"."ALL_COLUMNS"
    private static final String SQL_COLS_TAB =
        "select\r\n" +
            "	c.*,\r\n" +
            "	cc.ordinal_position  as KEY_SEQ\r\n" +
            "from\r\n" +
            "		 \"$ODBCJDBC\".\"ALL_COLUMNS\" c\r\n" +
            "	left outer join\r\n" +
            "		EXA_ALL_CONSTRAINT_COLUMNS cc\r\n" +
            "	on\r\n" +
            "		c.table_name = cc.constraint_table and\r\n" +
            "       c.table_schem = cc.constraint_schema and\r\n " + 
            "		cc.constraint_type = 'PRIMARY KEY' and\r\n" +
            "		cc.COLUMN_NAME = c.COLUMN_NAME\r\n" +
            "where\r\n" +
            "	table_schem = ? and\r\n" +
            "	table_name = ?\r\n" +
            "order by\r\n" +
            "	c.ordinal_position";
    // TODO: change to "$ODBCJDBC"."ALL_COLUMNS"
    private static final String SQL_COLS_ALL = "select\r\n" +
        "	c.*,\r\n" +
        "	cc.ordinal_position  as KEY_SEQ\r\n" +
        "from\r\n" +
        "		 \"$ODBCJDBC\".\"ALL_COLUMNS\" c\r\n" +
        "	left outer join\r\n" +
        "		EXA_ALL_CONSTRAINT_COLUMNS cc\r\n" +
        "	on\r\n" +
        "		c.table_name = cc.constraint_table and\r\n" +
        "       c.table_schem = cc.constraint_schema and\r\n " + 
        "		cc.constraint_type = 'PRIMARY KEY' and\r\n" +
        "		cc.COLUMN_NAME = c.COLUMN_NAME\r\n" +
        "where\r\n" +
        "	table_schem = ? and\r\n" +
        "order by\r\n" +
        "	table_name,c.ordinal_position";

    public ExasolTableCache() {
        super("TABLE_NAME");
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull ExasolSchema exasolSchema) throws SQLException {
        final JDBCPreparedStatement dbstat = session.prepareStatement(SQL_TABS);
        dbstat.setString(1, exasolSchema.getName());
        dbstat.setString(2, exasolSchema.getName());
        dbstat.setString(3, exasolSchema.getName());
        return dbstat;
    }

    @Override
    protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull ExasolSchema exasolSchema, @Nullable ExasolTable exasolTable)
        throws SQLException {
        String sql;

        if (exasolTable != null)
            sql = SQL_COLS_TAB;
        else
            sql = SQL_COLS_ALL;

        JDBCPreparedStatement dbstat = session.prepareStatement(sql);
        dbstat.setString(1, exasolSchema.getName());
        if (exasolTable != null)
            dbstat.setString(2, exasolTable.getName());

        return dbstat;
    }

    @Override
    protected ExasolTableColumn fetchChild(@NotNull JDBCSession session, @NotNull ExasolSchema owner, @NotNull ExasolTable parent,
                                           JDBCResultSet dbResult) throws SQLException, DBException {
        return new ExasolTableColumn(session.getProgressMonitor(), parent, dbResult);
    }


    @Override
    protected ExasolTable fetchObject(@NotNull JDBCSession session, @NotNull ExasolSchema owner, @NotNull JDBCResultSet resultSet)
        throws SQLException, DBException {
        return new ExasolTable(session.getProgressMonitor(), owner, resultSet);
    }

}
