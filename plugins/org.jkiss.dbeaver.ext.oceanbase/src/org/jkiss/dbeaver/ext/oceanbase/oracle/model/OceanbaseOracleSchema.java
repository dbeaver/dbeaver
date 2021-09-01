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

package org.jkiss.dbeaver.ext.oceanbase.oracle.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleMaterializedView;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleTable;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableTrigger;
import org.jkiss.dbeaver.ext.oracle.model.OracleTriggerColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleView;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

public class OceanbaseOracleSchema extends OracleSchema {
	final public OceanbaseTableCache oceanbaseTableCache = new OceanbaseTableCache();
	final public OceanbaseTableTriggerCache oceanbaseTableTriggerCache = new OceanbaseTableTriggerCache();

	public OceanbaseOracleSchema(OracleDataSource dataSource, long id, String name) {
		super(dataSource, id, name);
	}

	public OceanbaseOracleSchema(@NotNull OracleDataSource dataSource, @NotNull ResultSet dbResult) {
		super(dataSource, dbResult);
	}

	public class OceanbaseTableCache
			extends JDBCStructLookupCache<OceanbaseOracleSchema, OracleTableBase, OracleTableColumn> {

		OceanbaseTableCache() {
			super("OBJECT_NAME");
			setListOrderComparator(DBUtils.nameComparator());
		}

		@NotNull
		@Override
		public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull OceanbaseOracleSchema owner,
				@Nullable OracleTableBase object, @Nullable String objectName) throws SQLException {
			String tableOper = "=";

			boolean hasAllAllTables = owner.getDataSource().isViewAvailable(session.getProgressMonitor(), null,
					"ALL_ALL_TABLES");
			boolean useAlternativeQuery = CommonUtils
					.toBoolean(getDataSource().getContainer().getConnectionConfiguration()
							.getProviderProperty(OracleConstants.PROP_METADATA_USE_ALTERNATIVE_TABLE_QUERY));
			String tablesSource = hasAllAllTables ? "ALL_TABLES" : "TABLES";
			String tableTypeColumns = hasAllAllTables ? "t.TABLE_TYPE_OWNER,t.TABLE_TYPE"
					: "NULL as TABLE_TYPE_OWNER, NULL as TABLE_TYPE";

			JDBCPreparedStatement dbStat;
			if (!useAlternativeQuery) {
				dbStat = session.prepareStatement("SELECT " + OracleUtils.getSysCatalogHint(owner.getDataSource())
						+ " O.*,\n" + tableTypeColumns
						+ ",t.TABLESPACE_NAME,t.PARTITIONED,t.IOT_TYPE,t.IOT_NAME,t.TEMPORARY,t.SECONDARY,t.NESTED,t.NUM_ROWS\n"
						+ "FROM "
						+ OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "OBJECTS")
						+ " O\n" + ", "
						+ OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(),
								tablesSource)
						+ " t WHERE t.OWNER(+) = O.OWNER AND t.TABLE_NAME(+) = o.OBJECT_NAME\n"
						+ "AND O.OWNER=? AND O.OBJECT_TYPE IN ('TABLE', 'VIEW', 'MATERIALIZED VIEW')"
						+ (object == null && objectName == null ? "" : " AND O.OBJECT_NAME" + tableOper + "?")
						+ (object instanceof OracleTable ? " AND O.OBJECT_TYPE='TABLE'" : "")
						+ (object instanceof OracleView ? " AND O.OBJECT_TYPE='VIEW'" : "")
						+ (object instanceof OracleMaterializedView ? " AND O.OBJECT_TYPE='MATERIALIZED VIEW'" : ""));
				dbStat.setString(1, owner.getName());
				if (object != null || objectName != null)
					dbStat.setString(2, object != null ? object.getName() : objectName);
				return dbStat;
			} else {
				return getAlternativeTableStatement(session, owner, object, objectName, tablesSource, tableTypeColumns);
			}
		}

		@Override
		protected OracleTableBase fetchObject(@NotNull JDBCSession session, @NotNull OceanbaseOracleSchema owner,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			final String tableType = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
			if ("TABLE".equals(tableType)) {
				return new OceanbaseOracleTable(session.getProgressMonitor(), owner, dbResult);
			} else if ("MATERIALIZED VIEW".equals(tableType)) {
				return new OracleMaterializedView(owner, dbResult);
			} else {
				return new OceanbaseOracleView(owner, dbResult);
			}
		}

		@Override
		protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session,
				@NotNull OceanbaseOracleSchema owner, @Nullable OracleTableBase forTable) throws SQLException {
			String colsView;
			if (!owner.getDataSource().isViewAvailable(session.getProgressMonitor(), OracleConstants.SCHEMA_SYS,
					"ALL_TAB_COLS")) {
				colsView = "TAB_COLUMNS";
			} else {
				colsView = "TAB_COLS";
			}
			if (forTable instanceof OceanbaseOracleView) {
				JDBCPreparedStatement dbStat = session
						.prepareStatement("desc " + owner.getName() + "." + forTable.getName());
				return dbStat;
			}
			StringBuilder sql = new StringBuilder(500);
			sql.append("SELECT ").append(OracleUtils.getSysCatalogHint(owner.getDataSource()))
					.append("\nc.*,c.TABLE_NAME as OBJECT_NAME " + "FROM ")
					.append(OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), colsView))
					.append(" c\n" +
//                    "LEFT OUTER JOIN " + OracleUtils.getSysSchemaPrefix(getDataSource()) + "ALL_COL_COMMENTS cc ON CC.OWNER=c.OWNER AND cc.TABLE_NAME=c.TABLE_NAME AND cc.COLUMN_NAME=c.COLUMN_NAME\n" +
							"WHERE c.OWNER=?");
			if (forTable != null) {
				sql.append(" AND c.TABLE_NAME=?");
			}
			/*
			 * sql.append("\nORDER BY "); if (forTable != null) {
			 * sql.append("c.TABLE_NAME,"); } sql.append("c.COLUMN_ID");
			 */
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			dbStat.setString(1, owner.getName());
			if (forTable != null) {
				dbStat.setString(2, forTable.getName());
			}
			return dbStat;
		}

		@Override
		protected OracleTableColumn fetchChild(@NotNull JDBCSession session, @NotNull OceanbaseOracleSchema owner,
				@NotNull OracleTableBase table, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			if(table instanceof OceanbaseOracleView) {
				return new OceanbaseOracleViewColumn(session.getProgressMonitor(), table, dbResult);
			}
			return new OracleTableColumn(session.getProgressMonitor(), table, dbResult);
		}

		@Override
		protected void cacheChildren(OracleTableBase parent, List<OracleTableColumn> oracleTableColumns) {
			oracleTableColumns.sort(DBUtils.orderComparator());
			super.cacheChildren(parent, oracleTableColumns);
		}

		@NotNull
		private JDBCStatement getAlternativeTableStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner,
				@Nullable OracleTableBase object, @Nullable String objectName, String tablesSource,
				String tableTypeColumns) throws SQLException {
			boolean hasName = object == null && objectName != null;
			JDBCPreparedStatement dbStat;
			StringBuilder sql = new StringBuilder();
			String tableQuery = "SELECT t.OWNER, t.TABLE_NAME AS OBJECT_NAME, 'TABLE' AS OBJECT_TYPE, 'VALID' AS STATUS,"
					+ tableTypeColumns + ", t.TABLESPACE_NAME,\n"
					+ "t.PARTITIONED, t.IOT_TYPE, t.IOT_NAME, t.TEMPORARY, t.SECONDARY, t.NESTED, t.NUM_ROWS\n"
					+ "FROM " + OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(),
							tablesSource)
					+ " t\n" + "WHERE t.OWNER =?\n" + "AND NESTED = 'NO'\n";
			String viewQuery = "SELECT o.OWNER, o.OBJECT_NAME, 'VIEW' AS OBJECT_TYPE, o.STATUS, NULL, NULL, NULL, 'NO', NULL, NULL, o.TEMPORARY, o.SECONDARY, 'NO', 0\n"
					+ "FROM "
					+ OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "OBJECTS")
					+ " o\n" + "WHERE o.OWNER =?\n" + "AND o.OBJECT_TYPE = 'VIEW'\n";
			String mviewQuery = "SELECT o.OWNER, o.OBJECT_NAME, 'MATERIALIZED VIEW' AS OBJECT_TYPE, o.STATUS, NULL, NULL, NULL, 'NO', NULL, NULL, o.TEMPORARY, o.SECONDARY, 'NO', 0\n"
					+ "FROM "
					+ OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "OBJECTS")
					+ " o\n" + "WHERE o.OWNER =?\n" + "AND o.OBJECT_TYPE = 'MATERIALIZED VIEW'";
			String unionAll = "UNION ALL ";
			if (hasName) {
				sql.append("SELECT * FROM (");
			}
			if (object == null) {
				sql.append(tableQuery).append(unionAll).append(viewQuery).append(unionAll).append(mviewQuery);
			} else if (object instanceof OracleMaterializedView) {
				sql.append(mviewQuery);
			} else if (object instanceof OracleView) {
				sql.append(viewQuery);
			} else {
				sql.append(tableQuery);
			}
			if (hasName) {
				sql.append(") WHERE OBJECT_NAME").append("=?");
			} else if (object != null) {
				if (object instanceof OracleTable) {
					sql.append(" AND t.TABLE_NAME=?");
				} else {
					sql.append(" AND o.OBJECT_NAME=?");
				}
			}
			dbStat = session.prepareStatement(sql.toString());
			String ownerName = owner.getName();
			dbStat.setString(1, ownerName);
			if (object == null) {
				dbStat.setString(2, ownerName);
				dbStat.setString(3, ownerName);
				if (objectName != null) {
					dbStat.setString(4, objectName);
				}
			} else {
				dbStat.setString(2, object.getName());
			}
			return dbStat;
		}
	}

	class OceanbaseTableTriggerCache extends
			JDBCCompositeCache<OceanbaseOracleSchema, OracleTableBase, OracleTableTrigger, OracleTriggerColumn> {
		protected OceanbaseTableTriggerCache() {
			super(oceanbaseTableCache, OracleTableBase.class, "TABLE_NAME", "TRIGGER_NAME");
		}

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(JDBCSession session, OceanbaseOracleSchema schema,
				OracleTableBase table) throws SQLException {
			final JDBCPreparedStatement dbStmt = session.prepareStatement(
					"SELECT" + OracleUtils.getSysCatalogHint(schema.getDataSource()) + " t.*" + "\nFROM "
							+ OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), schema.getDataSource(),
									"TRIGGERS")
							+ " t " + "\nWHERE t.TABLE_OWNER=?" + (table == null ? "" : " AND t.TABLE_NAME=?")
							+ " AND t.BASE_OBJECT_TYPE='TABLE'" + "\nORDER BY t.TRIGGER_NAME");
			dbStmt.setString(1, schema.getName());
			if (table != null) {
				dbStmt.setString(2, table.getName());
			}
			return dbStmt;
		}

		@Nullable
		@Override
		protected OracleTableTrigger fetchObject(JDBCSession session, OceanbaseOracleSchema schema,
				OracleTableBase table, String childName, JDBCResultSet resultSet) throws SQLException, DBException {
			return new OracleTableTrigger(table, resultSet);
		}

		@Nullable
		@Override
		protected OracleTriggerColumn[] fetchObjectRow(JDBCSession session, OracleTableBase table,
				OracleTableTrigger trigger, JDBCResultSet resultSet) throws DBException {
			final OracleTableBase refTable = OracleTableBase.findTable(session.getProgressMonitor(),
					table.getDataSource(), JDBCUtils.safeGetString(resultSet, "TABLE_OWNER"),
					JDBCUtils.safeGetString(resultSet, "TABLE_NAME"));
			if (refTable != null) {
				final String columnName = JDBCUtils.safeGetString(resultSet, "TRIGGER_COLUMN_NAME");
				if (columnName == null) {
					return null;
				}
				final OracleTableColumn tableColumn = refTable.getAttribute(session.getProgressMonitor(), columnName);
				if (tableColumn == null) {
					log.debug("Column '" + columnName + "' not found in table '"
							+ refTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + "' for trigger '"
							+ trigger.getName() + "'");
					return null;
				}
				return new OracleTriggerColumn[] {
						new OracleTriggerColumn(session.getProgressMonitor(), trigger, tableColumn, resultSet) };
			}
			return null;
		}

		@Override
		protected void cacheChildren(DBRProgressMonitor monitor, OracleTableTrigger trigger,
				List<OracleTriggerColumn> columns) {
			trigger.setColumns(columns);
		}

		@Override
		protected boolean isEmptyObjectRowsAllowed() {
			return true;
		}
	}

	@Association
	@Override
	public Collection<OracleTable> getTables(DBRProgressMonitor monitor) throws DBException {
		return oceanbaseTableCache.getTypedObjects(monitor, this, OracleTable.class);
	}

	@Override
	public OracleTable getTable(DBRProgressMonitor monitor, String name) throws DBException {
		return oceanbaseTableCache.getObject(monitor, this, name, OracleTable.class);
	}

	@Association
	@Override
	public Collection<OracleView> getViews(DBRProgressMonitor monitor) throws DBException {
		return oceanbaseTableCache.getTypedObjects(monitor, this, OracleView.class);
	}

	@Override
	public OracleView getView(DBRProgressMonitor monitor, String name) throws DBException {
		return oceanbaseTableCache.getObject(monitor, this, name, OracleView.class);
	}

	@Association
	@Override
	public Collection<OracleMaterializedView> getMaterializedViews(DBRProgressMonitor monitor) throws DBException {
		return oceanbaseTableCache.getTypedObjects(monitor, this, OracleMaterializedView.class);
	}

	@Association
	@Override
	public OracleMaterializedView getMaterializedView(DBRProgressMonitor monitor, String name) throws DBException {
		return oceanbaseTableCache.getObject(monitor, this, name, OracleMaterializedView.class);
	}

	@Association
	public Collection<OracleTableTrigger> getTableTriggers(DBRProgressMonitor monitor) throws DBException {
		return oceanbaseTableTriggerCache.getAllObjects(monitor, this);
	}

	@Override
	public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
		final OracleTableBase table = oceanbaseTableCache.getObject(monitor, this, childName);
		if (table != null) {
			return table;
		}
		return super.getChild(monitor, childName);
	}

	@Override
	public synchronized void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
		monitor.subTask("Cache tables");
		oceanbaseTableCache.getAllObjects(monitor, this);
		if ((scope & STRUCT_ATTRIBUTES) != 0) {
			monitor.subTask("Cache table columns");
			oceanbaseTableCache.loadChildren(monitor, this, null);
		}
		super.cacheStructure(monitor, scope);
	}

	@Override
	public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		oceanbaseTableCache.clearCache();
		oceanbaseTableTriggerCache.clearCache();
		return super.refreshObject(monitor);
	}

	@Override
	public boolean isSystem() {
		return false;
	}

}
