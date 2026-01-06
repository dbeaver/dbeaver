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
import java.util.Collection;
import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.yashandb.model.util.YashanDBUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

public abstract class YashanDBTablePhysical extends YashanDBTableBase implements DBSObjectLazy<YashanDBDataSource> {

	private static final Log log = Log.getLog(YashanDBTablePhysical.class);

	protected YashanDBTablePhysical(YashanDBSchema schema, String name) {
		super(schema, name, false);
	}

	protected YashanDBTablePhysical(YashanDBSchema schema, ResultSet dbResult) {
		super(schema, dbResult);
		this.rowCount = JDBCUtils.safeGetLong(dbResult, "NUM_ROWS");
		this.tablespace = JDBCUtils.safeGetString(dbResult, "TABLESPACE_NAME");
		this.partitioned = JDBCUtils.safeGetBoolean(dbResult, "PARTITIONED", "Y");
		this.partitionCache = partitioned ? new PartitionCache() : null;
	}

	private boolean partitioned;
	private long rowCount;
	private Object tablespace;
	private PartitionInfo partitionInfo;
	private PartitionCache partitionCache;

	@Association
	@Property(viewable = true, order = 13)
	public boolean isPartitioned() {
		return partitioned;
	}

	@Property(category = DBConstants.CAT_STATISTICS, viewable = true, order = 20)
	public long getRowCount() {
		return rowCount;
	}

	@Property(viewable = true, order = 22, editable = true, updatable = false, listProvider = TablespaceListProvider.class)
	@LazyProperty(cacheValidator = YashanDBTablespace.TablespaceReferenceValidator.class)
	public Object getTablespace(DBRProgressMonitor monitor) throws DBException {
		return YashanDBTablespace.resolveTablespaceReference(monitor, this, null);
	}

	@Override
	public Object getLazyReference(Object propertyId) {
		return tablespace;
	}

	public Object getTablespace() {
		return tablespace;
	}

	public void setTablespace(YashanDBTablespace tablespace) {
		this.tablespace = tablespace;
	}

	@Override
	@Association
	public Collection<YashanDBTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
		return this.getContainer().indexCache.getObjects(monitor, getContainer(), this);
	}

	public YashanDBTableIndex getIndex(DBRProgressMonitor monitor, String name) throws DBException {
		return this.getContainer().indexCache.getObject(monitor, getContainer(), this, name);
	}

	@PropertyGroup
	@LazyProperty(cacheValidator = PartitionInfoValidator.class)
	public PartitionInfo getPartitionInfo(DBRProgressMonitor monitor) throws DBException {
		if (partitionInfo == null && partitioned) {
			try (final JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load partitioning info")) {
				try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM "
						+ YashanDBUtils.isAdminPriv((YashanDBDataSource) session.getDataSource(), "PART_TABLES")
						+ " WHERE OWNER=? AND TABLE_NAME=?")) {
					dbStat.setString(1, getContainer().getName());
					dbStat.setString(2, getName());
					try (JDBCResultSet dbResult = dbStat.executeQuery()) {
						if (dbResult.next()) {
							partitionInfo = new PartitionInfo(monitor, this.getDataSource(), dbResult);
						}
					}
				}
			} catch (SQLException e) {
				throw new DBException(e.getMessage());
			}
		}
		return partitionInfo;
	}

	@Association
	public Collection<YashanDBTablePartition> getPartitions(DBRProgressMonitor monitor) throws DBException {
		if (partitionCache == null) {
			return null;
		} else {
			this.partitionCache.getAllObjects(monitor, this);
			this.partitionCache.loadChildren(monitor, this, null);
			return this.partitionCache.getAllObjects(monitor, this);
		}
	}

	@Association
	public Collection<YashanDBTablePartition> getSubPartitions(DBRProgressMonitor monitor,
			YashanDBTablePartition partition) throws DBException {
		if (partitionCache == null) {
			return null;
		} else {
			this.partitionCache.getAllObjects(monitor, this);
			return this.partitionCache.getChildren(monitor, this, partition);
		}
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		this.getContainer().indexCache.clearObjectCache(this);
		return super.refreshObject(monitor);
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public DBSObjectState getObjectState() {
		return null;
	}

	@Override
	public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
		this.valid = YashanDBUtils.getObjectStatus(monitor, this, YashanDBObjectType.TABLE);
	}

	@Override
	public String getDescription(DBRProgressMonitor monitor) {
		return null;
	}

	private static class PartitionCache
			extends JDBCStructCache<YashanDBTablePhysical, YashanDBTablePartition, YashanDBTablePartition> {

		protected PartitionCache() {
			super("PARTITION_NAME");
		}

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session,
				@NotNull YashanDBTablePhysical table) throws SQLException {
			final JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM "
					+ YashanDBUtils.isAdminPriv((YashanDBDataSource) session.getDataSource(), "TAB_PARTITIONS")
					+ " WHERE TABLE_OWNER=? AND TABLE_NAME=? " + "ORDER BY PARTITION_POSITION");
			dbStat.setString(1, table.getContainer().getName());
			dbStat.setString(2, table.getName());
			return dbStat;
		}

		@Override
		protected YashanDBTablePartition fetchObject(@NotNull JDBCSession session, @NotNull YashanDBTablePhysical table,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new YashanDBTablePartition(table, false, resultSet);
		}

		@Override
		protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session,
				@NotNull YashanDBTablePhysical table, @Nullable YashanDBTablePartition forObject) throws SQLException {
			final JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT * FROM "
							+ YashanDBUtils.isAdminPriv((YashanDBDataSource) session.getDataSource(),
									"TAB_SUBPARTITIONS")
							+ " WHERE TABLE_OWNER=? AND TABLE_NAME=? "
							+ (forObject == null ? "" : "AND PARTITION_NAME=? ") + "ORDER BY PARTITION_POSITION");
			dbStat.setString(1, table.getContainer().getName());
			dbStat.setString(2, table.getName());
			if (forObject != null) {
				dbStat.setString(3, forObject.getName());
			}
			return dbStat;
		}

		@Override
		protected YashanDBTablePartition fetchChild(@NotNull JDBCSession session, @NotNull YashanDBTablePhysical table,
				@NotNull YashanDBTablePartition parent, @NotNull JDBCResultSet dbResult)
				throws SQLException, DBException {
			return new YashanDBTablePartition(table, true, dbResult);
		}

	}

	public static class PartitionInfo extends YashanDBPartitionBase.PartitionInfoBase {

		public PartitionInfo(DBRProgressMonitor monitor, YashanDBDataSource dataSource, ResultSet dbResult)
				throws DBException {
			super(monitor, dataSource, dbResult);
		}
	}

	public static class PartitionInfoValidator implements IPropertyCacheValidator<YashanDBTablePhysical> {
		@Override
		public boolean isPropertyCached(YashanDBTablePhysical object, Object propertyId) {
			return object.partitioned && object.partitionInfo != null;
		}
	}

	public static class TablespaceListProvider implements IPropertyValueListProvider<YashanDBTablePhysical> {
		@Override
		public boolean allowCustomValue() {
			return false;
		}

		@Override
		public Object[] getPossibleValues(YashanDBTablePhysical object) {
			final List<YashanDBTablespace> tablespaces = new ArrayList<>();
			try {
				tablespaces.addAll(object.getDataSource().getTablespaces(new VoidProgressMonitor()));
			} catch (DBException e) {
				log.error(e);
			}
			tablespaces.sort(DBUtils.<YashanDBTablespace>nameComparator());
			return tablespaces.toArray(new YashanDBTablespace[tablespaces.size()]);
		}
	}

}
