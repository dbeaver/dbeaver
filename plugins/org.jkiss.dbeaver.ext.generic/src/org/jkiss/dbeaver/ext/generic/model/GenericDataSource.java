/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.generic.model;

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.*;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.time.ExtendedDateFormat;

import java.sql.*;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * GenericDataSource
 */
public class GenericDataSource extends JDBCDataSource implements DBPTermProvider, IAdaptable, GenericStructContainer {
    private static final Log log = Log.getLog(GenericDataSource.class);

    private final TableTypeCache tableTypeCache;
    private final JDBCBasicDataTypeCache<GenericStructContainer, ? extends JDBCDataType> dataTypeCache;
    private List<GenericCatalog> catalogs;
    private List<GenericSchema> schemas;
    private final GenericMetaModel metaModel;
    private GenericObjectContainer structureContainer;

    private String queryGetActiveDB;
    private String querySetActiveDB;
    private String selectedEntityType;
    private boolean selectedEntityFromAPI;
    private boolean omitSingleCatalog;
    private String allObjectsPattern;
    private boolean supportsStructCache;
    private DBCQueryPlanner queryPlanner;
    private Format nativeFormatTimestamp, nativeFormatTime, nativeFormatDate;

    public GenericDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container, @NotNull GenericMetaModel metaModel, @NotNull SQLDialect dialect)
        throws DBException {
        super(monitor, container, dialect, false);
        this.metaModel = metaModel;
        final DBPDriver driver = container.getDriver();
        this.dataTypeCache = metaModel.createDataTypeCache(this);
        this.tableTypeCache = new TableTypeCache();
        this.queryGetActiveDB = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_QUERY_GET_ACTIVE_DB));
        this.querySetActiveDB = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_QUERY_SET_ACTIVE_DB));
        this.selectedEntityType = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_ACTIVE_ENTITY_TYPE));
        this.omitSingleCatalog = CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_OMIT_SINGLE_CATALOG), false);
        if (CommonUtils.isEmpty(this.selectedEntityType)) {
            this.selectedEntityType = null;
        }
        this.allObjectsPattern = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_ALL_OBJECTS_PATTERN));
        if (CommonUtils.isEmpty(this.allObjectsPattern)) {
            this.allObjectsPattern = "%";
        } else if ("null".equalsIgnoreCase(this.allObjectsPattern)) {
            this.allObjectsPattern = null;
        }

        // Init native formats
        nativeFormatTimestamp = makeNativeFormat(GenericConstants.PARAM_NATIVE_FORMAT_TIMESTAMP);
        nativeFormatTime = makeNativeFormat(GenericConstants.PARAM_NATIVE_FORMAT_TIME);
        nativeFormatDate = makeNativeFormat(GenericConstants.PARAM_NATIVE_FORMAT_DATE);

        initializeRemoteInstance(monitor);
    }

    @Override
    protected String getConnectionURL(DBPConnectionConfiguration connectionInfo) {
        // Recreate URL from parameters
        // Driver settings and URL template may have change since connection creation
        String connectionURL = getContainer().getDriver().getDataSourceProvider().getConnectionURL(getContainer().getDriver(), connectionInfo);
        if (connectionInfo.getUrl() != null && !CommonUtils.equalObjects(connectionURL, connectionInfo.getUrl())) {
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
        return getClass() != GenericDataSource.class;
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new GenericExecutionContext(instance, type);
    }

    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context, JDBCExecutionContext initFrom) throws DBException {
        super.initializeContextState(monitor, context, initFrom);
        if (initFrom != null) {
            GenericExecutionContext metaContext = (GenericExecutionContext) initFrom;
            ((GenericExecutionContext) context).initDefaultsFrom(monitor, metaContext);
        } else {
            ((GenericExecutionContext)context).refreshDefaults(monitor, true);
        }
    }

    public String getAllObjectsPattern() {
        return allObjectsPattern;
    }

    @NotNull
    public GenericMetaModel getMetaModel() {
        return metaModel;
    }

    @Nullable
    public GenericMetaObject getMetaObject(String id) {
        return metaModel.getMetaObject(id);
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData) {
        final GenericDataSourceInfo info = new GenericDataSourceInfo(getContainer().getDriver(), metaData);
        final JDBCSQLDialect dialect = (JDBCSQLDialect) getSQLDialect();

        final Object supportsReferences = getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SUPPORTS_REFERENCES);
        if (supportsReferences != null) {
            info.setSupportsReferences(Boolean.valueOf(supportsReferences.toString()));
        }

        final Object supportsIndexes = getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SUPPORTS_INDEXES);
        if (supportsIndexes != null) {
            info.setSupportsIndexes(Boolean.valueOf(supportsIndexes.toString()));
        }

        final Object supportsStoredCode = getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SUPPORTS_STORED_CODE);
        if (supportsStoredCode != null) {
            info.setSupportsStoredCode(Boolean.valueOf(supportsStoredCode.toString()));
        }

        final Object supportsSubqueries = getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SUPPORTS_SUBQUERIES);
        if (supportsSubqueries != null) {
            dialect.setSupportsSubqueries(Boolean.valueOf(supportsSubqueries.toString()));
        }

        final Object supportsStructCacheParam = getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SUPPORTS_STRUCT_CACHE);
        if (supportsStructCacheParam != null) {
            this.supportsStructCache = CommonUtils.toBoolean(supportsStructCacheParam);
        }
        return info;
    }

    @Override
    public void shutdown(DBRProgressMonitor monitor) {
        String queryShutdown = CommonUtils.toString(getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_QUERY_SHUTDOWN));
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
        String paramShutdown = CommonUtils.toString(getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SHUTDOWN_URL_PARAM));
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
    public Collection<GenericTableType> getTableTypes(DBRProgressMonitor monitor)
        throws DBException {
        return tableTypeCache.getAllObjects(monitor, this);
    }

    public List<GenericCatalog> getCatalogs() {
        return catalogs;
    }

    public GenericCatalog getCatalog(String name) {
        return DBUtils.findObject(getCatalogs(), name);
    }

    @Association
    public List<GenericSchema> getSchemas() {
        return schemas;
    }

    public GenericSchema getSchema(String name) {
        return DBUtils.findObject(getSchemas(), name);
    }

    @NotNull
    @Override
    public GenericDataSource getDataSource() {
        return this;
    }

    @Override
    public GenericStructContainer getObject() {
        return this;
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
    public Collection<GenericTableBase> getViews(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getViews(monitor);
    }

    @Override
    public Collection<GenericTable> getPhysicalTables(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getPhysicalTables(monitor);
    }

    @Override
    public Collection<GenericTableBase> getTables(DBRProgressMonitor monitor)
        throws DBException {
        return structureContainer == null ? null : structureContainer.getTables(monitor);
    }

    @Override
    public GenericTableBase getTable(DBRProgressMonitor monitor, String name)
        throws DBException {
        return structureContainer == null ? null : structureContainer.getTable(monitor, name);
    }

    @Override
    public Collection<GenericPackage> getPackages(DBRProgressMonitor monitor)
        throws DBException {
        return structureContainer == null ? null : structureContainer.getPackages(monitor);
    }

    @Override
    public Collection<GenericTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException {
        return structureContainer == null ? null : structureContainer.getIndexes(monitor);
    }

    @Override
    public Collection<GenericProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException {
        return structureContainer == null ? null : structureContainer.getProcedures(monitor);
    }

    @Override
    public Collection<? extends GenericProcedure> getProceduresOnly(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getProceduresOnly(monitor);
    }

    @Override
    public GenericProcedure getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
        return structureContainer == null ? null : structureContainer.getProcedure(monitor, uniqueName);
    }

    @Override
    public Collection<GenericProcedure> getProcedures(DBRProgressMonitor monitor, String name)
        throws DBException {
        return structureContainer == null ? null : structureContainer.getProcedures(monitor, name);
    }

    @Override
    public Collection<? extends GenericProcedure> getFunctionsOnly(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getFunctionsOnly(monitor);
    }

    @Override
    public Collection<? extends GenericSequence> getSequences(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getSequences(monitor);
    }

    @Override
    public Collection<? extends GenericSynonym> getSynonyms(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getSynonyms(monitor);
    }

    @Override
    public Collection<? extends GenericTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getTriggers(monitor);
    }

    @Override
    public Collection<? extends GenericTrigger> getTableTriggers(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getTableTriggers(monitor);
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);
        boolean omitCatalog = isOmitCatalog();
        boolean omitTypeCache = CommonUtils.toBoolean(getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_OMIT_TYPE_CACHE));
        if (!omitTypeCache) {
            // Cache data types
            try {
                dataTypeCache.getAllObjects(monitor, this);
            } catch (Exception e) {
                log.warn("Can't fetch database data types", e);
            }
            if (CommonUtils.isEmpty(dataTypeCache.getCachedObjects())) {
                // Use basic data types
                dataTypeCache.fillStandardTypes(this);
            }
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read generic metadata")) {
            // Read metadata
            JDBCDatabaseMetaData metaData = session.getMetaData();
            boolean catalogsFiltered = false;
            if (!omitCatalog) {
                // Read catalogs
                monitor.subTask("Extract catalogs");
                monitor.worked(1);
                final GenericMetaObject catalogObject = getMetaObject(GenericConstants.OBJECT_CATALOG);
                final DBSObjectFilter catalogFilters = getContainer().getObjectFilter(GenericCatalog.class, null, false);
                final List<String> catalogNames = new ArrayList<>();
                try {
                    try (JDBCResultSet dbResult = metaData.getCatalogs()) {
                        int totalCatalogs = 0;
                        while (dbResult.next()) {
                            String catalogName = GenericUtils.safeGetString(catalogObject, dbResult, JDBCConstants.TABLE_CAT);
                            if (CommonUtils.isEmpty(catalogName)) {
                                // Some drivers uses TABLE_QUALIFIER instead of catalog
                                catalogName = GenericUtils.safeGetStringTrimmed(catalogObject, dbResult, JDBCConstants.TABLE_QUALIFIER);
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
                    // Error reading catalogs - just warn about it
                    log.warn("Can't read catalog list", e);
                }
                if (!catalogNames.isEmpty() || catalogsFiltered) {
                    this.catalogs = new ArrayList<>();
                    for (String catalogName : catalogNames) {
                        GenericCatalog catalog = metaModel.createCatalogImpl(this, catalogName);
                        this.catalogs.add(catalog);
                    }
                }
            }

            if (CommonUtils.isEmpty(catalogs) && !catalogsFiltered) {
                // Catalogs not supported - try to read root schemas
                monitor.subTask("Extract schemas");
                monitor.worked(1);

                try {
                    List<GenericSchema> tmpSchemas = metaModel.loadSchemas(session, this, null);
                    if (tmpSchemas != null) {
                        this.schemas = tmpSchemas;
                    }
                } catch (Throwable e) {
                    log.warn("Can't read schema list", e);
                }

                if (CommonUtils.isEmpty(schemas)) {
                    this.structureContainer = new DataSourceObjectContainer();
                }
            }
        } catch (Throwable ex) {
            throw new DBException("Error reading metadata", ex, this);
        }
    }

    public boolean isOmitCatalog() {
        return CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_OMIT_CATALOG), false);
    }

    public boolean isOmitSchema() {
        return CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_OMIT_SCHEMA), false);
    }

    public boolean isOmitSingleSchema() {
        return CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_OMIT_SINGLE_SCHEMA), false);
    }

    public boolean isSchemaFiltersEnabled() {
        return CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SCHEMA_FILTER_ENABLED), true);
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

    GenericTableBase findTable(@NotNull DBRProgressMonitor monitor, String catalogName, String schemaName, String tableName)
        throws DBException {
        GenericObjectContainer container = null;
        if (!CommonUtils.isEmpty(catalogName) && !CommonUtils.isEmpty(catalogs)) {
            container = getCatalog(catalogName);
            if (container == null) {
                log.error("Catalog " + catalogName + " not found");
                return null;
            }
        }
        if (!CommonUtils.isEmpty(schemaName)) {
            if (container != null) {
                container = ((GenericCatalog) container).getSchema(monitor, schemaName);
            } else if (!CommonUtils.isEmpty(schemas)) {
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

    @Override
    public Class<? extends DBSObject> getChildType(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (!CommonUtils.isEmpty(catalogs)) {
            return GenericCatalog.class;
        } else if (!CommonUtils.isEmpty(schemas)) {
            return GenericSchema.class;
        } else {
            return GenericTable.class;
        }
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        if (!CommonUtils.isEmpty(catalogs)) {
            for (GenericCatalog catalog : catalogs) catalog.cacheStructure(monitor, scope);
        } else if (!CommonUtils.isEmpty(schemas)) {
            for (GenericSchema schema : schemas) schema.cacheStructure(monitor, scope);
        } else if (structureContainer != null) {
            structureContainer.cacheStructure(monitor, scope);
        }
    }

    private boolean isChild(DBSObject object) throws DBException {
        if (object instanceof GenericCatalog) {
            return !CommonUtils.isEmpty(catalogs) && catalogs.contains(GenericCatalog.class.cast(object));
        } else if (object instanceof GenericSchema) {
            return !CommonUtils.isEmpty(schemas) && schemas.contains(GenericSchema.class.cast(object));
        }
        return false;
    }

    boolean hasCatalogs() {
        return !CommonUtils.isEmpty(catalogs);
    }

    boolean hasSchemas() {
        return !CommonUtils.isEmpty(schemas);
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

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBSStructureAssistant.class) {
            return adapter.cast(new GenericStructureAssistant(this));
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
        if (GenericConstants.TERM_CATALOG.equals(objectType)) {
            term = getInfo().getCatalogTerm();
        } else if (GenericConstants.TERM_SCHEMA.equals(objectType)) {
            term = getInfo().getSchemaTerm();
        } else if (GenericConstants.TERM_PROCEDURE.equals(objectType)) {
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

    protected JDBCBasicDataTypeCache<GenericStructContainer, ? extends JDBCDataType> getDataTypeCache() {
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
            getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SPLIT_PROCEDURES_AND_FUNCTIONS),
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

    GenericCatalog getDefaultCatalog() {
        return null;
    }

    GenericSchema getDefaultSchema() {
        if (schemas != null) {
            for (GenericSchema schema : schemas) {
                if (schema.isVirtual()) {
                    return schema;
                }
            }
        }
        return null;
    }

    private class TableTypeCache extends JDBCObjectCache<GenericDataSource, GenericTableType> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull GenericDataSource owner) throws SQLException {
            return session.getMetaData().getTableTypes().getSourceStatement();
        }

        @Override
        protected GenericTableType fetchObject(@NotNull JDBCSession session, @NotNull GenericDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new GenericTableType(
                GenericDataSource.this,
                GenericUtils.safeGetString(
                    getMetaObject(GenericConstants.OBJECT_TABLE_TYPE),
                    resultSet,
                    JDBCConstants.TABLE_TYPE));
        }
    }

    private class DataSourceObjectContainer extends GenericObjectContainer {
        private DataSourceObjectContainer() {
            super(GenericDataSource.this);
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
        public GenericStructContainer getObject() {
            return GenericDataSource.this;
        }

        @Override
        public Class<? extends DBSEntity> getChildType(@NotNull DBRProgressMonitor monitor) throws DBException {
            return GenericTable.class;
        }

        @NotNull
        @Override
        public String getName() {
            return GenericDataSource.this.getName();
        }

        @Nullable
        @Override
        public String getDescription() {
            return GenericDataSource.this.getDescription();
        }

        @Override
        public DBSObject getParentObject() {
            return GenericDataSource.this.getParentObject();
        }
    }

}
