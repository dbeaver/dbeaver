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
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.yashandb.model.util.YashanDBUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPObjectStatisticsCollector;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.ArrayUtils;

public class YashanDBSchema extends YashanDBGlobalObject implements DBSSchema, DBPRefreshableObject, DBPSystemObject,
		DBSProcedureContainer, DBPObjectStatisticsCollector {

	private static final Log log = Log.getLog(YashanDBSchema.class);

	private static boolean FUNC_PROC_AS_CHILDREN = true;
	private static boolean SEQUENCE_AS_CHILDREN = true;
	private static boolean PACKAGE_AS_CHILDREN = true;
	private static boolean TYPE_AS_CHILDREN = true;

	private volatile boolean hasStatistics;

	public final TableCache tableCache = new TableCache();
	public final ConstraintCache constraintCache = new ConstraintCache();
	public final DataTypeCache dataTypeCache = new DataTypeCache();
	public final TableTriggerCache tableTriggerCache = new TableTriggerCache();
	public final SequenceCache sequenceCache = new SequenceCache();
	public final ForeignKeyCache foreignKeyCache = new ForeignKeyCache();
	public final IndexCache indexCache = new IndexCache();
	public final ProceduresCache proceduresCache = new ProceduresCache();
	public final PackageCache packageCache = new PackageCache();

	public final SynonymCache synonymCache = new SynonymCache();
	public final DBLinkCache dbLinkCache = new DBLinkCache();
	public final JobCache jobCache = new JobCache();
	public final SchedulerJobCache schedulerJobCache = new SchedulerJobCache();
	public final RecycleBin recycleBin = new RecycleBin();

	private long id;
	private String name;
	private Date createTime;
	private transient YashanDBUser user;

	public YashanDBSchema(YashanDBDataSource dataSource, long id, String name) {
		super(dataSource, id > 0);
		this.id = id;
		this.name = name;
	}

	public YashanDBSchema(@NotNull YashanDBDataSource dataSource, @NotNull ResultSet dbResult) {
		super(dataSource, true);
		this.id = JDBCUtils.safeGetLong(dbResult, "USER_ID");
		this.name = JDBCUtils.safeGetString(dbResult, "USERNAME");
		this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");
	}

	@Override
	@NotNull
	@Property(viewable = true, editable = true, order = 1)
	public String getName() {
		return name;
	}

	@NotNull
	@Property(viewable = true, order = 2)
	public Date getCreateTime() {
		return createTime;
	}

	@NotNull
	@Property(viewable = true, order = 3)
	public long getId() {
		return id;
	}

	@Property(order = 4)
	public YashanDBUser getSchemaUser(DBRProgressMonitor monitor) throws DBException {
		return getDataSource().getUser(monitor, name);
	}

	public void setName(String name) {
		this.name = name;
	}

	public YashanDBUser getUser() {
		return user;
	}

	public void setUser(YashanDBUser user) {
		this.user = user;
	}

	@Override
	public boolean isStatisticsCollected() {
		return hasStatistics;
	}

	public boolean isPublic() {
		return YashanDBConstants.USER_PUBLIC.equals(this.name);
	}

	@Override
	public void collectObjectStatistics(DBRProgressMonitor monitor, boolean totalSizeOnly, boolean forceRefresh)
			throws DBException {
		if (hasStatistics && !forceRefresh) {
			return;
		}
		try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
			boolean hasDBA = getDataSource().isViewAvailable(monitor, YashanDBConstants.SCHEMA_SYS, "DBA_SEGMENTS");
			try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT SEGMENT_NAME,SUM(bytes) TABLE_SIZE\n"
					+ "FROM " + YashanDBUtils.getSysSchemaPrefix(getDataSource())
					+ (hasDBA ? "DBA_SEGMENTS" : "USER_SEGMENTS") + " " + "s\n" + "WHERE S.SEGMENT_TYPE='TABLE'"
					+ (hasDBA ? " AND s.OWNER = ?" : "") + "\n" + "GROUP BY SEGMENT_NAME")) {
				if (hasDBA) {
					dbStat.setString(1, getName());
				}
				try (JDBCResultSet dbResult = dbStat.executeQuery()) {
					while (dbResult.next()) {
						String tableName = dbResult.getString(1);
						YashanDBTable table = getTable(monitor, tableName);
						if (table != null) {
							table.fetchTableSize(dbResult);
						}
					}
				}
			}
		} catch (SQLException e) {
			throw new DBCException("Error reading table statistics", e);
		} finally {
			for (YashanDBTableBase table : tableCache.getCachedObjects()) {
				if (table instanceof YashanDBTable && !((YashanDBTable) table).hasStatistics()) {
					((YashanDBTable) table).setTableSize(1024L);
				}
			}
			hasStatistics = true;
		}
	}

	private static YashanDBTableColumn getTableColumn(JDBCSession session, YashanDBTableBase parent, ResultSet dbResult,
			String columnName) throws DBException {
		YashanDBTableColumn tableColumn = columnName == null ? null
				: parent.getAttribute(session.getProgressMonitor(), columnName);
		if (tableColumn == null) {
			log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "'");
		}
		return tableColumn;
	}

	public TableCache getTableCache() {
		return tableCache;
	}

	public class TableCache extends JDBCStructLookupCache<YashanDBSchema, YashanDBTableBase, YashanDBTableColumn> {

		TableCache() {
			super("OBJECT_NAME");
			setListOrderComparator(DBUtils.nameComparator());
		}

		@Override
		public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull YashanDBSchema owner,
				@NotNull YashanDBTableBase object, @NotNull String objectName) throws SQLException {
			JDBCPreparedStatement dbStat = null;
			String sql = "SELECT  O.*,t.OWNER,t.TABLE_TYPE,t.TABLESPACE_NAME,t.PARTITIONED,t.TEMPORARY,t.NUM_ROWS "
					+ "FROM (SELECT * FROM " + YashanDBUtils.isAdminPriv(getDataSource(), "OBJECTS")
					+ " o where O.OWNER= ? AND O.OBJECT_NAME NOT LIKE '%BIN$%' and O.OBJECT_TYPE IN ('TABLE','VIEW','MATERIALIZED VIEW')"
					+ (object == null && objectName == null ? "" : " AND O.OBJECT_NAME = ?")
					+ (object instanceof YashanDBTable ? " AND O.OBJECT_TYPE='TABLE'" : "")
					+ (object instanceof YashanDBView ? " AND O.OBJECT_TYPE='VIEW'" : "")
					+ (object instanceof YashanDBMaterializedView ? " AND O.OBJECT_TYPE='MATERIALIZED VIEW'" : "")
					+ ") O LEFT JOIN " + YashanDBUtils.isAdminPriv(owner.getDataSource(), "TABLES") + " t "
					+ "on t.OWNER= O.OWNER AND t.TABLE_NAME = o.OBJECT_NAME";

			dbStat = session.prepareStatement(sql);
			dbStat.setString(1, owner.getName());
			if (object != null || objectName != null)
				dbStat.setString(2, object != null ? object.getName() : objectName);
			return dbStat;
		}

		@Override
		protected YashanDBTableBase fetchObject(@NotNull JDBCSession session, @NotNull YashanDBSchema owner,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			final String tableType = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
			if ("TABLE".equals(tableType)) {
				return new YashanDBTable(session.getProgressMonitor(), owner, dbResult);
			} else if ("VIEW".equals(tableType)) {
				return new YashanDBView(owner, dbResult);
			} else if ("MATERIALIZED VIEW".equals(tableType)) {
				return new YashanDBMaterializedView(owner, dbResult);
			} else {
				return null;
			}
		}

		@Override
		protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull YashanDBSchema owner,
				@NotNull YashanDBTableBase forTable) throws SQLException {

			String colsView;
			if (!owner.getDataSource().isViewAvailable(session.getProgressMonitor(), YashanDBConstants.SCHEMA_SYS,
					"ALL_TAB_COLS")) {
				colsView = "TAB_COLUMNS";
			} else {
				colsView = "TAB_COLS";
			}
			String s2 = "SELECT distinct(c.*),c.TABLE_NAME as OBJECT_NAME FROM ";
			StringBuilder sql = new StringBuilder(500);
			sql.append(s2).append(YashanDBUtils.isAdminPriv(getDataSource(), colsView))
					.append(" c\n" + "WHERE c.OWNER=?");

			if (forTable != null) {
				sql.append(" AND c.TABLE_NAME=?");
			}

			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			dbStat.setString(1, owner.getName());
			if (forTable != null) {
				dbStat.setString(2, forTable.getName());
			}
			return dbStat;
		}

		@Override
		protected YashanDBTableColumn fetchChild(@NotNull JDBCSession session, @NotNull YashanDBSchema owner,
				@NotNull YashanDBTableBase table, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new YashanDBTableColumn(session.getProgressMonitor(), table, dbResult);
		}

		@Override
		protected void cacheChildren(YashanDBTableBase parent, List<YashanDBTableColumn> yashanDBTableColumns) {
			yashanDBTableColumns.sort(DBUtils.orderComparator());
			super.cacheChildren(parent, yashanDBTableColumns);
		}
	}

	public class ConstraintCache extends
			JDBCCompositeCache<YashanDBSchema, YashanDBTableBase, YashanDBTableConstraint, YashanDBTableConstraintColumn> {

		ConstraintCache() {
			super(tableCache, YashanDBTableBase.class, "TABLE_NAME", "CONSTRAINT_NAME");
		}

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(JDBCSession session, YashanDBSchema owner,
				YashanDBTableBase forTable) throws SQLException {

			StringBuilder sql = new StringBuilder(500);
			JDBCPreparedStatement dbStat;

			sql.append("SELECT c.TABLE_NAME, c.CONSTRAINT_NAME,c.CONSTRAINT_TYPE,c.STATUS,c.SEARCH_CONDITION,"
					+ "col.COLUMN_NAME,col.POSITION\n" + "FROM "
					+ YashanDBUtils.isAdminPriv(getDataSource(), "CONSTRAINTS") + " c, "
					+ YashanDBUtils.isAdminPriv(getDataSource(), "CONS_COLUMNS") + " col\n"
					+ "WHERE c.CONSTRAINT_TYPE<>'FOREIGN KEY' AND c.OWNER=? AND c.OWNER=col.OWNER AND c.CONSTRAINT_NAME=col"
					+ ".CONSTRAINT_NAME");

			if (forTable != null) {
				sql.append(" AND c.TABLE_NAME=?");
			}
			sql.append("\nORDER BY c.CONSTRAINT_NAME,col.POSITION");

			dbStat = session.prepareStatement(sql.toString());
			dbStat.setString(1, YashanDBSchema.this.getName());
			if (forTable != null) {
				dbStat.setString(2, forTable.getName());
			}

			return dbStat;
		}

		@Nullable
		@Override
		protected YashanDBTableConstraint fetchObject(JDBCSession session, YashanDBSchema owner,
				YashanDBTableBase parent, String indexName, JDBCResultSet dbResult) throws SQLException, DBException {
			return new YashanDBTableConstraint(parent, dbResult);
		}

		@Nullable
		@Override
		protected YashanDBTableConstraintColumn[] fetchObjectRow(JDBCSession session, YashanDBTableBase parent,
				YashanDBTableConstraint object, JDBCResultSet dbResult) throws SQLException, DBException {
			final YashanDBTableColumn tableColumn = getTableColumn(session, parent, dbResult,
					JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME"));
			return tableColumn == null ? null
					: new YashanDBTableConstraintColumn[] { new YashanDBTableConstraintColumn(object, tableColumn,
							JDBCUtils.safeGetInt(dbResult, "POSITION")) };
		}

		@Override
		protected void cacheChildren(DBRProgressMonitor monitor, YashanDBTableConstraint constraint,
				List<YashanDBTableConstraintColumn> rows) {
			constraint.setAttributeReferences(rows);
		}
	}

	public class DataTypeCache extends JDBCObjectCache<YashanDBSchema, YashanDBDataType> {

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBSchema owner)
				throws SQLException {
			JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT * FROM " + YashanDBUtils.isAdminPriv(owner.getDataSource(), "TYPES") + " "
							+ "WHERE OWNER=? ORDER BY TYPE_NAME");
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected YashanDBDataType fetchObject(@NotNull JDBCSession session, @NotNull YashanDBSchema owner,
				@NotNull JDBCResultSet resultSet) throws SQLException {
			return new YashanDBDataType(owner, resultSet);
		}
	}

	public class ForeignKeyCache extends
			JDBCCompositeCache<YashanDBSchema, YashanDBTable, YashanDBTableForeignKey, YashanDBTableForeignKeyColumn> {

		ForeignKeyCache() {
			super(tableCache, YashanDBTable.class, "TABLE_NAME", "CONSTRAINT_NAME");

		}

		@Override
		protected void loadObjects(DBRProgressMonitor monitor, YashanDBSchema schema, YashanDBTable forParent)
				throws DBException {

			if (forParent == null) {
				constraintCache.getAllObjects(monitor, schema);
			}
			super.loadObjects(monitor, schema, forParent);
		}

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(JDBCSession session, YashanDBSchema owner, YashanDBTable table)
				throws SQLException {
			StringBuilder sql = new StringBuilder(500);
			JDBCPreparedStatement dbStat;
			String constraintsView = YashanDBUtils.isAdminPriv(getDataSource(), "CONSTRAINTS");
			String consColumnsView = YashanDBUtils.isAdminPriv(getDataSource(), "CONS_COLUMNS");

			sql.append(
					"SELECT c.TABLE_NAME, c.CONSTRAINT_NAME,c.CONSTRAINT_TYPE,c.STATUS,c.R_OWNER,c.R_CONSTRAINT_NAME,rc.TABLE_NAME as "
							+ "R_TABLE_NAME,c.DELETE_RULE, \n" + "col.COLUMN_NAME,col.POSITION\r\n" + "FROM "
							+ constraintsView + " c, " + consColumnsView + " col, " + constraintsView + " rc\n"
							+ "WHERE c.CONSTRAINT_TYPE='R' AND c.OWNER=?\n"
							+ "AND c.OWNER=col.OWNER AND c.CONSTRAINT_NAME=col.CONSTRAINT_NAME\n"
							+ "AND rc.OWNER=c.r_OWNER AND rc.CONSTRAINT_NAME=c.R_CONSTRAINT_NAME");

			if (table != null) {
				sql.append(" AND c.TABLE_NAME=?");
			}
			sql.append("\r\nORDER BY c.CONSTRAINT_NAME,col.POSITION");

			dbStat = session.prepareStatement(sql.toString());

			dbStat.setString(1, YashanDBSchema.this.getName());
			if (table != null) {
				dbStat.setString(2, table.getName());
			}
			return dbStat;
		}

		@Nullable
		@Override
		protected YashanDBTableForeignKey fetchObject(JDBCSession session, YashanDBSchema owner, YashanDBTable parent,
				String indexName, JDBCResultSet dbResult) throws SQLException, DBException {
			return new YashanDBTableForeignKey(session.getProgressMonitor(), parent, dbResult);
		}

		@Nullable
		@Override
		protected YashanDBTableForeignKeyColumn[] fetchObjectRow(JDBCSession session, YashanDBTable parent,
				YashanDBTableForeignKey object, JDBCResultSet dbResult) throws SQLException, DBException {

			YashanDBTableColumn column = getTableColumn(session, parent, dbResult,
					JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME"));

			if (column == null) {
				return null;
			}

			return new YashanDBTableForeignKeyColumn[] {
					new YashanDBTableForeignKeyColumn(object, column, 1 + JDBCUtils.safeGetInt(dbResult, "POSITION")) };
		}

		@Override
		@SuppressWarnings("unchecked")
		protected void cacheChildren(DBRProgressMonitor monitor, YashanDBTableForeignKey foreignKey,
				List<YashanDBTableForeignKeyColumn> rows) {
			foreignKey.setAttributeReferences((List) rows);
		}
	}

	public class IndexCache extends
			JDBCCompositeCache<YashanDBSchema, YashanDBTablePhysical, YashanDBTableIndex, YashanDBTableIndexColumn> {

		IndexCache() {
			super(tableCache, YashanDBTablePhysical.class, "TABLE_NAME", "INDEX_NAME");
		}

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(JDBCSession session, YashanDBSchema owner,
				YashanDBTablePhysical table) throws SQLException {
			StringBuilder sql = new StringBuilder();
			sql.append(
					"SELECT i.OWNER,i.INDEX_NAME,i.INDEX_TYPE,i.TABLE_OWNER,i.TABLE_NAME,i.UNIQUENESS,i.TABLESPACE_NAME,i.STATUS,i.NUM_ROWS,i"
							+ ".SAMPLE_SIZE,\n" + "ic.COLUMN_NAME,ic.COLUMN_POSITION,ic.COLUMN_LENGTH,ic.DESCEND\n"
							+ "FROM " + YashanDBUtils.isAdminPriv(getDataSource(), "INDEXES") + " i\n" + "JOIN "
							+ YashanDBUtils.isAdminPriv(getDataSource(), "IND_COLUMNS") + " ic "
							+ "ON i.owner = ic.index_owner AND i.index_name = ic.index_name\n" + "WHERE ");
			if (table == null) {
				sql.append("i.OWNER=?");
			} else {
				sql.append("i.TABLE_OWNER=? AND i.TABLE_NAME=?");
			}
			sql.append("\nORDER BY i.INDEX_NAME,ic.COLUMN_POSITION");

			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());

			if (table == null) {
				dbStat.setString(1, YashanDBSchema.this.getName());
			} else {
				dbStat.setString(1, YashanDBSchema.this.getName());
				dbStat.setString(2, table.getName());
			}
			return dbStat;
		}

		@Nullable
		@Override
		protected YashanDBTableIndex fetchObject(JDBCSession session, YashanDBSchema owner,
				YashanDBTablePhysical parent, String indexName, JDBCResultSet dbResult)
				throws SQLException, DBException {
			return new YashanDBTableIndex(owner, parent, indexName, dbResult);
		}

		@Nullable
		@Override
		protected YashanDBTableIndexColumn[] fetchObjectRow(JDBCSession session, YashanDBTablePhysical parent,
				YashanDBTableIndex object, JDBCResultSet dbResult) throws SQLException, DBException {

			String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME");
			int ordinalPosition = JDBCUtils.safeGetInt(dbResult, "COLUMN_POSITION");
			boolean isAscending = "ASC".equals(JDBCUtils.safeGetStringTrimmed(dbResult, "DESCEND"));

			YashanDBTableColumn tableColumn = columnName == null ? null
					: parent.getAttribute(session.getProgressMonitor(), columnName);
			if (tableColumn == null) {
				log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '"
						+ object.getName() + "'");
				return null;
			}

			return new YashanDBTableIndexColumn[] {
					new YashanDBTableIndexColumn(object, tableColumn, ordinalPosition, isAscending) };
		}

		@Override
		protected void cacheChildren(DBRProgressMonitor monitor, YashanDBTableIndex index,
				List<YashanDBTableIndexColumn> rows) {
			index.setColumns(rows);
		}
	}

	public class TableTriggerCache
			extends JDBCCompositeCache<YashanDBSchema, YashanDBTableBase, YashanDBTableTrigger, YashanDBTriggerColumn> {

		protected TableTriggerCache() {
			super(tableCache, YashanDBTableBase.class, "TABLE_NAME", "TRIGGER_NAME");
		}

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(JDBCSession session, YashanDBSchema schema,
				YashanDBTableBase table) throws SQLException {
			final JDBCPreparedStatement dbStmt = session
					.prepareStatement("SELECT t.*, c.*, c.COLUMN_NAME AS TRIGGER_COLUMN_NAME" + "\nFROM "
							+ YashanDBUtils.isAdminPriv(schema.getDataSource(), "TRIGGERS") + " t" + " LEFT JOIN "
							+ YashanDBUtils.isAdminPriv(schema.getDataSource(), "TRIGGER_COLS") + " c"
							+ " ON t.TABLE_OWNER=c.TABLE_OWNER AND t.TABLE_NAME=c.TABLE_NAME"
							+ " AND t.OWNER=c.TRIGGER_OWNER AND t.TRIGGER_NAME=c.TRIGGER_NAME"
							+ "\nWHERE t.TABLE_OWNER=?" + (table == null ? "" : " AND t.TABLE_NAME=?")
							+ " AND t.BASE_OBJECT_TYPE=" + (table instanceof YashanDBView ? "'VIEW'" : "'TABLE'")
							+ "\nORDER BY t.TRIGGER_NAME");
			dbStmt.setString(1, schema.getName());
			if (table != null) {
				dbStmt.setString(2, table.getName());
			}
			return dbStmt;
		}

		@Nullable
		@Override
		protected YashanDBTableTrigger fetchObject(JDBCSession session, YashanDBSchema schema, YashanDBTableBase table,
				String childName, JDBCResultSet resultSet) throws SQLException, DBException {
			return new YashanDBTableTrigger(table, resultSet);
		}

		@Nullable
		@Override
		protected YashanDBTriggerColumn[] fetchObjectRow(JDBCSession session, YashanDBTableBase table,
				YashanDBTableTrigger trigger, JDBCResultSet resultSet) throws DBException {
			final YashanDBTableBase refTable = YashanDBTableBase.findTable(session.getProgressMonitor(),
					table.getDataSource(), JDBCUtils.safeGetString(resultSet, "TABLE_OWNER"),
					JDBCUtils.safeGetString(resultSet, "TABLE_NAME"));
			if (refTable != null) {
				final String columnName = JDBCUtils.safeGetString(resultSet, "TRIGGER_COLUMN_NAME");
				if (columnName == null) {
					return null;
				}
				final YashanDBTableColumn tableColumn = refTable.getAttribute(session.getProgressMonitor(), columnName);
				if (tableColumn == null) {
					log.debug("Column '" + columnName + "' not found in table '"
							+ refTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + "' for trigger '"
							+ trigger.getName() + "'");
					return null;
				}
				return new YashanDBTriggerColumn[] {
						new YashanDBTriggerColumn(session.getProgressMonitor(), trigger, tableColumn, resultSet) };
			}
			return null;
		}

		@Override
		protected void cacheChildren(DBRProgressMonitor monitor, YashanDBTableTrigger trigger,
				List<YashanDBTriggerColumn> columns) {
			trigger.setColumns(columns);
		}

		@Override
		protected boolean isEmptyObjectRowsAllowed() {
			return true;
		}
	}

	public class SequenceCache extends JDBCObjectCache<YashanDBSchema, YashanDBSequence> {
		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBSchema owner)
				throws SQLException {
			final JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT * FROM " + YashanDBUtils.isAdminPriv(owner.getDataSource(), "SEQUENCES")
							+ " WHERE SEQUENCE_OWNER=? ORDER BY SEQUENCE_NAME");
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected YashanDBSequence fetchObject(@NotNull JDBCSession session, @NotNull YashanDBSchema owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new YashanDBSequence(owner, resultSet);
		}
	}

	public class ProceduresCache extends JDBCObjectLookupCache<YashanDBSchema, YashanDBProcedureStandalone> {

		@NotNull
		@Override
		public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull YashanDBSchema owner,
				@Nullable YashanDBProcedureStandalone object, @Nullable String objectName) throws SQLException {
			JDBCPreparedStatement dbStat = session.prepareStatement("SELECT o.*, a.SUBPROGRAM_ID  FROM "
					+ YashanDBUtils.isAdminPriv(owner.getDataSource(), "OBJECTS") + " o join "
					+ YashanDBUtils.isAdminPriv(owner.getDataSource(), "PROCEDURES") + " a on "
					+ "o.OBJECT_id=a.OBJECT_id" + " WHERE o.OBJECT_TYPE IN ('PROCEDURE','UDF') " + "AND o.OWNER=? "
					+ (object == null && objectName == null ? "" : "AND o.OBJECT_NAME=? ") + "ORDER BY o.OBJECT_NAME");
			dbStat.setString(1, owner.getName());
			if (object != null || objectName != null)
				dbStat.setString(2, object != null ? object.getName() : objectName);
			return dbStat;
		}

		@Override
		protected YashanDBProcedureStandalone fetchObject(@NotNull JDBCSession session, @NotNull YashanDBSchema owner,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new YashanDBProcedureStandalone(session.getProgressMonitor(), owner, dbResult);
		}

	}

	public class PackageCache extends JDBCObjectCache<YashanDBSchema, YashanDBPackage> {

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBSchema owner)
				throws SQLException {
			JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT OBJECT_NAME, CREATED,LAST_DDL_TIME,TEMPORARY,STATUS FROM "
							+ YashanDBUtils.isAdminPriv(owner.getDataSource(), "OBJECTS")
							+ " WHERE OBJECT_TYPE='PACKAGE' AND OWNER=? " + " ORDER BY OBJECT_NAME");
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected YashanDBPackage fetchObject(@NotNull JDBCSession session, @NotNull YashanDBSchema owner,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new YashanDBPackage(owner, dbResult);
		}

	}

	public class SynonymCache extends JDBCObjectLookupCache<YashanDBSchema, YashanDBSynonym> {

		@Override
		public JDBCStatement prepareLookupStatement(JDBCSession session, YashanDBSchema owner, YashanDBSynonym object,
				String objectName) throws SQLException {

			String synonymName = object != null ? object.getName() : objectName;
			StringBuilder sql = new StringBuilder();
			sql.append(
					"SELECT OWNER, SYNONYM_NAME, MAX(TABLE_OWNER) as TABLE_OWNER, MAX(TABLE_NAME) as TABLE_NAME, MAX(DB_LINK) as DB_LINK, MAX(OBJECT_TYPE) as OBJECT_TYPE FROM (\n")
					.append("SELECT S.*, NULL OBJECT_TYPE FROM ")
					.append(YashanDBUtils.isAdminPriv(owner.getDataSource(), "SYNONYMS"))
					.append(" S WHERE S.OWNER = ?");

			if (synonymName != null) {
				sql.append(" AND S.SYNONYM_NAME = ?");
			}

			sql.append("\nUNION ALL\n").append("SELECT S.*,O.OBJECT_TYPE FROM ")
					.append(YashanDBUtils.isAdminPriv(owner.getDataSource(), "SYNONYMS")).append(" S, ")
					.append(YashanDBUtils.isAdminPriv(owner.getDataSource(), "OBJECTS")).append(" O\n")
					.append("WHERE S.OWNER = ?\n");

			if (synonymName != null) {
				sql.append(" AND S.SYNONYM_NAME = ? ");
			}

			sql.append("AND O.OWNER=S.TABLE_OWNER AND O.OBJECT_NAME=S.TABLE_NAME AND O.SUBOBJECT_NAME IS NULL\n)\n");
			sql.append("GROUP BY OWNER, SYNONYM_NAME");

			if (synonymName == null) {
				sql.append("\nORDER BY SYNONYM_NAME");
			}
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			int paramNum = 1;
			dbStat.setString(paramNum++, owner.getName());
			if (synonymName != null) {
				dbStat.setString(paramNum++, synonymName);
			}
			dbStat.setString(paramNum++, owner.getName());
			if (synonymName != null) {
				dbStat.setString(paramNum++, synonymName);
			}
			return dbStat;
		}

		@Override
		protected YashanDBSynonym fetchObject(JDBCSession session, YashanDBSchema owner, JDBCResultSet resultSet)
				throws SQLException, DBException {
			return new YashanDBSynonym(owner, resultSet);
		}

	}

	static class DBLinkCache extends JDBCObjectCache<YashanDBSchema, YashanDBDBLink> {

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBSchema owner)
				throws SQLException {
			JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT * FROM " + YashanDBUtils.isAdminPriv(owner.getDataSource(), "DB_LINKS")
							+ " WHERE OWNER=? " + " ORDER BY DB_LINK");
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected YashanDBDBLink fetchObject(@NotNull JDBCSession session, @NotNull YashanDBSchema owner,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new YashanDBDBLink(session.getProgressMonitor(), owner, dbResult);
		}
	}

	static class JobCache extends JDBCObjectCache<YashanDBSchema, YashanDBJob> {
		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBSchema owner)
				throws SQLException {
			return session.prepareStatement(
					"SELECT * FROM " + YashanDBUtils.isAdminPriv(owner.getDataSource(), "JOBS") + " ORDER BY JOB");
		}

		@Override
		protected YashanDBJob fetchObject(@NotNull JDBCSession session, @NotNull YashanDBSchema owner,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new YashanDBJob(owner, dbResult);
		}
	}

	static class SchedulerJobCache extends JDBCObjectCache<YashanDBSchema, YashanDBSchedulerJob> {

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBSchema owner)
				throws SQLException {
			JDBCPreparedStatement dbStat = session.prepareStatement(
					"SELECT * FROM " + YashanDBUtils.isAdminPriv(owner.getDataSource(), "SCHEDULER_JOBS")
							+ " WHERE OWNER=? ORDER BY JOB_NAME");
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected YashanDBSchedulerJob fetchObject(@NotNull JDBCSession session, @NotNull YashanDBSchema owner,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new YashanDBSchedulerJob(owner, dbResult);
		}
	}

	static class RecycleBin extends JDBCObjectCache<YashanDBSchema, YashanDBRecycledObject> {

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBSchema owner)
				throws SQLException {
			final boolean isPublic = owner.isPublic();
			JDBCPreparedStatement dbStat = session.prepareStatement(isPublic
					? "SELECT * FROM " + YashanDBUtils.getSysSchemaPrefix(owner.getDataSource()) + "USER_RECYCLEBIN"
					: "SELECT * FROM " + YashanDBUtils.getSysSchemaPrefix(owner.getDataSource())
							+ "DBA_RECYCLEBIN WHERE OWNER=?");
			if (!isPublic) {
				dbStat.setString(1, owner.getName());
			}
			return dbStat;
		}

		@Override
		protected YashanDBRecycledObject fetchObject(@NotNull JDBCSession session, @NotNull YashanDBSchema owner,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new YashanDBRecycledObject(owner, dbResult);
		}
	}

	class SpecialPosition {

		private final String column;
		private final int pos;

		public SpecialPosition(String value) {
			String data[] = value.split(":");
			this.column = data[0];
			this.pos = data.length == 1 ? 0 : Integer.valueOf(data[1]);

		}

		public SpecialPosition(String column, int pos) {
			this.column = column;
			this.pos = pos;
		}

		public String getColumn() {
			return column;
		}

		public int getPos() {
			return pos;
		}

	}

	@Association
	public Collection<YashanDBSynonym> getSynonyms(DBRProgressMonitor monitor) throws DBException {
		return synonymCache.getAllObjects(monitor, this);
	}

	@Association
	public Collection<YashanDBDBLink> getDatabaseLinks(DBRProgressMonitor monitor) throws DBException {
		return dbLinkCache.getAllObjects(monitor, this);
	}

	@Association
	public Collection<YashanDBJob> getJobs(@NotNull DBRProgressMonitor monitor) throws DBException {
		return jobCache.getAllObjects(monitor, this);
	}

	@Association
	public Collection<YashanDBSchedulerJob> getSchedulerJobs(DBRProgressMonitor monitor) throws DBException {
		return schedulerJobCache.getAllObjects(monitor, this);
	}
	
    @Association
    public Collection<YashanDBRecycledObject> getRecycledObjects(DBRProgressMonitor monitor)
            throws DBException {
        return recycleBin.getAllObjects(monitor, this);
    }

	@Association
	public Collection<YashanDBTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
		return indexCache.getObjects(monitor, this, null);
	}

	@Association
	public Collection<YashanDBTable> getTables(DBRProgressMonitor monitor) throws DBException {
		return tableCache.getTypedObjects(monitor, this, YashanDBTable.class);
	}

	public YashanDBTable getTable(DBRProgressMonitor monitor, String name) throws DBException {
		return tableCache.getObject(monitor, this, name, YashanDBTable.class);
	}

	@Association
	public Collection<YashanDBView> getViews(DBRProgressMonitor monitor) throws DBException {
		return tableCache.getTypedObjects(monitor, this, YashanDBView.class);
	}

	public YashanDBView getView(DBRProgressMonitor monitor, String name) throws DBException {
		return tableCache.getObject(monitor, this, name, YashanDBView.class);
	}

	@Association
	public Collection<YashanDBMaterializedView> getMaterializedViews(DBRProgressMonitor monitor) throws DBException {
		return tableCache.getTypedObjects(monitor, this, YashanDBMaterializedView.class);
	}

	@Association
	public YashanDBMaterializedView getMaterializedView(DBRProgressMonitor monitor, String name) throws DBException {
		return tableCache.getObject(monitor, this, name, YashanDBMaterializedView.class);
	}

	@Association
	public Collection<YashanDBTableTrigger> getTableTriggers(DBRProgressMonitor monitor) throws DBException {
		tableTriggerCache.clearCache();
		return tableTriggerCache.getAllObjects(monitor, this);
	}

	@Association
	public Collection<YashanDBDataType> getDataTypes(DBRProgressMonitor monitor) throws DBException {
		return dataTypeCache.getAllObjects(monitor, this);
	}

	public YashanDBDataType getDataType(DBRProgressMonitor monitor, String name) throws DBException {
		return dataTypeCache.getObject(monitor, this, name);
	}

	@Association
	public Collection<YashanDBSequence> getSequences(DBRProgressMonitor monitor) throws DBException {
		return sequenceCache.getAllObjects(monitor, this);
	}

	@Association
	public Collection<YashanDBPackage> getPackages(DBRProgressMonitor monitor) throws DBException {
		return packageCache.getAllObjects(monitor, this);
	}

	@Association
	public Collection<YashanDBProcedureStandalone> getProceduresOnly(DBRProgressMonitor monitor) throws DBException {
		return getProcedures(monitor).stream().filter(proc -> proc.getProcedureType() == DBSProcedureType.PROCEDURE)
				.collect(Collectors.toList());
	}

	@Association
	public Collection<YashanDBProcedureStandalone> getFunctionsOnly(DBRProgressMonitor monitor) throws DBException {
		return getProcedures(monitor).stream().filter(proc -> proc.getProcedureType() == DBSProcedureType.FUNCTION)
				.collect(Collectors.toList());
	}

	@Association
	public Collection<YashanDBProcedureStandalone> getProcedures(DBRProgressMonitor monitor) throws DBException {
		return proceduresCache.getAllObjects(monitor, this);
	}

	@Override
	public YashanDBProcedureStandalone getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
		return proceduresCache.getObject(monitor, this, uniqueName);
	}

	@Override
	public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		hasStatistics = false;
		tableCache.clearCache();
		dataTypeCache.clearCache();
		constraintCache.clearCache();
		indexCache.clearCache();
		foreignKeyCache.clearCache();
		tableTriggerCache.clearCache();
		sequenceCache.clearCache();
		proceduresCache.clearCache();
		packageCache.clearCache();
		synonymCache.clearCache();
		dbLinkCache.clearCache();
		jobCache.clearCache();
		schedulerJobCache.clearCache();
		return this;
	}

	@Override
	public boolean isSystem() {
		return ArrayUtils.contains(YashanDBConstants.SYSTEM_SCHEMAS, getName());
	}

	@Override
	public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor) throws DBException {
		List<DBSObject> children = new ArrayList<>(tableCache.getAllObjects(monitor, this));
		if (FUNC_PROC_AS_CHILDREN) {
			children.addAll(proceduresCache.getAllObjects(monitor, this));
		}
		if (SEQUENCE_AS_CHILDREN) {
			children.addAll(sequenceCache.getAllObjects(monitor, this));
		}
		if (PACKAGE_AS_CHILDREN) {
			children.addAll(packageCache.getAllObjects(monitor, this));
		}
		if (TYPE_AS_CHILDREN) {
			children.addAll(dataTypeCache.getAllObjects(monitor, this));
		}
		return children;
	}

	@Override
	public DBSObject getChild(DBRProgressMonitor monitor, String childName) throws DBException {
		final YashanDBTableBase table = tableCache.getObject(monitor, this, childName);
		if (table != null) {
			return table;
		}
		return null;
	}

	@Override
	public Class<? extends DBSObject> getPrimaryChildType(DBRProgressMonitor monitor) throws DBException {
		return YashanDBTable.class;
	}

	@Override
	public synchronized void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
		monitor.subTask("Cache tables");
		tableCache.getAllObjects(monitor, this);
		if ((scope & STRUCT_ATTRIBUTES) != 0) {
			monitor.subTask("Cache table columns");
			tableCache.loadChildren(monitor, this, null);
		}
		if ((scope & STRUCT_ASSOCIATIONS) != 0) {
			monitor.subTask("Cache table indexes");
			indexCache.getObjects(monitor, this, null);
			monitor.subTask("Cache table constraints");
			constraintCache.getObjects(monitor, this, null);
			foreignKeyCache.getObjects(monitor, this, null);
		}

	}
}
