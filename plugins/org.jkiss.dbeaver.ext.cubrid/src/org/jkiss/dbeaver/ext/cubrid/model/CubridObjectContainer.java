/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.model;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericObjectContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndexColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericUtils;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.utils.CommonUtils;

public class CubridObjectContainer extends GenericObjectContainer implements GenericStructContainer {

	private final CubridDataSource dataSource;
	private final CubridUserCache cubridUserCache;
	private final CubridTableCache cubridTableCache;
	private final CubridSystemTableCache cubridSystemTableCache;
	private final CubridSystemViewCache cubridSystemViewCache;
	private final CubridIndexCache cubridIndexCache;

	protected CubridObjectContainer(CubridDataSource dataSource) {
		super(dataSource);
		this.dataSource = dataSource;
		this.cubridUserCache = new CubridUserCache();
		this.cubridTableCache = new CubridTableCache(dataSource);
		this.cubridSystemTableCache = new CubridSystemTableCache(dataSource);
		this.cubridSystemViewCache = new CubridSystemViewCache(dataSource);
		this.cubridIndexCache = new CubridIndexCache(cubridTableCache);
	}
	
	@NotNull
	@Override
	public CubridDataSource getDataSource() {
		return dataSource;
	}
	
	public CubridTableCache getCubridTableCache() {
		return this.cubridTableCache;
    }

	public CubridSystemTableCache getCubridSystemTableCache() {
		return this.cubridSystemTableCache;
    }

	public CubridIndexCache getCubridIndexCache() {
		return this.cubridIndexCache;
    }

	@Override
	public GenericStructContainer getObject() {
		return this.getDataSource();
	}

	@Override
	public GenericCatalog getCatalog() {
		return null;
	}

	@Override
	public GenericSchema getSchema() {
		return null;
	}

	@Override
	public Class<? extends DBSObject> getPrimaryChildType(DBRProgressMonitor monitor) throws DBException {
		return CubridTable.class;
	}

	@Override
	public DBSObject getParentObject() {
		return this.getDataSource().getParentObject();
	}

	@Override
	public String getName() {
		return this.getDataSource().getName();
	}

	@Override
	public String getDescription() {
		return this.getDataSource().getDescription();
	}

	public Collection<? extends CubridUser> getCubridUsers(DBRProgressMonitor monitor) throws DBException {
		return cubridUserCache.getAllObjects(monitor, this);
	}

	public List<? extends CubridTable> getPhysicalTables(DBRProgressMonitor monitor, String name) throws DBException {
		List<CubridTable> tables = new ArrayList<>();
		for(CubridTable table : this.cubridTableCache.getAllObjects(monitor, this)) {
			if(table.isPhysicalTable() && table.getOwner().getName().equals(name)) {
				tables.add(table);
			}
		}
		return tables;
	}

	public List<? extends CubridTable> getPhysicalSystemTables(DBRProgressMonitor monitor, String name) throws DBException {
		List<CubridTable> tables = new ArrayList<>();
		for(CubridTable table : this.cubridSystemTableCache.getAllObjects(monitor, this)) {
			if(table.isPhysicalTable() && table.getOwner().getName().equals(name)) {
				tables.add(table);
			}
		}
		return tables;
	}

	public List<? extends CubridView> getViews(DBRProgressMonitor monitor, String name) throws DBException {
		List<CubridView> tables = new ArrayList<>();
		for(CubridTable table : this.cubridTableCache.getAllObjects(monitor, this)) {
			if(table.isView() && table.getOwner().getName().equals(name)) {
				tables.add((CubridView) table);
			}
		}
		return tables;
	}

	public List<? extends CubridView> getSystemViews(DBRProgressMonitor monitor, String name) throws DBException {
		List<CubridView> views = new ArrayList<>();
		for(CubridTable table : this.cubridSystemViewCache.getAllObjects(monitor, this)) {
			if(table.getOwner().getName().equals(name)) {
				views.add((CubridView) table);
			}
		}
		return views;
	}

	public class CubridUserCache extends JDBCObjectCache<CubridObjectContainer, CubridUser> {

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull CubridObjectContainer container) throws SQLException {
			String sql= "select * from db_user";
			final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
			return dbStat;
		}

		@Override
		protected CubridUser fetchObject(JDBCSession session, CubridObjectContainer container, JDBCResultSet resultSet) throws SQLException, DBException {
			String name = resultSet.getString("name");
			String comment = resultSet.getString("comment");
			return new CubridUser(container, name, comment);
		}

	}
	
	public class CubridTableCache extends JDBCStructLookupCache<CubridObjectContainer, CubridTable, CubridTableColumn> {

		final CubridDataSource dataSource;
		final GenericMetaObject tableObject;
		final GenericMetaObject columnObject;

		protected CubridTableCache(CubridDataSource dataSource) {
			super(GenericUtils.getColumn(dataSource, CubridConstants.OBJECT_TABLE, JDBCConstants.TABLE_NAME));
			this.dataSource = dataSource;
			this.tableObject = dataSource.getMetaObject(CubridConstants.OBJECT_TABLE);
			this.columnObject = dataSource.getMetaObject(CubridConstants.OBJECT_TABLE_COLUMN);
			setListOrderComparator(DBUtils.<CubridTable>nameComparatorIgnoreCase());
		}

		public CubridDataSource getDataSource() {
			return dataSource;
		}

		@Override
		public @NotNull JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull CubridObjectContainer owner,
			@Nullable CubridTable object, @Nullable String objectName) throws SQLException {
			String sql= "select a.*, case when class_type = 'CLASS' then 'TABLE' \r\n"
				+ "when class_type = 'VCLASS' then 'VIEW' end as TABLE_TYPE, \r\n"
				+ "b.current_val from db_class a LEFT JOIN db_serial b on \r\n"
				+ "a.class_name = b.class_name where a.is_system_class='NO'";
			final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
			return dbStat;
		}

		@Override
		protected CubridTableColumn fetchChild(@NotNull JDBCSession arg0, @NotNull CubridObjectContainer owner,
			@NotNull CubridTable table, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new CubridTableColumn(table, dbResult);
		}

		@Override
		protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull CubridObjectContainer owner,
			@Nullable CubridTable forTable) throws SQLException {
			return dataSource.getMetaModel().prepareTableColumnLoadStatement(session, owner, forTable);
		}

		@Override
		protected @Nullable CubridTable fetchObject(@NotNull JDBCSession session, @NotNull CubridObjectContainer owner,
			@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return getDataSource().getMetaModel().createTableImpl(session, owner, tableObject, dbResult);
		}

	}

	public class CubridSystemTableCache extends CubridTableCache {

		protected CubridSystemTableCache(CubridDataSource dataSource) {
			super(dataSource);
		}

		@Override
		public @NotNull JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull CubridObjectContainer owner,
			@Nullable CubridTable object, @Nullable String objectName) throws SQLException {
			String sql= "select *, class_name as TABLE_NAME, case when class_type = 'CLASS' \r\n"
				+ "then 'TABLE' end as TABLE_TYPE from db_class\r\n"
				+ "where class_type = 'CLASS' \r\n"
				+ "and is_system_class = 'YES'";
			final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
			return dbStat;
		}

	}

	public class CubridSystemViewCache extends CubridTableCache {

		protected CubridSystemViewCache(CubridDataSource dataSource) {
			super(dataSource);
		}

		@Override
		public @NotNull JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull CubridObjectContainer owner,
			@Nullable CubridTable object, @Nullable String objectName) throws SQLException {
			String sql= "select *, case when class_type = 'VCLASS' \r\n"
				+ "then 'VIEW' end as TABLE_TYPE,\r\n"
				+ "class_name as TABLE_NAME from db_class\r\n"
				+ "where class_type='VCLASS'\r\n"
				+ "and is_system_class='YES'";
			final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
			return dbStat;
		}

	}

	public class CubridIndexCache extends JDBCCompositeCache<CubridObjectContainer, CubridTable, CubridTableIndex, GenericTableIndexColumn>{

		CubridIndexCache(CubridTableCache tableCache) {
			super(tableCache, CubridTable.class,
				GenericUtils.getColumn(tableCache.getDataSource(), GenericConstants.OBJECT_INDEX, JDBCConstants.TABLE_NAME),
				GenericUtils.getColumn(tableCache.getDataSource(), GenericConstants.OBJECT_INDEX, JDBCConstants.INDEX_NAME));
		}

		@Override
		protected JDBCStatement prepareObjectsStatement(JDBCSession session, CubridObjectContainer owner,
			CubridTable forParent) throws SQLException {
			return dataSource.getMetaModel().prepareIndexLoadStatement(session, forParent);
		}

		@Override
		protected CubridTableIndex fetchObject(JDBCSession session, CubridObjectContainer owner, CubridTable parent,
			String indexName, JDBCResultSet dbResult) throws SQLException, DBException {

			boolean isNonUnique = JDBCUtils.safeGetBoolean(dbResult, JDBCConstants.NON_UNIQUE);
			String indexQualifier = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.INDEX_QUALIFIER);
			long cardinality = JDBCUtils.safeGetLong(dbResult, JDBCConstants.INDEX_CARDINALITY);
			int indexTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.TYPE);
			String name = indexName;

			DBSIndexType indexType;
			switch (indexTypeNum) {
				case DatabaseMetaData.tableIndexStatistic:
					return null;
				case DatabaseMetaData.tableIndexClustered:
					indexType = DBSIndexType.CLUSTERED;
					break;
				case DatabaseMetaData.tableIndexHashed:
					indexType = DBSIndexType.HASHED;
					break;
				case DatabaseMetaData.tableIndexOther:
					indexType = DBSIndexType.OTHER;
					break;
				default:
					indexType = DBSIndexType.UNKNOWN;
					break;
			}
			if (CommonUtils.isEmpty(name)) {
				// [JDBC] Some drivers return empty index names
				name = parent.getName().toUpperCase(Locale.ENGLISH) + "_INDEX";
			}

			return new CubridTableIndex(parent, isNonUnique, indexQualifier, cardinality, name, indexType, true);
		}

		@Override
		protected GenericTableIndexColumn[] fetchObjectRow(JDBCSession session, CubridTable parent,
			CubridTableIndex object, JDBCResultSet dbResult) throws SQLException, DBException {
			int ordinalPosition = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
			boolean trimName = parent.getDataSource().getMetaModel().isTrimObjectNames();
			String columnName = trimName ? JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.COLUMN_NAME)
					: JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
			String ascOrDesc = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.ASC_OR_DESC);

			if (CommonUtils.isEmpty(columnName)) {
				// Maybe a statistics index without column
				return null;
			}
			GenericTableColumn tableColumn = parent.getAttribute(session.getProgressMonitor(), columnName);
			if (tableColumn == null) {
				return null;
			}

			return new GenericTableIndexColumn[] { new GenericTableIndexColumn(
				object,
				tableColumn,
				ordinalPosition,
				!"D".equalsIgnoreCase(ascOrDesc)) };
		}

		@Override
		protected void cacheChildren(DBRProgressMonitor monitor, CubridTableIndex object,
			List<GenericTableIndexColumn> children) {
			object.setColumns(children);
		}

	}

}
