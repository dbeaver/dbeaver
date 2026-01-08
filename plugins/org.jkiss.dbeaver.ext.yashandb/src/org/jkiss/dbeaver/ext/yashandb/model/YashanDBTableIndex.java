/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.util.YashanDBUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

public class YashanDBTableIndex extends JDBCTableIndex<YashanDBSchema, YashanDBTablePhysical>
		implements DBSObjectLazy, DBPScriptObject {

	private Object tablespace;
	private boolean nonUnique;
	private List<YashanDBTableIndexColumn> columns;
	private String indexDDL;

	public YashanDBTableIndex(YashanDBSchema schema, YashanDBTablePhysical table, String indexName,
			ResultSet dbResult) {
		super(schema, table, indexName, null, true);
		String indexTypeName = JDBCUtils.safeGetString(dbResult, "INDEX_TYPE");
		this.nonUnique = !"Y".equals(JDBCUtils.safeGetString(dbResult, "UNIQUENESS"));

		if (YashanDBConstants.INDEX_TYPE_NORMAL.getId().equals(indexTypeName)) {
			indexType = YashanDBConstants.INDEX_TYPE_NORMAL;
		} else if (YashanDBConstants.INDEX_TYPE_NORMAL_REV.getId().equals(indexTypeName)) {
			indexType = YashanDBConstants.INDEX_TYPE_NORMAL_REV;
		} else if (YashanDBConstants.INDEX_TYPE_FUNCTION_BASED_NORMAL.getId().equals(indexTypeName)) {
			indexType = YashanDBConstants.INDEX_TYPE_FUNCTION_BASED_NORMAL;
		} else if (YashanDBConstants.INDEX_TYPE_FUNCTION_BASED_NORMAL_REV.getId().equals(indexTypeName)) {
			indexType = YashanDBConstants.INDEX_TYPE_FUNCTION_BASED_NORMAL_REV;
		} else if (YashanDBConstants.INDEX_TYPE_LOB.getId().equals(indexTypeName)) {
			indexType = YashanDBConstants.INDEX_TYPE_LOB;
		} else if (YashanDBConstants.INDEX_TYPE_COLUMNAR.getId().equals(indexTypeName)) {
			indexType = YashanDBConstants.INDEX_TYPE_COLUMNAR;
		} else if (YashanDBConstants.INDEX_TYPE_RTREE.getId().equals(indexTypeName)) {
			indexType = YashanDBConstants.INDEX_TYPE_RTREE;
		} else {
			indexType = DBSIndexType.OTHER;
		}
		this.tablespace = JDBCUtils.safeGetString(dbResult, "TABLESPACE_NAME");
	}

	public YashanDBTableIndex(YashanDBSchema schema, YashanDBTablePhysical parent, String name, boolean unique,
			DBSIndexType indexType) {
		super(schema, parent, name, indexType, false);
		this.nonUnique = !unique;

	}

	@NotNull
	@Override
	public YashanDBDataSource getDataSource() {
		return getTable().getDataSource();
	}

	@Override
	@Property(viewable = true, order = 5)
	public boolean isUnique() {
		return !nonUnique;
	}

	public void setUnique(boolean unique) {
		this.nonUnique = !unique;
	}

	@Override
	public Object getLazyReference(Object propertyId) {
		return tablespace;
	}

	@Property(viewable = true, order = 10)
	@LazyProperty(cacheValidator = YashanDBTablespace.TablespaceReferenceValidator.class)
	public Object getTablespace(DBRProgressMonitor monitor) throws DBException {
		return YashanDBTablespace.resolveTablespaceReference(monitor, this, null);
	}

	@Nullable
	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public List<YashanDBTableIndexColumn> getAttributeReferences(DBRProgressMonitor monitor) {
		return columns;
	}

	@Nullable
	@Association
	public YashanDBTableIndexColumn getColumn(String columnName) {
		return DBUtils.findObject(columns, columnName);
	}

	void setColumns(List<YashanDBTableIndexColumn> columns) {
		this.columns = columns;
	}

	public void addColumn(YashanDBTableIndexColumn column) {
		if (columns == null) {
			columns = new ArrayList<>();
		}
		columns.add(column);
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getTable().getContainer(), this);
	}

	@Override
	public String toString() {
		return getFullyQualifiedName(DBPEvaluationContext.UI);
	}

	@Override
	@Property(hidden = true, order = -1)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		if (indexDDL == null && isPersisted()) {
			try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read index definition")) {
				indexDDL = JDBCUtils.queryString(session, "SELECT DBMS_METADATA.GET_DDL('INDEX', ?, ?) FROM DUAL",
						getName(), getTable().getSchema().getName());
			} catch (SQLException e) {
				throw new DBException(e.getMessage());
			}
		}
		return indexDDL;
	}
}
