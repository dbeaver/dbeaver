package org.jkiss.dbeaver.ext.cubrid.model;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericObjectContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericUtils;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class CubridObjectContainer extends GenericObjectContainer{

	private final CubridDataSource dataSource;
	private final CubridUserCache cubridUserCache;
	private final CubridTableCache cubridTableCache;
	private final CubridSystemTableCache cubridSystemTableCache;
	private final CubridSystemViewCache cubridSystemViewCache;

	protected CubridObjectContainer(CubridDataSource dataSource) {
		super(dataSource);
		this.dataSource = dataSource;
		this.cubridUserCache = new CubridUserCache();
		this.cubridTableCache = new CubridTableCache(dataSource);
		this.cubridSystemTableCache = new CubridSystemTableCache(dataSource);
		this.cubridSystemViewCache = new CubridSystemViewCache(dataSource);
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
			return container.getDataSource().getMetaModel().prepareCubridUserLoadStatement(session, container);
		}

		@Override
		protected CubridUser fetchObject(JDBCSession session, CubridObjectContainer container, JDBCResultSet resultSet) throws SQLException, DBException {
			String name = resultSet.getString("name");
			String comment = resultSet.getString("comment");
			return container.getDataSource().getMetaModel().createCubridUserImpl(container, name, comment);
		}

	}
	
	public class CubridTableCache extends JDBCStructLookupCache<CubridObjectContainer, CubridTable, GenericTableColumn> {

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
			return dataSource.getMetaModel().prepareTableLoadStatement(session, owner, object, objectName);
		}

		@Override
		protected GenericTableColumn fetchChild(@NotNull JDBCSession arg0, @NotNull CubridObjectContainer owner,
			@NotNull CubridTable table, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return null;
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
			return dataSource.getMetaModel().prepareSystemTableLoadStatement(session, owner, object, objectName);
		}

	}

	public class CubridSystemViewCache extends CubridTableCache {

		protected CubridSystemViewCache(CubridDataSource dataSource) {
			super(dataSource);
		}

		@Override
		public @NotNull JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull CubridObjectContainer owner,
			@Nullable CubridTable object, @Nullable String objectName) throws SQLException {
			return dataSource.getMetaModel().prepareSystemViewLoadStatement(session, owner, object, objectName);
		}

	}

}
