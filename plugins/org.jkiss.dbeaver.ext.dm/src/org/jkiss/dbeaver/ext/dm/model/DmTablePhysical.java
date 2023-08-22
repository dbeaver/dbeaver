package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.utils.DmUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
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
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

/**
 * Dm physical table
 * 
 * @author caosw
 *
 */
public abstract class DmTablePhysical extends DmTableBase implements DBSObjectLazy<DmDataSource> {

	private static final Log log = Log.getLog(DmTablePhysical.class);

	public static final String CAT_STATISTICS = "Statistics";

	private long rowCount;
	private Long realRowCount;
	private Object tablespace;
	private boolean partitioned;
	private PartitionInfo partitionInfo;
	private PartitionCache partitionCache;

	protected DmTablePhysical(DmSchema schema, String name) {
		super(schema, name, false);
	}

	protected DmTablePhysical(DmSchema schema, ResultSet dbResult) {
		super(schema, dbResult);
		this.rowCount = JDBCUtils.safeGetLong(dbResult, "NUM_ROWS");
		this.tablespace = JDBCUtils.safeGetString(dbResult, "TABLESPACE_NAME");
		this.partitioned = JDBCUtils.safeGetBoolean(dbResult, "PARTITIONED", "Y");
		this.partitionCache = partitioned ? new PartitionCache() : null;
	}

	@Property(category = CAT_STATISTICS, viewable = true, order = 20)
	public long getRowCount() {
		return rowCount;
	}

	@Property(category = CAT_STATISTICS, viewable = false, expensive = true, order = 21)
	public synchronized Long getRealRowCount(DBRProgressMonitor monitor) {
		if (realRowCount != null) {
			return realRowCount;
		}
		if (!isPersisted()) {
			return null;
		}
		// Query row count
		try (DBCSession session = DBUtils.openMetaSession(monitor, this, "Read row count")) {
			realRowCount = countData(new AbstractExecutionSource(this, session.getExecutionContext(), this), session,
					null, DBSDataContainer.FLAG_NONE);
		} catch (DBException e) {
			log.debug("Can't fetch row count", e);
		}
		if (realRowCount == null) {
			realRowCount = -1L;
		}
		return realRowCount;
	}

	public static class PartitionInfo extends DmPartitionBase.PartitionInfoBase {
		public PartitionInfo(DBRProgressMonitor monitor, DmDataSource dataSource, ResultSet dbResult)
				throws DBException {
			super(monitor, dataSource, dbResult);
		}
	}

	@Override
	public Object getLazyReference(Object propertyId) {
		return tablespace;
	}

	@Property(viewable = true, order = 22, editable = true, updatable = true, listProvider = TablespaceListProvider.class)
	@LazyProperty(cacheValidator = DmTablespace.TablespaceReferenceValidator.class)
	public Object getTablespace(DBRProgressMonitor monitor) throws DBException {
		return DmTablespace.resolveTablespaceReference(monitor, this, null);
	}

	public Object getTablespace() {
		return tablespace;
	}

	public void setTablespace(DmTablespace tablespace) {
		this.tablespace = tablespace;
	}

	public Collection<DmTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
		return this.getContainer().indexCache.getObjects(monitor, getContainer(), this);
	}

	public DmTableIndex getIndex(DBRProgressMonitor monitor, String name) throws DBException {
		return this.getContainer().indexCache.getObject(monitor, getContainer(), this, name);
	}

	@PropertyGroup
	@LazyProperty(cacheValidator = PartitionInfoValidator.class)
	public PartitionInfo getPartitionInfo(DBRProgressMonitor monitor) throws DBException {
		if (partitionInfo == null && partitioned) {
			try (final JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load partitioning info")) {
				try (JDBCPreparedStatement dbStat = session
						.prepareStatement("SELECT * FROM ALL_PART_TABLES WHERE OWNER=? AND TABLE_NAME=?")) {
					dbStat.setString(1, getContainer().getName());
					dbStat.setString(2, getName());
					try (JDBCResultSet dbResult = dbStat.executeQuery()) {
						if (dbResult.next()) {
							partitionInfo = new PartitionInfo(monitor, this.getDataSource(), dbResult);
						}
					}
				}
			} catch (SQLException e) {
				throw new DBException(e, getDataSource());
			}
		}
		return partitionInfo;
	}

	@Association
	public Collection<DmTablePartition> getPartitions(DBRProgressMonitor monitor) throws DBException {
		if (partitionCache == null) {
			return null;
		} else {
			this.partitionCache.getAllObjects(monitor, this);
			this.partitionCache.loadChildren(monitor, this, null);
			return this.partitionCache.getAllObjects(monitor, this);
		}
	}

	@Association
	public Collection<DmTablePartition> getSubPartitions(DBRProgressMonitor monitor, DmTablePartition partition)
			throws DBException {
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
	public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
		this.valid = DmUtils.getObjectStatus(monitor, this, DmObjectType.TABLE);
	}

	/**
	 * 分区缓存
	 * 
	 * @author caosw
	 *
	 */
	public static class PartitionCache extends JDBCStructCache<DmTablePhysical, DmTablePartition, DmTablePartition> {

		protected PartitionCache() {
			super("PARTITION_NAME");
		}

		@NotNull
		@Override
		protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull DmTablePhysical owner,
				@Nullable DmTablePartition forObject) throws SQLException {
			final JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT * FROM " + DmUtils.getSysSchemaPrefix(owner.getDataSource())
							+ "ALL_TAB_SUBPARTITIONS " + "WHERE TABLE_OWNER=? AND TABLE_NAME=? "
							+ (forObject == null ? "" : "AND PARTITION_NAME=?") + "ORDER BY SUBPARTITION_POSITION");
			dbStat.setString(1, owner.getContainer().getName());
			dbStat.setString(2, owner.getName());
			if (forObject != null) {
				dbStat.setString(2, forObject.getName());
			}
			return dbStat;
		}

		@Override
		protected DmTablePartition fetchChild(@NotNull JDBCSession session, @NotNull DmTablePhysical owner,
				@NotNull DmTablePartition parent, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new DmTablePartition(owner, true, dbResult);
		}

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DmTablePhysical owner)
				throws SQLException {
			final JDBCPreparedStatement dbStat = session.prepareStatement(
					"SELECT * FROM " + DmUtils.getSysSchemaPrefix(owner.getDataSource()) + "ALL_TAB_PARTITIONS "
							+ "WHERE TABLE_OWNER=? AND TABLE_NAME=? " + "ORDER BY PARTITION_POSITION");
			dbStat.setString(1, owner.getContainer().getName());
			dbStat.setString(2, owner.getName());
			return dbStat;
		}

		@Override
		protected DmTablePartition fetchObject(@NotNull JDBCSession session, @NotNull DmTablePhysical owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new DmTablePartition(owner, false, resultSet);
		}
	}

	public static class TablespaceListProvider implements IPropertyValueListProvider<DmTablePhysical> {
		@Override
		public boolean allowCustomValue() {
			return false;
		}

		@Override
		public Object[] getPossibleValues(DmTablePhysical object) {
			final List<DmTablespace> tablespaces = new ArrayList<>();
			try {
				tablespaces.addAll(object.getDataSource().getTablespaces(new VoidProgressMonitor()));
			} catch (DBException e) {
				log.error(e);
			}
			tablespaces.sort(DBUtils.<DmTablespace>nameComparator());
			return tablespaces.toArray(new DmTablespace[tablespaces.size()]);
		}
	}

	public static class PartitionInfoValidator implements IPropertyCacheValidator<DmTablePhysical> {
		@Override
		public boolean isPropertyCached(DmTablePhysical object, Object propertyId) {
			return object.partitioned && object.partitionInfo != null;
		}
	}
	
}
