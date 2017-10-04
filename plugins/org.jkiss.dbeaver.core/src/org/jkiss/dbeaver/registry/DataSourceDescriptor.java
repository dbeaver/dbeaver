/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPClientHome;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionEventType;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.impl.data.DefaultValueHandler;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.model.net.DBWNetworkHandler;
import org.jkiss.dbeaver.model.net.DBWTunnel;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.*;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVModel;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.formatter.DataFormatterProfile;
import org.jkiss.dbeaver.runtime.TasksJob;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
        DBPRefreshableObject
{
    private static final Log log = Log.getLog(DataSourceDescriptor.class);

    public static final String[][] CONNECT_VARIABLES = new String[][]{
        {RegistryConstants.VARIABLE_HOST, "target host"},
        {RegistryConstants.VARIABLE_PORT, "target port"},
        {RegistryConstants.VARIABLE_SERVER, "target server name"},
        {RegistryConstants.VARIABLE_DATABASE, "target database"},
        {RegistryConstants.VARIABLE_USER, "user name"},
        {RegistryConstants.VARIABLE_PASSWORD, "password (plain)"},
        {RegistryConstants.VARIABLE_URL, "JDBC URL"},

        {SystemVariablesResolver.VAR_WORKSPACE, "workspace path"},
        {SystemVariablesResolver.VAR_HOME, "user home path"},
        {SystemVariablesResolver.VAR_DBEAVER_HOME, "application install path"},
        {SystemVariablesResolver.VAR_APP_NAME, "application name"},
        {SystemVariablesResolver.VAR_APP_VERSION, "application version"},
    };

    @NotNull
    private final DBPDataSourceRegistry registry;
    @NotNull
    private final DataSourceOrigin origin;
    @NotNull
    private DriverDescriptor driver;
    @NotNull
    private DBPConnectionConfiguration connectionInfo;
    private DBPConnectionConfiguration tunnelConnectionInfo;
    @NotNull
    private String id;
    private String name;
    private String description;
    private boolean savePassword;
    private boolean showSystemObjects;
    private boolean showUtilityObjects;
    private boolean connectionReadOnly;
    private final Map<String, FilterMapping> filterMap = new HashMap<>();
    private DBDDataFormatterProfile formatterProfile;
    @Nullable
    private DBPClientHome clientHome;
    @Nullable
    private String lockPasswordHash;
    @Nullable
    private DataSourceFolder folder;

    @NotNull
    private DataSourcePreferenceStore preferenceStore;
    @Nullable
    private DBPDataSource dataSource;

    private final List<DBPDataSourceUser> users = new ArrayList<>();

    private volatile boolean connectFailed = false;
    private volatile Date connectTime = null;
    private volatile boolean disposed = false;
    private volatile boolean connecting = false;
    private boolean temporary;
    private final List<DBRProcessDescriptor> childProcesses = new ArrayList<>();
    private DBWTunnel tunnel;
    @NotNull
    private final DBVModel virtualModel;

    public DataSourceDescriptor(
        @NotNull DBPDataSourceRegistry registry,
        @NotNull String id,
        @NotNull DriverDescriptor driver,
        @NotNull DBPConnectionConfiguration connectionInfo)
    {
        this(registry, ((DataSourceRegistry)registry).getDefaultOrigin(), id, driver, connectionInfo);
    }

    DataSourceDescriptor(
        @NotNull DBPDataSourceRegistry registry,
        @NotNull DataSourceOrigin origin,
        @NotNull String id,
        @NotNull DriverDescriptor driver,
        @NotNull DBPConnectionConfiguration connectionInfo)
    {
        this.registry = registry;
        this.origin = origin;
        this.id = id;
        this.driver = driver;
        this.connectionInfo = connectionInfo;
        this.preferenceStore = new DataSourcePreferenceStore(this);
        this.virtualModel = new DBVModel(this);
    }

    // Copy constructor
    public DataSourceDescriptor(@NotNull DataSourceDescriptor source)
    {
        this.registry = source.registry;
        this.origin = source.origin;
        this.id = source.id;
        this.name = source.name;
        this.description = source.description;
        this.savePassword = source.savePassword;
        this.showSystemObjects = source.showSystemObjects;
        this.showUtilityObjects = source.showUtilityObjects;
        this.connectionReadOnly = source.connectionReadOnly;
        this.driver = source.driver;
        this.connectionInfo = source.connectionInfo;
        this.formatterProfile = source.formatterProfile;
        this.clientHome = source.clientHome;

        this.connectionInfo = new DBPConnectionConfiguration(source.connectionInfo);
        for (Map.Entry<String, FilterMapping> fe : source.filterMap.entrySet()) {
            this.filterMap.put(fe.getKey(), new FilterMapping(fe.getValue()));
        }
        this.lockPasswordHash = source.lockPasswordHash;
        this.folder = source.folder;

        this.preferenceStore = new DataSourcePreferenceStore(this);
        this.virtualModel = new DBVModel(this, source.virtualModel);
    }

    public boolean isDisposed()
    {
        return disposed;
    }

    public void dispose()
    {
        if (disposed) {
            log.warn("Dispose of already disposed data source");
            return;
        }
        synchronized (users) {
            users.clear();
        }
        disposed = true;
    }

    @NotNull
    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @NotNull
    @Override
    public DriverDescriptor getDriver()
    {
        return driver;
    }

    @NotNull
    @Override
    public DBPPlatform getPlatform() {
        return registry.getPlatform();
    }

    public void setDriver(@NotNull DriverDescriptor driver)
    {
        this.driver = driver;
    }

    @NotNull
    @Override
    public DBPConnectionConfiguration getConnectionConfiguration()
    {
        return connectionInfo;
    }

    public void setConnectionInfo(@NotNull DBPConnectionConfiguration connectionInfo)
    {
        this.connectionInfo = connectionInfo;
    }

    @NotNull
    @Override
    public DBPConnectionConfiguration getActualConnectionConfiguration()
    {
        return tunnelConnectionInfo != null ? tunnelConnectionInfo : connectionInfo;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 2)
    public String getDescription()
    {
        return description;
    }

    public boolean isSavePassword()
    {
        return savePassword;
    }

    public void setSavePassword(boolean savePassword)
    {
        this.savePassword = savePassword;
    }

    @Override
    public boolean isShowSystemObjects()
    {
        return showSystemObjects;
    }

    public void setShowSystemObjects(boolean showSystemObjects)
    {
        this.showSystemObjects = showSystemObjects;
    }

    @Override
    public boolean isShowUtilityObjects() {
        return showUtilityObjects;
    }

    public void setShowUtilityObjects(boolean showUtilityObjects) {
        this.showUtilityObjects = showUtilityObjects;
    }

    @Override
    public boolean isConnectionReadOnly()
    {
        return connectionReadOnly;
    }

    public void setConnectionReadOnly(boolean connectionReadOnly)
    {
        this.connectionReadOnly = connectionReadOnly;
    }

    @Override
    public boolean isDefaultAutoCommit()
    {
        if (connectionInfo.getBootstrap().getDefaultAutoCommit() != null) {
            return connectionInfo.getBootstrap().getDefaultAutoCommit();
        } else {
            return getConnectionConfiguration().getConnectionType().isAutocommit();
        }
    }

    @Override
    public void setDefaultAutoCommit(final boolean autoCommit, @Nullable DBCExecutionContext updateContext, boolean updateConnection, @Nullable final Runnable onFinish) throws DBException {
        if (updateContext != null) {
            final DBCTransactionManager txnManager = DBUtils.getTransactionManager(updateContext);
            if (updateConnection && txnManager != null) {
                TasksJob.runTask("Set auto-commit mode", new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                        try {
                            // Change auto-commit mode
                            txnManager.setAutoCommit(monitor, autoCommit);
                        } catch (DBCException e) {
                            throw new InvocationTargetException(e);
                        } finally {
                            monitor.done();
                            if (onFinish != null) {
                                onFinish.run();
                            }
                        }
                    }
                });
            }
        }
        // Save in preferences
        if (autoCommit == getConnectionConfiguration().getConnectionType().isAutocommit()) {
            connectionInfo.getBootstrap().setDefaultAutoCommit(null);
        } else {
            connectionInfo.getBootstrap().setDefaultAutoCommit(autoCommit);
        }
    }

    @Nullable
    @Override
    public DBPTransactionIsolation getActiveTransactionsIsolation()
    {
        if (dataSource != null) {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(dataSource.getDefaultContext(false));
            if (txnManager != null) {
                try {
                    return txnManager.getTransactionIsolation();
                } catch (DBCException e) {
                    log.debug("Can't determine isolation level", e);
                    return null;
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
    public void setDefaultTransactionsIsolation(@Nullable final DBPTransactionIsolation isolationLevel) throws DBException {
        if (isolationLevel == null) {
            connectionInfo.getBootstrap().setDefaultTransactionIsolation(null);
        } else {
            connectionInfo.getBootstrap().setDefaultTransactionIsolation(isolationLevel.getCode());
            if (dataSource != null) {
                TasksJob.runTask("Set transactions isolation level", new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        DBCTransactionManager txnManager = DBUtils.getTransactionManager(dataSource.getDefaultContext(false));
                        if (txnManager != null) {
                            try {
                                if (!txnManager.getTransactionIsolation().equals(isolationLevel)) {
                                    txnManager.setTransactionIsolation(monitor, isolationLevel);
                                }
                            } catch (DBCException e) {
                                throw new InvocationTargetException(e);
                            }
                        }
                    }
                });
            }
        }
    }

    public void setDefaultActiveObject(String defaultActiveObject) {
        connectionInfo.getBootstrap().setDefaultObjectName(defaultActiveObject);
    }

    public Collection<FilterMapping> getObjectFilters()
    {
        return filterMap.values();
    }

    @Nullable
    @Override
    public DBSObjectFilter getObjectFilter(Class<?> type, @Nullable DBSObject parentObject, boolean firstMatch)
    {
        FilterMapping filterMapping = getFilterMapping(type, parentObject, firstMatch);
        if (filterMapping != null) {
            return filterMapping.getFilter(parentObject, firstMatch);
        }
        return null;
    }

    @Nullable
    private FilterMapping getFilterMapping(Class<?> type, @Nullable DBSObject parentObject, boolean firstMatch)
    {
        if (filterMap.isEmpty()) {
            return null;
        }
        // Test all super classes
        for (Class<?> testType = type; testType != null; testType = testType.getSuperclass()) {
            FilterMapping filterMapping = getTypeFilterMapping(parentObject, firstMatch, testType);
            if (filterMapping != null) {
                return filterMapping;
            }
        }
        for (Class<?> testType : type.getInterfaces()) {
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
            if (filter != null && (firstMatch || !filter.isNotApplicable())) {
                return filterMapping;
            }
        }
        return null;
    }

    @Override
    public void setObjectFilter(Class<?> type, DBSObject parentObject, DBSObjectFilter filter)
    {
        FilterMapping filterMapping = getFilterMapping(type, parentObject, true);
        if (filterMapping != null) {
            // Update filter
            if (parentObject == null) {
                filterMapping.defaultFilter = filter;
            } else {
                filterMapping.customFilters.put(DBUtils.getObjectUniqueName(parentObject), filter);
            }
        }

        updateObjectFilter(type.getName(), parentObject == null ? null : DBUtils.getObjectUniqueName(parentObject), filter);
    }

    void clearFilters() {
        filterMap.clear();
    }

    void updateObjectFilter(String typeName, @Nullable String objectID, DBSObjectFilter filter)
    {
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
    public DBVModel getVirtualModel()
    {
        return virtualModel;
    }

    @Override
    public DBPClientHome getClientHome()
    {
        if (clientHome == null && !CommonUtils.isEmpty(connectionInfo.getClientHomeId())) {
            this.clientHome = driver.getClientHome(connectionInfo.getClientHomeId());
        }
        return clientHome;
    }

    @Override
    public DBWNetworkHandler[] getActiveNetworkHandlers() {
        if (tunnel == null) {
            return new DBWNetworkHandler[0];
        }
        return new DBWNetworkHandler[] { tunnel };
    }

    @NotNull
    DataSourceOrigin getOrigin() {
        return origin;
    }

    @Override
    public boolean isProvided() {
        return !origin.isDefault();
    }

    @Override
    public boolean isTemporary() {
        return temporary;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    @Override
    public DBSObject getParentObject()
    {
        return null;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
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

    public void setDescription(@Nullable String description)
    {
        this.description = description;
    }

    public Date getConnectTime() {
        return connectTime;
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
    public DBPDataSource getDataSource()
    {
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
    public boolean isPersisted()
    {
        return true;
    }

    @NotNull
    @Override
    public DBPDataSourceRegistry getRegistry()
    {
        return registry;
    }

    @Override
    public void persistConfiguration()
    {
        registry.flushConfig();
    }

    @Override
    public boolean isConnected()
    {
        return dataSource != null;
    }

    @Override
    public void initConnection(DBRProgressMonitor monitor, DBRProgressListener onFinish) {
        DataSourceHandler.connectToDataSource(monitor, this, onFinish);
    }

    public boolean connect(DBRProgressMonitor monitor, boolean initialize, boolean reflect)
        throws DBException
    {
        if (connecting) {
            log.debug("Can't connect - connect/disconnect is in progress");
            return false;
        }
        if (this.isConnected()) {
            log.debug("Can't connect - already connected");
            return false;
        }
        log.debug("Connect with '" + getName() + "' (" + getId() + ")");

        //final String oldName = getConnectionConfiguration().getUserName();
        //final String oldPassword = getConnectionConfiguration().getUserPassword();
        if (!isSavePassword()) {
            // Ask for password
            if (!DataSourceHandler.askForPassword(this, null, false)) {
                DataSourceHandler.updateDataSourceObject(this);
                return false;
            }
        }

        processEvents(monitor, DBPConnectionEventType.BEFORE_CONNECT);

        connecting = true;
        tunnelConnectionInfo = null;
        try {
            // Handle tunnel
            // Open tunnel and replace connection info with new one
            this.tunnel = null;
            DBWHandlerConfiguration tunnelConfiguration = null;
            for (DBWHandlerConfiguration handler : connectionInfo.getDeclaredHandlers()) {
                if (handler.isEnabled() && handler.getType() == DBWHandlerType.TUNNEL) {
                    tunnelConfiguration = handler;
                    break;
                }
            }
            monitor.beginTask("Connect to " + getName(), tunnelConfiguration != null ? 3 : 2);

            if (tunnelConfiguration != null) {
                monitor.subTask("Initialize tunnel");
                tunnel = tunnelConfiguration.createHandler(DBWTunnel.class);
                try {
                    if (!tunnelConfiguration.isSavePassword()) {
                        DBWTunnel.AuthCredentials rc = tunnel.getRequiredCredentials(tunnelConfiguration);
                        if (!DataSourceHandler.askForPassword(this, tunnelConfiguration, rc == DBWTunnel.AuthCredentials.PASSWORD)) {
                            DataSourceHandler.updateDataSourceObject(this);
                            tunnel = null;
                            return false;
                        }
                    }

/*
                    for (DBWHandlerConfiguration handler : getConnectionConfiguration().getDeclaredHandlers()) {
                        if (handler.isEnabled() && handler.isSecured() && !handler.isSavePassword()) {
                            if (!DataSourceHandler.askForPassword(this, handler)) {
                                DataSourceHandler.updateDataSourceObject(this);
                                return false;
                            }
                        }
                    }
*/

                    tunnelConnectionInfo = tunnel.initializeTunnel(monitor, registry.getPlatform(), tunnelConfiguration, connectionInfo);
                } catch (Exception e) {
                    throw new DBCException("Can't initialize tunnel", e);
                }
                monitor.worked(1);
            }

            monitor.subTask("Connect to data source");
            dataSource = getDriver().getDataSourceProvider().openDataSource(monitor, this);
            monitor.worked(1);

            if (initialize) {
                monitor.subTask("Initialize data source");
                try {
                    dataSource.initialize(monitor);
                } catch (Throwable e) {
                    log.error("Error initializing datasource", e);
                }

                // Change connection properties
                initConnectionState(monitor);
            }

            connectFailed = false;
            connectTime = new Date();

            processEvents(monitor, DBPConnectionEventType.AFTER_CONNECT);

            if (reflect) {
                getRegistry().notifyDataSourceListeners(new DBPEvent(
                    DBPEvent.Action.OBJECT_UPDATE,
                    DataSourceDescriptor.this,
                    true));
            }
            try {
                log.debug("Connected (" + getId() + ", " + getPropertyDriver() + ")");
            } catch (Throwable e) {
                log.debug("Connected (" + getId() + ", driver unknown)");
            }
            return true;
        } catch (Exception e) {
            log.debug("Connection failed (" + getId() + ")");
            if (tunnel != null) {
                try {
                    tunnel.closeTunnel(monitor);
                } catch (IOException e1) {
                    log.error("Error closing tunnel", e);
                } finally {
                    tunnel = null;
                    tunnelConnectionInfo = null;
                }
            }
            // Failed
            connectFailed = true;
            //if (reflect) {
                getRegistry().notifyDataSourceListeners(new DBPEvent(
                    DBPEvent.Action.OBJECT_UPDATE,
                    DataSourceDescriptor.this,
                    false));
            //}
            if (e instanceof DBException) {
                throw (DBException)e;
            } else {
                throw new DBException("Internal error connecting to " + getName(), e);
            }
        } finally {
            monitor.done();
            connecting = false;
        }
    }

    private void initConnectionState(DBRProgressMonitor monitor) throws DBException {
        if (dataSource == null) {
            return;
        }

        // Set active object
        if (dataSource instanceof DBSObjectSelector && dataSource instanceof DBSObjectContainer) {
            String activeObject = getConnectionConfiguration().getBootstrap().getDefaultObjectName();
            if (!CommonUtils.isEmptyTrimmed(activeObject)) {
                DBSObject child = ((DBSObjectContainer) dataSource).getChild(monitor, activeObject);
                if (child != null) {
                    try {
                        ((DBSObjectSelector) dataSource).setDefaultObject(monitor, child);
                    } catch (DBException e) {
                        log.warn("Can't select active object", e);
                    }
                } else {
                    log.debug("Object '" + activeObject + "' not found");
                }
            }
        }

    }

    private void processEvents(DBRProgressMonitor monitor, DBPConnectionEventType eventType)
    {
        DBPConnectionConfiguration info = getActualConnectionConfiguration();
        DBRShellCommand command = info.getEvent(eventType);
        if (command != null && command.isEnabled()) {
            final DBRProcessDescriptor processDescriptor = new DBRProcessDescriptor(command, getVariablesResolver());

            monitor.subTask("Execute process " + processDescriptor.getName());
            DBUserInterface.getInstance().executeProcess(processDescriptor);

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
    }

    @Override
    public boolean disconnect(final DBRProgressMonitor monitor)
        throws DBException
    {
        return disconnect(monitor, true);
    }

    public boolean disconnect(final DBRProgressMonitor monitor, boolean reflect)
        throws DBException
    {
        if (dataSource == null) {
            log.error("Datasource is not connected");
            return true;
        }
        if (connecting) {
            log.error("Connect/disconnect is in progress");
            return false;
        }

        connecting = true;
        try {
            releaseDataSourceUsers(monitor);

            monitor.beginTask("Disconnect from '" + getName() + "'", 5 + dataSource.getAllContexts().length);

            processEvents(monitor, DBPConnectionEventType.BEFORE_DISCONNECT);

            monitor.worked(1);

            // Close datasource
            monitor.subTask("Close connection");
            if (dataSource != null) {
                dataSource.shutdown(monitor);
            }
            monitor.worked(1);

            // Close tunnel
            if (tunnel != null) {
                monitor.subTask("Close tunnel");
                try {
                    tunnel.closeTunnel(monitor);
                } catch (Throwable e) {
                    log.error("Error closing tunnel", e);
                }
            }
            monitor.worked(1);

            processEvents(monitor, DBPConnectionEventType.AFTER_DISCONNECT);

            monitor.worked(1);

            monitor.done();

            // Terminate child processes
            synchronized (childProcesses) {
                for (Iterator<DBRProcessDescriptor> iter = childProcesses.iterator(); iter.hasNext(); ) {
                    DBRProcessDescriptor process = iter.next();
                    if (process.isRunning() && process.getCommand().isTerminateAtDisconnect()) {
                        process.terminate();
                    }
                    iter.remove();
                }
            }

            dataSource = null;
            connectTime = null;

            if (reflect) {
                // Reflect UI
                getRegistry().notifyDataSourceListeners(new DBPEvent(
                    DBPEvent.Action.OBJECT_UPDATE,
                    this,
                    false));
            }

            return true;
        } finally {
            connecting = false;
            log.debug("Disconnected (" + getId() + ")");
        }
    }

    private void releaseDataSourceUsers(DBRProgressMonitor monitor) {
        List<DBPDataSourceUser> usersStamp;
        synchronized (users) {
            usersStamp = new ArrayList<>(users);
        }

        int jobCount = 0;
        // Save all unsaved data
        for (DBPDataSourceUser user : usersStamp) {
            if (user instanceof Job) {
                jobCount++;
            }
            if (user instanceof DBPDataSourceHandler) {
                ((DBPDataSourceHandler) user).beforeDisconnect();
            }
        }
        if (jobCount > 0) {
            monitor.beginTask("Waiting for all active tasks to finish", jobCount);
            // Stop all jobs
            for (DBPDataSourceUser user : usersStamp) {
                if (user instanceof Job) {
                    Job job = (Job) user;
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
        throws DBException
    {
        return reconnect(monitor, true);
    }

    public boolean reconnect(final DBRProgressMonitor monitor, boolean reflect)
        throws DBException
    {
        if (connecting) {
            log.debug("Can't reconnect - connect/disconnect is in progress");
            return false;
        }
        if (isConnected()) {
            if (!disconnect(monitor, reflect)) {
                return false;
            }
        }
        return connect(monitor, true, reflect);
    }

    @Override
    public Collection<DBPDataSourceUser> getUsers()
    {
        synchronized (users) {
            return new ArrayList<>(users);
        }
    }

    @Override
    public void acquire(DBPDataSourceUser user)
    {
        synchronized (users) {
            if (users.contains(user)) {
                log.warn("Datasource user '" + user + "' already registered in datasource '" + getName() + "'");
            } else {
                users.add(user);
            }
        }
    }

    @Override
    public void release(DBPDataSourceUser user)
    {
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
    public DBDDataFormatterProfile getDataFormatterProfile()
    {
        if (this.formatterProfile == null) {
            this.formatterProfile = new DataFormatterProfile(getId(), preferenceStore);
        }
        return this.formatterProfile;
    }

    @Override
    public void setDataFormatterProfile(DBDDataFormatterProfile formatterProfile)
    {
        this.formatterProfile = formatterProfile;
    }

    @NotNull
    @Override
    public DBDValueHandler getDefaultValueHandler()
    {
        if (dataSource instanceof DBDPreferences) {
            return ((DBDPreferences) dataSource).getDefaultValueHandler();
        }
        return DefaultValueHandler.INSTANCE;
    }

    @NotNull
    @Override
    public DataSourcePreferenceStore getPreferenceStore()
    {
        return preferenceStore;
    }

    public void resetPassword()
    {
        connectionInfo.setUserPassword(null);
    }

    @Nullable
    @Override
    public <T> T getAdapter(Class<T> adapter)
    {
        if (DBPDataSourceContainer.class.isAssignableFrom(adapter)) {
            return adapter.cast(this);
        } else if (adapter == DBPPropertySource.class) {
            PropertyCollector coll = new PropertyCollector(this, true);
            coll.collectProperties();
            if (dataSource != null) {
                int conIndex = 0;
                for (DBCExecutionContext context : dataSource.getAllContexts()) {
                    conIndex++;
                    coll.addProperty("Connections", conIndex, String.valueOf(conIndex), new ContextInfo(context));
                }
            }
            return adapter.cast(coll);
        }
        return null;
    }

    @Override
    @NotNull
    public DBPImage getObjectImage()
    {
        return driver.getPlainIcon();
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState()
    {
        if (isConnected()) {
            return DBSObjectState.ACTIVE;
        } else if (connectFailed) {
            return DBSObjectState.INVALID;
        } else {
            return DBSObjectState.NORMAL;
        }
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor)
    {
        // just do nothing
    }

    public static String generateNewId(DriverDescriptor driver)
    {
        long rnd = new Random().nextLong();
        if (rnd < 0) rnd = -rnd;
        return driver.getId() + "-" + Long.toHexString(System.currentTimeMillis()) + "-" + Long.toHexString(rnd);
    }

    @Property(viewable = true, order = 20, category = "Driver")
    public String getPropertyDriverType()
    {
        return driver.getName();
    }

    @Property(order = 3, category = "Server")
    public String getPropertyAddress()
    {
        StringBuilder addr = new StringBuilder();
        if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
            addr.append(connectionInfo.getHostName());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            addr.append(':').append(connectionInfo.getHostPort());
        }
        return addr.toString();
    }

    @Property(order = 4, category = "Server")
    public String getPropertyDatabase()
    {
        return connectionInfo.getDatabaseName();
    }

    @Property(order = 5, category = "Server")
    public String getPropertyURL()
    {
        return connectionInfo.getUrl();
    }

    @Nullable
    @Property(order = 6, category = "Server")
    public String getPropertyServerName()
    {
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
    @Property(order = 21, category = "Driver")
    public String getPropertyDriver()
    {
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
    public String getPropertyConnectTime()
    {
        if (connectTime != null) {
            return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(connectTime);
        }
        return null;
    }

    @Property(order = 9)
    public String getPropertyConnectType()
    {
        return connectionInfo.getConnectionType().getName();
    }

    public void addChildProcess(DBRProcessDescriptor process)
    {
        synchronized (childProcesses) {
            childProcesses.add(process);
        }
    }

    public void copyFrom(DataSourceDescriptor descriptor) {
        filterMap.clear();
        for (FilterMapping mapping : descriptor.getObjectFilters()) {
            filterMap.put(mapping.typeName, new FilterMapping(mapping));
        }
        virtualModel.copyFrom(descriptor.getVirtualModel());

        setDescription(descriptor.getDescription());
        setSavePassword(descriptor.isSavePassword());
        setShowSystemObjects(descriptor.isShowSystemObjects());
        setShowUtilityObjects(descriptor.isShowUtilityObjects());
        setConnectionReadOnly(descriptor.isConnectionReadOnly());
    }

    @Override
    @NotNull
    public ISecurePreferences getSecurePreferences() {
        return registry.getSecurePreferences().node(id);
    }

    @Override
    public String toString() {
        return name + " [" + driver + "]";
    }

    public boolean equalSettings(Object obj) {
        if (!(obj instanceof DataSourceDescriptor)) {
            return false;
        }
        DataSourceDescriptor source = (DataSourceDescriptor) obj;
        return 
            CommonUtils.equalObjects(this.name, source.name) &&
            CommonUtils.equalObjects(this.description, source.description) &&
            CommonUtils.equalObjects(this.savePassword, source.savePassword) &&
            CommonUtils.equalObjects(this.showSystemObjects, source.showSystemObjects) &&
            CommonUtils.equalObjects(this.showUtilityObjects, source.showUtilityObjects) &&
            CommonUtils.equalObjects(this.connectionReadOnly, source.connectionReadOnly) &&
            CommonUtils.equalObjects(this.driver, source.driver) &&
            CommonUtils.equalObjects(this.connectionInfo, source.connectionInfo) &&
            CommonUtils.equalObjects(this.filterMap, source.filterMap) &&
            CommonUtils.equalObjects(this.formatterProfile, source.formatterProfile) &&
            CommonUtils.equalObjects(this.clientHome, source.clientHome) &&
            CommonUtils.equalObjects(this.lockPasswordHash, source.lockPasswordHash) &&
            CommonUtils.equalObjects(this.folder, source.folder);
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
    public GeneralUtils.IVariableResolver getVariablesResolver() {
        return new GeneralUtils.IVariableResolver() {
            @Override
            public String get(String name) {
                String propValue = getActualConnectionConfiguration().getProperties().get(name);
                if (propValue != null) {
                    return propValue;
                }

                name = name.toLowerCase(Locale.ENGLISH);
                switch (name) {
                    case RegistryConstants.VARIABLE_HOST: return getActualConnectionConfiguration().getHostName();
                    case RegistryConstants.VARIABLE_PORT: return getActualConnectionConfiguration().getHostPort();
                    case RegistryConstants.VARIABLE_SERVER: return getActualConnectionConfiguration().getServerName();
                    case RegistryConstants.VARIABLE_DATABASE: return getActualConnectionConfiguration().getDatabaseName();
                    case RegistryConstants.VARIABLE_USER: return getActualConnectionConfiguration().getUserName();
                    case RegistryConstants.VARIABLE_PASSWORD: return getActualConnectionConfiguration().getUserPassword();
                    case RegistryConstants.VARIABLE_URL: return getActualConnectionConfiguration().getUrl();
                    default: return SystemVariablesResolver.INSTANCE.get(name);
                }
            }
        };
    }
}
