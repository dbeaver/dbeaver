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
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableColumn;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCStatementImpl;

import java.sql.SQLException;

public final class ExasolTableCache
		extends JDBCStructCache<ExasolSchema, ExasolTable, ExasolTableColumn> {

	private static final String SQL_COLS_TAB = "select " + "	c.* " + "from "
			+ "		 \"$ODBCJDBC\".\"ALL_COLUMNS\" c " + "where "
			+ "	table_schem = '%s' and " + "	table_name = '%s' " + "order by "
			+ "	c.ordinal_position";
	private static final String SQL_COLS_ALL = "select " + "	c.* " + "from "
			+ "		 \"$ODBCJDBC\".\"ALL_COLUMNS\" c " + "where "
			+ "	table_schem = '%s'" + "order by "
			+ "	table_name,c.ordinal_position";
	
	private static final String SQL_TABLES = "SELECT * FROM \"$ODBCJDBC\".ALL_TABLES WHERE TABLE_SCHEM = '%s' and TABLE_TYPE = 'TABLE' order by TABLE_NAME";

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
		
		String sql = String.format(SQL_TABLES, exasolSchema.getName());
		
		JDBCStatement dbstat = session.createStatement();
		
		((JDBCStatementImpl) dbstat).setQueryString(sql);

		return dbstat;

	}

	@SuppressWarnings("rawtypes")
	@Override
	protected JDBCStatement prepareChildrenStatement(
			@NotNull JDBCSession session, @NotNull ExasolSchema exasolSchema,
			@Nullable ExasolTable exasolTable) throws SQLException
	{
		String sql;

		if (exasolTable != null)
			sql = String.format(SQL_COLS_TAB,ExasolUtils.quoteString(exasolSchema.getName()),ExasolUtils.quoteString(exasolTable.getName()));
		else
			sql = String.format(SQL_COLS_ALL,ExasolUtils.quoteString(exasolSchema.getName()));

		JDBCStatement dbstat = session.createStatement();
		
		((JDBCStatementImpl) dbstat).setQueryString(sql);

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
