/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


public class ExasolTableIndex extends JDBCTableIndex<ExasolSchema, ExasolTable> implements DBPNamedObject {

	private long size;
	private Timestamp createTime;
	private Timestamp lastCommit;
	private ExasolTable table;
	private List<ExasolTableIndexColumn> columns;
	private Boolean isGeometry;
	private DBSIndexType type;
	
	public ExasolTableIndex(ExasolTable table, String indexName, DBSIndexType indexType, boolean persisted) {
		super(table.getContainer(), table, indexName, indexType, persisted);
		this.size = -1;
		this.type = new DBSIndexType("LOCAL","LOCAL");
		this.table = table;
		this.isGeometry = false;
	}
	
	public ExasolTableIndex(DBRProgressMonitor monitor, ExasolTable table, String indexName, ResultSet dbResult)
	{
		super(
				table.getContainer(),
				table,
				indexName,
				new DBSIndexType(JDBCUtils.safeGetString(dbResult, "INDEX_TYPE"), JDBCUtils.safeGetString(dbResult, "INDEX_TYPE")),
				true
				);
		this.size = JDBCUtils.safeGetLong(dbResult, "MEM_OBJECT_SIZE");
		this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");
		this.lastCommit = JDBCUtils.safeGetTimestamp(dbResult, "LAST_COMMIT");
		this.table = table;
		this.type = super.getIndexType();
		this.isGeometry = JDBCUtils.safeGetBoolean(dbResult, "IS_GEOMETRY", false);
		
	}

	@Override
	public ExasolSchema getContainer() {
		return this.getTable().getContainer();
	}

	@Override
	public ExasolTable getTable() {
		return this.table;
	}

	@Override
	public boolean isUnique() {
		// All indices are non unique in exasol
		return false;
	}

	@Override
	public DBSIndexType getIndexType() {
		return DBSIndexType.STATISTIC;
	}
	
	@Property(viewable = true, editable = false, order = 15)
	public String getName()
	{
		return super.getName();
	}

	@Property(viewable = true, editable=false, order = 17)
	public DBSIndexType getType() {
		return type;
	}

	@Property(viewable = true, editable=false, order = 20, formatter = ByteNumberFormat.class)
	public long getSize() {
		return size;
	}


	@Property(viewable = true, editable=false, order = 30)
	public Timestamp getCreateTime() {
		return createTime;
	}

	@Property(viewable = true, editable=false, order = 40)
	public Timestamp getLastCommit() {
		return lastCommit;
	}

	@Override
	public List<ExasolTableIndexColumn> getAttributeReferences(DBRProgressMonitor monitor) throws DBException {
		return this.columns;	
	}

	@Override
	public ExasolDataSource getDataSource() {
		return table.getSchema().getDataSource();
	}

	@Override
	@Property(viewable = false,  order = 100)	
	public String getDescription() {
		// no description possible
		return null;
	}

	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return type.getName() + " " + this.getColumnString();
	}

	public List<ExasolTableIndexColumn> getColumns() {
		return columns;
	}

	public void setColumns(List<ExasolTableIndexColumn> columns) {
		this.columns = columns;
	}
	
	public ExasolTableIndexColumn getColumn(String columnName) {
		return DBUtils.findObject(columns, columnName);
	}

	public Boolean getIsGeometry() {
		return isGeometry;
	}
	
	public String getSimpleColumnString( ) {
		String[] colNames = this.columns.stream().map(c -> c.getName()).toArray(String[]::new);
		return "(" + CommonUtils.joinStrings(",",colNames ) + ")";
	}
	
	public String getColumnString() {
		String[] colNames = this.columns.stream().map(c -> DBUtils.getQuotedIdentifier(c)).toArray(String[]::new);
		return "(" + CommonUtils.joinStrings(",", colNames ) + ")";
		
	}
	
	public void addColumn(ExasolTableIndexColumn column) {
		if (columns == null) {
			columns = new ArrayList<ExasolTableIndexColumn>();
		}
		columns.add(column);
	}
	
	

}
