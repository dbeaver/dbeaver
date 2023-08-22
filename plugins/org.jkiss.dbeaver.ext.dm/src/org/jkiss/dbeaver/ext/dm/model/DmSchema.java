package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.sound.midi.Soundbank;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.utils.DmConstants;
import org.jkiss.dbeaver.ext.dm.model.utils.DmUtils;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
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
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

/**
 * DM Schema
 * 
 * @author caosw
 *
 */
public class DmSchema extends DmGlobalObject
		implements DBSSchema, DBPRefreshableObject, DBPSystemObject, DBSProcedureContainer {

	private static final Log log = Log.getLog(DmSchema.class);

	final public TableCache tableCache = new TableCache();
	final public DataTypeCache dataTypeCache = new DataTypeCache();
	final public SynonymCache synonymCache = new SynonymCache();
	final public DBLinkCache dbLinkCache = new DBLinkCache();
	final public ConstraintCache constraintCache = new ConstraintCache();
	final public IndexCache indexCache = new IndexCache();
	final public TriggerCache triggerCache = new TriggerCache();
	final public ForeignKeyCache foreignKeyCache = new ForeignKeyCache();
	final public SequenceCache sequenceCache = new SequenceCache();
	final public ProceduresCache proceduresCache = new ProceduresCache();
	final public PackageCache packageCache = new PackageCache();
	final public MViewCache mviewCache = new MViewCache();
	final public RecyclebinCache recyclebinCache = new RecyclebinCache();

	private long id;
	private String name;
	private Date createTime;
	private transient DmUser user;

	public DmSchema(DmDataSource dataSource, long id, String name) {
		super(dataSource, id > 0);
		this.id = id;
		this.name = name;
	}

	public DmSchema(@NotNull DmDataSource dataSource, @NotNull ResultSet dbResult) {
		super(dataSource, true);
		this.id = JDBCUtils.safeGetLong(dbResult, "SCH_ID");
		this.name = JDBCUtils.safeGetString(dbResult, "SCH_NAME");
		if (CommonUtils.isEmpty(this.name)) {
			log.warn("Empty schema name fetched");
			this.name = "? " + super.hashCode();
		}
		this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CRTDATE");
	}

	@Property(order = 200)
	public long getId() {
		return id;
	}

	@Property(order = 190)
	public Date getCreateTime() {
		return createTime;
	}

	@Nullable
	@Override
	public String getDescription() {
		return null;
	}

	public DmUser getUser() {
		return user;
	}

	public void setUser(DmUser user) {
		this.user = user;
	}

	@Association
	public Collection<DmTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
		return indexCache.getObjects(monitor, this, null);
	}

	@Association
	public Collection<DmTable> getTables(DBRProgressMonitor monitor) throws DBException {
		return tableCache.getTypedObjects(monitor, this, DmTable.class);
	}

	public DmTable getTable(DBRProgressMonitor monitor, String name) throws DBException {
		return tableCache.getObject(monitor, this, name, DmTable.class);
	}

	@Association
	public Collection<DmView> getViews(DBRProgressMonitor monitor) throws DBException {
		return tableCache.getTypedObjects(monitor, this, DmView.class);
	}

	public DmView getView(DBRProgressMonitor monitor, String name) throws DBException {
		return tableCache.getObject(monitor, this, name, DmView.class);
	}

	@Association
	public Collection<DmSequence> getSequences(DBRProgressMonitor monitor) throws DBException {
		return sequenceCache.getAllObjects(monitor, this);
	}

	@Association
	public Collection<DmDataType> getDataTypes(DBRProgressMonitor monitor) throws DBException {
		return dataTypeCache.getAllObjects(monitor, this);
	}

	@Association
	public Collection<DmMaterializedView> getMaterializedViews(DBRProgressMonitor monitor) throws DBException {
		return mviewCache.getAllObjects(monitor, this);
	}

	@Override
	@NotNull
	@Property(viewable = true, editable = true, order = 1)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Association
	public Collection<DmProcedureStandalone> getProcedures(DBRProgressMonitor monitor) throws DBException {
		return proceduresCache.getAllObjects(monitor, this);
	}

	@Association
	public Collection<DmTable> getRecycles(DBRProgressMonitor monitor) throws DBException {
		return recyclebinCache.getTypedObjects(monitor, this, DmTable.class);
	}

	@Override
	public DmProcedureStandalone getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
		return proceduresCache.getObject(monitor, this, uniqueName);
	}

	@Override
	public boolean isSystem() {
		return ArrayUtils.contains(DmConstants.SYSTEM_SCHEMAS, getName());
	}

	@Override
	public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
		tableCache.clearCache();
		foreignKeyCache.clearCache();
		constraintCache.clearCache();
		indexCache.clearCache();
		sequenceCache.clearCache();
		synonymCache.clearCache();
		triggerCache.clearCache();
		dataTypeCache.clearCache();
		dbLinkCache.clearCache();
		packageCache.clearCache();
		mviewCache.clearCache();
		recyclebinCache.clearCache();
		return this;
	}

	public boolean isPublic() {
		return DmConstants.USER_PUBLIC.equals(this.name);
	}

	public DmDataType getDataType(DBRProgressMonitor monitor, String name) throws DBException {
		DmDataType type = isPublic() ? getTypeBySynonym(monitor, name) : dataTypeCache.getObject(monitor, this, name);
		if (type == null) {
			if (!isPublic()) {
				return getTypeBySynonym(monitor, name);
			}
		}
		return type;
	}

	@NotNull
	private DmDataType getTypeBySynonym(DBRProgressMonitor monitor, String name) throws DBException {
		final DmSynonym synonym = synonymCache.getObject(monitor, this, name);
		if (synonym != null && (synonym.getObjectType() == DmObjectType.TYPE
				|| synonym.getObjectType() == DmObjectType.TYPE_BODY)) {
			Object object = synonym.getObject(monitor);
			if (object instanceof DmDataType) {
				return (DmDataType) object;
			}
		}
		return null;
	}

	@Association
	public Collection<DmSynonym> getSynonyms(DBRProgressMonitor monitor) throws DBException {
		return synonymCache.getAllObjects(monitor, this);
	}

	@Association
	public DmSynonym getSynonym(DBRProgressMonitor monitor, String name) throws DBException {
		return synonymCache.getObject(monitor, this, name);
	}

	@Association
	public Collection<DmDBLink> getDatabaseLinks(DBRProgressMonitor monitor) throws DBException {
		return dbLinkCache.getAllObjects(monitor, this);
	}

	@Association
	public Collection<DmSchemaTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException {
		return triggerCache.getAllObjects(monitor, this);
	}

	@Association
	public Collection<DmPackage> getPackages(DBRProgressMonitor monitor) throws DBException {
		return packageCache.getAllObjects(monitor, this);
	}

	@Association
	public Collection<DmTableTrigger> getTableTriggers(DBRProgressMonitor monitor) throws DBException {
		List<DmTableTrigger> allTableTriggers = new ArrayList<DmTableTrigger>();
		for (DmTableBase table : tableCache.getAllObjects(monitor, this)) {
			Collection<DmTableTrigger> triggers = table.getTriggers(monitor);
			if (!CommonUtils.isEmpty(triggers)) {
				allTableTriggers.addAll(triggers);
			}
		}
		allTableTriggers.sort(Comparator.comparing(DmTrigger::getName));
		return allTableTriggers;
	}

	public String toString() {
		return "Schema " + name;
	}

	private static DmTableColumn getTableColumn(JDBCSession session, DmTableBase parent, ResultSet dbResult,
			String columnName) throws DBException {
		DmTableColumn tableColumn = columnName == null ? null
				: parent.getAttribute(session.getProgressMonitor(), columnName);
		if (tableColumn == null) {
			log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "'");
		}
		return tableColumn;
	}

	private List<SpecialPosition> parsePositions(String value) {
		if (value == null) {
			return Collections.emptyList();
		}
		if (value.length() < 3) {
			return Collections.emptyList();
		}
		List<SpecialPosition> result = new ArrayList<>(1);
		String data[] = value.split(",");
		for (String s : data) {
			result.add(new SpecialPosition(s));
		}
		return result;
	}

	/**
	 * DM Table Cache
	 * 
	 * @author caosw
	 *
	 */
	public static class TableCache extends JDBCStructLookupCache<DmSchema, DmTableBase, DmTableColumn> {

		public TableCache() {
			super("TABLE_NAME");
			setListOrderComparator(DBUtils.nameComparator());
		}

		@NotNull
		@Override
		public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull DmSchema owner,
				@NotNull DmTableBase object, @NotNull String objectName) throws SQLException {
//			String tableOper = "=";
//			boolean hasAllAllTables = owner.getDataSource().isViewAvailable(session.getProgressMonitor(), null,
//					"ALL_ALL_TABLES");
//			String tablesSource = hasAllAllTables ? "ALL_TABLES" : "TABLES";
			 JDBCPreparedStatement dbStat=null;
//				dbStat = session.prepareStatement("SELECT "
//						+ DmUtils.getSysCatalogHint(owner.getDataSource())// t.TABLE_TYPE_OWNER，t.TABLE_TYPE去除
//						+ " t.OWNER,t.TABLE_NAME as TABLE_NAME,'TABLE' as OBJECT_TYPE,'VALID' as STATUS,t.TABLE_TYPE_OWNER,t.TABLE_TYPE,t.TABLESPACE_NAME,t.PARTITIONED,t.IOT_TYPE,t.IOT_NAME,t.TEMPORARY,t.SECONDARY,t.NESTED,t.NUM_ROWS \n"
//						+ "\tFROM "
//						+ DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), tablesSource)
//						+ " t\n" + "\tWHERE t.OWNER=? AND NESTED='NO'"
//						+ " t\n" + "\tWHERE t.OWNER=? "
//						+ (object == null && objectName == null ? "" : " AND t.TABLE_NAME" + tableOper + "?")
//						+ "AND t.TABLE_NAME NOT like '%YANYSK%'" + "\n" + "UNION ALL\n" + "\tSELECT "
//						+ DmUtils.getSysCatalogHint(owner.getDataSource())
//						+ " o.OWNER,o.OBJECT_NAME as TABLE_NAME,'VIEW' as OBJECT_TYPE,o.STATUS,NULL,NULL,NULL,'NO',NULL,NULL,o.TEMPORARY,o.SECONDARY,'NO',0 \n"
//						+ "\tFROM "
//						+ DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "OBJECTS")
//						+ " o \n" + "\tWHERE o.OWNER=? AND o.OBJECT_TYPE='VIEW'"
//						+ (object == null && objectName == null ? "" : " AND o.OBJECT_NAME" + tableOper + "?")
//						+ " AND o.OBJECT_NAME NOT like '%YANYSK%'" + "\n");
//			int index = 1;
//			dbStat.setString(index++, owner.getName());
//			if (object != null || objectName != null)
//				dbStat.setString(index++, object != null ? object.getName() : objectName);
//			dbStat.setString(index++, owner.getName());
//			if (object != null || objectName != null)
//				dbStat.setString(index, object != null ? object.getName() : objectName); 
			 // 此处VALID 为是否有效，如果不加上这个列名，那么表名显示将有一个❌号。
			 StringBuilder stringBuilder=new StringBuilder();
			 stringBuilder.append("SELECT 'VALID' as STATUS,TAB_OBJ.NAME TABLE_NAME,TAB_OBJ.ID,TAB_OBJ.SUBTYPE$ OBJECT_TYPE,TAB_OBJ.INFO3,TAB_OBJ.SCHID,SCH_OBJ.NAME OWNER,TAB_OBJ.CRTDATE "+ 
			 		              " FROM ( " + 
			 		              " SELECT TAB_OBJ_INNER.NAME,TAB_OBJ_INNER.ID,TAB_OBJ_INNER.SUBTYPE$,TAB_OBJ_INNER.INFO3,TAB_OBJ_INNER.SCHID,TAB_OBJ_INNER.CRTDATE FROM SYS.SYSOBJECTS TAB_OBJ_INNER WHERE TAB_OBJ_INNER.type$ = 'SCHOBJ') TAB_OBJ, " + 
			 		               " ( " + 
			 		               " SELECT ID,NAME FROM SYS.SYSOBJECTS WHERE TYPE$ = 'SCH') SCH_OBJ "+ 
			 		               " WHERE TAB_OBJ.SCHID = SCH_OBJ.ID AND SCH_OBJ.NAME=? ");
			 if(object!=null||objectName!=null) {
				 stringBuilder.append(" AND TAB_OBJ.NAME='"+(object != null ? object.getName() : objectName)+"' "); // 三目运算符必须要带上括号 否则会只带三目运算符的结果,其余字符串都不会被带上去
			 }
			 stringBuilder.append(" AND TAB_OBJ.NAME NOT like '%YANYSK%' ORDER BY TAB_OBJ.NAME;");
				/*
				 * dbStat=session.
				 * prepareStatement("SELECT 'VALID' as STATUS,TAB_OBJ.NAME TABLE_NAME,TAB_OBJ.ID,TAB_OBJ.SUBTYPE$ OBJECT_TYPE,TAB_OBJ.INFO3,TAB_OBJ.SCHID,SCH_OBJ.NAME OWNER,TAB_OBJ.CRTDATE "
				 * + "FROM (" +
				 * "SELECT TAB_OBJ_INNER.NAME,TAB_OBJ_INNER.ID,TAB_OBJ_INNER.SUBTYPE$,TAB_OBJ_INNER.INFO3,TAB_OBJ_INNER.SCHID,TAB_OBJ_INNER.CRTDATE FROM SYS.SYSOBJECTS TAB_OBJ_INNER WHERE TAB_OBJ_INNER.type$ = 'SCHOBJ') TAB_OBJ,"
				 * + "(" +"SELECT ID,NAME FROM SYS.SYSOBJECTS WHERE TYPE$ = 'SCH') SCH_OBJ "+
				 * "WHERE TAB_OBJ.SCHID = SCH_OBJ.ID AND SCH_OBJ.NAME=? AND TAB_OBJ.NAME NOT like '%YANYSK%' ORDER BY TAB_OBJ.NAME;"
				 * );
				 */
			dbStat=session.prepareStatement(stringBuilder.toString());
			
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected DmTableBase fetchObject(@NotNull JDBCSession session, @NotNull DmSchema owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			final String tableType = JDBCUtils.safeGetString(resultSet, "OBJECT_TYPE");
			if ("UTAB".equals(tableType)) {
				return new DmTable(session.getProgressMonitor(), owner, resultSet);
			} else if("VIEW".equals(tableType)) {
				return new DmView(owner, resultSet);
			}
			return null;
		}

		@Override
		protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull DmSchema owner,
				@NotNull DmTableBase forTable) throws SQLException {
			String colsView = "ALL_TAB_COLS";
			if (!owner.getDataSource().isViewAvailable(session.getProgressMonitor(), DmConstants.SCHEMA_SYS,
					colsView)) {
				colsView = "ALL_TAB_COLUMNS";
			}
			StringBuilder sql = new StringBuilder(500);
			sql.append("SELECT ").append(DmUtils.getSysCatalogHint(owner.getDataSource())).append("\nc.* " + "FROM ")
					.append(DmUtils.getSysSchemaPrefix(owner.getDataSource())).append(colsView)
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
		protected DmTableColumn fetchChild(@NotNull JDBCSession session, @NotNull DmSchema owner,
				@NotNull DmTableBase parent, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new DmTableColumn(session.getProgressMonitor(), parent, dbResult);
		}

		@Override
		protected void cacheChildren(DmTableBase parent, List<DmTableColumn> dmTableColumns) {
			dmTableColumns.sort(DBUtils.orderComparator());
			super.cacheChildren(parent, dmTableColumns);
		}
	}

	/**
	 * DM DataType Cache
	 * 
	 * @author caosw
	 *
	 */
	static class DataTypeCache extends JDBCObjectCache<DmSchema, DmDataType> {

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DmSchema owner)
				throws SQLException {
			JDBCPreparedStatement dbStat = session.prepareStatement("SELECT "
					+ DmUtils.getSysCatalogHint(owner.getDataSource()) + " * FROM USER_TYPES ORDER BY TYPE_NAME");
//			JDBCPreparedStatement dbStat = session.prepareStatement(
//					"SELECT DISTINCT DATA_TYPE FROM "
//							+ DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "TAB_COLUMNS")
//							+ " ORDER BY DATA_TYPE");
			return dbStat;
		}

		@Override
		protected DmDataType fetchObject(@NotNull JDBCSession session, @NotNull DmSchema owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new DmDataType(owner, resultSet);
		}

	}

	/**
	 * DM DBLink Cache
	 * 
	 * @author caosw
	 *
	 */
	static class DBLinkCache extends JDBCObjectCache<DmSchema, DmDBLink> {
		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DmSchema owner)
				throws SQLException {
			JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM "
					+ DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "DB_LINKS")
					+ " WHERE OWNER=? " + " ORDER BY DB_LINK");
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected DmDBLink fetchObject(@NotNull JDBCSession session, @NotNull DmSchema owner,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new DmDBLink(session.getProgressMonitor(), owner, dbResult);
		}
	}

	/**
	 * DM Synonym Cache
	 * 
	 * @author caosw
	 *
	 */
	static class SynonymCache extends JDBCObjectLookupCache<DmSchema, DmSynonym> {

		@NotNull
		@Override
		public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull DmSchema owner,
				DmSynonym object, String objectName) throws SQLException {
			String synonymTypeFilter = (session.getDataSource().getContainer().getPreferenceStore()
					.getBoolean(DmConstants.PREF_DBMS_READ_ALL_SYNONYMS) ? ""
							: "AND O.OBJECT_TYPE NOT IN ('JAVA CLASS','PACKAGE BODY')\n");
			String synonymName = object != null ? object.getName() : objectName;
			StringBuilder sql = new StringBuilder();
			sql.append(
					"SELECT OWNER, SYNONYM_NAME, MAX(TABLE_OWNER) as TABLE_OWNER, MAX(TABLE_NAME) as TABLE_NAME, MAX(DB_LINK) as DB_LINK, MAX(OBJECT_TYPE) as OBJECT_TYPE FROM (\n")
					.append("SELECT S.*, NULL OBJECT_TYPE FROM ").append(DmUtils
							.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "SYNONYMS"))
					.append(" S WHERE S.OWNER = ?");
			if (synonymName != null)
				sql.append(" AND S.SYNONYM_NAME = ?");
			sql.append("\nUNION ALL\n").append("SELECT S.*,O.OBJECT_TYPE FROM ")
					.append(DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(),
							"SYNONYMS"))
					.append(" S, ").append(DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(),
							owner.getDataSource(), "OBJECTS"))
					.append(" O\n").append("WHERE S.OWNER = ?\n");
			if (synonymName != null)
				sql.append(" AND S.SYNONYM_NAME = ? ");
			sql.append(synonymTypeFilter).append("AND O.OWNER=S.TABLE_OWNER AND O.OBJECT_NAME=S.TABLE_NAME\n)\n");
			sql.append("GROUP BY OWNER, SYNONYM_NAME");
			if (synonymName == null) {
				sql.append("\nORDER BY SYNONYM_NAME");
			}
//			System.out.println(sql);
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			int paramNum = 1;
			dbStat.setString(paramNum++, owner.getName());
			if (synonymName != null)
				dbStat.setString(paramNum++, synonymName);
			dbStat.setString(paramNum++, owner.getName());
			if (synonymName != null)
				dbStat.setString(paramNum++, synonymName);
			return dbStat;
		}

		@Override
		protected DmSynonym fetchObject(@NotNull JDBCSession session, @NotNull DmSchema owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new DmSynonym(owner, resultSet);
		}
	}

	/**
	 * Constraint Cache
	 * 
	 * 约束的SQL语句 此处需要修改
	 * @author caosw
	 *
	 */
	class ConstraintCache
			extends JDBCCompositeCache<DmSchema, DmTableBase, DmTableConstraint, DmTableConstraintColumn> {
		ConstraintCache() {
			super(tableCache, DmTableBase.class, "TABLE_NAME", "CONSTRAINT_NAME");
		}

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(JDBCSession session, DmSchema owner, DmTableBase forTable)
				throws SQLException {
			StringBuilder sql = new StringBuilder(500);
			JDBCPreparedStatement dbStat;
			
			/* 
				sql.append("SELECT ").append(DmUtils.getSysCatalogHint(owner.getDataSource())).append("\n"
						+ "c.TABLE_NAME, c.CONSTRAINT_NAME,c.CONSTRAINT_TYPE,c.STATUS,c.SEARCH_CONDITION,"
						+ "col.COLUMN_NAME,col.POSITION\n" + "FROM "
						+ " SYS.USER_CONSTRAINTS "    //DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "CONSTRAINTS")
						+ " c, "
						+ " SYS.USER_CONS_COLUMNS " //DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "CONS_COLUMNS")
						+ " col\n"
						+ "WHERE c.CONSTRAINT_TYPE<>'R' AND c.OWNER=? AND c.OWNER=col.OWNER AND c.CONSTRAINT_NAME=col.CONSTRAINT_NAME");
				if (forTable != null) {
					sql.append(" AND c.TABLE_NAME=?");
				}
				sql.append("\nORDER BY c.CONSTRAINT_NAME,col.POSITION");
*/
			//此处使用DM主键获取方式
			    sql.append(" SELECT CON_OBJ.NAME AS CONSTRAINT_NAME, CONS.ID , CONS.TYPE$ AS CONSTRAINT_TYPE, INDS.ID, INDS.XTYPE, CONS.VALID AS STATUS, CON_OBJ.CRTDATE, SCH_OBJ.ID, SCH_OBJ.NAME,TAB_OBJ.ID,TAB_OBJ.NAME AS TABLE_NAME,")
			    .append(" COLS.NAME AS COLUMN_NAME,COLS.TYPE$,COLS.LENGTH$ ")
			    .append(" FROM (SELECT * FROM SYS.SYSCONS WHERE TYPE$ = 'P' OR TYPE$ = 'U' OR TYPE$ = 'C') CONS, SYS.SYSINDEXES INDS, ")
			    .append(" (SELECT NAME,ID,CRTDATE FROM SYS.SYSOBJECTS WHERE SUBTYPE$ = 'CONS') CON_OBJ, ")
			    .append(" (SELECT ID,NAME FROM SYS.SYSOBJECTS WHERE TYPE$ = 'SCH') SCH_OBJ, ")
			    .append(" (SELECT ID,NAME,SCHID FROM SYS.SYSOBJECTS WHERE TYPE$ = 'SCHOBJ' AND SUBTYPE$ LIKE '_TAB') TAB_OBJ,SYS.SYSCOLUMNS COLS ")
			    .append(" WHERE CONS.INDEXID = INDS.ID AND CON_OBJ.ID = CONS.ID AND CONS.TABLEID = TAB_OBJ.ID AND TAB_OBJ.SCHID = SCH_OBJ.ID AND TAB_OBJ.ID = COLS.ID ")
			    .append(" AND SF_COL_IS_IDX_KEY(INDS.KEYNUM,INDS.KEYINFO,COLS.COLID) = 1 AND SCH_OBJ.NAME=? ");
				if (forTable != null) {
					sql.append(" AND TAB_OBJ.NAME=? ");
				}
				sql.append("ORDER BY CON_OBJ.NAME");
				dbStat = session.prepareStatement(sql.toString());
				dbStat.setString(1, DmSchema.this.getName());
				if (forTable != null) {
					dbStat.setString(2, forTable.getName());
				}
			return dbStat;
		}

		@Nullable
		@Override
		protected DmTableConstraint fetchObject(JDBCSession session, DmSchema owner, DmTableBase parent,
				String indexName, JDBCResultSet dbResult) throws SQLException, DBException {
			return new DmTableConstraint(parent, dbResult);
		}

		@Nullable
		@Override
		protected DmTableConstraintColumn[] fetchObjectRow(JDBCSession session, DmTableBase parent,
				DmTableConstraint object, JDBCResultSet dbResult) throws SQLException, DBException {
			if (JDBCUtils.safeGetString(dbResult, "COLUMN_NAMES_NUMS") != null) {

				List<SpecialPosition> positions = parsePositions(
						JDBCUtils.safeGetString(dbResult, "COLUMN_NAMES_NUMS"));

				DmTableConstraintColumn[] result = new DmTableConstraintColumn[positions.size()];

				for (int idx = 0; idx < positions.size(); idx++) {

					final DmTableColumn column = getTableColumn(session, parent, dbResult,
							positions.get(idx).getColumn());

					if (column == null) {
						continue;
					}

					result[idx] = new DmTableConstraintColumn(object, column, positions.get(idx).getPos());
				}

				return result;

			} else {

				final DmTableColumn tableColumn = getTableColumn(session, parent, dbResult,
						JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME"));
				return tableColumn == null ? null
						: new DmTableConstraintColumn[] { new DmTableConstraintColumn(object, tableColumn,
								JDBCUtils.safeGetInt(dbResult, "POSITION")) };
			}
		}

		@Override
		protected void cacheChildren(DBRProgressMonitor monitor, DmTableConstraint constraint,
				List<DmTableConstraintColumn> rows) {
			constraint.setColumns(rows);
		}
	}

	/**
	 * Trigger Cache
	 * 
	 * @author caosw
	 *
	 */
	static class TriggerCache extends JDBCObjectCache<DmSchema, DmSchemaTrigger> {
		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DmSchema owner)
				throws SQLException {
			JDBCPreparedStatement dbStat = session.prepareStatement("SELECT *\n" + "FROM "
					+ DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "TRIGGERS")
					+ " WHERE OWNER=? \n" + "ORDER BY TRIGGER_NAME");
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected DmSchemaTrigger fetchObject(@NotNull JDBCSession session, @NotNull DmSchema owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new DmSchemaTrigger(owner, resultSet);
		}
	}

	/**
	 * Index Cache
	 * 
	 * 
	 * 修改 ALL_INDEXES 为 USER_INDEXES
	 * 修改 ALL_IND_COLUMNS 为 USER_IND_COLUMNS
	 * 
	 * @author caosw
	 *
	 */
	class IndexCache extends JDBCCompositeCache<DmSchema, DmTablePhysical, DmTableIndex, DmTableIndexColumn> {
		IndexCache() {
			super(tableCache, DmTablePhysical.class, "TABLE_NAME", "INDEX_NAME");
		}
    
		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(JDBCSession session, DmSchema owner, DmTablePhysical forTable)
				throws SQLException {
			
			StringBuilder sql = new StringBuilder();
			/*sql.append("SELECT ").append(DmUtils.getSysCatalogHint(owner.getDataSource())).append(" "
					+ "i.TABLE_OWNER AS OWNER,i.INDEX_NAME,i.INDEX_TYPE,i.TABLE_OWNER,i.TABLE_NAME,i.UNIQUENESS,i.TABLESPACE_NAME,i.STATUS,i.NUM_ROWS,i.SAMPLE_SIZE,\n"
					+ "ic.COLUMN_NAME,ic.COLUMN_POSITION,ic.COLUMN_LENGTH,ic.DESCEND\n" + "FROM "
					+ " USER_INDEXES " + " i\n" //DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "INDEXES")
					+ "JOIN "
					+ " USER_IND_COLUMNS " //DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "IND_COLUMNS")
					+ " ic ON ic.TABLE_NAME=i.TABLE_NAME AND ic.INDEX_NAME=i.INDEX_NAME \n" + "WHERE ");
			if (forTable == null) {
				sql.append("i.TABLE_OWNER=?");
			} else {
				sql.append("i.TABLE_OWNER=? AND i.TABLE_NAME=?");
			}
			sql.append("\nORDER BY i.INDEX_NAME,ic.COLUMN_POSITION");*/
			sql.append(" SELECT ")
			.append(" DISTINCT IND_OBJ.NAME AS INDEX_NAME,INDS.ISUNIQUE AS UNIQUENESS,INDS.TYPE$ AS INDEX_TYPE,SCH_OBJ.NAME AS TABLE_OWNER,SCH_OBJ.NAME AS OWNER, ")
			.append(" TAB_OBJ.NAME AS TABLE_NAME,IND_OBJ.VALID AS STATUS,COLS.NAME AS COLUMN_NAME,COLS.TYPE$ AS COLUMN_TYPE,COLS.LENGTH$ AS COLUMN_LENGTH,'ASC' AS DESCEND ")
	        .append(" FROM ")
	        .append(" ( SELECT * FROM SYS.SYSINDEXES WHERE ROOTFILE != -1 OR (XTYPE & 1000) = 1000 OR (XTYPE & 2000) = 2000 OR (XTYPE & 08) = 08 OR (FLAG & 08) = 08 OR (XTYPE & 8000) = 8000 OR (XTYPE & 40) = 40) INDS, ")
			.append(" ( SELECT DISTINCT IND_OBJ_INNER.ID,IND_OBJ_INNER.NAME,IND_OBJ_INNER.CRTDATE,IND_OBJ_INNER.PID,IND_OBJ_INNER.VALID,IND_OBJ_INNER.INFO7 FROM SYS.SYSOBJECTS IND_OBJ_INNER WHERE IND_OBJ_INNER.SUBTYPE$ = 'INDEX' ) IND_OBJ, ")
			.append(" ( SELECT ID,NAME,SCHID FROM SYS.SYSOBJECTS WHERE TYPE$ = 'SCHOBJ' AND SUBTYPE$ LIKE '_TAB') TAB_OBJ, ")
			.append(" (SELECT ID,NAME FROM SYS.SYSOBJECTS WHERE TYPE$ = 'SCH') SCH_OBJ, ")
			.append(" SYS.SYSCOLUMNS COLS ")
			.append(" WHERE ")
			.append(" INDS.ID = IND_OBJ.ID AND IND_OBJ.PID = TAB_OBJ.ID AND TAB_OBJ.SCHID = SCH_OBJ.ID AND TAB_OBJ.ID = COLS.ID ")
			.append(" and SF_COL_IS_IDX_KEY(INDS.KEYNUM, INDS.KEYINFO, COLS.COLID) = 1 ");
			if (forTable == null) {
				sql.append(" AND SCH_OBJ.NAME = ? ");
			} else {
				sql.append(" AND SCH_OBJ.NAME = ? AND TAB_OBJ.NAME = ? ");
			}
			sql.append(" ORDER BY IND_OBJ.NAME ");
			
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			if (forTable == null) {
				dbStat.setString(1, DmSchema.this.getName());
			} else {
				dbStat.setString(1, DmSchema.this.getName());
				dbStat.setString(2, forTable.getName());
			}
			return dbStat;
		}

		@Nullable
		@Override
		protected DmTableIndex fetchObject(JDBCSession session, DmSchema owner, DmTablePhysical parent,
				String indexName, JDBCResultSet dbResult) throws SQLException, DBException {
			return new DmTableIndex(owner, parent, indexName, dbResult);
		}

		@Nullable
		@Override
		protected DmTableIndexColumn[] fetchObjectRow(JDBCSession session, DmTablePhysical parent, DmTableIndex object,
				JDBCResultSet dbResult) throws SQLException, DBException {
			String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME");
			int ordinalPosition = JDBCUtils.safeGetInt(dbResult, "COLUMN_POSITION");
			boolean isAscending = "ASC".equals(JDBCUtils.safeGetStringTrimmed(dbResult, "DESCEND"));
			String columnExpression = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_EXPRESSION");

			DmTableColumn tableColumn = columnName == null ? null
					: parent.getAttribute(session.getProgressMonitor(), columnName);
			if (tableColumn == null) {
				log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '"
						+ object.getName() + "'");
				return null;
			}

			return new DmTableIndexColumn[] {
					new DmTableIndexColumn(object, tableColumn, ordinalPosition, isAscending, columnExpression) };
		}

		@Override
		protected void cacheChildren(DBRProgressMonitor monitor, DmTableIndex index, List<DmTableIndexColumn> rows) {
			index.setColumns(rows);
		}
	}

	/**
	 * Procedures Cache
	 * 
	 * 
	 * @author caosw
	 *
	 */
	static class ProceduresCache extends JDBCObjectLookupCache<DmSchema, DmProcedureStandalone> {
		@NotNull
		@Override
		public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull DmSchema owner,
				@NotNull DmProcedureStandalone object, @NotNull String objectName) throws SQLException {
			JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT " + DmUtils.getSysCatalogHint(owner.getDataSource()) + " * FROM "
							+ DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(),
									"OBJECTS")
							+ " " + "WHERE OBJECT_TYPE IN ('PROCEDURE','FUNCTION') " + "AND OWNER=? "
							+ (object == null && objectName == null ? "" : "AND OBJECT_NAME=? ")
							+ "ORDER BY OBJECT_NAME");
			dbStat.setString(1, owner.getName());
			if (object != null || objectName != null)
				dbStat.setString(2, object != null ? object.getName() : objectName);
			return dbStat;
		}

		@Override
		protected DmProcedureStandalone fetchObject(@NotNull JDBCSession session, @NotNull DmSchema owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new DmProcedureStandalone(owner, resultSet);
		}

	}

	/**
	 * Sequence Cache
	 * 
	 * @author caosw
	 *
	 */
	static class SequenceCache extends JDBCObjectCache<DmSchema, DmSequence> {

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DmSchema owner)
				throws SQLException {
			final JDBCPreparedStatement dbStat = session.prepareStatement("SELECT "
					+ DmUtils.getSysCatalogHint(owner.getDataSource()) + " * FROM "
					+ DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "SEQUENCES")
					+ " WHERE SEQUENCE_OWNER=? ORDER BY SEQUENCE_NAME");
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected DmSequence fetchObject(@NotNull JDBCSession session, @NotNull DmSchema owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new DmSequence(owner, resultSet);
		}

	}

	/**
	 * ForeignKey Cache
	 * 
	 * 外键SQL语句 此处需要修改
	 * 
	 * @author caosw
	 *
	 */
	class ForeignKeyCache extends JDBCCompositeCache<DmSchema, DmTable, DmTableForeignKey, DmTableForeignKeyColumn> {
		ForeignKeyCache() {
			super(tableCache, DmTable.class, "TABLE_NAME", "CONSTRAINT_NAME");
		}

		@Override
		protected void loadObjects(DBRProgressMonitor monitor, DmSchema schema, DmTable forParent) throws DBException {
			if (forParent == null) {
				constraintCache.getAllObjects(monitor, schema);
			}
			super.loadObjects(monitor, schema, forParent);
		}

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(JDBCSession session, DmSchema owner, DmTable forTable)
				throws SQLException {
		/*	boolean useSimpleConnection = CommonUtils
					.toBoolean(session.getDataSource().getContainer().getConnectionConfiguration()
							.getProviderProperty(DmConstants.PROP_METADATA_USE_SIMPLE_CONSTRAINTS));
							*/
			StringBuilder sql = new StringBuilder(500);
			JDBCPreparedStatement dbStat;
			/* if (owner.getDataSource().isAtLeastV11() && forTable != null && !useSimpleConnection) {
				sql.append("SELECT \r\n" + "    c.TABLE_NAME,\r\n" + "    c.CONSTRAINT_NAME,\r\n"
						+ "    c.CONSTRAINT_TYPE,\r\n" + "    c.STATUS,\r\n" + "    c.R_OWNER,\r\n"
						+ "    c.R_CONSTRAINT_NAME,\r\n" + "    (SELECT rc.TABLE_NAME FROM "
						+ DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "CONSTRAINTS")
						+ " rc WHERE rc.OWNER = c.r_OWNER AND rc.CONSTRAINT_NAME = c.R_CONSTRAINT_NAME) AS R_TABLE_NAME,\r\n"
						+ "    c.DELETE_RULE,\r\n" + "    (\r\n"
						+ "      SELECT LISTAGG(COLUMN_NAME || ':' || POSITION,',') WITHIN GROUP (ORDER BY \"POSITION\") \r\n"
						+ "      FROM ALL_CONS_COLUMNS col\r\n"
						+ "      WHERE col.OWNER =? AND col.TABLE_NAME = ? AND col.CONSTRAINT_NAME = c.CONSTRAINT_NAME GROUP BY CONSTRAINT_NAME \r\n"
						+ "    ) COLUMN_NAMES_NUMS\r\n" + "FROM\r\n" + "    "
						+ DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "CONSTRAINTS")
						+ " c\r\n" + "WHERE\r\n" + "    c.CONSTRAINT_TYPE = 'R'\r\n" + "    AND c.OWNER = ?\r\n"
						+ "    AND c.TABLE_NAME = ?");
				dbStat = session.prepareStatement(sql.toString());
				dbStat.setString(1, DmSchema.this.getName());
				dbStat.setString(2, forTable.getName());
				dbStat.setString(3, DmSchema.this.getName());
				dbStat.setString(4, forTable.getName());
			} else if (owner.getDataSource().isAtLeastV10() && forTable != null && !useSimpleConnection) {
				sql.append("SELECT \r\n" + "    c.TABLE_NAME,\r\n" + "    c.CONSTRAINT_NAME,\r\n"
						+ "    c.CONSTRAINT_TYPE,\r\n" + "    c.STATUS,\r\n" + "    c.R_OWNER,\r\n"
						+ "    c.R_CONSTRAINT_NAME,\r\n" + "    (SELECT rc.TABLE_NAME FROM "
						+ DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "CONSTRAINTS")
						+ " rc WHERE rc.OWNER = c.r_OWNER AND rc.CONSTRAINT_NAME = c.R_CONSTRAINT_NAME) AS R_TABLE_NAME,\r\n"
						+ "    c.DELETE_RULE,\r\n" + "    (\r\n"
						+ "        SELECT LTRIM(MAX(SYS_CONNECT_BY_PATH(cname || ':' || p,','))    KEEP (DENSE_RANK LAST ORDER BY curr),',') \r\n"
						+ "        FROM   (SELECT \r\n"
						+ "                       col.CONSTRAINT_NAME cn,col.POSITION p,col.COLUMN_NAME cname,\r\n"
						+ "                       ROW_NUMBER() OVER (PARTITION BY col.CONSTRAINT_NAME ORDER BY col.POSITION) AS curr,\r\n"
						+ "                       ROW_NUMBER() OVER (PARTITION BY col.CONSTRAINT_NAME ORDER BY col.POSITION) -1 AS prev\r\n"
						+ "                FROM   "
						+ DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "CONS_COLUMNS")
						+ " col \r\n" + "                WHERE  col.OWNER =? AND col.TABLE_NAME = ? \r\n"
						+ "                )  WHERE cn = c.CONSTRAINT_NAME GROUP BY cn CONNECT BY prev = PRIOR curr AND cn = PRIOR cn START WITH curr = 1      \r\n"
						+ "        ) COLUMN_NAMES_NUMS\r\n" + "FROM\r\n" + "    "
						+ DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "CONSTRAINTS")
						+ " c\r\n" + "WHERE\r\n" + "    c.CONSTRAINT_TYPE = 'R'\r\n" + "    AND c.OWNER = ?\r\n"
						+ "    AND c.TABLE_NAME = ?");
				dbStat = session.prepareStatement(sql.toString());
				dbStat.setString(1, DmSchema.this.getName());
				dbStat.setString(2, forTable.getName());
				dbStat.setString(3, DmSchema.this.getName());
				dbStat.setString(4, forTable.getName());
			} else {*/
				/*sql.append("SELECT " + DmUtils.getSysCatalogHint(owner.getDataSource()) + " \r\n"
						+ "c.TABLE_NAME, c.CONSTRAINT_NAME,c.CONSTRAINT_TYPE,c.STATUS,c.R_OWNER,c.R_CONSTRAINT_NAME,rc.TABLE_NAME as R_TABLE_NAME,c.DELETE_RULE, \n"
						+ "col.COLUMN_NAME,col.POSITION\r\n" + "FROM "
						+ "ALL_CONSTRAINTS"//DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "CONSTRAINTS")
						+ " c, "
						+ "ALL_CONS_COLUMNS"//DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "CONS_COLUMNS")
						+ " col, "
						+ "ALL_CONSTRAINTS"//DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "CONSTRAINTS")
						+ " rc\n" + "WHERE c.CONSTRAINT_TYPE='R' AND c.OWNER=?\n"
						+ "AND c.OWNER=col.OWNER AND c.CONSTRAINT_NAME=col.CONSTRAINT_NAME\n"
						+ "AND rc.OWNER=c.r_OWNER AND rc.CONSTRAINT_NAME=c.R_CONSTRAINT_NAME");*/
			sql.append(" SELECT CON_OBJ.NAME AS CONSTRAINT_NAME,CONS.VALID AS STATUS,SCH_OBJ.NAME AS R_OWNER,'R' AS CONSTRAINT_TYPE,TAB_OBJ.NAME AS TABLE_NAME,")
			   .append("COLS.NAME AS COLUMN_NAME,COLS.TYPE$ AS COLUMN_TYPE,TAB.NAME AS R_TABLE_NAME,SCH_OBJ.NAME AS R_OWNER ")
			   .append(" FROM ")
			   .append(" (SELECT * FROM SYS.SYSCONS WHERE TYPE$ = 'F') CONS, ")
			   .append(" (SELECT NAME,ID,CRTDATE FROM SYS.SYSOBJECTS WHERE SUBTYPE$ = 'CONS') CON_OBJ, ")
			   .append(" (SELECT ID,NAME FROM SYS.SYSOBJECTS WHERE TYPE$ = 'SCH') SCH_OBJ, ")
			   .append(" (SELECT ID,NAME,SCHID FROM SYS.SYSOBJECTS WHERE TYPE$ = 'SCHOBJ' AND SUBTYPE$ LIKE '_TAB') TAB_OBJ, ")
			   .append(" SYS.SYSCOLUMNS COLS, ")
			   .append(" SYS.SYSINDEXES INDS, ")
			   .append(" (SELECT * FROM SYS.SYSOBJECTS WHERE SUBTYPE$ = 'INDEX') TAB_INDEX, ")
			   .append(" (SELECT * FROM SYS.SYSOBJECTS WHERE SUBTYPE$ = 'UTAB') TAB ")
			   .append(" WHERE ")
			   .append(" CON_OBJ.ID = CONS.ID AND CONS.TABLEID = TAB_OBJ.ID AND TAB_OBJ.SCHID = SCH_OBJ.ID AND TAB_OBJ.ID = COLS.ID AND CONS.INDEXID = INDS.ID AND COLS.ID = CONS.TABLEID AND CONS.FINDEXID = TAB_INDEX.ID")
			   .append(" AND TAB_INDEX.PID = TAB.ID AND SF_COL_IS_IDX_KEY(INDS.KEYNUM, INDS.KEYINFO, COLS.COLID)=1 	AND SCH_OBJ.NAME=?");
				if (forTable != null) {
					sql.append("AND TAB_OBJ.NAME=?");
				}
				sql.append("ORDER BY CON_OBJ.NAME");
				dbStat = session.prepareStatement(sql.toString());
				dbStat.setString(1, DmSchema.this.getName());
				if (forTable != null) {
					dbStat.setString(2, forTable.getName());
				}
			return dbStat;
		}

		@Nullable
		@Override
		protected DmTableForeignKey fetchObject(JDBCSession session, DmSchema owner, DmTable parent, String indexName,
				JDBCResultSet dbResult) throws SQLException, DBException {
			return new DmTableForeignKey(session.getProgressMonitor(), parent, dbResult);
		}

		@Nullable
		@Override
		protected DmTableForeignKeyColumn[] fetchObjectRow(JDBCSession session, DmTable parent,
				DmTableForeignKey object, JDBCResultSet dbResult) throws SQLException, DBException {
			if (JDBCUtils.safeGetString(dbResult, "COLUMN_NAMES_NUMS") != null) {
				List<SpecialPosition> positions = parsePositions(
						JDBCUtils.safeGetString(dbResult, "COLUMN_NAMES_NUMS"));
				DmTableForeignKeyColumn[] result = new DmTableForeignKeyColumn[positions.size()];
				for (int idx = 0; idx < positions.size(); idx++) {
					DmTableColumn column = getTableColumn(session, parent, dbResult, positions.get(idx).getColumn());
					if (column == null) {
						continue;
					}
					result[idx] = new DmTableForeignKeyColumn(object, column, positions.get(idx).getPos());
				}
				return result;
			} else {
				DmTableColumn column = getTableColumn(session, parent, dbResult,
						JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME"));
				if (column == null) {
					return null;
				}
				return new DmTableForeignKeyColumn[] {
						new DmTableForeignKeyColumn(object, column, JDBCUtils.safeGetInt(dbResult, "POSITION")) };
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		protected void cacheChildren(DBRProgressMonitor monitor, DmTableForeignKey foreignKey,
				List<DmTableForeignKeyColumn> rows) {
			foreignKey.setColumns((List) rows);
		}
	}

	static class MViewCache extends JDBCObjectLookupCache<DmSchema, DmMaterializedView> {

		@NotNull
		@Override
		public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull DmSchema owner,
				@NotNull DmMaterializedView object, String objectName) throws SQLException {
			JDBCPreparedStatement dbStat = session.prepareStatement("SELECT M.* FROM "
					+ DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "USERS")
					+ " U, USER_MVIEWS M WHERE M.SCHID = U.USER_ID" + " AND U.USERNAME=? "
					+ (object == null && objectName == null ? "" : "AND M.MVIEW_NAME=? ") + "ORDER BY M.MVIEW_NAME");
			dbStat.setString(1, owner.getName());
			if (object != null || objectName != null)
				dbStat.setString(2, object != null ? object.getName() : objectName);
			return dbStat;
		}

		@Override
		protected DmMaterializedView fetchObject(@NotNull JDBCSession session, @NotNull DmSchema owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new DmMaterializedView(owner, resultSet);
		}

	}

	/**
	 * package cache
	 * 
	 * @author caosw
	 *
	 */
	static class PackageCache extends JDBCObjectCache<DmSchema, DmPackage> {

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DmSchema owner)
				throws SQLException {
			JDBCPreparedStatement dbStat = session.prepareStatement("SELECT "
					+ DmUtils.getSysCatalogHint(owner.getDataSource()) + " * FROM "
					+ DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "OBJECTS")
					+ " WHERE OBJECT_TYPE='PACKAGE' AND OWNER=? " + " ORDER BY OBJECT_NAME");
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected DmPackage fetchObject(@NotNull JDBCSession session, @NotNull DmSchema owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new DmPackage(owner, resultSet);
		}

	}

	static class RecyclebinCache extends TableCache {

		@Nullable
		@Override
		public JDBCStatement prepareLookupStatement(@Nullable JDBCSession session, @Nullable DmSchema owner,
				@Nullable DmTableBase object, @Nullable String objectName) throws SQLException {
			 // 此处VALID 为是否有效，如果不加上这个列名，那么表名显示将有一个❌号。
			 StringBuilder stringBuilder=new StringBuilder();
			 stringBuilder.append("SELECT 'VALID' as STATUS,TAB_OBJ.NAME TABLE_NAME,TAB_OBJ.ID,TAB_OBJ.SUBTYPE$ OBJECT_TYPE,TAB_OBJ.INFO3,TAB_OBJ.SCHID,SCH_OBJ.NAME OWNER,TAB_OBJ.CRTDATE "+ 
			 		              " FROM ( " + 
			 		              " SELECT TAB_OBJ_INNER.NAME,TAB_OBJ_INNER.ID,TAB_OBJ_INNER.SUBTYPE$,TAB_OBJ_INNER.INFO3,TAB_OBJ_INNER.SCHID,TAB_OBJ_INNER.CRTDATE FROM SYS.SYSOBJECTS TAB_OBJ_INNER WHERE TAB_OBJ_INNER.type$ = 'SCHOBJ') TAB_OBJ, " + 
			 		               " ( " + 
			 		               " SELECT ID,NAME FROM SYS.SYSOBJECTS WHERE TYPE$ = 'SCH') SCH_OBJ "+ 
			 		               " WHERE TAB_OBJ.SCHID = SCH_OBJ.ID AND SCH_OBJ.NAME=? ");
			 if(object!=null||objectName!=null) {
				 stringBuilder.append(" AND TAB_OBJ.NAME='"+object.getName()+"' ");
			 }
			stringBuilder.append(" AND TAB_OBJ.NAME LIKE '%YANYSK%' ORDER BY TAB_OBJ.NAME;");
			JDBCPreparedStatement dbStat=session.prepareStatement(stringBuilder.toString());
			dbStat.setString(1, owner.getName());
			return dbStat;
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

	@Override
	public Collection<DBSObject> getChildren(DBRProgressMonitor monitor) throws DBException {
		List<DBSObject> children = new ArrayList<DBSObject>();
		children.addAll(tableCache.getAllObjects(monitor, this));

		children.addAll(packageCache.getAllObjects(monitor, this));
		return children;
	}

	@Override
	public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
		final DmTableBase table = tableCache.getObject(monitor, this, childName);
		if (table != null) {
			return table;
		}
		DmSynonym synonym = synonymCache.getObject(monitor, this, childName);
		if (synonym != null) {
			return synonym;
		}
		return packageCache.getObject(monitor, this, childName);
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

	@Override //该方法用于获取其子类型，模式子类型即为表
	public Class<? extends DBSEntity> getPrimaryChildType(DBRProgressMonitor monitor) throws DBException {
		return DmTable.class; 
	}
}
