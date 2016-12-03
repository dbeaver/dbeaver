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
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;

import java.sql.SQLException;

/**
 * @author Karl
 */
public final class ExasolTableCache
		extends JDBCStructCache<ExasolSchema, ExasolTable, ExasolTableColumn> {

	private static final String SQL_COLS_TAB = "select " + "	c.* " + "from "
			+ "		 \"$ODBCJDBC\".\"ALL_COLUMNS\" c " + "where "
			+ "	table_schem = ? and " + "	table_name = ? " + "order by "
			+ "	c.ordinal_position";
	private static final String SQL_COLS_ALL = "select " + "	c.* " + "from "
			+ "		 \"$ODBCJDBC\".\"ALL_COLUMNS\" c " + "where "
			+ "	table_schem = ?" + "order by "
			+ "	table_name,c.ordinal_position";

	public ExasolTableCache()
	{
		super("TABLE_NAME");
	}

	@Override
	protected JDBCStatement prepareObjectsStatement(
			@NotNull JDBCSession session, @NotNull ExasolSchema exasolSchema)
			throws SQLException
	{
		JDBCDatabaseMetaData meta = session.getMetaData();

		return meta.getTables("EXA_DB", exasolSchema.getName(), null,
				new String[] { "TABLE" }).getSourceStatement();
	}

	@Override
	protected JDBCStatement prepareChildrenStatement(
			@NotNull JDBCSession session, @NotNull ExasolSchema exasolSchema,
			@Nullable ExasolTable exasolTable) throws SQLException
	{
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
	protected ExasolTableColumn fetchChild(@NotNull JDBCSession session,
			@NotNull ExasolSchema owner, @NotNull ExasolTable parent,
			JDBCResultSet dbResult) throws SQLException, DBException
	{
		return new ExasolTableColumn(session.getProgressMonitor(), parent,
				dbResult);
	}

	@Override
	protected ExasolTable fetchObject(@NotNull JDBCSession session,
			@NotNull ExasolSchema owner, @NotNull JDBCResultSet resultSet)
			throws SQLException, DBException
	{
		return new ExasolTable(session.getProgressMonitor(), owner, resultSet);
	}

}
