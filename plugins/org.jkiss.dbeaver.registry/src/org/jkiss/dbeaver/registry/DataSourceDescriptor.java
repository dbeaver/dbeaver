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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.access.DBAAuthCredentials;
import org.jkiss.dbeaver.model.access.DBAAuthModelExternal;
import org.jkiss.dbeaver.model.access.DBACredentialsProvider;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.*;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.dpi.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.SimpleExclusiveLock;
import org.jkiss.dbeaver.model.impl.data.DefaultValueHandler;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.model.net.*;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.rm.RMProjectType;
import org.jkiss.dbeaver.model.runtime.*;
import org.jkiss.dbeaver.model.secret.*;
import org.jkiss.dbeaver.model.security.SMObjectType;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadata;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.virtual.DBVModel;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.formatter.DataFormatterProfile;
import org.jkiss.dbeaver.registry.internal.RegistryMessages;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.IVariableResolver;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.StringReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.*;

/**
 * DataSourceDescriptor
 */
public class DataSourceDescriptor
    implements
    DBPDataSourceContainer,
    DBPImageProvider,
    IAdaptable,
    DBPStatefulObject,
    DBPRefreshableObject,
    DBSSecretObject {

    private static final Log log = Log.getLog(DataSourceDescriptor.class);

    public static final String CATEGORY_CONNECTIONS = "Connections";
    public static final String CATEGORY_SERVER = "Server";
    public static final String CATEGORY_DRIVER = "Driver";
    public static final String CATEGORY_DRIVER_FILES = "Driver Files";

    // Secret key prefix
    public static final String DATASOURCE_KEY_PREFIX = "/datasources/";

    @NotNull
    private final DBPDataSourceRegistry registry;
    @NotNull
    private final DBPDataSourceConfigurationStorage storage;
    @NotNull
    private DBPDataSourceOrigin origin;

    @NotNull
    private DBPDriver driver;
    @NotNull
    private final DBPDriver originalDriver;
    @Nullable
    private DBPDriverSubstitutionDescriptor driverSubstitution;
    @NotNull
    private DBPConnectionConfiguration connectionInfo;
    // Copy of connection info with resolved params (cache)
    private DBPConnectionConfiguration resolvedConnectionInfo;

    @NotNull
    private String id;
    private String name;
    private String description;
    // Password is saved in configuration
    private boolean savePassword;
    // Password is shared.
    // It will be saved in local configuration even if project uses secured storage
    private boolean sharedCredentials;
    // store original flag state to detect changes
    private transient boolean originalShareCredentials;
    @Nullable
    private transient List<DBSSecretValue> availableSharedCredentials;
    @Nullable
    private transient DBSSecretValue selectedSharedCredentials;

    private boolean connectionReadOnly;
    private boolean forceUseSingleConnection;
    private List<DBPDataSourcePermission> connectionModifyRestrictions;
    private final Map<String, FilterMapping> filterMap = new HashMap<>();
    private DBDDataFormatterProfile formatterProfile;
    @Nullable
    private DBPNativeClientLocation clientHome;
    @Nullable
    private String lockPasswordHash;
    @Nullable
    private DataSourceFolder folder;

    @NotNull
    private final DataSourcePreferenceStore preferenceStore;
    @NotNull
    private final Map<String, String> tags;
    @NotNull
    private final Map<String, String> extensions;
    @Nullable
    private DBPDataSource dataSource;
    @Nullable
    private String lastConnectionError;

    private boolean temporary;
    private boolean hidden;
    private boolean template;
    private boolean dpiEnabled;

    @NotNull
    private DataSourceNavigatorSettings navigatorSettings;
    @NotNull
    private DBVModel virtualModel;
    private final boolean manageable;

    private boolean accessCheckRequired = true;

    private volatile boolean connectFailed = false;
    private volatile Date connectTime = null;
    private volatile boolean disposed = false;
    private volatile boolean connecting = false;

    // secrets resolved from secret controller
    private volatile boolean secretsResolved = false;
    // secrets resolved from secret controller and contains db creds (we may not have db creds in the case when we store only ssh)
    private volatile boolean secretsContainsDatabaseCreds = false;

    private final List<DBRProcessDescriptor> childProcesses = new ArrayList<>();
    private transient DBWNetworkHandler proxyHandler;
    private transient DBWTunnel tunnelHandler;
    private final List<DBPDataSourceTask> users = new ArrayList<>();
    private transient String clientApplicationName;
    // DPI controller
    private transient DPIProcessController dpiController;

    private transient final DBPExclusiveResource exclusiveLock = new SimpleExclusiveLock();

    public DataSourceDescriptor(
        @NotNull DBPDataSourceRegistry registry,
        @NotNull String id,
        @NotNull DBPDriver driver,
        @NotNull DBPConnectionConfiguration connectionInfo) {
        this(registry, ((DataSourceRegistry) registry).getDefaultStorage(), DataSourceOriginLocal.INSTANCE, id, driver, connectionInfo);
    }

    public DataSourceDescriptor(
        @NotNull DBPDataSourceRegistry registry,
        @NotNull DBPDataSourceConfigurationStorage storage,
        @NotNull DBPDataSourceOrigin origin,
        @NotNull String id,
        @NotNull DBPDriver driver,
        @NotNull DBPConnectionConfiguration connectionInfo
    ) {
        this(registry, storage, origin, id, driver, driver, connectionInfo);
    }

    public DataSourceDescriptor(
        @NotNull DBPDataSourceRegistry registry,
        @NotNull DBPDataSourceConfigurationStorage storage,
        @NotNull DBPDataSourceOrigin origin,
        @NotNull String id,
        @NotNull DBPDriver originalDriver,
        @NotNull DBPDriver substitutedDriver,
        @NotNull DBPConnectionConfiguration connectionInfo
    ) {
        this.registry = registry;
        this.storage = storage;
        this.origin = origin;
        this.manageable = storage.isDefault();
        this.id = id;
        this.originalDriver = originalDriver;
        this.driver = substitutedDriver;
        this.connectionInfo = connectionInfo;
        this.tags = new LinkedHashMap<>();
        this.extensions = new LinkedHashMap<>();
        this.preferenceStore = new DataSourcePreferenceStore(registry.getPreferenceStore(), this);
        this.virtualModel = new DBVModel(this);
        this.navigatorSettings = new DataSourceNavigatorSettings(DataSourceNavigatorSettings.getDefaultSettings());
        this.forceUseSingleConnection = driver.isSingleConnection();
    }

    // Copy constructor
    public DataSourceDescriptor(@NotNull DataSourceDescriptor source, @NotNull DBPDataSourceRegistry registry) {
        this(source, registry, true);
    }

    /**
     * Copies datasource configuration
     *
     * @param setDefaultStorage sets storage to default (in order to allow connection copy-paste with following save in default configuration)
     */
    public DataSourceDescriptor(@NotNull DataSourceDescriptor source, @NotNull DBPDataSourceRegistry registry, boolean setDefaultStorage) {
        this.registry = registry;
        this.storage = setDefaultStorage ? ((DataSourceRegistry) registry).getDefaultStorage() : source.storage;
        this.origin = source.origin;
        this.manageable = setDefaultStorage && ((DataSourceRegistry) registry).getDefaultStorage().isDefault();
        this.accessCheckRequired = manageable;
        this.id = source.id;
        this.name = source.name;
        this.description = source.description;
        this.savePassword = source.savePassword;
        this.sharedCredentials = source.sharedCredentials;
        this.originalShareCredentials = this.sharedCredentials;
        this.navigatorSettings = new DataSourceNavigatorSettings(source.navigatorSettings);
        this.connectionReadOnly = source.connectionReadOnly;
        this.forceUseSingleConnection = source.forceUseSingleConnection;
        this.driver = source.driver;
        this.originalDriver = source.originalDriver;
        this.driverSubstitution = source.driverSubstitution;
        this.clientHome = source.clientHome;

        this.connectionModifyRestrictions = source.connectionModifyRestrictions == null ? null : new ArrayList<>(source.connectionModifyRestrictions);

        this.connectionInfo = new DBPConnectionConfiguration(source.connectionInfo);
        for (Map.Entry<String, FilterMapping> fe : source.filterMap.entrySet()) {
            this.filterMap.put(fe.getKey(), new FilterMapping(fe.getValue()));
        }
        this.lockPasswordHash = source.lockPasswordHash;
        if (source.getRegistry() == registry) {
            this.folder = source.folder;
        } else if (source.folder != null) {
            // Cross-registry copy
            this.folder = (DataSourceFolder) registry.getFolder(source.folder.getFolderPath());
        }

        this.tags = new LinkedHashMap<>(source.tags);
        this.extensions = new LinkedHashMap<>(source.extensions);
        this.preferenceStore = new DataSourcePreferenceStore(this);
        this.preferenceStore.setProperties(source.preferenceStore.getProperties());
        this.preferenceStore.setDefaultProperties(source.preferenceStore.getDefaultProperties());

        if (source.formatterProfile == null || source.formatterProfile.getProfileName().equals(source.getId())) {
            this.formatterProfile = null;
        } else {
            this.formatterProfile = new DataFormatterProfile(source.formatterProfile.getProfileName(), preferenceStore);
        }

        this.virtualModel = new DBVModel(this, source.virtualModel);
    }

    public boolean isDisposed() {
        return disposed;
    }

    public void dispose() {
        if (disposed) {
            log.warn("Dispose of already disposed data source");
            return;
        }
        synchronized (users) {
            users.clear();
        }
        this.virtualModel.dispose();
        disposed = true;
    }

    @NotNull
    @Override
    @Property(name = "ID", viewable = false, order = 0)
    public String getId() {
        return id;
    }

    public void setId(@NotNull String id) {
        this.id = id;
    }

    @NotNull
    @Override
    public DBPDriver getDriver() {
        return driver;
    }

    @Nullable
    @Override
    public DBPDriverSubstitutionDescriptor getDriverSubstitution() {
        return driverSubstitution;
    }

    @Override
    public void setDriverSubstitution(@Nullable DBPDriverSubstitutionDescriptor driverSubstitution) {
        this.driverSubstitution = driverSubstitution;
    }

    @NotNull
    public DBPDriver getOriginalDriver() {
        return originalDriver;
    }

    @NotNull
    @Override
    public DBPDataSourceConfigurationStorage getConfigurationStorage() {
        return storage;
    }

    @Property(viewable = true, order = 3)
    @NotNull
    @Override
    public DBPDataSourceOrigin getOrigin() {
        if (origin instanceof DataSourceOriginLazy) {
            DBPDataSourceOrigin realOrigin;
            try {
                realOrigin = ((DataSourceOriginLazy) this.origin).resolveRealOrigin();
            } catch (DBException e) {
                log.debug("Error reading datasource origin", e);
                realOrigin = null;
            }
            if (realOrigin != null) {
                this.origin = realOrigin;
            } else {
                // Do not replace source origin config.
                // Possibly different product/config and origin is not available for now.
                return DataSourceOriginLocal.INSTANCE;
            }
        }
        return origin;
    }

    @NotNull
    DBPDataSourceOrigin getOriginSource() {
        return origin;
    }

    public void setDriver(@NotNull DriverDescriptor driver) {
        this.driver = driver;
        this.forceUseSingleConnection = driver.isSingleConnection();
    }

    @NotNull
    @Override
    public DBPConnectionConfiguration getConnectionConfiguration() {
        return connectionInfo;
    }

    public void setConnectionInfo(@NotNull DBPConnectionConfiguration connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    @NotNull
    @Override
    public DBPConnectionConfiguration getActualConnectionConfiguration() {
        return this.resolvedConnectionInfo != null ? this.resolvedConnectionInfo : this.connectionInfo;
    }

    @NotNull
    @Override
    public DataSourceNavigatorSettings getNavigatorSettings() {
        return navigatorSettings;
    }

    public void setNavigatorSettings(DBNBrowseSettings copyFrom) {
        this.navigatorSettings = new DataSourceNavigatorSettings(copyFrom);
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 2)
    public String getDescription() {
        return description;
    }

    public boolean isSavePassword() {
        return savePassword;
    }

    @Override
    public void setSavePassword(boolean savePassword) {
        this.savePassword = savePassword;
    }

    public boolean isCredentialsSaved() throws DBException {
        if (!getProject().isUseSecretStorage()) {
            return savePassword;
        }
        resolveSecretsIfNeeded();

        if (isSharedCredentials() && !isSharedCredentialsSelected()) {
            return false;
        }

        if (secretsResolved && secretsContainsDatabaseCreds) {
            return true;
        }
        if (savePassword) {
            // Check actual credentials
            // They may be ready if we are in test connection mode
            DBAAuthCredentials authCreds = getConnectionConfiguration().getAuthModel().loadCredentials(this, getConnectionConfiguration());
            return authCreds.isComplete();
        }
        return false;
    }

    @Override
    public boolean isSharedCredentials() {
        return sharedCredentials;
    }

    //ignore changed store type
    public void forceSetSharedCredentials(boolean sharedCredentials) {
        this.sharedCredentials = sharedCredentials;
        this.originalShareCredentials = sharedCredentials;
    }

    @Override
    public void setSharedCredentials(boolean sharedCredentials) {
        this.sharedCredentials = sharedCredentials;
    }

    @Override
    public boolean isConnectionReadOnly() {
        return connectionReadOnly;
    }

    public void setConnectionReadOnly(boolean connectionReadOnly) {
        this.connectionReadOnly = connectionReadOnly;
    }

    @Override
    public boolean hasModifyPermission(DBPDataSourcePermission permission) {
        if ((permission == DBPDataSourcePermission.PERMISSION_EDIT_DATA ||
            permission == DBPDataSourcePermission.PERMISSION_EDIT_METADATA) && connectionReadOnly) {
            return false;
        }
        if (CommonUtils.isEmpty(connectionModifyRestrictions)) {
            return getConnectionConfiguration().getConnectionType().hasModifyPermission(permission);
        } else {
            return !connectionModifyRestrictions.contains(permission);
        }
    }

    @Override
    public List<DBPDataSourcePermission> getModifyPermission() {
        if (CommonUtils.isEmpty(this.connectionModifyRestrictions)) {
            return Collections.emptyList();
        } else {
            return new ArrayList<>(this.connectionModifyRestrictions);
        }
    }

    @Override
    public void setModifyPermissions(@Nullable Collection<DBPDataSourcePermission> permissions) {
        if (CommonUtils.isEmpty(permissions)) {
            this.connectionModifyRestrictions = null;
        } else {
            this.connectionModifyRestrictions = new ArrayList<>(permissions);
        }
    }

    @Override
    public boolean isDefaultAutoCommit() {
        if (connectionInfo.getBootstrap().getDefaultAutoCommit() != null) {
            return connectionInfo.getBootstrap().getDefaultAutoCommit();
        } else {
            return getConnectionConfiguration().getConnectionType().isAutocommit();
        }
    }

    @Override
    public void setDefaultAutoCommit(final boolean autoCommit) {
        // Save in preferences
        if (autoCommit == getConnectionConfiguration().getConnectionType().isAutocommit()) {
            connectionInfo.getBootstrap().setDefaultAutoCommit(null);
        } else {
            connectionInfo.getBootstrap().setDefaultAutoCommit(autoCommit);
        }
    }

    public void resetAllSecrets() {
        this.secretsResolved = false;
        this.secretsContainsDatabaseCreds = false;
        this.availableSharedCredentials = null;
        this.selectedSharedCredentials = null;
        if (sharedCredentials) {
            // For shared credentials reset cache also
            connectionInfo.setUserName(null);
            connectionInfo.setUserPassword(null);
        }
    }

    @Nullable
    @Override
    public DBPTransactionIsolation getActiveTransactionsIsolation() {
        if (dataSource != null) {
            DBSInstance defaultInstance = dataSource.getDefaultInstance();
            if (defaultInstance != null) {
                DBCTransactionManager txnManager = DBUtils.getTransactionManager(DBUtils.getDefaultContext(defaultInstance, false));
                if (txnManager != null) {
                    try {
                        return txnManager.getTransactionIsolation();
                    } catch (DBCException e) {
                        log.debug("Can't determine isolation level", e);
                        return null;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Integer getDefaultTransactionsIsolation() {
        return connectionInfo.getBootstrap().getDefaultTransactionIsolation();
    }

    @Override
    public void setDefaultTransactionsIsolation(@Nullable final DBPTransactionIsolation isolationLevel) {
        if (isolationLevel == null) {
            connectionInfo.getBootstrap().setDefaultTransactionIsolation(null);
        } else {
            connectionInfo.getBootstrap().setDefaultTransactionIsolation(isolationLevel.getCode());
        }
    }

    @Override
    public boolean isExtraMetadataReadEnabled() {
        return !preferenceStore.getBoolean(ModelPreferences.META_DISABLE_EXTRA_READ);
    }

    public Collection<FilterMapping> getObjectFilters() {
        return filterMap.values();
    }

    @Nullable
    @Override
    public DBSObjectFilter getObjectFilter(Class<?> type, @Nullable DBSObject parentObject, boolean firstMatch) {
        FilterMapping filterMapping = getFilterMapping(type, parentObject, firstMatch);
        if (filterMapping != null) {
            return filterMapping.getFilter(parentObject, firstMatch);
        }
        return null;
    }

    @Nullable
    private FilterMapping getFilterMapping(@NotNull Class<?> type, @Nullable DBSObject parentObject, boolean firstMatch) {
        if (filterMap.isEmpty()) {
            return null;
        }
        // Test all interfaces
        for (Class<?> testType : type.getInterfaces()) {
            FilterMapping filterMapping = getTypeFilterMapping(parentObject, firstMatch, testType);
            if (filterMapping != null) {
                return filterMapping;
            }
        }
        // Test all super classes
        for (Class<?> testType = type; testType != null; testType = testType.getSuperclass()) {
            FilterMapping filterMapping = getTypeFilterMapping(parentObject, firstMatch, testType);
            if (filterMapping != null) {
                return filterMapping;
            }
        }

        return null;
    }

    private FilterMapping getTypeFilterMapping(@Nullable DBSObject parentObject, boolean firstMatch, Class<?> testType) {
        FilterMapping filterMapping = filterMap.get(testType.getName());
        DBSObjectFilter filter;
        if (filterMapping == null) {
            // Try to find using interfaces and superclasses
            for (Class<?> it : testType.getInterfaces()) {
                filterMapping = filterMap.get(it.getName());
                if (filterMapping != null) {
                    filter = filterMapping.getFilter(parentObject, firstMatch);
                    if (filter != null && (firstMatch || filter.isEnabled())) return filterMapping;
                }
            }
        }
        if (filterMapping != null) {
            filter = filterMapping.getFilter(parentObject, firstMatch);
            if (filter != null && (firstMatch || filter.isEnabled())) {
                return filterMapping;
            }
        }
        return null;
    }

    @Override
    public void setObjectFilter(Class<?> type, DBSObject parentObject, DBSObjectFilter filter) {
        FilterMapping filterMapping = getFilterMapping(type, parentObject, true);
        if (filterMapping != null) {
            // Update filter
            if (parentObject == null) {
                filterMapping.defaultFilter = filter;
            } else {
                filterMapping.customFilters.put(FilterMapping.getFilterContainerUniqueID(parentObject), filter);
            }
        }

        updateObjectFilter(type.getName(), parentObject == null ? null : FilterMapping.getFilterContainerUniqueID(parentObject), filter);
    }

    @Nullable
    @Override
    public String getClientApplicationName() {
        return this.clientApplicationName;
    }

    @Override
    public void setClientApplicationName(@NotNull String applicationName) {
        this.clientApplicationName = applicationName;
    }

    void clearFilters() {
        filterMap.clear();
    }

    void updateObjectFilter(String typeName, @Nullable String objectID, DBSObjectFilter filter) {
        FilterMapping filterMapping = filterMap.get(typeName);
        if (filterMapping == null) {
            filterMapping = new FilterMapping(typeName);
            filterMap.put(typeName, filterMapping);
        }
        if (objectID == null) {
            filterMapping.defaultFilter = filter;
        } else {
            filterMapping.customFilters.put(objectID, filter);
        }
    }

    @Override
    @NotNull
    public DBVModel getVirtualModel() {
        return virtualModel;
    }

    public boolean hasSharedVirtualModel() {
        return !CommonUtils.equalObjects(virtualModel.getId(), getId());
    }

    public void setVirtualModel(@NotNull DBVModel virtualModel) {
        this.virtualModel.dispose();

        if (virtualModel.getId().equals(getId())) {
            // DS-specific model
            this.virtualModel = virtualModel;
            this.virtualModel.setDataSourceContainer(this);
        } else {
            // Shared model
            this.virtualModel = new DBVModel(this, virtualModel);
            this.virtualModel.setId(virtualModel.getId());
        }
    }

    @Override
    public DBPNativeClientLocation getClientHome() {
        if (clientHome == null && !CommonUtils.isEmpty(connectionInfo.getClientHomeId())) {
            this.clientHome = DBUtils.findObject(driver.getNativeClientLocations(), connectionInfo.getClientHomeId());
        }
        return clientHome;
    }

    @NotNull
    @Override
    public DBWNetworkHandler[] getActiveNetworkHandlers() {
        if (proxyHandler == null && tunnelHandler == null) {
            return new DBWNetworkHandler[0];
        }
        return proxyHandler == null ?
            new DBWNetworkHandler[]{tunnelHandler} :
            tunnelHandler == null ?
                new DBWNetworkHandler[]{proxyHandler} :
                new DBWNetworkHandler[]{proxyHandler, tunnelHandler};
    }

    @NotNull
    DBPDataSourceConfigurationStorage getStorage() {
        return storage;
    }

    public boolean isDetached() {
        return hidden || temporary;
    }

    public boolean isManageable() {
        return manageable;
    }

    @Override
    public boolean isAccessCheckRequired() {
        return isManageable() && accessCheckRequired;
    }

    public void setAccessCheckRequired(boolean accessCheckRequired) {
        this.accessCheckRequired = accessCheckRequired;
    }

    @Override
    public boolean isProvided() {
        return !storage.isDefault();
    }

    @Override
    public boolean isExternallyProvided() {
        return getOrigin().isDynamic();
    }

    @Override
    public boolean isTemplate() {
        return template;
    }

    public void setTemplate(boolean template) {
        this.template = template;
    }

    @Override
    public boolean isTemporary() {
        return temporary;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    @Override
    public DBSObject getParentObject() {
        return null;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        if (dataSource instanceof DBPRefreshableObject) {
            dataSource = (DBPDataSource) ((DBPRefreshableObject) dataSource).refreshObject(monitor);
        } else {
            this.reconnect(monitor, false);
        }

        getRegistry().notifyDataSourceListeners(new DBPEvent(
            DBPEvent.Action.OBJECT_UPDATE,
            DataSourceDescriptor.this));

        return this;
    }

    @Override
    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public Date getConnectTime() {
        return connectTime;
    }

    @NotNull
    @Override
    public SQLDialectMetadata getScriptDialect() {
        return driver.getScriptDialect();
    }

    public boolean isLocked() {
        return !CommonUtils.isEmpty(lockPasswordHash);
    }

    @Nullable
    public String getLockPasswordHash() {
        return lockPasswordHash;
    }

    void setLockPasswordHash(@Nullable String lockPasswordHash) {
        this.lockPasswordHash = lockPasswordHash;
    }

    @Nullable
    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @Nullable
    @Override
    public DataSourceFolder getFolder() {
        return folder;
    }

    @Override
    public void setFolder(@Nullable DBPDataSourceFolder folder) {
        this.folder = (DataSourceFolder) folder;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public DBPDataSourceRegistry getRegistry() {
        return registry;
    }

    @NotNull
    @Override
    public DBPProject getProject() {
        return registry.getProject();
    }

    @Override
    public boolean persistConfiguration() {
        try {
            registry.updateDataSource(this);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Datasource update error", "Error updating datasource", e);
            return false;
        }

        Throwable lastError = registry.getLastError();
        if (lastError != null) {
            DBWorkbench.getPlatformUI().showError("Save error", "Error saving datasource configuration", lastError);
            return false;
        }

        return true;
    }

    boolean persistSecretIfNeeded(boolean force, boolean isNewDataSource) throws DBException {
        if (isDetached()) {
            // Do not save secrets for hidden or temporary datasources
            return false;
        }

        if (shouldRemoveExistsSecrets()) {
            DBSSecretController secretController = DBSSecretController.getProjectSecretController(getProject());
            secretController.deleteObjectSecrets(this);
            this.originalShareCredentials = isSharedCredentials();
        }
        // Save only if secrets were already resolved or it is a new connection
        if (secretsResolved || (force && getProject().isUseSecretStorage())) {
            DBSSecretController secretController = DBSSecretController.getProjectSecretController(getProject());
            persistSecrets(secretController, isNewDataSource);
        }
        return true;
    }

    void removeSecretIfNeeded() throws DBException {
        // Delete secrets (on connection delete)
        if (getProject().isUseSecretStorage()) {
            DBSSecretController secretController = DBSSecretController.getProjectSecretController(getProject());
            secretController.deleteObjectSecrets(this);
        }
    }

    @Override
    public void persistSecrets(DBSSecretController secretController) throws DBException {
        persistSecrets(secretController, false);
    }

    //TODO move secret management logic to separate service
    void persistSecrets(DBSSecretController secretController, boolean isNewDataSource) throws DBException {
        if (!CommonUtils.isBitSet(
            secretController.getSupportedFeatures(),
            isSharedCredentials() ? DBSSecretController.FEATURE_SHARED_SECRETS_EDIT : DBSSecretController.FEATURE_PRIVATE_SECRETS_EDIT)
        ) {
            // Secret edit disabled
            log.debug("Secret save is disabled in secret controller. Auth settings won't be saved.");
            return;
        }

        var secret = saveToSecret();
        if (!isSharedCredentials()) {
            // Do not persist empty secrets for new datasources
            // If secret controller is external then it may take quite a time + may cause errors because of missing secret
            if (!isNewDataSource || secret != null) {
                secretController.setPrivateSecretValue(this, new DBSSecretValue(getSecretValueId(), "", secret));
            }
            secretsResolved = true;
        } else {
            if (selectedSharedCredentials != null) {
                String subjectId = DataSourceUtils.getSubjectFromSecret(selectedSharedCredentials);
                var selectedSharedCredentialsCopy = selectedSharedCredentials;
                selectedSharedCredentialsCopy.setValue(secret);
                try {
                    secretController.setSubjectSecretValue(subjectId, this,
                        new DBSSecretValue(subjectId, getSecretValueId(), "", secret));
                    //the list of available secrets has changed, force update
                    resetAllSecrets();
                    // to resave current secret without additional click in UI
                    setSelectedSharedCredentials(selectedSharedCredentialsCopy);
                } catch (DBException e) {
                    throw new DBException("Cannot set team '" + subjectId + "' credentials: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * this method always forcibly updates available secrets and should always return actual secrets,
     * to get secrets using cache use {@link #listSharedCredentialFromCache}
     */
    @NotNull
    public synchronized List<DBSSecretValue> listSharedCredentials() throws DBException {
        if (!isSharedCredentials()) {
            return List.of();
        }
        resetAllSecrets();
        resolveSecretsIfNeeded();
        if (availableSharedCredentials == null) {
            this.availableSharedCredentials = List.of();
        }
        return availableSharedCredentials;
    }

    /**
     * returns secrets from the cache or reads them if they do not exist, may contain outdated secrets
     */
    private synchronized List<DBSSecretValue> listSharedCredentialFromCache() throws DBException {
        if (availableSharedCredentials != null) {
            return availableSharedCredentials;
        }
        return listSharedCredentials();
    }

    @Nullable
    public DBSSecretValue getSelectedSharedCredentials() {
        return selectedSharedCredentials;
    }

    @Override
    public synchronized void setSelectedSharedCredentials(@NotNull DBSSecretValue secretValue) {
        this.selectedSharedCredentials = secretValue;
        loadFromSecret(this.selectedSharedCredentials.getValue());
    }

    @Override
    public synchronized boolean isSharedCredentialsSelected() {
        return selectedSharedCredentials != null;
    }

    private boolean shouldRemoveExistsSecrets() {
        return isSharedCredentials() != originalShareCredentials;
    }

    @Override
    public void resolveSecrets(DBSSecretController secretController) throws DBException {
        if (!isSharedCredentials()) {
            // try to load private user credentials
            String secretValue = secretController.getPrivateSecretValue(getSecretValueId());
            loadFromSecret(secretValue);
            if (secretValue == null && !DBWorkbench.isDistributed()) {
                // Backward compatibility
                loadFromLegacySecret(secretController);
            }
        } else {
            this.availableSharedCredentials = secretController.discoverCurrentUserSecrets(this);
            if (this.availableSharedCredentials.size() == 1) {
                setSelectedSharedCredentials(availableSharedCredentials.get(0));
            }
        }

        secretsResolved = true;
    }

    @Override
    public boolean isConnected() {
        return dataSource != null && !connecting;
    }

    @Override
    public boolean isConnecting() {
        return connecting;
    }

    @Nullable
    @Override
    public String getConnectionError() {
        return lastConnectionError;
    }

    public boolean connect(DBRProgressMonitor monitor, boolean initialize, boolean reflect) throws DBException {
        if (connecting) {
            log.debug("Can't connect - connect/disconnect is in progress");
            return false;
        }
        if (this.isConnected()) {
            log.debug("Can't connect - already connected");
            return false;
        }

        boolean detachedProcess = DBWorkbench.getPlatform().getApplication().isDetachedProcess();
        boolean succeeded = false;
        connecting = true;
        try {
            getDriver().downloadRequiredDependencies(monitor);
            if (isDetachedProcessEnabled() && !detachedProcess) {
                // Open detached connection
                succeeded = openDetachedConnection(monitor);
            }
            if (!succeeded) {
                if (!detachedProcess) {
                    updateDataSourceObject(succeeded, DBPEvent.Action.BEFORE_CONNECT);
                }

                try {
                    // Open local connection
                    succeeded = connect0(monitor, initialize, reflect);
                } finally {
                    if (!detachedProcess) {
                        updateDataSourceObject(succeeded, DBPEvent.Action.AFTER_CONNECT);
                    }
                }
            }

            return succeeded;
        }
        finally {
            connecting = false;

            if (!detachedProcess) {
                updateDataSourceObject(succeeded, DBPEvent.Action.OBJECT_UPDATE);
            }
        }
    }

    public boolean isDetachedProcessEnabled() {
        return dpiEnabled;
    }

    public void setDetachedProcessEnabled(boolean enabled) {
        dpiEnabled = enabled;
    }


    private boolean openDetachedConnection(DBRProgressMonitor monitor) {
        try {
            DPIProvider provider = GeneralUtils.adapt(this, DPIProvider.class);
            if (provider == null) {
                log.debug("DPI provider not available");
                return false;
            }
            dpiController = provider.detachDatabaseProcess(monitor, this);
            Map<String, String> credentials = new LinkedHashMap<>();
            DPIController dpiClient = dpiController.getClient();
            DPISession session = dpiClient.openSession();
            if (session == null) {
                throw new IllegalStateException("No session");
            }
            log.debug("New DPI session: " + session.getSessionId());
            if (!(getRegistry() instanceof DataSourcePersistentRegistry persistentRegistry)) {
                throw new IllegalStateException("Illegal registry " + getRegistry().getClass());
            }
            DataSourceConfigurationManagerBuffer buffer = new DataSourceConfigurationManagerBuffer();
            persistentRegistry.saveConfigurationToManager(new VoidProgressMonitor(),
                buffer,
                dsc -> dsc.equals(this)
            );

            String[] driverLibraries = getDriver().getDriverLibraries()
                .stream()
                .map(DBPDriverLibrary::getLocalFile)
                .filter(Objects::nonNull)
                .map(path -> path.toAbsolutePath().toString())
                .toArray(String[]::new);
            this.dataSource = dpiClient.openDataSource(
                new DPIDataSourceParameters(
                    session.getSessionId(),
                    new String(buffer.getData(), StandardCharsets.UTF_8),
                    driverLibraries,
                    credentials
                )
            );
            log.debug("Opened data source: " + dataSource);
        } catch (Exception e) {
            log.debug("Error starting DPI child process", e);
            closeDetachedProcess();
            return false;
        }
        return true;
    }

    private void closeDetachedProcess() {
        if (dpiController != null) {
            dpiController.close();
            dpiController = null;
        }
    }

    private boolean connect0(DBRProgressMonitor monitor, boolean initialize, boolean reflect) throws DBException {
        log.debug("Connect with '" + getName() + "' (" + getId() + ")");

        resolveSecretsIfNeeded();

        if (isSharedCredentials() && !isSharedCredentialsSelected()) {
            var sharedCreds = listSharedCredentialFromCache();
            if (!CommonUtils.isEmpty(sharedCreds)) {
                log.debug("Shared credentials not selected - use first one: " + sharedCreds.get(0).getDisplayName());
                setSelectedSharedCredentials(sharedCreds.get(0));
                monitor.subTask("Use first available shared credentials");
            } else {
                log.debug("Shared credentials not found - attempt to connect as is");
            }
        }

        resolvedConnectionInfo = new DBPConnectionConfiguration(connectionInfo);

        // Update auth properties if possible
        lastConnectionError = null;
        try {
            processEvents(monitor, DBPConnectionEventType.BEFORE_CONNECT);

            // 1. Get credentials from origin
            boolean authProvidedFromOrigin = false;
            DBPDataSourceOrigin dsOrigin = getOrigin();
            if (dsOrigin instanceof DBACredentialsProvider cp) {
                String originID = dsOrigin.getType() +
                    (dsOrigin.getSubType() == null ? "" : ("/" + dsOrigin.getSubType()));
                monitor.beginTask("Read auth parameters from " + originID, 1);
                try {
                    authProvidedFromOrigin = cp.provideAuthParameters(monitor, this, resolvedConnectionInfo);
                    if (authProvidedFromOrigin) {
                        log.debug("Auth parameters were provided by origin " + originID);
                    }
                } finally {
                    monitor.done();
                }
            }

            // 2. Get credentials from global provider
            if (!authProvidedFromOrigin) {
                boolean authProvidedFromCredProvider = true;
                DBACredentialsProvider authProvider = registry.getAuthCredentialsProvider();
                if (authProvider != null) {
                    authProvidedFromCredProvider = authProvider.provideAuthParameters(monitor, this, resolvedConnectionInfo);
                } else {
                    // 3. Use legacy password provider
                    if (!isSavePassword() && !getDriver().isAnonymousAccess()) {
                        // Ask for password
                        authProvidedFromCredProvider = askForPassword(this, null, DBWTunnel.AuthCredentials.CREDENTIALS);
                    }
                }
                if (!authProvidedFromCredProvider) {
                    // Auth parameters were canceled
                    return false;
                }
            }

            resolvePropertiesFromProfile();

            // Handle tunnelHandler
            // Open tunnelHandler and replace connection info with new one
            this.proxyHandler = null;
            this.tunnelHandler = null;
            DBWHandlerConfiguration tunnelConfiguration = null, proxyConfiguration = null;
            for (DBWHandlerConfiguration handler : resolvedConnectionInfo.getHandlers()) {
                if (handler.isEnabled()) {
                    // Set driver explicitly.
                    // Handler config may have null driver if it was copied from profile config.
                    handler.setDataSource(this);

                    if (handler.getType() == DBWHandlerType.TUNNEL) {
                        tunnelConfiguration = handler;
                    } else if (handler.getType() == DBWHandlerType.PROXY) {
                        proxyConfiguration = handler;
                    }
                }
            }

            String target = getActualConnectionConfiguration().getUrl();
            if (CommonUtils.isEmpty(target)) {
                target = getName();
            }
            monitor.beginTask("Connect to " + target, tunnelConfiguration != null ? 3 : 2);

            // Use ds exclusive lock to initialize network handlers
            Object dsLock = exclusiveLock.acquireExclusiveLock();
            try {
                // Setup proxy handler
                if (proxyConfiguration != null) {
                    monitor.subTask("Initialize proxy");
                    proxyHandler = proxyConfiguration.createHandler(DBWNetworkHandler.class);
                    proxyHandler.initializeHandler(monitor, proxyConfiguration, resolvedConnectionInfo);
                }

                if (tunnelConfiguration != null) {
                    monitor.subTask("Initialize tunnel");
                    tunnelHandler = tunnelConfiguration.createHandler(DBWTunnel.class);
                    try {
                        if (!tunnelConfiguration.isSavePassword()) {
                            DBWTunnel.AuthCredentials rc = tunnelHandler.getRequiredCredentials(tunnelConfiguration);
                            if (rc != DBWTunnel.AuthCredentials.NONE) {
                                if (!askForPassword(this, tunnelConfiguration, rc)) {
                                    tunnelHandler = null;
                                    return false;
                                }
                            }
                        }
                        // We need to resolve jump server differently due to it being a part of ssh configuration
                        DBExecUtils.startContextInitiation(this);
                        try {
                            DBPDataSourceProvider dataSourceProvider = driver.getDataSourceProvider();
                            if (dataSourceProvider instanceof DBWHandlerConfigurator) {
                                ((DBWHandlerConfigurator) dataSourceProvider).activateHandler(tunnelHandler, resolvedConnectionInfo, tunnelConfiguration);
                            }
                            resolvedConnectionInfo = tunnelHandler.initializeHandler(monitor, tunnelConfiguration, resolvedConnectionInfo);
                        } finally {
                            DBExecUtils.finishContextInitiation(this);
                        }
                    } catch (Exception e) {
                        throw new DBCException("Can't initialize tunnel", e);
                    }
                    monitor.worked(1);
                }

                monitor.subTask("Connect to data source");

                openDataSource(monitor, initialize);

                try {
                    log.debug("Connected (" + getId() + ", " + getPropertyDriver() + ")");
                } catch (Throwable e) {
                    log.debug("Connected (" + getId() + ", driver unknown)");
                }

                this.connectFailed = false;
            } finally {
                exclusiveLock.releaseExclusiveLock(dsLock);
            }

            processEvents(monitor, DBPConnectionEventType.AFTER_CONNECT);

            return true;
        } catch (Throwable e) {
            terminateChildProcesses();
            lastConnectionError = e.getMessage();
            //log.debug("Connection failed (" + getId() + ")", e);
            if (dataSource != null) {
                try {
                    dataSource.shutdown(monitor);
                } catch (Exception e1) {
                    log.debug("Error closing failed connection", e1);
                } finally {
                    dataSource = null;
                }
            }

            if (tunnelHandler != null) {
                try {
                    tunnelHandler.closeTunnel(monitor);
                } catch (Exception e1) {
                    log.error("Error closing tunnel", e1);
                } finally {
                    tunnelHandler = null;
                }
            }
            if (isSharedCredentials()) {
                this.resetAllSecrets();
            }
            proxyHandler = null;
            // Failed
            connectFailed = true;
            if (e instanceof DBException) {
                throw (DBException) e;
            } else {
                throw new DBException("Internal error connecting to " + getName(), e);
            }
        } finally {
            monitor.done();
        }
    }

    private void terminateChildProcesses() {
        synchronized (childProcesses) {
            for (Iterator<DBRProcessDescriptor> iter = childProcesses.iterator(); iter.hasNext(); ) {
                DBRProcessDescriptor process = iter.next();
                if (process.isRunning() && process.getCommand().isTerminateAtDisconnect()) {
                    process.terminate();
                }
                iter.remove();
            }
        }
    }

    private void resolvePropertiesFromProfile() throws DBException {
        DBSSecretController secretController = getProject().isUseSecretStorage() ?
            DBSSecretController.getProjectSecretController(getProject()) : null;
        // Update config from profile
        if (!CommonUtils.isEmpty(resolvedConnectionInfo.getConfigProfileName())) {
            // Update config from profile
            DBWNetworkProfile profile = registry.getNetworkProfile(
                resolvedConnectionInfo.getConfigProfileSource(),
                resolvedConnectionInfo.getConfigProfileName());
            if (profile != null) {
                if (secretController != null) {
                    profile.resolveSecrets(secretController);
                }
                for (DBWHandlerConfiguration handlerCfg : profile.getConfigurations()) {
                    if (handlerCfg.isEnabled()) {
                        resolvedConnectionInfo.updateHandler(new DBWHandlerConfiguration(handlerCfg));
                    }
                }
            }
        }
        // Process variables
        if (preferenceStore.getBoolean(ModelPreferences.CONNECT_USE_ENV_VARS)) {
            IVariableResolver variableResolver = new DataSourceVariableResolver(
                this, this.resolvedConnectionInfo);
            this.resolvedConnectionInfo.resolveDynamicVariables(variableResolver);
        }

    }

    private void resolveSecretsIfNeeded() throws DBException {
        if (secretsResolved || !getProject().isUseSecretStorage()) {
            return;
        }
        if (registry.getDataSource(getId()) == null) {
            // Datasource not saved yet - secrets are unavailable
            return;
        }
        var secretController = DBSSecretController.getProjectSecretController(getProject());
        resolveSecrets(secretController);
    }

    public void openDataSource(DBRProgressMonitor monitor, boolean initialize) throws DBException {
        final DBPDataSourceProvider provider = driver.getDataSourceProvider();
        final DBPDataSourceProviderSynchronizable providerSynchronizable = GeneralUtils.adapt(provider, DBPDataSourceProviderSynchronizable.class);

        if (providerSynchronizable != null && providerSynchronizable.isSynchronizationEnabled(this)) {
            try {
                monitor.beginTask("Synchronize local data source", 1);
                providerSynchronizable.syncLocalDataSource(monitor, this);
                monitor.worked(1);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError(
                    RegistryMessages.dialog_data_source_synchronization_fail_title,
                    NLS.bind(RegistryMessages.dialog_data_source_synchronization_fail_local_message, getName()),
                    e
                );
                throw e;
            } finally {
                monitor.done();
            }
        }

        this.dataSource = provider.openDataSource(monitor, this);
        this.connectTime = new Date();
        monitor.worked(1);

        if (initialize) {
            monitor.subTask("Initialize data source");
            // Disable manual commit for init stage (because it may produce errors and modify transaction scope)
            boolean revertMetaToManualCommit = false;
            try (DBCSession session = DBUtils.openMetaSession(monitor, this, "Read server information")) {
                DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
                if (txnManager != null && !txnManager.isAutoCommit()) {
                    try {
                        txnManager.setAutoCommit(monitor, true);
                        revertMetaToManualCommit = true;
                    } catch (Throwable e) {
                        log.debug("Cannot set auto-commit flag: " + e.getMessage());
                    }
                }

                try {
                    dataSource.initialize(monitor);
                    dataSource.getSQLDialect().afterDataSourceInitialization(dataSource);
                } catch (Throwable e) {
                    log.error("Error initializing datasource", e);
                    throw e;
                }
                if (revertMetaToManualCommit) {
                    txnManager.setAutoCommit(monitor, false);
                }
            }
        }
    }

    private void processEvents(DBRProgressMonitor monitor, DBPConnectionEventType eventType) throws DBException {
        DBPConnectionConfiguration info = getActualConnectionConfiguration();
        DBRShellCommand command = info.getEvent(eventType);
        if (command != null && command.isEnabled()) {
            final DBRProcessDescriptor processDescriptor = new DBRProcessDescriptor(command, getVariablesResolver(true));

            monitor.subTask("Execute process " + processDescriptor.getName());
            DBWorkbench.getPlatformUI().executeProcess(processDescriptor);

            {
                // Run output grab job
                new AbstractJob(processDescriptor.getName() + ": output reader") {
                    @Override
                    protected IStatus run(DBRProgressMonitor monitor) {
                        try {
                            String output = processDescriptor.dumpErrors();
                            log.debug("Process error output:\n" + output);
                        } catch (Exception e) {
                            log.debug(e);
                        }
                        return Status.OK_STATUS;
                    }
                }.schedule();
            }

            if (command.isWaitProcessFinish()) {
                int resultCode;
                if (command.getWaitProcessTimeoutMs() >= 0) {
                    resultCode = processDescriptor.waitFor(command.getWaitProcessTimeoutMs());
                } else {
                    resultCode = processDescriptor.waitFor();
                }
                log.debug(processDescriptor.getName() + " result code: " + resultCode);
            }
            addChildProcess(processDescriptor);
        }

        for (DataSourceHandlerDescriptor handlerDesc : DataSourceProviderRegistry.getInstance().getDataSourceHandlers()) {
            switch (eventType) {
                case BEFORE_CONNECT -> handlerDesc.getInstance().beforeConnect(monitor, this);
                case AFTER_DISCONNECT -> handlerDesc.getInstance().beforeDisconnect(monitor, this);
            }
        }
    }

    @Override
    public boolean disconnect(final DBRProgressMonitor monitor) {
        return disconnect(monitor, true);
    }

    private boolean disconnect(final DBRProgressMonitor monitor, boolean reflect) {
        if (dataSource == null) {
            log.error("Datasource is not connected");
            return true;
        }
        if (connecting) {
            log.error("Connect/disconnect is in progress");
            return false;
        }

        connecting = true;
        releaseDataSourceUsers(monitor);
        try {
            if (dpiController != null) {
                closeDetachedProcess();
                return true;
            }
            monitor.beginTask("Disconnect from '" + getName() + "'", 5 + dataSource.getAvailableInstances().size());

            processEvents(monitor, DBPConnectionEventType.BEFORE_DISCONNECT);

            monitor.worked(1);

            final var provider = driver.getDataSourceProvider();
            final var providerSynchronizable = GeneralUtils.adapt(provider, DBPDataSourceProviderSynchronizable.class);

            if (providerSynchronizable != null && providerSynchronizable.isSynchronizationEnabled(this)) {
                if (!providerSynchronizable.isLocalDataSourceSynchronized(monitor, this)) {
                    try {
                        monitor.beginTask("Synchronize remote data source", 1);
                        providerSynchronizable.syncRemoteDataSource(monitor, this);
                        monitor.worked(1);
                    } catch (DBException e) {
                        DBWorkbench.getPlatformUI().showError(
                            RegistryMessages.dialog_data_source_synchronization_fail_title,
                            NLS.bind(RegistryMessages.dialog_data_source_synchronization_fail_remote_message, dataSource.getName()),
                            e
                        );
                        throw e;
                    } finally {
                        monitor.done();
                    }
                }
            }

            // Close datasource
            monitor.subTask("Close connection");
            if (dataSource != null) {
                dataSource.shutdown(monitor);
            }
            monitor.worked(1);

            // Close tunnelHandler
            if (tunnelHandler != null) {
                monitor.subTask("Close tunnel");
                try {
                    tunnelHandler.closeTunnel(monitor);
                } catch (Throwable e) {
                    log.error("Error closing tunnel", e);
                }
            }
            monitor.worked(1);

            proxyHandler = null;

            processEvents(monitor, DBPConnectionEventType.AFTER_DISCONNECT);

            monitor.worked(1);

            monitor.done();

            return true;
        } catch (Exception e) {
            log.error("Error during datasource disconnect", e);
            return false;
        } finally {
            // Terminate child processes
            terminateChildProcesses();

            this.dataSource = null;
            this.resolvedConnectionInfo = null;
            // Reset resolved secrets
            resetAllSecrets();

            this.connectTime = null;

            if (reflect) {
                // Reflect UI
                updateDataSourceObject(false, DBPEvent.Action.OBJECT_UPDATE);
            }

            connecting = false;
            log.debug("Disconnected (" + getId() + ")");
        }
    }

    private void releaseDataSourceUsers(DBRProgressMonitor monitor) {
        List<DBPDataSourceTask> usersStamp;
        synchronized (users) {
            usersStamp = new ArrayList<>(users);
        }

        int jobCount = 0;
        // Save all unsaved data
        for (DBPDataSourceTask user : usersStamp) {
            if (user instanceof Job) {
                jobCount++;
            }
            if (user instanceof DBPDataSourceAcquirer) {
                ((DBPDataSourceAcquirer) user).beforeDisconnect();
            }
        }
        if (jobCount > 0) {
            monitor.beginTask("Waiting for all active tasks to finish", jobCount);
            // Stop all jobs
            for (DBPDataSourceTask user : usersStamp) {
                if (user instanceof Job job) {
                    monitor.subTask("Stop '" + job.getName() + "'");
                    if (job.getState() == Job.RUNNING) {
                        job.cancel();
                        try {
                            // Wait for 3 seconds
                            for (int i = 0; i < 30; i++) {
                                Thread.sleep(100);
                                if (job.getState() != Job.RUNNING) {
                                    break;
                                }
                            }
                        } catch (InterruptedException e) {
                            // its ok, do nothing
                        }
                    }
                    monitor.worked(1);
                }
            }
            monitor.done();
        }
    }

    @Override
    public boolean reconnect(final DBRProgressMonitor monitor)
        throws DBException {
        return reconnect(monitor, true);
    }

    public boolean reconnect(final DBRProgressMonitor monitor, boolean reflect)
        throws DBException {
        if (connecting) {
            log.debug("Can't reconnect - connect/disconnect is in progress");
            return false;
        }
        if (isConnected() && !disconnect(monitor, reflect)) {
            return false;
        }
        return connect(monitor, true, reflect);
    }

    @Override
    public Collection<DBPDataSourceTask> getTasks() {
        synchronized (users) {
            return new ArrayList<>(users);
        }
    }

    @Override
    public void acquire(DBPDataSourceTask user) {
        synchronized (users) {
            if (users.contains(user)) {
                log.warn("Datasource user '" + user + "' already registered in datasource '" + getName() + "'");
            } else {
                users.add(user);
            }
        }
    }

    @Override
    public void release(DBPDataSourceTask user) {
        synchronized (users) {
            if (!users.remove(user)) {
                if (!isDisposed()) {
                    log.warn("Datasource user '" + user + "' is not registered in datasource '" + getName() + "'");
                }
            }
        }
    }

    @Override
    public void fireEvent(DBPEvent event) {
        registry.notifyDataSourceListeners(event);
    }

    @Override
    public Map<String, String> getTags() {
        return new LinkedHashMap<>(tags);
    }

    public void setTags(Map<String, String> tags) {
        this.tags.clear();
        this.tags.putAll(tags);
    }

    @Override
    public String getTagValue(String tagName) {
        return tags.get(tagName);
    }

    @Override
    public void setTagValue(String tagName, String tagValue) {
        tags.put(tagName, tagValue);
    }

    @Nullable
    @Override
    public String getExtension(@NotNull String name) {
        return extensions.get(name);
    }

    @Override
    public void setExtension(@NotNull String name, @Nullable String value) {
        if (value == null) {
            this.extensions.remove(name);
        } else {
            this.extensions.put(name, value);
        }
    }

    @NotNull
    public Map<String, String> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, String> extensions) {
        this.extensions.clear();
        this.extensions.putAll(extensions);
    }

    @Override
    public DBDDataFormatterProfile getDataFormatterProfile() {
        if (this.formatterProfile == null) {
            this.formatterProfile = new DataFormatterProfile(getId(), preferenceStore);
        }
        return this.formatterProfile;
    }

    @Override
    public boolean isUseNativeDateTimeFormat() {
        return getPreferenceStore().getBoolean(ModelPreferences.RESULT_NATIVE_DATETIME_FORMAT);
    }

    @Override
    public boolean isUseNativeNumericFormat() {
        return getPreferenceStore().getBoolean(ModelPreferences.RESULT_NATIVE_NUMERIC_FORMAT);
    }

    @Override
    public boolean isUseScientificNumericFormat() {
        return getPreferenceStore().getBoolean(ModelPreferences.RESULT_SCIENTIFIC_NUMERIC_FORMAT);
    }

    @NotNull
    @Override
    public DBDValueHandler getDefaultValueHandler() {
        if (dataSource instanceof DBDFormatSettings) {
            return ((DBDFormatSettings) dataSource).getDefaultValueHandler();
        }
        return DefaultValueHandler.INSTANCE;
    }

    @NotNull
    @Override
    public DataSourcePreferenceStore getPreferenceStore() {
        return preferenceStore;
    }

    @Override
    public void resetPassword() {
        connectionInfo.setUserPassword(null);
        ObjectPropertyDescriptor.extractAnnotations(
                null,
                ObjectPropertyDescriptor.getObjectClass(connectionInfo.getAuthModel().createCredentials()),
                (o, p) -> true,
                null
            ).stream()
            .filter(ObjectPropertyDescriptor::isPassword)
            .forEach(passwordProperty -> connectionInfo.setAuthProperty(passwordProperty.getKeyName(), null));
    }

    @Nullable
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (DBPDataSourceContainer.class.isAssignableFrom(adapter)) {
            return adapter.cast(this);
        } else if (adapter == DBPPropertySource.class) {
            PropertyCollector coll = new PropertyCollector(this, true);
            coll.collectProperties();
            if (dataSource != null) {
                int conIndex = 0;
                for (DBSInstance instance : dataSource.getAvailableInstances()) {
                    for (DBCExecutionContext context : instance.getAllContexts()) {
                        conIndex++;
                        coll.addProperty(CATEGORY_CONNECTIONS, "context-" + context.getContextId(), String.valueOf(conIndex), context.getContextName());
                    }
                }
            }
            if (driver.getClassLoader() instanceof URLClassLoader) {
                final URL[] urls = ((URLClassLoader) driver.getClassLoader()).getURLs();
                for (int urlIndex = 0; urlIndex < urls.length; urlIndex++) {
                    Object path = urls[urlIndex];
                    try {
                        path = Paths.get(((URL) path).toURI());
                    } catch (Exception ignored) {
                    }
                    coll.addProperty(CATEGORY_DRIVER_FILES, "driver-file-" + urlIndex, String.valueOf(urlIndex), path);
                }
            }
            return adapter.cast(coll);
        }
        return null;
    }

    @Override
    @NotNull
    public DBPImage getObjectImage() {
        return driver.getPlainIcon();
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        if (isConnected()) {
            return DBSObjectState.ACTIVE;
        } else if (connectFailed) {
            return DBSObjectState.INVALID;
        } else {
            return DBSObjectState.NORMAL;
        }
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) {
        // just do nothing
    }

    public static String generateNewId(DBPDriver driver) {
        long rnd = new Random().nextLong();
        if (rnd < 0) rnd = -rnd;
        return driver.getId() + "-" + Long.toHexString(System.currentTimeMillis()) + "-" + Long.toHexString(rnd);
    }

    @Property(viewable = true, order = 20, category = CATEGORY_DRIVER)
    public String getPropertyDriverType() {
        return driver.getName();
    }

    @Property(order = 30, category = CATEGORY_SERVER)
    public String getPropertyAddress() {
        StringBuilder addr = new StringBuilder();
        if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
            addr.append(connectionInfo.getHostName());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            addr.append(':').append(connectionInfo.getHostPort());
        }
        return addr.toString();
    }

    @Property(order = 31, category = CATEGORY_SERVER)
    public String getPropertyDatabase() {
        return connectionInfo.getDatabaseName();
    }

    @Property(order = 32, category = CATEGORY_SERVER)
    public String getPropertyURL() {
        return connectionInfo.getUrl();
    }

    @Nullable
    @Property(order = 33, category = CATEGORY_SERVER)
    public String getPropertyServerName() {
        if (dataSource != null) {
            String serverName = dataSource.getInfo().getDatabaseProductName();
            String serverVersion = dataSource.getInfo().getDatabaseProductVersion();
            if (serverName != null) {
                return serverName + (serverVersion == null ? "" : " [" + serverVersion + "]");
            }
        }
        return null;
    }

    @Nullable
    @Property(order = 34, category = CATEGORY_SERVER)
    public Map<String, Object> getPropertyServerDetails() {
        if (dataSource != null) {
            return dataSource.getInfo().getDatabaseProductDetails();
        }
        return null;
    }

    @Nullable
    @Property(order = 21, category = CATEGORY_DRIVER)
    public String getPropertyDriver() {
        if (dataSource != null) {
            String driverName = dataSource.getInfo().getDriverName();
            String driverVersion = dataSource.getInfo().getDriverVersion();
            if (driverName != null) {
                return driverName + (driverVersion == null ? "" : " [" + driverVersion + "]");
            }
        }
        return null;
    }

    @Nullable
    @Property(order = 8)
    public String getPropertyConnectTime() {
        if (connectTime != null) {
            return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(connectTime);
        }
        return null;
    }

    @Property(order = 9)
    public String getPropertyConnectType() {
        return connectionInfo.getConnectionType().getName();
    }

    private void addChildProcess(DBRProcessDescriptor process) {
        synchronized (childProcesses) {
            childProcesses.add(process);
        }
    }

    public void copyFrom(DataSourceDescriptor descriptor) {
        this.origin = descriptor.origin;
        this.filterMap.clear();
        for (FilterMapping mapping : descriptor.getObjectFilters()) {
            this.filterMap.put(mapping.typeName, new FilterMapping(mapping));
        }
        this.virtualModel.copyFrom(descriptor.getVirtualModel());

        this.description = descriptor.description;
        this.tags.putAll(descriptor.tags);
        this.extensions.putAll(descriptor.extensions);
        this.savePassword = descriptor.savePassword;
        this.connectionReadOnly = descriptor.connectionReadOnly;
        this.forceUseSingleConnection = descriptor.forceUseSingleConnection;

        this.dpiEnabled = descriptor.dpiEnabled;

        this.navigatorSettings = new DataSourceNavigatorSettings(descriptor.getNavigatorSettings());
    }

    @Override
    public String toString() {
        return name + " [" + driver + "]";
    }

    public boolean equalSettings(Object obj) {
        if (!(obj instanceof DataSourceDescriptor source)) {
            return false;
        }
        return
            CommonUtils.equalOrEmptyStrings(this.name, source.name) &&
                CommonUtils.equalOrEmptyStrings(this.description, source.description) &&
                equalConfiguration(source);
    }

    public boolean equalConfiguration(DataSourceDescriptor source) {
        return
            CommonUtils.equalObjects(this.savePassword, source.savePassword) &&
                CommonUtils.equalObjects(this.sharedCredentials, source.sharedCredentials) &&
                CommonUtils.equalObjects(this.connectionReadOnly, source.connectionReadOnly) &&
                CommonUtils.equalObjects(this.forceUseSingleConnection, source.forceUseSingleConnection) &&
                CommonUtils.equalObjects(this.navigatorSettings, source.navigatorSettings) &&
                CommonUtils.equalObjects(this.driver, source.driver) &&
                CommonUtils.equalObjects(this.originalDriver, source.originalDriver) &&
                CommonUtils.equalObjects(this.driverSubstitution, source.driverSubstitution) &&
                CommonUtils.equalObjects(this.connectionInfo, source.connectionInfo) &&
                this.dpiEnabled == source.dpiEnabled &&
                CommonUtils.equalObjects(this.filterMap, source.filterMap) &&
                CommonUtils.equalObjects(this.formatterProfile, source.formatterProfile) &&
                CommonUtils.equalObjects(this.clientHome, source.clientHome) &&
                CommonUtils.equalObjects(this.lockPasswordHash, source.lockPasswordHash) &&
                CommonUtils.equalObjects(this.folder, source.folder) &&
                CommonUtils.equalObjects(this.extensions, source.extensions) &&
                CommonUtils.equalObjects(this.preferenceStore, source.preferenceStore) &&
                CommonUtils.equalsContents(this.connectionModifyRestrictions, source.connectionModifyRestrictions);
    }

    @NotNull
    @Override
    public String getProjectId() {
        return getProject().getId();
    }

    @NotNull
    @Override
    public String getSecretObjectId() {
        return getId();
    }

    @NotNull
    public String getSecretValueId() {
        return RMProjectType.getPlainProjectId(getProject()) + DATASOURCE_KEY_PREFIX + getId();
    }

    @NotNull
    @Override
    public String getSecretObjectType() {
        return SMObjectType.datasource.name();
    }

    public static class ContextInfo implements DBPObject {
        private final DBCExecutionContext context;

        public ContextInfo(DBCExecutionContext context) {
            this.context = context;
        }

        @Property(viewable = true, order = 1)
        public String getName() {
            return context.getContextName();
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    @Override
    public IVariableResolver getVariablesResolver(boolean actualConfig) {
        DBPConnectionConfiguration configuration = actualConfig ? getActualConnectionConfiguration() : getConnectionConfiguration();
        return new DataSourceVariableResolver(this, configuration);
    }

    @Override
    public DBPDataSourceContainer createCopy(DBPDataSourceRegistry forRegistry) {
        DataSourceDescriptor copy = new DataSourceDescriptor(this, forRegistry, true);
        copy.setId(DataSourceDescriptor.generateNewId(copy.getDriver()));
        return copy;
    }

    @Override
    public DBPExclusiveResource getExclusiveLock() {
        return exclusiveLock;
    }

    @Override
    public boolean isForceUseSingleConnection() {
        return this.forceUseSingleConnection;
    }

    @Override
    public void setForceUseSingleConnection(boolean value) {
        this.forceUseSingleConnection = value;
    }

    @Nullable
    @Override
    public String getRequiredExternalAuth() {
        var dsOrigin = getOrigin();
        if (dsOrigin instanceof DBPDataSourceOriginExternal externalOrigin) {
            return externalOrigin.getSubType();
        }

        var reqAuthProvider = getConnectionConfiguration().getAuthModelDescriptor().getRequiredAuthProviderId();
        if (!CommonUtils.isEmpty(reqAuthProvider)) {
            return reqAuthProvider;
        }
        if (getConnectionConfiguration().getAuthModel() instanceof DBAAuthModelExternal<?> authModelExternal) {
            return authModelExternal.getRequiredExternalAuth(getConnectionConfiguration());
        }
        return CommonUtils.isEmpty(reqAuthProvider) ? null : reqAuthProvider;
    }

    public static boolean askForPassword(
        @NotNull DataSourceDescriptor dataSourceContainer,
        @Nullable DBWHandlerConfiguration networkHandler,
        @NotNull DBWTunnel.AuthCredentials authType
    ) {
        DBPConnectionConfiguration actualConfig = dataSourceContainer.getActualConnectionConfiguration();
        DBPConnectionConfiguration connConfig = dataSourceContainer.getConnectionConfiguration();

        final String prompt = networkHandler != null ?
            NLS.bind(RegistryMessages.dialog_connection_auth_title_for_handler, networkHandler.getTitle()) :
            "'" + dataSourceContainer.getName() + RegistryMessages.dialog_connection_auth_title; //$NON-NLS-1$
        final String user = networkHandler != null ? networkHandler.getUserName() : actualConfig.getUserName();
        final String password = networkHandler != null ? networkHandler.getPassword() : actualConfig.getUserPassword();

        DBPAuthInfo authInfo;
        try {
            authInfo = askCredentials(dataSourceContainer, authType, prompt, user, password, !dataSourceContainer.isTemporary());
        } catch (Exception e) {
            log.debug(e);
            authInfo = new DBPAuthInfo(user, password, false);
        }
        if (authInfo == null) {
            return false;
        }

        if (networkHandler != null) {
            networkHandler.setUserName(authInfo.getUserName());
            networkHandler.setPassword(authInfo.getUserPassword());
            networkHandler.setSavePassword(authInfo.isSavePassword());
            actualConfig.updateHandler(networkHandler);

            if (authInfo.isSavePassword() && connConfig != actualConfig) {
                // Save changes in real connection info
                connConfig.updateHandler(networkHandler);
            }
        } else {
            if (authType == DBWTunnel.AuthCredentials.CREDENTIALS) {
                actualConfig.setUserName(authInfo.getUserName());
            }
            actualConfig.setUserPassword(authInfo.getUserPassword());
            dataSourceContainer.setSavePassword(authInfo.isSavePassword());
        }
        if (authInfo.isSavePassword()) {
            if (authInfo.isSavePassword() && connConfig != actualConfig) {
                if (authType == DBWTunnel.AuthCredentials.CREDENTIALS) {
                    if (networkHandler != null) {
                        networkHandler.setUserName(authInfo.getUserName());
                    } else {
                        connConfig.setUserName(authInfo.getUserName());
                    }
                }
                if (networkHandler != null) {
                    networkHandler.setPassword(authInfo.getUserPassword());
                } else {
                    connConfig.setUserPassword(authInfo.getUserPassword());
                }
            }
            try {
                // Update connection properties
                dataSourceContainer.getRegistry().updateDataSource(dataSourceContainer);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Error saving datasource", null, e);
            }
        }

        return true;
    }

    private static DBPAuthInfo askCredentials(
        @NotNull DataSourceDescriptor dataSourceContainer,
        @NotNull DBWTunnel.AuthCredentials authType,
        String prompt,
        String user,
        String password,
        boolean canSavePassword)
    {
        DBPAuthInfo authInfo;
        authInfo = DBWorkbench.getPlatformUI().promptUserCredentials(prompt,
            RegistryMessages.dialog_connection_auth_username, user,
            authType == DBWTunnel.AuthCredentials.PASSWORD
                ? RegistryMessages.dialog_connection_auth_passphrase
                : RegistryMessages.dialog_connection_auth_password, password,
            false,
            canSavePassword
        );
        return authInfo;
    }

    private void updateDataSourceObject(boolean succeeded, DBPEvent.Action action) {
        getRegistry().notifyDataSourceListeners(new DBPEvent(
            action,
            this,
            succeeded));
    }

    /**
     * Saves datasource secret credentials to secret value (json)
     */
    @Nullable
    //TODO move out?
    public String saveToSecret() {
        Map<String, Object> props = new LinkedHashMap<>();

        if (isSavePassword()) {
            // Primary props
            if (!CommonUtils.isEmpty(connectionInfo.getUserName())) {
                props.put(RegistryConstants.ATTR_USER, connectionInfo.getUserName());
            }
            if (!CommonUtils.isEmpty(connectionInfo.getUserPassword())) {
                props.put(RegistryConstants.ATTR_PASSWORD, connectionInfo.getUserPassword());
            }
            // Additional auth props
            if (!CommonUtils.isEmpty(connectionInfo.getAuthProperties())) {
                props.put(RegistryConstants.TAG_PROPERTIES, connectionInfo.getAuthProperties());
            }
            if (props.isEmpty()) {
                props.put(RegistryConstants.ATTR_EMPTY_DATABASE_CREDENTIALS, true);
            }
            this.secretsContainsDatabaseCreds = true;
        } else {
            this.secretsContainsDatabaseCreds = false;
        }

        //do not store handlers in secret for shared connections
        if (!isSharedCredentials()) {
            // Handlers. If config profile is set then props are saved there
            DBWNetworkProfile activeProfile = CommonUtils.isEmpty(connectionInfo.getConfigProfileName())
                ? null
                : this.getRegistry().getNetworkProfile(connectionInfo.getConfigProfileSource(), connectionInfo.getConfigProfileName());

            List<Map<String, Object>> handlersConfigs = new ArrayList<>();
            for (DBWHandlerConfiguration hc : connectionInfo.getHandlers()) {
                DBWHandlerConfiguration profileConfig = activeProfile == null ? null : activeProfile.getConfiguration(hc.getHandlerDescriptor());
                if (profileConfig == null || !profileConfig.isEnabled()) {
                    Map<String, Object> handlerProps = hc.saveToSecret();
                    if (!handlerProps.isEmpty()) {
                        handlerProps.put(RegistryConstants.ATTR_ID, hc.getId());
                        handlersConfigs.add(handlerProps);
                    }
                }
            }
            if (!handlersConfigs.isEmpty()) {
                props.put(RegistryConstants.TAG_HANDLERS, handlersConfigs);
            }
        }
        if (props.isEmpty()) {
            return null;
        }

        // Info fields (we don't use them anyhow)
        // Add them only if we have real props
        // Add them first (just to make secret easy-to-read during debugging)
        Map<String, Object> propsFull = new LinkedHashMap<>();
        propsFull.put("datasource-name", getName());
        propsFull.put("datasource-driver", getDriver().getFullId());
        propsFull.putAll(props);

        return DBInfoUtils.SECRET_GSON.toJson(propsFull);
    }

    public void loadFromSecret(@Nullable String secretValue) {
        if (secretValue == null) {
            if (DBWorkbench.isDistributed()) {
                // In distributed mode we reset saved password in case of null secret
                connectionInfo.getHandlers().forEach(handler ->
                    handler.setSavePassword(false)
                );
                savePassword = false;
            }
            return;
        }

        Map<String, Object> props;
        try {
            props = JSONUtils.parseMap(DBInfoUtils.SECRET_GSON, new StringReader(secretValue));
        } catch (Exception e) {
            log.error("Error parsing secret value", e);
            return;
        }

        // Primary props
        var dbUserName = JSONUtils.getString(props, RegistryConstants.ATTR_USER);
        var dbPassword = JSONUtils.getString(props, RegistryConstants.ATTR_PASSWORD);
        var dbAuthProperties = JSONUtils.deserializeStringMapOrNull(props, RegistryConstants.TAG_PROPERTIES);
        connectionInfo.setUserName(dbUserName);
        connectionInfo.setUserPassword(dbPassword);
        // Additional auth props
        if (!CommonUtils.isEmpty(dbAuthProperties)) {
            for (Map.Entry<String, String> ap : dbAuthProperties.entrySet()) {
                connectionInfo.setAuthProperty(ap.getKey(), ap.getValue());
            }
        }

        boolean emptyDatabaseCredsSaved = CommonUtils.toBoolean(props.getOrDefault(RegistryConstants.ATTR_EMPTY_DATABASE_CREDENTIALS, false));
        this.secretsContainsDatabaseCreds = props.containsKey(RegistryConstants.ATTR_USER)
            || props.containsKey(RegistryConstants.ATTR_PASSWORD)
            || props.containsKey(RegistryConstants.TAG_PROPERTIES)
            || emptyDatabaseCredsSaved;

        if (DBWorkbench.isDistributed()) {
            // In distributed mode we detect saved password dynamically
            this.savePassword = secretsContainsDatabaseCreds;
        }


        // Handlers
        List<Map<String, Object>> handlerList = JSONUtils.getObjectList(props, RegistryConstants.TAG_HANDLERS);
        Set<String> handlersFromSecret = new HashSet<>(); // secrets do not store all handler configs
        if (!CommonUtils.isEmpty(handlerList)) {
            for (Map<String, Object> handlerMap : handlerList) {
                String handlerId = JSONUtils.getString(handlerMap, RegistryConstants.ATTR_ID);
                DBWHandlerConfiguration hc = connectionInfo.getHandler(handlerId);
                if (hc == null) {
                    log.warn("Handler '" + handlerId + "' not found in datasource '" + getId() + "'. Secret configuration will be lost.");
                    continue;
                }
                handlersFromSecret.add(handlerId);
                var hcUsername = JSONUtils.getString(handlerMap, RegistryConstants.ATTR_USER);
                var hcPassword = JSONUtils.getString(handlerMap, RegistryConstants.ATTR_PASSWORD);
                var hcProperties = JSONUtils.deserializeStringMap(handlerMap, RegistryConstants.TAG_PROPERTIES);
                hc.setUserName(hcUsername);
                hc.setPassword(hcPassword);
                hc.setSecureProperties(hcProperties);
                hc.setSavePassword(
                    CommonUtils.isNotEmpty(hcUsername) || CommonUtils.isNotEmpty(hcPassword) || !CommonUtils.isEmpty(hcProperties)
                );
            }
        }
        //private secret contains handlers config, shared - not
        if (!isSharedCredentials()) {
            connectionInfo.getHandlers().forEach(
                handler -> {
                    if (!handlersFromSecret.contains(handler.getId())) {
                        handler.setSavePassword(false);
                    }
                }
            );
        }
    }

    private void loadFromLegacySecret(DBSSecretController secretController) {
        if (!(secretController instanceof DBSSecretBrowser sBrowser)) {
            return;
        }

        // Datasource props
        String keyPrefix = "datasources/" + getId();
        Path itemPath = Path.of(keyPrefix);
        try {
            for (DBSSecret secret : sBrowser.listSecrets(itemPath.toString())) {
                String secretId = secret.getId();
                String secretValue = secretController.getPrivateSecretValue(secretId);
                switch (secret.getName()) {
                    case RegistryConstants.ATTR_USER -> connectionInfo.setUserName(secretValue);
                    case RegistryConstants.ATTR_PASSWORD -> connectionInfo.setUserPassword(secretValue);
                    default -> connectionInfo.setAuthProperty(secretId, secretValue);
                }
            }
            // Handlers
            for (DBWHandlerConfiguration hc : connectionInfo.getHandlers()) {
                itemPath = Path.of(keyPrefix + "/network/" + hc.getId());
                for (DBSSecret secret : sBrowser.listSecrets(itemPath.toString())) {
                    String secretId = secret.getId();
                    switch (secret.getName()) {
                        case RegistryConstants.ATTR_USER -> hc.setUserName(
                            secretController.getPrivateSecretValue(secretId));
                        case RegistryConstants.ATTR_PASSWORD -> hc.setPassword(
                            secretController.getPrivateSecretValue(secretId));
                        default -> hc.setProperty(
                            secretId,
                            secretController.getPrivateSecretValue(secretId));
                    }
                }
            }
        } catch (DBException e) {
            log.error("Error reading datasource '" + getId() + "' legacy secrets", e);
        }
    }


}
