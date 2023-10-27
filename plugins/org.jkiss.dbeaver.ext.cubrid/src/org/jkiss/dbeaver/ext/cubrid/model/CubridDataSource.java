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

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.model.meta.CubridMetaModel;
import org.jkiss.dbeaver.ext.cubrid.model.meta.CubridMetaObject;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.*;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.ForTest;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.cache.SimpleObjectCache;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.time.ExtendedDateFormat;

import java.sql.*;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * CubridDataSource
 */
public class CubridDataSource extends JDBCDataSource implements DBPTermProvider, IAdaptable, CubridStructContainer {
    private static final Log log = Log.getLog(CubridDataSource.class);

    private final TableTypeCache tableTypeCache;
    private final JDBCBasicDataTypeCache<CubridStructContainer, ? extends JDBCDataType> dataTypeCache;
    private List<CubridCatalog> catalogs;
    private SimpleObjectCache<CubridStructContainer, CubridSchema> schemas;
    private final CubridMetaModel metaModel;
    private CubridObjectContainer structureContainer;
    boolean catalogsFiltered;

    private String queryGetActiveDB;
    private String querySetActiveDB;
    private String selectedEntityType;
    private boolean selectedEntityFromAPI;
    private boolean omitSingleCatalog;
    private String allObjectsPattern;
    private boolean supportsStructCache;
    private DBCQueryPlanner queryPlanner;
    private Format nativeFormatTimestamp, nativeFormatTime, nativeFormatDate;
    private ArrayList<CubridUser> owners;


    public CubridDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container, @NotNull CubridMetaModel metaModel, @NotNull SQLDialect dialect)
        throws DBException {
        super(monitor, container, dialect, false);
        this.metaModel = metaModel;
        final DBPDriver driver = container.getDriver();
        this.dataTypeCache = metaModel.createDataTypeCache(this);
        this.tableTypeCache = new TableTypeCache();
        this.queryGetActiveDB = CommonUtils.toString(driver.getDriverParameter(CubridConstants.PARAM_QUERY_GET_ACTIVE_DB));
        this.querySetActiveDB = CommonUtils.toString(driver.getDriverParameter(CubridConstants.PARAM_QUERY_SET_ACTIVE_DB));
        this.selectedEntityType = CommonUtils.toString(driver.getDriverParameter(CubridConstants.PARAM_ACTIVE_ENTITY_TYPE));
        this.omitSingleCatalog = CommonUtils.getBoolean(driver.getDriverParameter(CubridConstants.PARAM_OMIT_SINGLE_CATALOG), false);
        if (CommonUtils.isEmpty(this.selectedEntityType)) {
            this.selectedEntityType = null;
        }
        this.allObjectsPattern = CommonUtils.toString(driver.getDriverParameter(CubridConstants.PARAM_ALL_OBJECTS_PATTERN));
        if (CommonUtils.isEmpty(this.allObjectsPattern)) {
            this.allObjectsPattern = "%";
        } else if ("null".equalsIgnoreCase(this.allObjectsPattern)) {
            this.allObjectsPattern = null;
        }

        // Init native formats
        nativeFormatTimestamp = makeNativeFormat(CubridConstants.PARAM_NATIVE_FORMAT_TIMESTAMP);
        nativeFormatTime = makeNativeFormat(CubridConstants.PARAM_NATIVE_FORMAT_TIME);
        nativeFormatDate = makeNativeFormat(CubridConstants.PARAM_NATIVE_FORMAT_DATE);

        initializeRemoteInstance(monitor);
    }

    // Constructor for tests
    @ForTest
    public CubridDataSource(@NotNull DBRProgressMonitor monitor, @NotNull CubridMetaModel metaModel, @NotNull DBPDataSourceContainer container, @NotNull SQLDialect dialect)
            throws DBException {
        super(monitor, container, dialect, false);
        this.metaModel = metaModel;
        this.dataTypeCache = metaModel.createDataTypeCache(this);
        this.tableTypeCache = new TableTypeCache();
    }

    @NotNull
    @Override
    public CubridDataSource getDataSource() {
        return this;
    }

    @Override
    protected String getConnectionURL(DBPConnectionConfiguration connectionInfo) {
        // Recreate URL from parameters
        // Driver settings and URL template may have change since connection creation
        String connectionURL = getContainer().getDriver().getConnectionURL(connectionInfo);
        if (!getContainer().getDriver().isSampleURLApplicable() && connectionInfo.getUrl() != null && !CommonUtils.equalObjects(connectionURL, connectionInfo.getUrl())) {
            log.warn("Actual connection URL (" + connectionURL + ") differs from previously saved (" + connectionInfo.getUrl() + "). " +
                "Probably driver properties were changed. Please go to the connection '" + getContainer().getName() + "' editor.");
            connectionInfo.setUrl(connectionURL);
        }
        return connectionURL;
    }

    @Override
    protected Connection openConnection(@NotNull DBRProgressMonitor monitor, @Nullable JDBCExecutionContext context, @NotNull String purpose) throws DBCException {
        Connection jdbcConnection = super.openConnection(monitor, context, purpose);

        if (isPopulateClientAppName() && !getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_CLIENT_NAME_DISABLE)) {
            populateClientAppName(context, purpose, jdbcConnection);
        }

        return jdbcConnection;
    }

    private void populateClientAppName(JDBCExecutionContext context, @NotNull String purpose, Connection jdbcConnection) {
        // Provide client info
        // "ApplicationName" property seems to be pretty standard
        boolean wasPopulated = false;
        try {
            final ResultSet ciList = jdbcConnection.getMetaData().getClientInfoProperties();
            if (ciList != null) {
                try {
                    while (ciList.next()) {
                        final String name = JDBCUtils.safeGetString(ciList, "NAME");
                        int maxLength = JDBCUtils.safeGetInt(ciList, "MAX_LEN");
                        if (JDBCConstants.APPLICATION_NAME_CLIENT_PROPERTY.equals(name)) {
                            String appName = DBUtils.getClientApplicationName(getContainer(), context, purpose);
                            if (maxLength <= 0) {
                                maxLength = 48;
                            }
                            jdbcConnection.setClientInfo(JDBCConstants.APPLICATION_NAME_CLIENT_PROPERTY, CommonUtils.truncateString(appName, maxLength));
                            wasPopulated = true;
                            break;
                        }
                    }
                } finally {
                    ciList.close();
                }
            }
        } catch (Throwable e) {
            log.debug("Error reading and setting client application name: " + e.getMessage());
        }
        if (!wasPopulated) {
            String appName = DBUtils.getClientApplicationName(getContainer(), context, purpose);
            try {
                jdbcConnection.setClientInfo(JDBCConstants.APPLICATION_NAME_CLIENT_PROPERTY, appName);
            } catch (Throwable e) {
                log.debug("Error setting client application name: " + e.getMessage());
            }
        }
    }

    /**
     * Disable by default. Some drivers fail to connect when client app name is specified
     * Enable for all derived classes.
     */
    protected boolean isPopulateClientAppName() {
        return getClass() != CubridDataSource.class;
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new CubridExecutionContext(instance, type);
    }

    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context, JDBCExecutionContext initFrom) throws DBException {
        super.initializeContextState(monitor, context, initFrom);
        if (initFrom != null) {
            CubridExecutionContext metaContext = (CubridExecutionContext) initFrom;
            ((CubridExecutionContext) context).initDefaultsFrom(monitor, metaContext);
        } else {
            ((CubridExecutionContext)context).refreshDefaults(monitor, true);
        }
    }

    public Collection<CubridUser> getOwners() {
    	return owners;
    }
    
    public String getAllObjectsPattern() {
        return allObjectsPattern;
    }

    @NotNull
    public CubridMetaModel getMetaModel() {
        return metaModel;
    }

    @Nullable
    public CubridMetaObject getMetaObject(String id) {
        return metaModel.getMetaObject(id);
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData) {
        final CubridDataSourceInfo info = new CubridDataSourceInfo(getContainer().getDriver(), metaData);
        final JDBCSQLDialect dialect = (JDBCSQLDialect) getSQLDialect();

        final Object supportsReferences = getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_SUPPORTS_REFERENCES);
        if (supportsReferences != null) {
            info.setSupportsReferences(CommonUtils.toBoolean(supportsReferences));
        }

        final Object supportsIndexes = getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_SUPPORTS_INDEXES);
        if (supportsIndexes != null) {
            info.setSupportsIndexes(CommonUtils.toBoolean(supportsIndexes));
        }

        final Object supportsViews = getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_SUPPORTS_VIEWS);
        if (supportsViews != null) {
            info.setSupportsViews(CommonUtils.toBoolean(supportsViews));
        }

        final Object supportsStoredCode = getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_SUPPORTS_STORED_CODE);
        if (supportsStoredCode != null) {
            info.setSupportsStoredCode(CommonUtils.toBoolean(supportsStoredCode));
        }

        final Object supportsSubqueries = getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_SUPPORTS_SUBQUERIES);
        if (supportsSubqueries != null) {
            dialect.setSupportsSubqueries(CommonUtils.toBoolean(supportsSubqueries));
        }

        final Object supportsStructCacheParam = getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_SUPPORTS_STRUCT_CACHE);
        if (supportsStructCacheParam != null) {
            this.supportsStructCache = CommonUtils.toBoolean(supportsStructCacheParam);
        }
        final Object supportsCatalogSelection = getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_SUPPORTS_CATALOG_SELECTION);
        if (supportsCatalogSelection != null) {
            info.supportsCatalogSelection = CommonUtils.toBoolean(supportsCatalogSelection);
        }
        final Object supportSchemaSelection = getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_SUPPORTS_SCHEMA_SELECTION);
        if (supportSchemaSelection != null) {
            info.supportsSchemaSelection = CommonUtils.toBoolean(supportSchemaSelection);
        }
        return info;
    }

    @Override
    public void shutdown(DBRProgressMonitor monitor) {
        String queryShutdown = CommonUtils.toString(getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_QUERY_SHUTDOWN));
        if (!CommonUtils.isEmpty(queryShutdown)) {
            for (JDBCRemoteInstance instance : getAvailableInstances()) {
                for (JDBCExecutionContext context : instance.getAllContexts()) {
                    try (JDBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, "Shutdown database")) {
                        JDBCUtils.executeStatement(session, queryShutdown);
                    } catch (Throwable e) {
                        log.error("Error shutting down database", e);
                    }
                }
            }
        }

        super.shutdown(monitor);
        String paramShutdown = CommonUtils.toString(getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_SHUTDOWN_URL_PARAM));
        if (!CommonUtils.isEmpty(paramShutdown)) {
            monitor.subTask("Shutdown embedded database");
            try {
                Properties shutdownProps = new Properties();
                DBPConnectionConfiguration connectionInfo = getContainer().getActualConnectionConfiguration();
                if (!CommonUtils.isEmpty(connectionInfo.getUserName())) {
                    shutdownProps.put(DBConstants.DATA_SOURCE_PROPERTY_USER, connectionInfo.getUserName());
                }
                if (!CommonUtils.isEmpty(connectionInfo.getUserPassword())) {
                    shutdownProps.put(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD, connectionInfo.getUserPassword());
                }

                final Driver driver = getDriverInstance(new VoidProgressMonitor()); // Use void monitor - driver already loaded
                if (driver != null) {
                    driver.connect(
                        getContainer().getActualConnectionConfiguration().getUrl() + paramShutdown,
                        shutdownProps.isEmpty() ? null : shutdownProps);
                }
            } catch (Exception e) {
                log.debug("Shutdown finished: :" + e.getMessage());
            }
            monitor.worked(1);
        }
    }

    boolean supportsStructCache() {
        return supportsStructCache;
    }

    @Association
    public Collection<CubridTableType> getTableTypes(DBRProgressMonitor monitor)
        throws DBException {
        return tableTypeCache.getAllObjects(monitor, this);
    }

    public List<CubridCatalog> getCatalogs() {
        return catalogs;
    }

    public CubridCatalog getCatalog(String name) {
        return DBUtils.findObject(getCatalogs(), name);
    }

    public final Collection<CubridCatalog> getCatalogList() {
//        if (getDataSource().isMergeEntities()) {
//            return null;
//        }
        return getCatalogs();
    }

    public final Collection<CubridSchema> getSchemaList() {
        if (getDataSource().isMergeEntities()) {
            return null;
        }
        return getSchemas();
    }

    @Association
    public List<CubridSchema> getSchemas() {
        return schemas == null ? null : schemas.getCachedObjects();
    }

    public CubridSchema getSchema(String name) {
        return schemas == null ? null : schemas.getCachedObject(name);
    }

    public SimpleObjectCache getSchemaCache() {
        return schemas;
    }

    @Override
    public CubridStructContainer getObject() {
        return this;
    }

    @Override
    public CubridCatalog getCatalog() {
        return null;
    }

    @Override
    public CubridSchema getSchema() {
        return null;
    }

    @Override
    public TableCache getTableCache() {
        return structureContainer.getTableCache();
    }

    @Override
    public IndexCache getIndexCache() {
        return structureContainer.getIndexCache();
    }

    @Override
    public ConstraintKeysCache getConstraintKeysCache() {
        return structureContainer.getConstraintKeysCache();
    }

    @Override
    public ForeignKeysCache getForeignKeysCache() {
        return structureContainer.getForeignKeysCache();
    }

    @Override
    public TableTriggerCache getTableTriggerCache() {
        return structureContainer.getTableTriggerCache();
    }

    @Override
    public CubridObjectContainer.CubridSequenceCache getSequenceCache() {
        return structureContainer.getSequenceCache();
    }

    @Override
    public CubridObjectContainer.CubridSynonymCache getSynonymCache() {
        return structureContainer.getSynonymCache();
    }

    @Override
    public List<? extends CubridView> getViews(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getViews(monitor);
    }

    @Override
    public List<? extends CubridTable> getPhysicalTables(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getPhysicalTables(monitor);
    }

    @Override
    public List<? extends CubridTableBase> getTables(DBRProgressMonitor monitor)
        throws DBException {
        return structureContainer == null ? null : structureContainer.getTables(monitor);
    }

    @Override
    public CubridTableBase getTable(DBRProgressMonitor monitor, String name)
        throws DBException {
        return structureContainer == null ? null : structureContainer.getTable(monitor, name);
    }

    @Override
    public Collection<CubridPackage> getPackages(DBRProgressMonitor monitor)
        throws DBException {
        return structureContainer == null ? null : structureContainer.getPackages(monitor);
    }

    @Override
    public Collection<CubridTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException {
        return structureContainer == null ? null : structureContainer.getIndexes(monitor);
    }

    @Override
    public Collection<? extends CubridProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException {
        return structureContainer == null ? null : structureContainer.getProcedures(monitor);
    }

    @Override
    public Collection<? extends CubridProcedure> getProceduresOnly(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getProceduresOnly(monitor);
    }

    @Override
    public CubridProcedure getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
        return structureContainer == null ? null : structureContainer.getProcedure(monitor, uniqueName);
    }

    @Override
    public Collection<CubridProcedure> getProcedures(DBRProgressMonitor monitor, String name)
        throws DBException {
        return structureContainer == null ? null : structureContainer.getProcedures(monitor, name);
    }

    @Override
    public Collection<? extends CubridProcedure> getFunctionsOnly(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getFunctionsOnly(monitor);
    }

    @Override
    public Collection<? extends CubridSequence> getSequences(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getSequences(monitor);
    }

    @Override
    public Collection<? extends CubridSynonym> getSynonyms(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getSynonyms(monitor);
    }

    @Override
    public Collection<? extends CubridTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getTriggers(monitor);
    }

    @Override
    public Collection<? extends CubridTrigger> getTableTriggers(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getTableTriggers(monitor);
    }
    
    @Override
    public Collection<? extends CubridUser> getCubridUsers(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getCubridUsers(monitor);
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);
        boolean omitCatalog = isOmitCatalog();
        boolean omitTypeCache = CommonUtils.toBoolean(getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_OMIT_TYPE_CACHE));
        if (!omitTypeCache) {
            // Cache data types
            try {
                dataTypeCache.getAllObjects(monitor, this);
            } catch (Exception e) {
                log.warn("Can't fetch database data types: " + e.getMessage());
            }
            if (CommonUtils.isEmpty(dataTypeCache.getCachedObjects())) {
                // Use basic data types
                dataTypeCache.fillStandardTypes(this);
            }
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read cubrid metadata")) {
            // Read metadata
            JDBCDatabaseMetaData metaData = session.getMetaData();
            if (!omitCatalog) {
                // Read catalogs
                monitor.subTask("Extract catalogs");
                monitor.worked(1);
                final CubridMetaObject catalogObject = getMetaObject(CubridConstants.OBJECT_CATALOG);
                final DBSObjectFilter catalogFilters = getContainer().getObjectFilter(CubridCatalog.class, null, false);
                final List<String> catalogNames = getCatalogsNames(monitor, metaData, catalogObject, catalogFilters);
                if (!catalogNames.isEmpty() || catalogsFiltered) {
                    this.catalogs = new ArrayList<>();
                    for (String catalogName : catalogNames) {
                        CubridCatalog catalog = metaModel.createCatalogImpl(this, catalogName);
                        this.catalogs.add(catalog);
                    }
                }
            }
            if (CommonUtils.isEmpty(catalogs) && !catalogsFiltered) {
                // Catalogs not supported - try to read root schemas
                monitor.subTask("Extract schemas");
                monitor.worked(1);

                try {
                    List<CubridSchema> tmpSchemas = metaModel.loadSchemas(session, this, null);
                    if (tmpSchemas != null) {
                        this.schemas = new SimpleObjectCache<>();
                        this.schemas.setCaseSensitive(getSQLDialect().storesUnquotedCase() != DBPIdentifierCase.MIXED);
                        this.schemas.setCache(tmpSchemas);
                    }
                } catch (Throwable e) {
                    if (metaModel.isSchemasOptional()) {
                        log.warn("Can't read schema list", e);
                    } else {
                        if (e instanceof DBException) {
                            throw (DBException) e;
                        }
                        throw new DBException("Error reading schema list", e, this);
                    }
                }

                if (isMergeEntities() || (schemas == null || schemas.isEmpty())) {
                    this.structureContainer = new DataSourceObjectContainer();
                }
            }
        } catch (Throwable ex) {
            if (ex instanceof DBException) {
                throw (DBException) ex;
            }
            throw new DBException("Error reading metadata", ex, this);
        }
        
        owners = new ArrayList<CubridUser>();
	    try (JDBCSession session = DBUtils.openMetaSession(monitor, structureContainer, allObjectsPattern)) {
		    String sql = CubridConstants.OWNER_QUERY;
			JDBCPreparedStatement dbStat = session.prepareStatement(sql);
			ResultSet rs = dbStat.executeQuery();
			while(rs.next()) {
				CubridUser owner = new CubridUser(rs.getString("name"));
	        	owners.add(owner);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public List<String> getCatalogsNames(@NotNull DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData, CubridMetaObject catalogObject, @Nullable DBSObjectFilter catalogFilters) throws DBException {
        final List<String> catalogNames = new ArrayList<>();
        try {
            try (JDBCResultSet dbResult = metaData.getCatalogs()) {
                int totalCatalogs = 0;
                while (dbResult.next()) {
                    String catalogName = CubridUtils.safeGetString(catalogObject, dbResult, JDBCConstants.TABLE_CAT);
                    if (CommonUtils.isEmpty(catalogName)) {
                        // Some drivers uses TABLE_QUALIFIER instead of catalog
                        catalogName = CubridUtils.safeGetStringTrimmed(catalogObject, dbResult, JDBCConstants.TABLE_QUALIFIER);
                        if (CommonUtils.isEmpty(catalogName)) {
                            continue;
                        }
                    }
                    totalCatalogs++;
                    if (catalogFilters == null || catalogFilters.matches(catalogName)) {
                        catalogNames.add(catalogName);
                        monitor.subTask("Extract catalogs - " + catalogName);
                    } else {
                        catalogsFiltered = true;
                    }
                    if (monitor.isCanceled()) {
                        break;
                    }
                }
                if (totalCatalogs == 1 && omitSingleCatalog) {
                    // Just one catalog. Looks like DB2 or PostgreSQL
                    // Let's just skip it and use only schemas
                    // It's ok to use "%" instead of catalog name anyway
                    catalogNames.clear();
                }
            }
        } catch (UnsupportedOperationException | SQLFeatureNotSupportedException e) {
            // Just skip it
            log.debug("Catalog list not supported: " + e.getMessage());
        } catch (Throwable e) {
            if (metaModel.isCatalogsOptional()) {
                // Error reading catalogs - just warn about it
                log.warn("Can't read catalog list", e);
            } else {
                throw new DBException("Error reading catalog list", e);
            }
        }
        return catalogNames;
    }

    public boolean isOmitCatalog() {
        return CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_OMIT_CATALOG), false);
    }

    public boolean isOmitSchema() {
        return CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_OMIT_SCHEMA), false);
    }

    public boolean isOmitSingleSchema() {
        return CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_OMIT_SINGLE_SCHEMA), false);
    }

    public boolean isSchemaFiltersEnabled() {
        return CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_SCHEMA_FILTER_ENABLED), true);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        super.refreshObject(monitor);

        this.structureContainer = null;
        this.tableTypeCache.clearCache();
        this.catalogs = null;
        this.schemas = null;

        this.initialize(monitor);

        return this;
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (metaModel instanceof DBCQueryTransformProvider) {
            DBCQueryTransformer transformer = ((DBCQueryTransformProvider) metaModel).createQueryTransformer(type);
            if (transformer != null) {
                return transformer;
            }
        }
        return super.createQueryTransformer(type);
    }

    CubridTableBase findTable(@NotNull DBRProgressMonitor monitor, String catalogName, String schemaName, String tableName)
        throws DBException {
        CubridObjectContainer container = null;
        if (!CommonUtils.isEmpty(catalogName) && !CommonUtils.isEmpty(catalogs)) {
            container = getCatalog(catalogName);
            if (container == null) {
                log.error("Catalog " + catalogName + " not found");
                return null;
            }
        }
        if (!CommonUtils.isEmpty(schemaName)) {
            if (container != null) {
                container = ((CubridCatalog) container).getSchema(monitor, schemaName);
            } else if (schemas != null && !schemas.isEmpty()) {
                container = this.getSchema(schemaName);
            } else {
                container = structureContainer;
            }
            if (container == null) {
                log.debug("Schema '" + schemaName + "' not found");
                return null;
            }
        }
        if (container == null) {
            container = structureContainer;
        }
        return container.getTable(monitor, tableName);
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        if (!CommonUtils.isEmpty(getCatalogs())) {
            return getCatalogs();
        } else if (!CommonUtils.isEmpty(getSchemas())) {
            return getSchemas();
        } else if (structureContainer != null) {
            return structureContainer.getTables(monitor);
        } else {
            return null;
        }
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        if (!CommonUtils.isEmpty(getCatalogs())) {
            return getCatalog(childName);
        } else if (!CommonUtils.isEmpty(getSchemas())) {
            return getSchema(childName);
        } else if (structureContainer != null) {
            return structureContainer.getChild(monitor, childName);
        } else {
            return null;
        }
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        if (!CommonUtils.isEmpty(catalogs)) {
            return CubridCatalog.class;
        } else if (schemas != null && !schemas.isEmpty()) {
            return CubridSchema.class;
        } else {
            return CubridTable.class;
        }
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        if (!CommonUtils.isEmpty(catalogs)) {
            for (CubridCatalog catalog : catalogs) catalog.cacheStructure(monitor, scope);
        } else if (schemas != null && !schemas.isEmpty()) {
            for (CubridSchema schema : schemas.getCachedObjects()) schema.cacheStructure(monitor, scope);
        } else if (structureContainer != null) {
            structureContainer.cacheStructure(monitor, scope);
        }
    }

    private boolean isChild(DBSObject object) throws DBException {
        if (object instanceof CubridCatalog) {
            return !CommonUtils.isEmpty(catalogs) && catalogs.contains(object);
        } else if (object instanceof CubridSchema) {
            return schemas != null && !schemas.isEmpty() && schemas.getCachedObjects().contains(object);
        }
        return false;
    }

    boolean hasCatalogs() {
        return !CommonUtils.isEmpty(catalogs);
    }

    boolean hasSchemas() {
        return schemas != null && !schemas.isEmpty();
    }

    String getQueryGetActiveDB() {
        return queryGetActiveDB;
    }

    String getQuerySetActiveDB() {
        return querySetActiveDB;
    }

    String getSelectedEntityType() {
        return selectedEntityType;
    }

    void setSelectedEntityType(String selectedEntityType) {
        this.selectedEntityType = selectedEntityType;
    }

    boolean isSelectedEntityFromAPI() {
        return selectedEntityFromAPI;
    }

    void setSelectedEntityFromAPI(boolean selectedEntityFromAPI) {
        this.selectedEntityFromAPI = selectedEntityFromAPI;
    }

    public boolean isMergeEntities() {
        return getContainer().getNavigatorSettings().isMergeEntities();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBSStructureAssistant.class) {
            return adapter.cast(new CubridStructureAssistant(this));
        } else if (adapter == DBCQueryPlanner.class) {
            if (queryPlanner == null) {
                queryPlanner = metaModel.getQueryPlanner(this);
            }
            return adapter.cast(queryPlanner);
        } else if (adapter == DBDValueHandlerProvider.class) {
            if (metaModel instanceof DBDValueHandlerProvider) {
                return adapter.cast(metaModel);
            }
        }
        return super.getAdapter(adapter);
    }

    @Override
    public String getObjectTypeTerm(String path, String objectType, boolean multiple) {
        String term = null;
        if (CubridConstants.TERM_CATALOG.equals(objectType)) {
            term = getInfo().getCatalogTerm();
        } else if (CubridConstants.TERM_SCHEMA.equals(objectType)) {
            term = getInfo().getSchemaTerm();
        } else if (CubridConstants.TERM_PROCEDURE.equals(objectType)) {
            term = getInfo().getProcedureTerm();
        }
        if (term != null && multiple) {
            term += "s";
        }
        return term;
    }

    @Nullable
    @Override
    public ErrorPosition[] getErrorPosition(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, @NotNull String query, @NotNull Throwable error) {
        ErrorPosition position = metaModel.getErrorPosition(error);
        return position == null ? null : new ErrorPosition[]{position};
    }

    protected JDBCBasicDataTypeCache<CubridStructContainer, ? extends JDBCDataType> getDataTypeCache() {
        return dataTypeCache;
    }

    public Collection<? extends DBSDataType> getDataTypes(DBRProgressMonitor monitor) throws DBException {
        return dataTypeCache.getAllObjects(monitor, this);
    }

    @Override
    public Collection<? extends DBSDataType> getLocalDataTypes() {
        return dataTypeCache.getCachedObjects();
    }

    @Override
    public DBSDataType getLocalDataType(String typeName) {
        return dataTypeCache.getCachedObject(typeName);
    }

    @Override
    public DBSDataType getLocalDataType(int typeID) {
        return dataTypeCache.getCachedObject(typeID);
    }

    @NotNull
    public DBPDataKind resolveDataKind(@NotNull String typeName, int valueType) {
        DBSDataType dataType = getLocalDataType(typeName);
        if (dataType != null) {
            return super.resolveDataKind(dataType.getTypeName(), dataType.getTypeID());
        }
        return super.resolveDataKind(typeName, valueType);
    }

    public boolean splitProceduresAndFunctions() {
        return CommonUtils.getBoolean(
            getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_SPLIT_PROCEDURES_AND_FUNCTIONS),
            false);
    }

    // Native formats

    public Format getNativeFormatTimestamp() {
        return nativeFormatTimestamp;
    }

    public Format getNativeFormatTime() {
        return nativeFormatTime;
    }

    public Format getNativeFormatDate() {
        return nativeFormatDate;
    }

    private Format makeNativeFormat(String paramName) {
        Object param = getContainer().getDriver().getDriverParameter(paramName);
        if (param == null) {
            return null;
        }
        try {
            return new ExtendedDateFormat(CommonUtils.toString(param));
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    CubridCatalog getDefaultCatalog() {
        return null;
    }

    CubridSchema getDefaultSchema() {
        if (schemas != null) {
            for (CubridSchema schema : schemas.getCachedObjects()) {
                if (schema.isVirtual()) {
                    return schema;
                }
            }
        }
        return null;
    }

    public boolean supportsCatalogChangeInTransaction() {
        return true;
    }

    private class TableTypeCache extends JDBCObjectCache<CubridDataSource, CubridTableType> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull CubridDataSource owner) throws SQLException {
            return session.getMetaData().getTableTypes().getSourceStatement();
        }

        @Override
        protected CubridTableType fetchObject(@NotNull JDBCSession session, @NotNull CubridDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new CubridTableType(
                CubridDataSource.this,
                CubridUtils.safeGetString(
                    getMetaObject(CubridConstants.OBJECT_TABLE_TYPE),
                    resultSet,
                    JDBCConstants.TABLE_TYPE));
        }
    }

    private class DataSourceObjectContainer extends CubridObjectContainer {
        private DataSourceObjectContainer() {
            super(CubridDataSource.this);
        }

        @Override
        public CubridCatalog getCatalog() {
            return null;
        }

        @Override
        public CubridSchema getSchema() {
            return null;
        }

        @Override
        public CubridStructContainer getObject() {
            return CubridDataSource.this;
        }

        @NotNull
        @Override
        public Class<? extends DBSEntity> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
            return CubridTable.class;
        }

        @NotNull
        @Override
        public String getName() {
            return CubridDataSource.this.getName();
        }

        @Nullable
        @Override
        public String getDescription() {
            return CubridDataSource.this.getDescription();
        }

        @Override
        public DBSObject getParentObject() {
            return CubridDataSource.this.getParentObject();
        }
    }

}
