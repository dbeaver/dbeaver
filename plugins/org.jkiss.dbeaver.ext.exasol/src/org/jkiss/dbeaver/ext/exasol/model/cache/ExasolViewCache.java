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
import org.jkiss.dbeaver.ext.exasol.ExasolSysTablePrefix;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableColumn;
import org.jkiss.dbeaver.ext.exasol.model.ExasolView;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;

import java.sql.SQLException;

/**
 * Cache for Exasol Views
 *
 * @author Karl Griesser
 */
public class ExasolViewCache extends JDBCStructCache<ExasolSchema, ExasolView, ExasolTableColumn> {

	private static final String SQL_COLS_VIEW = "SELECT " + 
			"c.* " + 
			"FROM " + 
			"SYS.%s_COLUMNS c " + 
			"WHERE " + 
			"COLUMN_SCHEMA = '%s' " + 
			"AND COLUMN_TABLE = '%s' " + 
			"ORDER BY " + 
			"COLUMN_ORDINAL_POSITION ";
	private static final String SQL_COLS_ALL = "SELECT " + 
			"c.* " + 
			"FROM " + 
			"SYS.%s_COLUMNS c " + 
			"WHERE " + 
			"COLUMN_SCHEMA = '%s' AND COLUMN_OBJECT_TYPE = 'TABLE' " + 
			"ORDER BY " + 
			"COLUMN_ORDINAL_POSITION ";


    public ExasolViewCache() {
        super("TABLE_NAME");

    }

	@Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull ExasolSchema exasolSchema) throws SQLException {
        JDBCDatabaseMetaData meta = session.getMetaData();

        return meta.getTables("EXA_DB", exasolSchema.getName(), null,
                new String[] { "VIEW", "SYSTEM TABLE" }).getSourceStatement();
    }

    @Override
    protected ExasolView fetchObject(@NotNull JDBCSession session, @NotNull ExasolSchema exasolSchema, @NotNull JDBCResultSet dbResult) throws SQLException,
        DBException {
        return new ExasolView(session.getProgressMonitor(), exasolSchema, dbResult);
    }

	@Override
    protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull ExasolSchema exasolSchema, @Nullable ExasolView forView) throws SQLException {
        String sql;
		String tablePrefix = exasolSchema.getDataSource().getTablePrefix(ExasolSysTablePrefix.ALL);

		if (forView != null) {
            sql = String.format(SQL_COLS_VIEW,tablePrefix, ExasolUtils.quoteString(exasolSchema.getName()), ExasolUtils.quoteString(forView.getName())) ;
        } else {
            sql = String.format(SQL_COLS_ALL,tablePrefix, ExasolUtils.quoteString(exasolSchema.getName()));
        }

        JDBCStatement dbStat = session.createStatement();
        
        dbStat.setQueryString(sql);

        return dbStat;

    }

    @Override
    protected ExasolTableColumn fetchChild(@NotNull JDBCSession session, @NotNull ExasolSchema exasolSchema, @NotNull ExasolView exasolView, @NotNull JDBCResultSet dbResult)
        throws SQLException, DBException {
        return new ExasolTableColumn(session.getProgressMonitor(), exasolView, dbResult);
    }

}
