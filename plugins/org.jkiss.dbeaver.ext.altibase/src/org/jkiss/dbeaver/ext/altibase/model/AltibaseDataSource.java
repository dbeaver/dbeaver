/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.altibase.model.plan.AltibaseQueryPlanner;
import org.jkiss.dbeaver.ext.altibase.model.session.AltibaseServerSessionManager;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericSynonym;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPObjectStatisticsCollector;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.output.DBCOutputWriter;
import org.jkiss.dbeaver.model.exec.output.DBCServerOutputReader;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AltibaseDataSource extends GenericDataSource implements DBPObjectStatisticsCollector {

    private static final Log log = Log.getLog(AltibaseDataSource.class);

    final TablespaceCache tablespaceCache = new TablespaceCache();
    final UserCache userCache = new UserCache();
    final RoleCache roleCache = new RoleCache();
    final ReplicationCache replCache;
    final JobCache jobCache;
    final DbLinkCache dbLinkCache;
    final MemoryModuleCache memoryModuleCache;
    
    private boolean hasStatistics;

    private GenericSchema publicSchema;
    private boolean isPasswordExpireWarningShown;
    private AltibaseOutputReader outputReader;

    private String dbName;
    String queryGetActiveDB;

    public AltibaseDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, AltibaseMetaModel metaModel)
            throws DBException {
        super(monitor, container, metaModel, new AltibaseSQLDialect());

        queryGetActiveDB = CommonUtils.toString(container.getDriver().getDriverParameter(GenericConstants.PARAM_QUERY_GET_ACTIVE_DB));
        replCache = new ReplicationCache(this);
        jobCache = new JobCache();
        dbLinkCache = new DbLinkCache();
        memoryModuleCache = new MemoryModuleCache();
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);

        // PublicSchema is for global objects such as public synonym.
        publicSchema = new GenericSchema(this, null, AltibaseConstants.USER_PUBLIC);
        publicSchema.setVirtual(true);
    }

    @Override
    protected void initializeContextState(
            @NotNull DBRProgressMonitor monitor, 
            @NotNull JDBCExecutionContext context, 
            JDBCExecutionContext initFrom) throws DBException {

        super.initializeContextState(monitor, context, initFrom);

        // Enable DBMS output
        if (outputReader == null) {
            outputReader = new AltibaseOutputReader();
        }

        outputReader.enableServerOutput(
            monitor,
            context,
            outputReader.isServerOutputEnabled());
    }

    @NotNull
    public AltibaseMetaModel getMetaModel() {
        return (AltibaseMetaModel) super.getMetaModel();
    }

    @Override
    public boolean isOmitCatalog() {
        return true;
    }

    /**
     * Get database name.
     */
    public String getDbName(JDBCSession session) throws DBException {
        if (dbName == null) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(queryGetActiveDB)) {
                try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                    resultSet.next();
                    dbName = JDBCUtils.safeGetStringTrimmed(resultSet, 1);
                }
            } catch (SQLException e) {
                throw new DBDatabaseException(e, this);
            }
        }

        return dbName;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);

        this.tablespaceCache.clearCache();
        this.userCache.clearCache();
        this.roleCache.clearCache();
        this.replCache.clearCache();
        this.jobCache.clearCache();
        this.dbLinkCache.clearCache();

        hasStatistics = false;

        this.initialize(monitor);

        return this;
    }

    @Nullable
    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        DBSObject child = super.getChild(monitor, childName);

        // If it's unable to find the target object in schema, then need to find it from non-schema objects
        if (child == null) {
            child = this.getReplication(monitor, childName);
        }

        return child;
    }

    @Nullable
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBCServerOutputReader.class) {
            return adapter.cast(outputReader);
        } else if (adapter == DBCQueryPlanner.class) {
            return adapter.cast(new AltibaseQueryPlanner(this));
        } else if (adapter == DBAServerSessionManager.class) {
            return adapter.cast(new AltibaseServerSessionManager(this));
        }

        return super.getAdapter(adapter);
    }

    @Override
    protected Connection openConnection(
            @NotNull DBRProgressMonitor monitor, 
            @Nullable JDBCExecutionContext context, 
            @NotNull String purpose) throws DBCException {
        try {
            Connection connection = super.openConnection(monitor, context, purpose);
            try {
                for (SQLWarning warninig = connection.getWarnings();
                    warninig != null && !isPasswordExpireWarningShown;
                    warninig = warninig.getNextWarning()
                ) {
                    if (checkForPasswordWillExpireWarning(warninig)) {
                        isPasswordExpireWarningShown = true;
                    }
                }
            } catch (SQLException e) {
                log.debug("Can't get connection warnings", e);
            }
            return connection;
        } catch (DBCException e) {
            throw e;
        }
    }

    private boolean checkForPasswordWillExpireWarning(@NotNull SQLWarning warning) {
        if ((warning != null) && (warning.getErrorCode() == AltibaseConstants.EC_PASSWORD_WILL_EXPIRE)) {
            DBWorkbench.getPlatformUI().showWarningMessageBox(
                    AltibaseConstants.SQL_WARNING_TITILE,
                    warning.getMessage() + 
                    AltibaseConstants.NEW_LINE + 
                    AltibaseConstants.PASSWORD_WILL_EXPIRE_WARN_DESCRIPTION);

            return true;
        }
        return false;
    }

    @NotNull
    @Override
    public AltibaseDataSource getDataSource() {
        return this;
    }

    @Override
    public boolean splitProceduresAndFunctions() {
        return true;
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return AltibaseSchema.class;
    }

    /*
     * Altibase metadata does not provide information about the target synonym's original object type.
     */
    public DBSObject findSynonymTargetObject(DBRProgressMonitor monitor, @Nullable String refSchemaName, @NotNull String refObjName)
            throws DBException {
        DBSObject refObj = null;
        AltibaseSchema refSchema = (AltibaseSchema) this.getSchema(refSchemaName);

        if (refSchema != null) {
            // No object type from database metadata, so need to find it one by one.
            if (refObj == null) {
                refObj = refSchema.getTable(monitor, refObjName);
            } 
            if (refObj == null) {
                refObj = refSchema.getSequence(monitor, refObjName);
            }
            if (refObj == null) {
                refObj = refSchema.getSynonym(monitor, refObjName);
            }
            if (refObj == null) {
                refObj = refSchema.getProcedureByName(monitor, refObjName);
            }
            if (refObj == null) {
                refObj = refSchema.getPackage(monitor, refObjName);
            }
            if (refObj == null) {
                refObj = refSchema.getIndex(monitor, refObjName);
            }
            if (refObj == null) {
                refObj = refSchema.getTableTrigger(monitor, refObjName);
            }
        } else {
            /*
             *  Though Public synonym does not have its own schema, but SYSTEM_.SYS_SYONYMS_.OBJECT_OWNER_NAME returns 
             *  the object creator as owner like Oracle.
             *  So, first look for it in the owner's private schema, and if it is not found, then try to find it at public schema.
             *  If found, judge it as a public schema.
             */
            if (refObj == null) {
                refObj = publicSchema.getSynonym(monitor, refObjName);
                if (refObj != null) {
                    ((AltibaseSynonym) refObj).setPublicSynonym();
                }
            }
        }

        return refObj;
    }

    /**
     * Returns public synonym as a collection.
     */
    public Collection<? extends GenericSynonym> getPublicSynonyms(DBRProgressMonitor monitor) throws DBException {
        return publicSchema.getSynonyms(monitor);
    }

    ///////////////////////////////////////////////
    // Tablespace

    /**
     * Get tablespace colection
     */
    @Association
    public Collection<AltibaseTablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException {
        return tablespaceCache.getAllObjects(monitor, this);
    }

    /**
     * Get tablespace cache
     */
    public TablespaceCache getTablespaceCache() {
        return tablespaceCache;
    }

    static class TablespaceCache extends JDBCObjectCache<AltibaseDataSource, AltibaseTablespace> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(
                @NotNull JDBCSession session, @NotNull AltibaseDataSource owner) throws SQLException {
            return session.prepareStatement("SELECT * FROM V$TABLESPACES ORDER BY NAME ASC");
        }

        @Override
        protected AltibaseTablespace fetchObject(
                @NotNull JDBCSession session, 
                @NotNull AltibaseDataSource owner, 
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new AltibaseTablespace(owner, resultSet);
        }
    }

    /**
     * Get User cache
     */
    static class UserCache extends JDBCObjectCache<AltibaseDataSource, AltibaseUser> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, 
                @NotNull AltibaseDataSource owner) throws SQLException {
            return session.prepareStatement(
                "SELECT u.*, tbs1.name AS default_tbs_name, tbs2.name AS temp_tbs_name "
                + "FROM SYSTEM_.SYS_USERS_ u, V$TABLESPACES tbs1, V$TABLESPACES tbs2 "
                + "WHERE u.user_type = 'U' AND u.DEFAULT_TBS_ID = tbs1.id AND u.TEMP_TBS_ID = tbs2.id "
                + "ORDER BY user_name");
        }

        @Override
        protected AltibaseUser fetchObject(@NotNull JDBCSession session, @NotNull AltibaseDataSource owner, 
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new AltibaseUser(owner, resultSet);
        }
    }

    /**
     * Returns Altibase users. 
     */
    @Association
    public Collection<AltibaseUser> getUsers(DBRProgressMonitor monitor) throws DBException {
        return userCache.getAllObjects(monitor, this);
    }

    /**
     * Returns a specific Altibase user. 
     */
    @Association
    public AltibaseUser getUser(DBRProgressMonitor monitor, String name) throws DBException {
        return userCache.getObject(monitor, this, name);
    }

    /**
     * Altibase Roles
     */
    static class RoleCache extends JDBCObjectCache<AltibaseDataSource, AltibaseRole> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, 
                @NotNull AltibaseDataSource owner) throws SQLException {
            return session.prepareStatement(
                "SELECT * "
                + " FROM SYSTEM_.SYS_USERS_ u "
                + " WHERE u.user_type = 'R' AND u.user_name <> 'PUBLIC'"
                + " ORDER BY user_name");
        }

        @Override
        protected AltibaseRole fetchObject(@NotNull JDBCSession session, @NotNull AltibaseDataSource owner, 
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new AltibaseRole(owner, resultSet);
        }
    }

    /**
     * Returns Altibase roles 
     */
    @Association
    public Collection<AltibaseRole> getRoles(DBRProgressMonitor monitor) throws DBException {
        return roleCache.getAllObjects(monitor, this);
    }

    /**
     * Returns a specific Altibase role.
     */
    @Association
    public AltibaseRole getRole(DBRProgressMonitor monitor, String name) throws DBException {
        return roleCache.getObject(monitor, this, name);
    }

    /**
     * Returns Altibase grantee. 
     */
    public AltibaseGrantee getGrantee(DBRProgressMonitor monitor, String name) throws DBException {
        AltibaseUser user = userCache.getObject(monitor, this, name);
        if (user != null) {
            return user;
        }
        return roleCache.getObject(monitor, this, name);
    }


    ///////////////////////////////////////////////
    // Replications
    static class ReplicationCache extends JDBCStructLookupCache<GenericStructContainer, AltibaseReplication, AltibaseReplicationItem> {

        final AltibaseDataSource dataSource;

        protected ReplicationCache(AltibaseDataSource dataSource) {
            super("Replication");
            this.dataSource = dataSource;
            setListOrderComparator(DBUtils.<AltibaseReplication>nameComparatorIgnoreCase());
        }

        public AltibaseDataSource getDataSource() {
            return dataSource;
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, 
                @Nullable AltibaseReplication object, @Nullable String objectName) throws SQLException {
            String replName = (object != null) ? object.getName() : objectName;
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT"
                            + " r.replication_name,"
                            + " r.is_started,"
                            + " DECODE( r.conflict_resolution,"
                                + " 0, 'Default', "
                                + " 1, 'Master', "
                                + " 2, 'Slave', "
                                + " 'Unknown') AS conflict_resolution,"
                            + " DECODE( r.repl_mode, "
                                + " 0, 'Lazy', "
                                + " 2,'Eager', "
                                + " 'Unknown') AS repl_mode, "
                            + " DECODE( r.role,"
                                + " 0, 'General',"
                                + " 1, 'Log Analyzer',"
                                + " 2, 'Propagable Logging',"
                                + " 3, 'Propagation',"
                                + " 4, 'Propagation for Log Analyzer ',"
                                + " 'Unknown') AS role,"
                            + " r.options,"
                            + " DECODE( r.invalid_recovery,"
                                + " 0, 'Valid',"
                                + " 1, 'Invalid',"
                                + " 'Unknown') AS recoverable,"
                            + " parallel_applier_count,"
                            + " rh.host_ip || ':' || rh.port_no AS remote_addr,"
                            + " rh.conn_type AS remote_conn_type,"
                            + " r.xsn,"
                            + " r.remote_last_ddl_xsn,"
                            + " r.remote_fault_detect_time,"
                            + " r.give_up_time,"
                            + " r.give_up_xsn,"
                            + " r.remote_xsn,"
                            + " r.applier_init_buffer_size,"
                            + " r.peer_replication_name"
                        + " FROM system_.sys_replications_ r, system_.sys_repl_hosts_ rh"
                        + " WHERE"
                            + " r.replication_name = rh.replication_name"
                            + (CommonUtils.isEmpty(replName) ? "" : " AND r.replication_name = ?")
                        + " ORDER BY r.replication_name"
                    );

            if (CommonUtils.isNotEmpty(replName)) {
                dbStat.setString(1, replName);
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected AltibaseReplication fetchObject(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, 
                @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new AltibaseReplication(owner, dbResult);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, 
                @NotNull AltibaseReplication forTable) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM system_.sys_repl_items_"
                    + " WHERE replication_name = ?"
                    + " ORDER BY local_user_name, local_table_name, local_partition_name, "
                    + " remote_user_name, remote_table_name, remote_partition_name");
            dbStat.setString(1, forTable.getName());
            return dbStat;
        }

        @Override
        protected AltibaseReplicationItem fetchChild(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, 
                @NotNull AltibaseReplication replication, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new AltibaseReplicationItem(replication, dbResult);
        }
    }

    /**
     * Get Replication Cache
     */

    public ReplicationCache getReplicationCache() {
        return replCache;
    }

    /**
     * Return all cached replications.
     */
    @Association
    public Collection<AltibaseReplication> getReplications(DBRProgressMonitor monitor) throws DBException {
        return replCache.getAllObjects(monitor, this);
    }

    /**
     * Return a specific cached replication.
     */
    @Association
    public AltibaseReplication getReplication(DBRProgressMonitor monitor, String name) throws DBException {
        return replCache.getObject(monitor, this, name);
    }

    ///////////////////////////////////////////////
    // Jobs
    

    public JobCache getJobCache() {
        return jobCache;
    }

    static class JobCache extends JDBCObjectLookupCache<GenericStructContainer, AltibaseJob> {
        
        @Override
        protected AltibaseJob fetchObject(@NotNull JDBCSession session, GenericStructContainer owner, 
                @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new AltibaseJob(owner, dbResult);
        }

        @Override
        public JDBCStatement prepareLookupStatement(JDBCSession session, GenericStructContainer owner,
                AltibaseJob object, String objectName) throws SQLException {
            boolean isNullObject = object == null && objectName == null;
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM system_.sys_jobs_ s "
                    + (isNullObject ? "" : "  WHERE s.job_name = ?")
                    + " ORDER BY job_name ASC");
            
            if (!isNullObject) {
                dbStat.setString(1, object != null ? object.getName() : objectName);
            }
            
            return dbStat;
        }
    }

    @Association
    public Collection<AltibaseJob> getJobs(@NotNull DBRProgressMonitor monitor) throws DBException {
        return jobCache.getAllObjects(monitor, this);
    }

    ///////////////////////////////////////////////
    // Modules

    @Association
    public Collection<AltibaseMemoryModule> getMemoryModules(DBRProgressMonitor monitor) throws DBException {
        return memoryModuleCache.getAllObjects(monitor, this);
    }

    public MemoryModuleCache getModuleCache() {
        return memoryModuleCache;
    }

    static class MemoryModuleCache extends JDBCObjectCache<GenericStructContainer, AltibaseMemoryModule> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, 
                @NotNull GenericStructContainer owner) throws SQLException {
            return session.prepareStatement("SELECT * FROM v$memstat ORDER BY max_total_size DESC");
        }

        @Override
        protected AltibaseMemoryModule fetchObject(@NotNull JDBCSession session, 
                @NotNull GenericStructContainer owner, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new AltibaseMemoryModule(owner, dbResult);
        }
    }

    @Association
    public Collection<AltibaseMemoryModule> getModules(@NotNull DBRProgressMonitor monitor) throws DBException {
        return memoryModuleCache.getAllObjects(monitor, this);
    }

    ///////////////////////////////////////////////
    // Public DB Links
    
    public DbLinkCache getDbLinkCache() {
        return dbLinkCache;
    }
    
    static class DbLinkCache extends JDBCObjectLookupCache<GenericStructContainer, AltibaseDbLink> {

        @Override
        protected AltibaseDbLink fetchObject(@NotNull JDBCSession session, GenericStructContainer owner, 
                @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new AltibaseDbLink(owner, dbResult);
        }

        @Override
        public JDBCStatement prepareLookupStatement(JDBCSession session, GenericStructContainer owner,
                AltibaseDbLink object, String objectName) throws SQLException {
            boolean isNullObject = object == null && objectName == null;
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT null as USER_NAME, l.* FROM system_.sys_database_links_ l"
                    + " WHERE user_mode = 0"
                    + (isNullObject ? "" : " AND l.link_name = ?")
                    + " ORDER BY link_name ASC");
            
            if (!isNullObject) {
                dbStat.setString(1, object != null ? object.getName() : objectName);
            }
            
            return dbStat;
        }
    }
    
    @Association
    public Collection<AltibaseDbLink> getPublicDbLinks(@NotNull DBRProgressMonitor monitor) throws DBException {
        return dbLinkCache.getAllObjects(monitor, this);
    }

    ///////////////////////////////////////////////
    // Statistics

    @Override
    public boolean isStatisticsCollected() {
        return hasStatistics;
    }

    void resetStatistics() {
        hasStatistics = false;
    }

    @Override
    public void collectObjectStatistics(DBRProgressMonitor monitor, boolean totalSizeOnly, boolean forceRefresh) throws DBException {
        if (hasStatistics && !forceRefresh) {
            return;
        }

        try {
            for (AltibaseTablespace tbs : tablespaceCache.getAllObjects(monitor, AltibaseDataSource.this)) {
                AltibaseTablespace tablespace = tablespaceCache.getObject(monitor, AltibaseDataSource.this, tbs.getName());
                if (tablespace != null) {
                    tablespace.loadSizes(monitor);
                }
            }
        } catch (DBException e) {
            throw new DBDatabaseException("Can't read tablespace statistics", e, getDataSource());
        } finally {
            hasStatistics = true;
        }
    }

    ///////////////////////////////////////////////
    // Altibase Properties
    @NotNull
    public List<AltibaseProperty> getProperties(DBRProgressMonitor monitor)
            throws DBException {
        return loadPropertyList(monitor);
    }

    @NotNull
    private List<AltibaseProperty> loadPropertyList(@NotNull DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load properties")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM V$PROPERTY ORDER BY NAME ASC")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<AltibaseProperty> propertyList = new ArrayList<>();
                    while (dbResult.next()) {
                        AltibaseProperty parameter = new AltibaseProperty(this, dbResult);
                        propertyList.add(parameter);
                    }
                    return propertyList;
                }
            }
        } catch (SQLException ex) {
            throw new DBException("Failed to load database properties", ex);
        }
    }
    
    ///////////////////////////////////////////////
    // DBMS Procedure Output
    private class AltibaseOutputReader implements DBCServerOutputReader {

        private StringBuilder callBackMsg = new StringBuilder();

        @Override
        public boolean isServerOutputEnabled() {
            return getContainer().getPreferenceStore().getBoolean(AltibaseConstants.PREF_DBMS_OUTPUT);
        }

        @Override
        public boolean isAsyncOutputReadSupported() {
            return false;
        }

        public void enableServerOutput(
                DBRProgressMonitor monitor, 
                DBCExecutionContext context, 
                boolean enable) throws DBCException {

            Connection conn = null;
            ClassLoader classLoader = null;
            @SuppressWarnings("rawtypes")
            Class class4MsgCallback = null;
            Object instance4Callback = null;
            Method method2RegisterCallback = null;

            String connClassNamePrefix = "Altibase";
            String className4Connection = "N/A";
            String className4MessageCallback = "N/A";

            try (JDBCSession session = (JDBCSession) context.openSession(monitor, 
                    DBCExecutionPurpose.UTIL, (enable ? "Enable" : "Disable") + " DBMS output")) {

                conn = session.getOriginal();
                classLoader = conn.getClass().getClassLoader();
                if (classLoader == null) {
                    throw new SecurityException("Failed to load ClassLoader");
                }

                // To support Altibasex_x.jar driver
                connClassNamePrefix = conn.getClass().getName().split("\\.")[0];
                className4Connection = connClassNamePrefix + AltibaseConstants.CLASS_NAME_4_CONNECTION_POSTFIX;
                className4MessageCallback = connClassNamePrefix + AltibaseConstants.CLASS_NAME_4_MESSAGE_CALLBACK_POSTFIX;

                class4MsgCallback = classLoader.loadClass(className4MessageCallback);
                if (class4MsgCallback == null) {
                    throw new ClassNotFoundException("Failed to load class: " + className4MessageCallback);
                }

                instance4Callback = Proxy.newProxyInstance(classLoader, 
                        new Class[] { class4MsgCallback }, 
                        new InvocationHandler() {
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                if ("print".equals(method.getName())) {
                                    callBackMsg.append((String) args[0]);
                        }

                        return null;
                    }
                });

                if (instance4Callback == null) {
                    throw new InstantiationException("Failed to instantiate class: " + className4MessageCallback);
                }

                method2RegisterCallback = classLoader
                        .loadClass(className4Connection)
                        .getMethod(AltibaseConstants.METHOD_NAME_4_REGISTER_MESSAGE_CALLBACK, class4MsgCallback);

                if (method2RegisterCallback == null) {
                    throw new NoSuchMethodException(String.format(
                            "Failed to get method: %s of class %s ", 
                            AltibaseConstants.METHOD_NAME_4_REGISTER_MESSAGE_CALLBACK,
                            className4MessageCallback));
                }

                method2RegisterCallback.invoke(conn, instance4Callback);

            } catch (Exception e) {
                log.error("Failed to register DBMS output message callback method: " + e.getMessage());
                throw new DBCException(e, context);
            }
        }

        @Override
        public void readServerOutput(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBCExecutionContext context,
                @Nullable DBCExecutionResult executionResult,
                @Nullable DBCStatement statement,
                @NotNull DBCOutputWriter output) throws DBCException {
            if (callBackMsg != null) {
                output.println(null, callBackMsg.toString());
                callBackMsg.delete(0, callBackMsg.length());
            }
        }
    }
}
