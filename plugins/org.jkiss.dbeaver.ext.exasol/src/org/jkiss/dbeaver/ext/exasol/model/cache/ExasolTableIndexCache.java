/*
 * DBeaver - Universal Database Manager
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.ExasolSysTablePrefix;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableColumn;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableIndex;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableIndexColumn;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class ExasolTableIndexCache extends JDBCCompositeCache<ExasolSchema, ExasolTable, ExasolTableIndex, ExasolTableIndexColumn> {


	private static final String QUERYINDEX = "/*snapshot execution*/ SELECT * FROM SYS.%s_INDICES WHERE INDEX_SCHEMA = '%s'";
	private static final Pattern indexCols = Pattern.compile(".+\\((.+)\\)");

	public ExasolTableIndexCache(ExasolTableCache parentCache) {
		super(parentCache, ExasolTable.class, "INDEX_TABLE", "REMARKS");
	}


	@Override
	protected JDBCStatement prepareObjectsStatement(JDBCSession session, ExasolSchema schema, ExasolTable table)
			throws SQLException {
	String tablePrefix = schema.getDataSource().getTablePrefix(ExasolSysTablePrefix.ALL);
		
	
		StringBuilder sql = new StringBuilder(
				String.format(
							QUERYINDEX, 
							tablePrefix,
							ExasolUtils.quoteString(schema.getName())
						)
				);
		
		// table provided - append filter
		if (table != null) {
			sql.append(
					String.format(" AND INDEX_TABLE = '%s'", ExasolUtils.quoteString(table.getName()))
			);
		}
		
		
		JDBCStatement dbstat =  session.createStatement();
		
		dbstat.setQueryString(sql.toString());
		
		return dbstat;
	}


	@Override
	protected ExasolTableIndex fetchObject(JDBCSession session, ExasolSchema schema, ExasolTable table,
			String indexName, JDBCResultSet dbResult) throws SQLException, DBException {
		return new ExasolTableIndex(session.getProgressMonitor(), table, indexName, dbResult);
	}


	@Override
	protected ExasolTableIndexColumn[] fetchObjectRow(JDBCSession session, ExasolTable parent,
			ExasolTableIndex forObject, JDBCResultSet resultSet) throws SQLException, DBException {
		
		//ToDo: fix regex
		Matcher m = indexCols.matcher(JDBCUtils.safeGetString(resultSet, "REMARKS"));
		
		
		String[] colString;
		ArrayList<ExasolTableIndexColumn> indexCols = new ArrayList<ExasolTableIndexColumn>(); 
		
		if (m.find())
		{
			colString = m.group(1).split(",");
			for (int i = 0; i < colString.length; i++) {
				ExasolTableColumn tableColumn = colString[i] == null ? null : parent.getAttribute(session.getProgressMonitor(), colString[i]); 
				indexCols.add(
						new ExasolTableIndexColumn(forObject, tableColumn, i+1)
						);
				
			}
		}
		
		
		ExasolTableIndexColumn[] arrayIndexCols = new ExasolTableIndexColumn[indexCols.size()];
		arrayIndexCols = indexCols.toArray(arrayIndexCols);
		return arrayIndexCols;
		
	}


	@Override
	protected void cacheChildren(DBRProgressMonitor monitor, ExasolTableIndex index,List<ExasolTableIndexColumn> cols) {
		index.setColumns(cols);
		
	}


}
