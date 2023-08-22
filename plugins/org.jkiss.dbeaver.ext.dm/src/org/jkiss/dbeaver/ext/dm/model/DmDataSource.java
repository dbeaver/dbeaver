package org.jkiss.dbeaver.ext.dm.model;

import java.util.Iterator;
import java.util.List;

import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.sql.PreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.utils.CommonUtils;
import java.sql.ResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.ext.dm.model.plan.DmQueryPlanner;
import org.jkiss.dbeaver.ext.dm.model.utils.DmConstants;
import org.jkiss.dbeaver.ext.dm.model.utils.DmUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import java.sql.SQLException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.Collection;
import org.jkiss.dbeaver.DBException;
import java.util.HashMap;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import java.util.Map;
import org.jkiss.dbeaver.Log;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;

public class DmDataSource extends JDBCDataSource implements IAdaptable
{
    private static final Log log;
    public final SchemaCache schemaCache;
    final DataTypeCache dataTypeCache;
    public final TablespaceCache tablespaceCache;
    final RoleCache roleCache;
    public final UserCache userCache;
    private final Map<String, Boolean> availableViews;
    private boolean useRuleHint;
    private DmSchema publicSchema;
    private boolean isAdmin;
    private boolean isAdminVisible;
    private DBRProgressMonitor monitor;
    private String defaultPath;
    private String planTableName;
    
    static {
        log = Log.getLog(DmDataSource.class);
    }
    
    public DmDataSource(final DBRProgressMonitor monitor, final DBPDataSourceContainer container) throws DBException {
        super(monitor, container, (SQLDialect)new DmSQLDialect());
        this.schemaCache = new SchemaCache();
        this.dataTypeCache = new DataTypeCache();
        this.tablespaceCache = new TablespaceCache();
        this.roleCache = new RoleCache();
        this.userCache = new UserCache();
        this.availableViews = new HashMap<String, Boolean>();
        this.monitor = monitor;
    }
    
    @Association
    public Collection<DmSchema> getSchemas(final DBRProgressMonitor monitor) throws DBException {
        this.dataTypeCache.getAllObjects(monitor, this);
        return (Collection<DmSchema>)this.schemaCache.getAllObjects(monitor, this);
    }
    
    public DmSchema getSchema(final DBRProgressMonitor monitor, final String name) throws DBException {
        if (this.publicSchema != null && this.publicSchema.getName().equals(name)) {
            return this.publicSchema;
        }
        return (DmSchema)this.schemaCache.getObject(monitor, this, name);
    }
    
    @Association
    public Collection<DmTablespace> getTablespaces(final DBRProgressMonitor monitor) throws DBException {
        return (Collection<DmTablespace>)this.tablespaceCache.getAllObjects(monitor, this);
    }
    
    @Association
    public Collection<DmUser> getUsers(final DBRProgressMonitor monitor) throws DBException {
        return (Collection<DmUser>)this.userCache.getAllObjects(monitor, this);
    }
    
    @Association
    public DmUser getUser(final DBRProgressMonitor monitor, final String name) throws DBException {
        return (DmUser)this.userCache.getObject(monitor, this, name);
    }
    
    @Association
    public Collection<DmRole> getRoles(final DBRProgressMonitor monitor) throws DBException {
        return (Collection<DmRole>)this.roleCache.getAllObjects(monitor, this);
    }
    
    public DmGrantee getGrantee(final DBRProgressMonitor monitor, final String name) throws DBException {
        final DmUser user = (DmUser)this.userCache.getObject(monitor, this, name);
        if (user != null) {
            return user;
        }
        return (DmGrantee)this.roleCache.getObject(monitor, this, name);
    }
    
    @Association
    public Collection<DmSynonym> getPublicSynonyms(final DBRProgressMonitor monitor) throws DBException {
        return this.publicSchema.getSynonyms(monitor);
    }
    
    @Association
    public Collection<DmDBLink> getPublicDatabaseLinks(final DBRProgressMonitor monitor) throws DBException {
        return this.publicSchema.getDatabaseLinks(monitor);
    }
    
    @NotNull
    public DmDataSource getDataSource() {
        return this;
    }
    
    public Collection<? extends DBSDataType> getLocalDataTypes() {
        Collection<DmDataType> dataTypes = null;
        try {
            dataTypes = (Collection<DmDataType>)this.dataTypeCache.getAllObjects(this.monitor, this);
        }
        catch (DBException e) {
            e.printStackTrace();
        }
        return (Collection<? extends DBSDataType>)dataTypes;
    }
    
    public DmDataType getLocalDataType(final String typeName) {
        return (DmDataType)this.dataTypeCache.getCachedObject(typeName);
    }
    
    public Collection<DmSchema> getChildren(@NotNull final DBRProgressMonitor monitor) throws DBException {
        return this.getSchemas(monitor);
    }
    
    public DBSObject getChild(@NotNull final DBRProgressMonitor monitor, @NotNull final String childName) throws DBException {
        return (DBSObject)this.getSchema(monitor, childName);
    }
    
    public Class<? extends DmSchema> getChildType(final DBRProgressMonitor monitor) throws DBException {
        return DmSchema.class;
    }
    
    public void cacheStructure(final DBRProgressMonitor monitor, final int scope) throws DBException {
    }
    
    public boolean isAtLeastV9() {
        return this.getInfo().getDatabaseVersion().getMajor() >= 9;
    }
    
    public boolean isAtLeastV10() {
        return this.getInfo().getDatabaseVersion().getMajor() >= 10;
    }
    
    public boolean isAtLeastV11() {
        return this.getInfo().getDatabaseVersion().getMajor() >= 11;
    }
    
    public boolean isAtLeastV12() {
        return this.getInfo().getDatabaseVersion().getMajor() >= 12;
    }
    
    public boolean isViewAvailable(@NotNull DBRProgressMonitor monitor, @Nullable String schemaName, @NotNull String viewName){
        viewName = viewName.toUpperCase();
        Boolean available;
        synchronized (availableViews) {
            available = availableViews.get(viewName);
        }
        if (available == null) {
            try {
                try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Check view existence")) {
                    String viewNameQuoted = DBUtils.getQuotedIdentifier(this, viewName);
                    try (final JDBCPreparedStatement dbStat = session.prepareStatement(
                        "SELECT 1 FROM " +
                            (schemaName == null ? viewNameQuoted : DBUtils.getQuotedIdentifier(this, schemaName) + "." + viewNameQuoted) +
                            " WHERE 1<>1"))
                    {
                        dbStat.setFetchSize(1);
                        dbStat.execute();
                        available = true;
                    }
                }
            } catch (Exception e) {
                available = false;
            }
            synchronized (availableViews) {
                availableViews.put(viewName, available);
            }
        }
        return available;
    }
    
    public boolean isAdmin() {
        return this.isAdmin;
    }
    
    public boolean isAdminVisible() {
        return this.isAdmin || this.isAdminVisible;
    }
    
    public boolean isUseRuleHint() {
        return this.useRuleHint;
    }
    
    @NotNull
    public DmDataType resolveDataType(@NotNull final DBRProgressMonitor monitor, @NotNull final String typeFullName) throws DBException {
        final int divPos = typeFullName.indexOf(46);
        if (divPos == -1) {
            return this.getLocalDataType(typeFullName);
        }
        final String schemaName = typeFullName.substring(0, divPos);
        final String typeName = typeFullName.substring(divPos + 1);
        final DmSchema schema = this.getSchema(monitor, schemaName);
        if (schema == null) {
            return null;
        }
        return schema.getDataType(monitor, typeName);
    }
    
    @Nullable
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBCQueryPlanner.class) {
            return adapter.cast(new DmQueryPlanner(this));
        }
        return super.getAdapter(adapter);
    }
    
    
    static class DataTypeCache extends JDBCObjectCache<DmDataSource, DmDataType>
    {
        @NotNull
        protected JDBCStatement prepareObjectsStatement(@NotNull final JDBCSession session, @NotNull final DmDataSource owner) throws SQLException {
            final JDBCStatement statement = (JDBCStatement)session.prepareStatement("SELECT " + DmUtils.getSysCatalogHint(owner.getDataSource()) + " DISTINCT DATA_TYPE FROM " + DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner, "TAB_COLUMNS") + " ORDER BY DATA_TYPE");
            return statement;
        }
        
        @NotNull
        protected DmDataType fetchObject(@NotNull final JDBCSession session, @NotNull final DmDataSource owner, @NotNull final JDBCResultSet resultSet) throws SQLException, DBException {
            return new DmDataType((DBSObject)owner, (ResultSet)resultSet);
        }
    }
    
    static class RoleCache extends JDBCObjectCache<DmDataSource, DmRole>
    {
        @NotNull
        protected JDBCStatement prepareObjectsStatement(@NotNull final JDBCSession session, @NotNull final DmDataSource owner) throws SQLException {
            return (JDBCStatement)session.prepareStatement("SELECT * FROM DBA_ROLES ORDER BY ROLE");
        }
        
        protected DmRole fetchObject(@NotNull final JDBCSession session, @NotNull final DmDataSource owner, @NotNull final JDBCResultSet resultSet) throws SQLException, DBException {
            return new DmRole(owner, (ResultSet)resultSet);
        }
    }
    
    /**
     * DM Schema 此处获取有问题，此处的SQL只是获取数据库中所有的USER 用户名，由于在Oracle中一个用户对应一个Schema,直接获取用户名作为Schema没有问题
     * 但是在DM中是先创建用户，创建完用户之后在创建Schema(此处DBeaver沿用Oracle的思路不正确，即直接创建用户和Schema,后续可以修改).由于有可能一个用户对应多个Schema(
        CREATE SCHEMA "模式名" AUTHORIZATION "用户名"，创建Schema时其属于对应用户)，直接获取用户可能获取不全。此处可以建议走系统视图查找
        SELECT * from all_objects where object_type = 'SCH'; 查找所有Schema 数据

        由于查询可能会变慢，暂时先不使用该方式查询，后续依情况而定。

       由于一个模式对应一个用户，在DM里切换模式也不支持，只能切换用户

       20210115 切换SQL
     * @author saorionesan
     *
     */
    static class SchemaCache extends JDBCObjectCache<DmDataSource, DmSchema>
    {
        SchemaCache() {
            this.setListOrderComparator(DBUtils.nameComparator());
        }
        
        @NotNull
        protected JDBCStatement prepareObjectsStatement(@NotNull final JDBCSession session, @NotNull final DmDataSource owner) throws SQLException {
            final StringBuilder schemasQuery = new StringBuilder();
            final boolean showAllSchemas = !CommonUtils.toBoolean((Object)owner.getContainer().getConnectionConfiguration().getProviderProperty("@dbeaver-check-schema-content@"));
            schemasQuery.append("SELECT SCH_OBJ.NAME AS SCH_NAME, SCH_OBJ.ID AS SCH_ID, SCH_OBJ.CRTDATE , USER_OBJ.NAME AS USER_NAME FROM ");
            schemasQuery.append(" (SELECT NAME, ID, PID, CRTDATE FROM SYS.SYSOBJECTS WHERE TYPE$='SCH' ) SCH_OBJ, ");
            schemasQuery.append(" (SELECT NAME, ID FROM SYS.SYSOBJECTS WHERE TYPE$='UR' AND SUBTYPE$='USER') USER_OBJ ");
            schemasQuery.append(" WHERE SCH_OBJ.PID=USER_OBJ.ID ");
            final DBSObjectFilter schemaFilters = owner.getContainer().getObjectFilter((Class)DmSchema.class, (DBSObject)null, false);
            if (schemaFilters != null) {
                JDBCUtils.appendFilterClause(schemasQuery, schemaFilters, "SCH_OBJ.NAME", false);
            }
            schemasQuery.append(" ORDER BY SCH_OBJ.NAME "); //由于filter 会在查询语句后面加上like匹配，确保order语句在SQL语句的最后一行
            final JDBCPreparedStatement dbStat = session.prepareStatement(schemasQuery.toString());
            if (schemaFilters != null) {
                JDBCUtils.setFilterParameters((PreparedStatement)dbStat, 1, schemaFilters);
            }
            return (JDBCStatement)dbStat;
        }
        
        protected DmSchema fetchObject(@NotNull final JDBCSession session, @NotNull final DmDataSource owner, @NotNull final JDBCResultSet resultSet) throws SQLException, DBException {
            return new DmSchema(owner, (ResultSet)resultSet);
        }
        
        protected void invalidateObjects(final DBRProgressMonitor monitor, final DmDataSource owner, final Iterator<DmSchema> objectIter) {
            this.setListOrderComparator(DBUtils.nameComparator());
        }
    }
    
    /**
     * 修改获取TableSpace SQL,使用系统表V$TABLESPACE 来获取
     * @author saorionesan
     */
    
    static class TablespaceCache extends JDBCObjectCache<DmDataSource, DmTablespace>
    {
        @NotNull
        protected JDBCStatement prepareObjectsStatement(@NotNull final JDBCSession session, @NotNull final DmDataSource owner) throws SQLException {
        	//return session.prepareStatement("SELECT * FROM " + DmUtils.getSysUserViewName(session.getProgressMonitor(), owner, "TABLESPACES") + " ORDER BY TABLESPACE_NAME");
            return session.prepareStatement("SELECT * FROM SYS.V$TABLESPACE ORDER BY NAME");
        }
        
        protected DmTablespace fetchObject(@NotNull final JDBCSession session, @NotNull final DmDataSource owner, @NotNull final JDBCResultSet resultSet) throws SQLException, DBException {
            return new DmTablespace(owner, resultSet);
        }
    }
    
    static class UserCache extends JDBCObjectCache<DmDataSource, DmUser>
    {
        @NotNull
        protected JDBCStatement prepareObjectsStatement(@NotNull final JDBCSession session, @NotNull final DmDataSource owner) throws SQLException {
            return (JDBCStatement)session.prepareStatement("SELECT * FROM " + DmUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner, "USERS") + " ORDER BY USERNAME");
        }
        
        protected DmUser fetchObject(@NotNull final JDBCSession session, @NotNull final DmDataSource owner, @NotNull final JDBCResultSet resultSet) throws SQLException, DBException {
            return new DmUser(owner, (ResultSet)resultSet);
        }
    }
    
    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new DmExecutionContext(instance, type);
    }

    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context, JDBCExecutionContext initFrom) throws DBException {
        if (initFrom != null) {
            ((DmExecutionContext)context).setCurrentSchema(monitor, ((DmExecutionContext)initFrom).getDefaultSchema());
        } else {
            ((DmExecutionContext)context).refreshDefaults(monitor, true);
        }

        {
            DBPConnectionConfiguration connectionInfo = getContainer().getConnectionConfiguration();

            try (JDBCSession session = context.openSession(monitor, DBCExecutionPurpose.META, "Set connection parameters")) {
                // Set session settings
                String sessionLanguage = connectionInfo.getProviderProperty(DmConstants.PROP_SESSION_LANGUAGE);
                if (sessionLanguage != null) {
                    try {
                        JDBCUtils.executeSQL(
                            session,
                            "ALTER SESSION SET NLS_LANGUAGE='" + sessionLanguage + "'");
                    } catch (Throwable e) {
                        log.warn("Can't set session language", e);
                    }
                }
                String sessionTerritory = connectionInfo.getProviderProperty(DmConstants.PROP_SESSION_TERRITORY);
                if (sessionTerritory != null) {
                    try {
                        JDBCUtils.executeSQL(
                            session,
                            "ALTER SESSION SET NLS_TERRITORY='" + sessionTerritory + "'");
                    } catch (Throwable e) {
                        log.warn("Can't set session territory", e);
                    }
                }
                String nlsDateFormat = connectionInfo.getProviderProperty(DmConstants.PROP_SESSION_NLS_DATE_FORMAT);
                if (nlsDateFormat != null) {
                    try {
                        JDBCUtils.executeSQL(
                            session,
                            "ALTER SESSION SET NLS_DATE_FORMAT='" + nlsDateFormat + "'");
                    } catch (Throwable e) {
                        log.warn("Can't set session NLS date format", e);
                    }
                }

                if (JDBCExecutionContext.TYPE_METADATA.equals(context.getContextName())) {
                    if (CommonUtils.toBoolean(connectionInfo.getProviderProperty(DmConstants.PROP_USE_META_OPTIMIZER))) {
                        // See #5633
                        try {
                            JDBCUtils.executeSQL(session, "ALTER SESSION SET \"_optimizer_push_pred_cost_based\" = FALSE");
                            JDBCUtils.executeSQL(session, "ALTER SESSION SET \"_optimizer_squ_bottomup\" = FALSE");
                            JDBCUtils.executeSQL(session, "ALTER SESSION SET \"_optimizer_cost_based_transformation\" = 'OFF'");
                        } catch (Throwable e) {
                            log.warn("Can't set session optimizer parameters", e);
                        }
                    }
                }
            }
        }
    }

    public DmSchema getDefaultSchema() {
        return (DmSchema) DBUtils.getDefaultContext(this, true).getContextDefaults().getDefaultSchema();
    }
    
    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        super.initialize(monitor);

        DBPConnectionConfiguration connectionInfo = getContainer().getConnectionConfiguration();

        {
            String useRuleHintProp = connectionInfo.getProviderProperty(DmConstants.PROP_USE_RULE_HINT);
            if (useRuleHintProp != null) {
                useRuleHint = CommonUtils.getBoolean(useRuleHintProp, false);
            }
        }

        this.publicSchema = new DmSchema(this, 1, DmConstants.USER_PUBLIC);
        {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load data source meta info")) {
                // Check DBA role
                this.isAdmin = "YES".equals(
                    JDBCUtils.queryString(
                        session,
                        "SELECT 'YES' FROM USER_ROLE_PRIVS WHERE GRANTED_ROLE='DBA'"));
                this.isAdminVisible = isAdmin;
                if (!isAdminVisible) {
                    String showAdmin = connectionInfo.getProviderProperty(DmConstants.PROP_ALWAYS_SHOW_DBA);
                    if (showAdmin != null) {
                        isAdminVisible = CommonUtils.getBoolean(showAdmin, false);
                    }
                }
            } catch (SQLException e) {
                //throw new DBException(e);
                log.warn(e);
            }
            
        }
        
        { //获取表空间默认路径
        	try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load DefaultPath")) {
        		String path=JDBCUtils.queryString(
                        session,
                        "SELECT PATH FROM SYS.V$DATAFILE  LIMIT 0,1");
        		String[] files=path.split("/"); 
        		this.defaultPath=path.substring(0,path.indexOf(files[files.length-1]));
        	}catch (Exception e) {
				// TODO: handle exception
        		log.warn(e);
			}
        }
        
        // Cache data types
        {
            List<DmDataType> dtList = new ArrayList<>();
            for (Map.Entry<String, DmDataType.TypeDesc> predefinedType : DmDataType.PREDEFINED_TYPES.entrySet()) {
                DmDataType dataType = new DmDataType(this, predefinedType.getKey(), true);
                dtList.add(dataType);
            }
            this.dataTypeCache.setCache(dtList);
        }
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        super.refreshObject(monitor);

        this.schemaCache.clearCache();
        //this.dataTypeCache.clearCache();
        this.tablespaceCache.clearCache();
        this.userCache.clearCache();
        this.roleCache.clearCache();
        this.initialize(monitor);

        return this;
    }

	@Override
	public Class<? extends DmSchema> getPrimaryChildType(DBRProgressMonitor monitor) throws DBException {
		// TODO Auto-generated method stub
		return DmSchema.class;
	}
    
    public boolean isAtLeastV8() {
        return getInfo().getDatabaseVersion().getMajor() >= 8; //DM 8 版本
    }

	public String getDefaultPath() {
		return defaultPath;
	}

	public void setDefaultPath(String defaultPath) {
		this.defaultPath = defaultPath;
	}
	
    @Nullable
    public String getPlanTableName(JDBCSession session)
        throws DBException
    {
        if (planTableName == null) {
            String[] candidateNames;
            String tableName = getContainer().getPreferenceStore().getString(DmConstants.PREF_EXPLAIN_TABLE_NAME);
            if (!CommonUtils.isEmpty(tableName)) {
                candidateNames = new String[]{tableName};
            } else {
                candidateNames = new String[]{"PLAN_TABLE", "TOAD_PLAN_TABLE"};
            }
            for (String candidate : candidateNames) {
                try {
                    JDBCUtils.executeSQL(session, "SELECT 1 FROM " + candidate);
                } catch (SQLException e) {
                    // No such table
                    continue;
                }
                planTableName = candidate;
                break;
            }
            if (planTableName == null) {
                final String newPlanTableName = candidateNames[0];
                // Plan table not found - try to create new one
                if (!DBWorkbench.getPlatformUI().confirmAction(
                    "Oracle PLAN_TABLE missing",
                    "PLAN_TABLE not found in current user's session. " +
                        "Do you want DBeaver to create new PLAN_TABLE (" + newPlanTableName + ")?")) {
                    return null;
                }
                planTableName = createPlanTable(session, newPlanTableName);
            }
        }
        return planTableName;
    }

    private String createPlanTable(JDBCSession session, String tableName) throws DBException {
        try {
            JDBCUtils.executeSQL(session, DmConstants.PLAN_TABLE_DEFINITION.replace("${TABLE_NAME}", tableName));
        } catch (SQLException e) {
            throw new DBException("Error creating PLAN table", e, this);
        }
        return tableName;
    }
    
    
	@Override
	public DBCQueryTransformer createQueryTransformer(DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimit();
        }
		return super.createQueryTransformer(type);
	}

}
