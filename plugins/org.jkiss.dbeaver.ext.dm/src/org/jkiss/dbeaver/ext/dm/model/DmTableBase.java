package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.source.DmStatefulObject;
import org.jkiss.dbeaver.ext.dm.model.utils.DmConstants;
import org.jkiss.dbeaver.ext.dm.model.utils.DmUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.utils.CommonUtils;

/**
 * DM Table Base (Table And View)
 * Table - Column Comment,Trigger,Priv
 * @author caosw
 *
 */
public abstract class DmTableBase extends JDBCTable<DmDataSource, DmSchema>
		implements DBPNamedObject2, DBPRefreshableObject, DmStatefulObject {

	private static Log log = Log.getLog(DmTableBase.class);
	
	public Map<String, String> commentsMap=new HashMap<>(); //存放当前表所有注释

	public static class TableAdditionalInfo {
		volatile boolean loaded = false;

		boolean isLoaded() {
			return loaded;
		}
	}
	

	public static class AdditionalInfoValidator implements IPropertyCacheValidator<DmTableBase> {

		@Override
		public boolean isPropertyCached(DmTableBase object, Object propertyId) {
			return object.getAdditionalInfo().isLoaded();
		}
	}

	public static class CommentsValidator implements IPropertyCacheValidator<DmTableBase> {

		@Override
		public boolean isPropertyCached(DmTableBase object, Object propertyId) {
			return object.comment != null;
		}
	}

	public final TriggerCache triggerCache = new TriggerCache();
	private final TablePrivCache tablePrivCache = new TablePrivCache();

	public abstract TableAdditionalInfo getAdditionalInfo();

	public abstract String getTableTypeName();

	protected boolean valid;
	private String comment;

	protected DmTableBase(DmSchema schema, String name, boolean persisted) {
		super(schema, name, persisted);
	}

	protected DmTableBase(DmSchema schema, ResultSet dbResult) {
		super(schema, true);
		setName(JDBCUtils.safeGetString(dbResult, "TABLE_NAME"));
		this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));
		loadColComments(new VoidProgressMonitor());//获取表注释
	}

	private void loadColComments(DBRProgressMonitor monitor) {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load tableCol Comments")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT \"COLNAME\",\"COMMENT$\" FROM SYS.SYSCOLUMNCOMMENTS WHERE TVNAME = ? AND SCHNAME = ?")) {
            	dbStat.setString(1, getName());
            	dbStat.setString(2, getSchema().getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while(dbResult.next()) {
                    	commentsMap.put(JDBCUtils.safeGetString(dbResult, "COLNAME"),JDBCUtils.safeGetString(dbResult, "COMMENT$"));
                    }
                }
            } catch (SQLException e) {
                log.warn("获取DM表或视图列注释失败");
            }
        }catch (Exception e) {
			log.warn("获取列注释失败");
		}
	}
	
	@Override
	public JDBCStructCache<DmSchema, ? extends JDBCTable, ? extends JDBCTableColumn> getCache() {
		return getContainer().tableCache;
	}

	@Override
	@NotNull
	public DmSchema getSchema() {
		return super.getContainer();
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
	public String getName() {
		return super.getName();
	}

	@Nullable
	@Override
	public String getDescription() {
		return getComment();
	}

	@Override
	@NotNull
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getContainer(), this);
	}

	@Property(viewable = true, editable = true, updatable = true,length = PropertyLength.MULTILINE, order = 100)
	@LazyProperty(cacheValidator = CommentsValidator.class)
	public String getComment(DBRProgressMonitor monitor) throws DBException {
		if (comment == null) {
			try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table comments")) {
				comment = queryTableComment(session);
			} catch (SQLException e) {
				log.warn("Can't fetch table '" + getName() + "' comment", e);
			}
		}
		return comment;
	}

	protected String queryTableComment(JDBCSession session) throws SQLException {
		return JDBCUtils.queryString(session,
				"SELECT COMMENTS FROM "
						+ DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(),
								(DmDataSource) session.getDataSource(), "TAB_COMMENTS")
						+ " " + "WHERE OWNER=? AND TABLE_NAME=? AND TABLE_TYPE=?",
				getSchema().getName(), getName(), getTableTypeName());
	}

	void loadColumnComments(DBRProgressMonitor monitor) {
		try {
			try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table column comments")) {
				try (JDBCPreparedStatement stat = session.prepareStatement("SELECT COLUMN_NAME,COMMENTS FROM "
						+ DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(),
								(DmDataSource) session.getDataSource(), "COL_COMMENTS")
						+ " cc " + "WHERE CC.OWNER=? AND cc.TABLE_NAME=?")) {
					stat.setString(1, getSchema().getName());
					stat.setString(2, getName());
					try (JDBCResultSet rs = stat.executeQuery()) {
						while (rs.next()) {
							String colName = rs.getString(1);
							String colComment = rs.getString(2);
							DmTableColumn col = getAttribute(monitor, colName);
							if (col == null) {
								log.warn("Column '" + colName + "' not found in table '"
										+ getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
							} else {
								col.setComment(CommonUtils.notEmpty(colComment));
							}
						}
					}
				}
			}
			for (DmTableColumn col : getAttributes(monitor)) {
				col.cacheComment();
			}
		} catch (Exception e) {
			log.warn("Error fetching table '" + getName() + "' column comments", e);
		}
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public List<DmTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
		return getContainer().tableCache.getChildren(monitor, getContainer(), this);
	}

	@Override
	public DmTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
			throws DBException {
		return getContainer().tableCache.getChild(monitor, getContainer(), this, attributeName);
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		getContainer().constraintCache.clearObjectCache(this);
		return getContainer().tableCache.refreshObject(monitor, getContainer(), this);
	}

	@Association
	public List<DmTableTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException {
		return triggerCache.getAllObjects(monitor, this);
	}

	@Override
	public Collection<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
		return null;
	}

	@Nullable
	@Override
	@Association
	public Collection<DmTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
		return getContainer().constraintCache.getObjects(monitor, getContainer(), this);
	}

	public DmTableConstraint getConstraint(DBRProgressMonitor monitor, String ukName) throws DBException {
		return getContainer().constraintCache.getObject(monitor, getContainer(), this, ukName);
	}

	public DBSTableForeignKey getForeignKey(DBRProgressMonitor monitor, String ukName) throws DBException {
		return DBUtils.findObject(getAssociations(monitor), ukName);
	}

	@Override
	public Collection<DmTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
		return null;
	}

	public String getDDL(DBRProgressMonitor monitor, DmDDLFormat ddlFormat, Map<String, Object> options)
			throws DBException {
		return DmUtils.getDDL(monitor, getTableTypeName(), this, ddlFormat, options);
	}

	@NotNull
	@Override
	public DBSObjectState getObjectState() {
		return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
	}

	public static DmTableBase findTable(DBRProgressMonitor monitor, DmDataSource dataSource, String ownerName,
			String tableName) throws DBException {
		DmSchema refSchema = dataSource.getSchema(monitor, ownerName);
		if (refSchema == null) {
			log.warn("Referenced schema '" + ownerName + "' not found");
			return null;
		} else {
			DmTableBase refTable = refSchema.tableCache.getObject(monitor, refSchema, tableName);
			if (refTable == null) {
				log.warn("Referenced table '" + tableName + "' not found in schema '" + ownerName + "'");
			}
			return refTable;
		}
	}

	@Association
	public Collection<DmPrivTable> getTablePrivs(DBRProgressMonitor monitor) throws DBException {
		return tablePrivCache.getAllObjects(monitor, this);
	}

	/**
	 * Table Trigger Cache
	 * 
	 * @author caosw
	 *
	 */
	static class TriggerCache extends JDBCStructCache<DmTableBase, DmTableTrigger, DmTriggerColumn> {
		TriggerCache() {
			super("TRIGGER_NAME");
		}

		@Override
		protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull DmTableBase owner,
				@NotNull DmTableTrigger forObject) throws SQLException {
			JDBCPreparedStatement dbStat = session.prepareStatement(
					"SELECT TRIGGER_NAME,TABLE_OWNER,TABLE_NAME,COLUMN_NAME,COLUMN_LIST,COLUMN_USAGE\n" + "FROM "
							+ DmUtils.getSysSchemaPrefix(owner.getDataSource())
							+ "ALL_TRIGGER_COLS WHERE TABLE_OWNER=? AND TABLE_NAME=?"
							+ (forObject == null ? "" : " AND TRIGGER_NAME=?") + "\nORDER BY TRIGGER_NAME");
			dbStat.setString(1, owner.getContainer().getName());
			dbStat.setString(2, owner.getName());
			if (forObject != null) {
				dbStat.setString(3, forObject.getName());
			}
			return dbStat;
		}

		@Override
		protected DmTriggerColumn fetchChild(JDBCSession session, DmTableBase owner, DmTableTrigger parent,
				JDBCResultSet dbResult) throws SQLException, DBException {
			DmTableBase refTable = DmTableBase.findTable(session.getProgressMonitor(), owner.getDataSource(),
					JDBCUtils.safeGetString(dbResult, "TABLE_OWNER"), JDBCUtils.safeGetString(dbResult, "TABLE_NAME"));
			if (refTable != null) {
				final String columnName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
				DmTableColumn tableColumn = refTable.getAttribute(session.getProgressMonitor(), columnName);
				if (tableColumn == null) {
					log.debug("Column '" + columnName + "' not found in table '"
							+ refTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + "' for trigger '"
							+ parent.getName() + "'");
				}
				return new DmTriggerColumn(session.getProgressMonitor(), parent, tableColumn, dbResult);
			}
			return null;
		}

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DmTableBase owner)
				throws SQLException {
			JDBCPreparedStatement dbStat = session.prepareStatement("SELECT *\n" + "FROM "
					+ DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "TRIGGERS")
					+ " WHERE TABLE_OWNER=? AND TABLE_NAME=?\n" + "ORDER BY TRIGGER_NAME");
			dbStat.setString(1, owner.getSchema().getName());
			dbStat.setString(2, owner.getName());
			return dbStat;
		}

		@Override
		protected DmTableTrigger fetchObject(@NotNull JDBCSession session, @NotNull DmTableBase owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new DmTableTrigger(owner, resultSet);
		}
	}
	
	public boolean isView() {
		return false;
	}

	/**
	 * Table Priv Cache
	 * 
	 * @author caosw
	 *
	 */
	static class TablePrivCache extends JDBCObjectCache<DmTableBase, DmPrivTable> {
		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DmTableBase tableBase)
				throws SQLException {
			boolean hasDBA = tableBase.getDataSource().isViewAvailable(session.getProgressMonitor(),
					DmConstants.SCHEMA_SYS, DmConstants.VIEW_DBA_TAB_PRIVS);
			final JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT p.*\n" + "FROM " + (hasDBA ? "DBA_TAB_PRIVS p" : "ALL_TAB_PRIVS p") + "\n"
							+ "WHERE p." + (hasDBA ? "OWNER" : "TABLE_SCHEMA") + "=? AND p.TABLE_NAME =?");
			dbStat.setString(1, tableBase.getSchema().getName());
			dbStat.setString(2, tableBase.getName());
			return dbStat;
		}

		@Override
		protected DmPrivTable fetchObject(@NotNull JDBCSession session, @NotNull DmTableBase tableBase,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new DmPrivTable(tableBase, resultSet);
		}
	}
}
