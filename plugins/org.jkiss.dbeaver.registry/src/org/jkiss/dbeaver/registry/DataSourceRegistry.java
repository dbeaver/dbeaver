/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.access.DBAAuthProfile;
import org.jkiss.dbeaver.model.access.DBACredentialsProvider;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.connection.DBPAuthModelDescriptor;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderRegistry;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.model.runtime.*;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.virtual.DBVModel;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.resource.DBeaverNature;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DataSourceRegistry implements DBPDataSourceRegistry, DataSourcePersistentRegistry {
    @Deprecated
    public static final String DEFAULT_AUTO_COMMIT = "default.autocommit"; //$NON-NLS-1$
    @Deprecated
    public static final String DEFAULT_ISOLATION = "default.isolation"; //$NON-NLS-1$
    @Deprecated
    public static final String DEFAULT_ACTIVE_OBJECT = "default.activeObject"; //$NON-NLS-1$

    private static final long DISCONNECT_ALL_TIMEOUT = 5000;

    private static final Log log = Log.getLog(DataSourceRegistry.class);

    public static final String OLD_CONFIG_FILE_NAME = "data-sources.xml"; //$NON-NLS-1$

    private final DBPProject project;
    private final DataSourceConfigurationManager configurationManager;

    private final List<DBPDataSourceConfigurationStorage> storages = new ArrayList<>();
    private final Map<String, DataSourceDescriptor> dataSources = new LinkedHashMap<>();
    private final List<DBPEventListener> dataSourceListeners = new ArrayList<>();
    private final List<DataSourceFolder> dataSourceFolders = new ArrayList<>();
    private final List<DBSObjectFilter> savedFilters = new ArrayList<>();
    private final List<DBWNetworkProfile> networkProfiles = new ArrayList<>();
    private final Map<String, DBAAuthProfile> authProfiles = new LinkedHashMap<>();
    private volatile boolean saveInProgress = false;

    private final DBVModel.ModelChangeListener modelChangeListener = new DBVModel.ModelChangeListener();
    private volatile ConfigSaver configSaver;
    private DBACredentialsProvider authCredentialsProvider;
    protected Throwable lastError;

    public DataSourceRegistry(DBPProject project) {
        this(project, new DataSourceConfigurationManagerNIO(project));
    }

    public DataSourceRegistry(@NotNull DBPProject project, DataSourceConfigurationManager configurationManager) {
        this.project = project;
        this.configurationManager = configurationManager;

        loadDataSources(true);

        if (!isMultiUser()) {
            DataSourceProviderRegistry.getInstance().fireRegistryChange(this, true);

            addDataSourceListener(modelChangeListener);
        }
    }

    // Multi-user registry:
    // - doesn't register listeners
    // -
    private boolean isMultiUser() {
        return DBWorkbench.getPlatform().getApplication().isMultiuser();
    }

    @Override
    public void dispose() {
        if (!isMultiUser()) {
            removeDataSourceListener(modelChangeListener);
            DataSourceProviderRegistry.getInstance().fireRegistryChange(this, false);
        }
        synchronized (dataSourceListeners) {
            if (!this.dataSourceListeners.isEmpty()) {
                log.warn("Some data source listeners are still registered: " + dataSourceListeners);
            }
            this.dataSourceListeners.clear();
        }
        // Disconnect in 5 seconds or die
        closeConnections(DISCONNECT_ALL_TIMEOUT);
        // Do not save config on shutdown.
        // Some data source might be broken due to misconfiguration
        // and we don't want to lose their config just after restart
//        if (getProjectNode().isOpen()) {
//            flushConfig();
//        }
        // Dispose and clear all descriptors
        synchronized (dataSources) {
            for (DataSourceDescriptor dataSourceDescriptor : this.dataSources.values()) {
                dataSourceDescriptor.dispose();
            }
            this.dataSources.clear();
        }
    }

    private void closeConnections(long waitTime) {
        boolean hasConnections = false;
        synchronized (dataSources) {
            for (DataSourceDescriptor dataSource : dataSources.values()) {
                if (dataSource.isConnected()) {
                    hasConnections = true;
                    break;
                }
            }
        }
        if (!hasConnections) {
            return;
        }
        final DisconnectTask disconnectTask = new DisconnectTask();
        if (!RuntimeUtils.runTask(disconnectTask, "Disconnect from data sources", waitTime)) {
            log.warn("Some data source connections wasn't closed on shutdown in " + waitTime + "ms. Probably network timeout occurred.");
        }
    }

    @NotNull
    DBPDataSourceConfigurationStorage getDefaultStorage() {
        synchronized (storages) {
            for (DBPDataSourceConfigurationStorage storage : storages) {
                if (storage.isDefault()) {
                    return storage;
                }
            }
            List<DBPDataSourceConfigurationStorage> storages = getConfigurationManager().getConfigurationStorages();
            for (DBPDataSourceConfigurationStorage storage : storages) {
                if (storage.isDefault()) {
                    this.storages.add(storage);
                    return storage;
                }
            }
            // No default storage. Seems to be an internal error
            log.warn("no default storage in registry " + this);
            try {
                java.nio.file.Path configPath = this.getProject().getMetadataFolder(false).resolve(MODERN_CONFIG_FILE_NAME);
                Files.createFile(configPath);
                DBPDataSourceConfigurationStorage defaultStorage = new DataSourceFileStorage(configPath, false, true);
                this.storages.add(defaultStorage);
                return defaultStorage;
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create a default storage in registry " + this, e);
            }
        }
    }

    ////////////////////////////////////////////////////
    // Data sources

    @Nullable
    @Override
    public DataSourceDescriptor getDataSource(String id) {
        synchronized (dataSources) {
            return dataSources.get(id);
        }
    }

    @Nullable
    @Override
    public DataSourceDescriptor getDataSource(DBPDataSource dataSource) {
        synchronized (dataSources) {
            for (DataSourceDescriptor dsd : dataSources.values()) {
                if (dsd.getDataSource() == dataSource) {
                    return dsd;
                }
            }
        }
        return null;
    }

    @Nullable
    @Override
    public DataSourceDescriptor findDataSourceByName(String name) {
        synchronized (dataSources) {
            for (DataSourceDescriptor dsd : dataSources.values()) {
                if (!dsd.isHidden() && dsd.getName().equals(name)) {
                    return dsd;
                }
            }
        }
        return null;
    }

    @NotNull
    @Override
    public List<? extends DBPDataSourceContainer> getDataSourcesByProfile(@NotNull DBWNetworkProfile profile) {
        List<DataSourceDescriptor> dsCopy;
        synchronized (dataSources) {
            dsCopy = CommonUtils.copyList(dataSources.values());
        }
        dsCopy.removeIf(ds -> !CommonUtils.equalObjects(ds.getConnectionConfiguration().getConfigProfileName(), profile.getProfileName()));
        return dsCopy;
    }

    @NotNull
    @Override
    public List<DataSourceDescriptor> getDataSources() {
        List<DataSourceDescriptor> dsCopy;
        synchronized (dataSources) {
            dsCopy = CommonUtils.copyList(dataSources.values());
        }
        dsCopy.sort((o1, o2) -> CommonUtils.notNull(o1.getName(), o1.getId()).compareToIgnoreCase(
            CommonUtils.notNull(o2.getName(), o2.getId())));
        return dsCopy;
    }

    @NotNull
    @Override
    public DBPDataSourceContainer createDataSource(DBPDriver driver, DBPConnectionConfiguration connConfig) {
        return new DataSourceDescriptor(this, DataSourceDescriptor.generateNewId(driver), driver, connConfig);
    }

    @NotNull
    @Override
    public DBPDataSourceContainer createDataSource(DBPDataSourceContainer source) {
        DataSourceDescriptor newDS = new DataSourceDescriptor((DataSourceDescriptor) source, this);
        newDS.setId(DataSourceDescriptor.generateNewId(source.getDriver()));
        return newDS;
    }

    @NotNull
    @Override
    public List<DataSourceFolder> getAllFolders() {
        return dataSourceFolders;
    }

    @NotNull
    @Override
    public List<DataSourceFolder> getRootFolders() {
        List<DataSourceFolder> rootFolders = new ArrayList<>();
        for (DataSourceFolder folder : dataSourceFolders) {
            if (folder.getParent() == null) {
                rootFolders.add(folder);
            }
        }
        return rootFolders;
    }

    @Override
    public DataSourceFolder addFolder(DBPDataSourceFolder parent, String name) {
        DataSourceFolder folder = new DataSourceFolder(this, (DataSourceFolder) parent, name, null);
        dataSourceFolders.add(folder);
        return folder;
    }

    @Override
    public void removeFolder(DBPDataSourceFolder folder, boolean dropContents) {
        final DataSourceFolder folderImpl = (DataSourceFolder) folder;
        final String folderPath = folder.getFolderPath();

        for (DataSourceFolder child : folderImpl.getChildren()) {
            removeFolder(child, dropContents);
        }
        dataSourceFolders.remove(folderImpl);

        final DBPDataSourceFolder parent = folder.getParent();
        if (parent != null) {
            folderImpl.setParent(null);
        }
        for (DataSourceDescriptor ds : dataSources.values()) {
            if (ds.getFolder() == folder) {
                if (dropContents) {
                    removeDataSource(ds);
                } else {
                    ds.setFolder(parent);
                }
            }
        }
        persistDataFolderDelete(folderPath, dropContents);
    }

    @Override
    public void moveFolder(@NotNull String oldPath, @NotNull String newPath) {
        DBPDataSourceFolder folder = getFolder(oldPath);
        var result = Path.of(newPath);
        var newName = result.getFileName().toString();
        var parent = result.getParent();
        var parentFolder = parent == null ? null : getFolder(parent.toString().replace("\\", "/"));
        folder.setParent(parentFolder);
        if (!CommonUtils.equalObjects(folder.getName(), newName)) {
            folder.setName(newName);
        }
    }

    private DataSourceFolder findRootFolder(String name) {
        for (DataSourceFolder root : getRootFolders()) {
            if (root.getName().equals(name)) {
                return root;
            }
        }
        return null;
    }

    @Override
    public DBPDataSourceFolder getFolder(String path) {
        return findFolderByPath(path, true, null);
    }

    DataSourceFolder findFolderByPath(String path, boolean create, ParseResults results) {
        DataSourceFolder parent = null;
        for (String name : path.split("/")) {
            DataSourceFolder folder = parent == null ? findRootFolder(name) : parent.getChild(name);
            if (folder == null) {
                if (!create) {
                    log.warn("Folder '" + path + "' not found");
                    break;
                } else {
                    folder = addFolder(parent, name);
                }
            }
            parent = folder;
            if (results != null) {
                results.updatedFolders.add(parent);
            }
        }
        return parent;
    }

    void addDataSourceFolder(DataSourceFolder folder) {
        if (dataSourceFolders.contains(folder)) {
            return;
        }
        dataSourceFolders.add(folder);
    }

    ////////////////////////////////////////////////////
    // Saved filters

    @Nullable
    @Override
    public DBSObjectFilter getSavedFilter(String name) {
        for (DBSObjectFilter filter : savedFilters) {
            if (CommonUtils.equalObjects(filter.getName(), name)) {
                return filter;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public List<DBSObjectFilter> getSavedFilters() {
        return savedFilters;
    }

    @Override
    public void updateSavedFilter(DBSObjectFilter filter) {
        DBSObjectFilter filterCopy = new DBSObjectFilter(filter);
        for (int i = 0; i < savedFilters.size(); i++) {
            if (CommonUtils.equalObjects(savedFilters.get(i).getName(), filter.getName())) {
                savedFilters.set(i, filterCopy);
                return;
            }
        }
        savedFilters.add(filterCopy);
    }

    @Override
    public void removeSavedFilter(String filterName) {
        for (int i = 0; i < savedFilters.size(); ) {
            if (CommonUtils.equalObjects(savedFilters.get(i).getName(), filterName)) {
                savedFilters.remove(i);
            } else {
                i++;
            }
        }
    }

    void addSavedFilter(DBSObjectFilter filter) {
        savedFilters.add(filter);
    }

    ////////////////////////////////////////////////////
    // Config profiles

    @Nullable
    @Override
    public DBWNetworkProfile getNetworkProfile(String name) {
        synchronized (networkProfiles) {
            return networkProfiles.stream().filter(profile -> CommonUtils.equalObjects(profile.getProfileName(), name)).findFirst().orElse(null);
        }
    }

    @NotNull
    @Override
    public List<DBWNetworkProfile> getNetworkProfiles() {
        return networkProfiles;
    }

    @Override
    public void updateNetworkProfile(DBWNetworkProfile profile) {
        for (int i = 0; i < networkProfiles.size(); i++) {
            if (CommonUtils.equalObjects(networkProfiles.get(i).getProfileName(), profile.getProfileName())) {
                networkProfiles.set(i, profile);
                return;
            }
        }
        networkProfiles.add(profile);
    }

    @Override
    public void removeNetworkProfile(DBWNetworkProfile profile) {
        try {
            DBSSecretController secretController = DBSSecretController.getProjectSecretController(getProject());
            secretController.setSecretValue(
                profile.getSecretKeyId(),
                null);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Secret remove error", "Error removing network profile credentials from secret storage", e);
        }
        networkProfiles.remove(profile);
    }

    ////////////////////////////////////////////////////
    // Auth profiles

    @Nullable
    @Override
    public DBAAuthProfile getAuthProfile(String id) {
        synchronized (authProfiles) {
            return authProfiles.get(id);
        }
    }

    @NotNull
    @Override
    public List<DBAAuthProfile> getAllAuthProfiles() {
        synchronized (authProfiles) {
            return new ArrayList<>(authProfiles.values());
        }
    }

    @NotNull
    @Override
    public List<DBAAuthProfile> getApplicableAuthProfiles(@Nullable DBPDriver driver) {
        DBPDataSourceProviderRegistry dspRegistry = DBWorkbench.getPlatform().getDataSourceProviderRegistry();
        synchronized (authProfiles) {
            return authProfiles.values().stream().filter(p -> {
                DBPAuthModelDescriptor authModel = dspRegistry.getAuthModel(p.getAuthModelId());
                return authModel != null && authModel.isApplicableTo(driver);
            }).collect(Collectors.toList());
        }
    }

    @Override
    public void updateAuthProfile(DBAAuthProfile profile) {
        synchronized (authProfiles) {
            authProfiles.put(profile.getProfileId(), profile);
        }
    }

    @Override
    public void removeAuthProfile(DBAAuthProfile profile) {
        // Remove secrets
        if (getProject().isUseSecretStorage()) {
            try {
                DBSSecretController secretController = DBSSecretController.getProjectSecretController(getProject());
                secretController.setSecretValue(
                    profile.getSecretKeyId(),
                    null);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Secret remove error", "Error removing auth profile credentials from secret storage", e);
            }
        }
        synchronized (authProfiles) {
            authProfiles.remove(profile.getProfileId());
        }
    }

    ////////////////////////////////////////////////////
    // Data sources

    public void addDataSource(@NotNull DBPDataSourceContainer dataSource) throws DBException {
        final DataSourceDescriptor descriptor = (DataSourceDescriptor) dataSource;
        addDataSourceToList(descriptor);
        descriptor.persistSecretIfNeeded(true);
        if (!descriptor.isDetached()) {
            persistDataSourceCreate(dataSource);
        }
        notifyDataSourceListeners(new DBPEvent(DBPEvent.Action.OBJECT_ADD, descriptor, true));
    }

    void addDataSourceToList(@NotNull DataSourceDescriptor descriptor) {
        synchronized (dataSources) {
            this.dataSources.put(descriptor.getId(), descriptor);
            DBPDataSourceConfigurationStorage storage = descriptor.getStorage();
            if (!storages.contains(storage) && !descriptor.isDetached()) {
                storages.add(storage);
            }
        }
    }

    public void removeDataSource(@NotNull DBPDataSourceContainer dataSource) {
        final DataSourceDescriptor descriptor = (DataSourceDescriptor) dataSource;
        synchronized (dataSources) {
            this.dataSources.remove(descriptor.getId());
        }
        if (!descriptor.isDetached()) {
            persistDataSourceDelete(dataSource);
        }
        try {
            descriptor.removeSecretIfNeeded();
        } catch (DBException e) {
            log.error("Error deleting old secrets", e);
        }
        try {
            this.fireDataSourceEvent(DBPEvent.Action.OBJECT_REMOVE, dataSource);
        } finally {
            descriptor.dispose();
        }
    }

    public void updateDataSource(@NotNull DBPDataSourceContainer dataSource) throws DBException {
        if (!(dataSource instanceof DataSourceDescriptor)) {
            return;
        }
        if (!dataSources.containsKey(dataSource.getId())) {
            addDataSource(dataSource);
        } else {
            if (!((DataSourceDescriptor) dataSource).isDetached()) {
                persistDataSourceUpdate(dataSource);
            }
            DataSourceDescriptor descriptor = (DataSourceDescriptor) dataSource;
            descriptor.persistSecretIfNeeded(false);
            this.fireDataSourceEvent(DBPEvent.Action.OBJECT_UPDATE, dataSource);
        }
    }

    protected void persistDataSourceCreate(@NotNull DBPDataSourceContainer container) {
        persistDataSourceUpdate(container);
    }

    protected void persistDataSourceUpdate(@NotNull DBPDataSourceContainer container) {
        saveDataSources();
    }

    protected void persistDataFolderDelete(@NotNull String folderPath, boolean dropContents) {
        saveDataSources();
    }

    protected void persistDataSourceDelete(@NotNull DBPDataSourceContainer container) {
        saveDataSources();
    }

    @Override
    public void flushConfig() {
        if (project.isInMemory()) {
            return;
        }
        // Use async config saver to avoid too frequent configuration re-save during some massive configuration update
        if (configSaver == null) {
            configSaver = new ConfigSaver();
        }
        configSaver.schedule(100);
    }

    @Override
    public void refreshConfig() {
        if (!saveInProgress) {
            this.loadDataSources(true);
        }
    }

    @Override
    public Throwable getLastError() {
        Throwable error = this.lastError;
        this.lastError = null;
        return error;
    }

    @Override
    public boolean hasError() {
        return this.lastError != null;
    }

    @Override
    public void addDataSourceListener(@NotNull DBPEventListener listener) {
        synchronized (dataSourceListeners) {
            dataSourceListeners.add(listener);
        }
    }

    @Override
    public boolean removeDataSourceListener(@NotNull DBPEventListener listener) {
        synchronized (dataSourceListeners) {
            return dataSourceListeners.remove(listener);
        }
    }

    private void fireDataSourceEvent(
        DBPEvent.Action action,
        DBSObject object) {
        notifyDataSourceListeners(new DBPEvent(action, object));
    }

    public void notifyDataSourceListeners(final DBPEvent event) {
        final List<DBPEventListener> listeners;
        synchronized (dataSourceListeners) {
            if (dataSourceListeners.isEmpty()) {
                return;
            }
            listeners = new ArrayList<>(dataSourceListeners);
        }
        new Job("Notify datasource events") {
            {
                setSystem(true);
            }

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                for (DBPEventListener listener : listeners) {
                    listener.handleDataSourceEvent(event);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    @Nullable
    @Override
    public DBACredentialsProvider getAuthCredentialsProvider() {
        return authCredentialsProvider;
    }

    @Override
    public void setAuthCredentialsProvider(DBACredentialsProvider authCredentialsProvider) {
        this.authCredentialsProvider = authCredentialsProvider;
    }


    public static List<DBPDataSourceContainer> getAllDataSources() {
        List<DBPDataSourceContainer> result = new ArrayList<>();
        DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
        for (DBPProject project : workspace.getProjects()) {
            if (project.isOpen() && project.isRegistryLoaded()) {
                result.addAll(project.getDataSourceRegistry().getDataSources());
            }
        }
        return result;
    }

    @Override
    public Set<DBPDataSourceFolder> getTemporaryFolders() {
        Set<DBPDataSourceFolder> result = new HashSet<>(Collections.emptySet());
        Set<DBPDataSourceFolder> folders = getDataSources().stream()
            .filter(DBPDataSourceContainer::isTemporary)
            .map(DBPDataSourceContainer::getFolder)
            .collect(Collectors.toSet());
        for (DBPDataSourceFolder folder : folders) {
            while (folder != null) {
                result.add(folder);
                folder = folder.getParent();
            }
        }
        return result;
    }

    private void loadDataSources(boolean refresh) {
        loadDataSources(configurationManager.getConfigurationStorages(), configurationManager, refresh, true);
    }

    @Override
    public boolean loadDataSources(
        @NotNull List<DBPDataSourceConfigurationStorage> storages,
        @NotNull DataSourceConfigurationManager manager,
        boolean refresh,
        boolean purgeUntouched
    ) {
        // need this to show is the data source was updated
        boolean configChanged = false;
        if (!project.isOpen() || project.isInMemory()) {
            return false;
        }
        // Clear filters before reload
        savedFilters.clear();

        // Parse datasources
        ParseResults parseResults = new ParseResults();
        // Modern way - search json configs in metadata folder
        for (DBPDataSourceConfigurationStorage cfgStorage : storages) {
            if (loadDataSources(cfgStorage, manager, false, parseResults)) {
                configChanged = true;
            }
        }

        // Reflect changes
        if (refresh) {
            for (DBPDataSourceContainer ds : parseResults.updatedDataSources) {
                fireDataSourceEvent(DBPEvent.Action.OBJECT_UPDATE, ds);
            }
            for (DBPDataSourceContainer ds : parseResults.addedDataSources) {
                addDataSourceToList((DataSourceDescriptor) ds);
                fireDataSourceEvent(DBPEvent.Action.OBJECT_ADD, ds);
            }
            for (DataSourceFolder folder : parseResults.addedFolders) {
                addDataSourceFolder(folder);
            }

            if (purgeUntouched) {
                List<DataSourceDescriptor> removedDataSource = new ArrayList<>();
                for (DataSourceDescriptor ds : dataSources.values()) {
                    if (!parseResults.addedDataSources.contains(ds) && !parseResults.updatedDataSources.contains(ds) &&
                        !ds.isProvided() && !ds.isExternallyProvided() && !ds.isDetached())
                    {
                        removedDataSource.add(ds);
                    }
                }
                for (DataSourceDescriptor ds : removedDataSource) {
                    this.dataSources.remove(ds.getId());
                    this.fireDataSourceEvent(DBPEvent.Action.OBJECT_REMOVE, ds);
                    ds.dispose();
                }

                List<DataSourceFolder> removedFolder = new ArrayList<>();
                for (DataSourceFolder folder : dataSourceFolders) {
                    if (!parseResults.addedFolders.contains(folder) && !parseResults.updatedFolders.contains(folder)) {
                        removedFolder.add(folder);
                    }
                }
                for (DataSourceFolder folder : removedFolder) {
                    if (!parseResults.addedFolders.contains(folder) && !parseResults.updatedFolders.contains(folder)) {
                        dataSourceFolders.remove(folder);
                        folder.setParent(null);
                    }
                }
            }
        }
        return configChanged;
    }

    private boolean loadDataSources(
        @NotNull DBPDataSourceConfigurationStorage storage,
        @NotNull DataSourceConfigurationManager manager,
        boolean refresh,
        @NotNull ParseResults parseResults
    ) {
        boolean configChanged = false;
        try {
            DataSourceSerializer serializer;
            if (storage instanceof DataSourceFileStorage && ((DataSourceFileStorage) storage).isLegacy()) {
                serializer = new DataSourceSerializerLegacy(this);
            } else {
                serializer = new DataSourceSerializerModern(this);
            }
            configChanged = serializer.parseDataSources(storage, manager, parseResults, refresh);
            updateProjectNature();
            lastError = null;
        } catch (Exception ex) {
            lastError = ex;
            log.error("Error loading datasource config from " + storage.getStorageId(), ex);
        }
        return configChanged;
    }

    @Override
    public void saveDataSources() {
        saveDataSources(new VoidProgressMonitor());
    }

    protected void saveDataSources(DBRProgressMonitor monitor) {
        if (project.isInMemory()) {
            return;
        }

        updateProjectNature();
        saveInProgress = true;
        try {
            for (DBPDataSourceConfigurationStorage storage : storages) {
                if (storage instanceof DataSourceFileStorage && ((DataSourceFileStorage) storage).isLegacy()) {
                    // Legacy storage. We must save it in the modern format
                    ((DataSourceFileStorage) storage).convertToModern(project);
                }

                List<DataSourceDescriptor> localDataSources = getDataSources(storage);

                try {
                    DataSourceSerializer serializer = new DataSourceSerializerModern(this);
                    serializer.saveDataSources(
                        monitor,
                        configurationManager,
                        storage,
                        localDataSources);
                    try {
                        if (project.isUseSecretStorage() && !configurationManager.isSecure()) {
                            DBSSecretController
                                .getProjectSecretController(project)
                                .flushChanges();
                        }
                        lastError = null;
                    } catch (Throwable e) {
                        log.error("Error saving secured preferences", e);
                        lastError = e;
                    }
                } catch (Exception ex) {
                    log.error("Error saving datasources configuration", ex);
                    lastError = ex;
                }
            }
        } finally {
            saveInProgress = false;
        }
    }

    private List<DataSourceDescriptor> getDataSources(DBPDataSourceConfigurationStorage storage) {
        List<DataSourceDescriptor> result = new ArrayList<>();
        synchronized (dataSources) {
            for (DataSourceDescriptor ds : dataSources.values()) {
                if (CommonUtils.equalObjects(ds.getStorage(), storage)) {
                    result.add(ds);
                }
            }
        }

        return result;
    }

    private void updateProjectNature() {
        if (isMultiUser()) {
            return;
        }
        try {
            IProject eclipseProject = project.getEclipseProject();
            if (eclipseProject != null) {
                final IProjectDescription description = eclipseProject.getDescription();
                if (description != null) {
                    String[] natureIds = description.getNatureIds();
                    if (!dataSources.isEmpty()) {
                        // Add nature
                        if (!ArrayUtils.contains(natureIds, DBeaverNature.NATURE_ID)) {
                            description.setNatureIds(ArrayUtils.add(String.class, natureIds, DBeaverNature.NATURE_ID));
                            try {
                                eclipseProject.setDescription(description, new NullProgressMonitor());
                            } catch (CoreException e) {
                                log.debug("Can't set project nature", e);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug(e);
        }
    }

    @Override
    public DBPProject getProject() {
        return project;
    }

    public DataSourceConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    @Override
    public String toString() {
        return project.getName() + " (" + getClass().getSimpleName() + ")";
    }

    public void saveConfigurationToManager(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DataSourceConfigurationManager configurationManager,
        @Nullable Predicate<DBPDataSourceContainer> filter
    ) {
        List<DataSourceDescriptor> localDataSources = getDataSources();
        if (filter != null) {
            localDataSources.removeIf(filter.negate());
        }

        try {
            DataSourceSerializer serializer = new DataSourceSerializerModern(this);
            serializer.saveDataSources(
                monitor,
                configurationManager,
                getDefaultStorage(),
                localDataSources);
            try {
                if (!configurationManager.isSecure()) {
                    DBSSecretController
                        .getProjectSecretController(project)
                        .flushChanges();
                }
                lastError = null;
            } catch (Throwable e) {
                lastError = e;
                log.error("Error saving secured preferences", e);
            }
        } catch (Exception ex) {
            lastError = ex;
            log.error("Error saving datasources configuration", ex);
        }

    }

    @Override
    public void checkForErrors() throws DBException {
        Throwable lastError = getLastError();
        if (lastError != null) {
            if (lastError instanceof DBException) {
                throw (DBException) lastError;
            }
            throw new DBException(lastError.getMessage(), lastError.getCause());
        }
    }

    @Override
    public void persistSecrets(DBSSecretController secretController) throws DBException {
        for (DBPDataSourceContainer ds : getDataSources()) {
            ds.persistSecrets(secretController);
        }
        for (DBWNetworkProfile np : getNetworkProfiles()) {
            np.persistSecrets(secretController);
        }
        for (DBAAuthProfile ap : getAllAuthProfiles()) {
            ap.persistSecrets(secretController);
        }
    }

    @Override
    public void resolveSecrets(DBSSecretController secretController) throws DBException {
        for (DBPDataSourceContainer ds : getDataSources()) {
            ds.resolveSecrets(secretController);
        }
        for (DBWNetworkProfile np : getNetworkProfiles()) {
            np.resolveSecrets(secretController);
        }
        for (DBAAuthProfile ap : getAllAuthProfiles()) {
            ap.resolveSecrets(secretController);
        }
    }

    static class ParseResults {
        Set<DBPDataSourceContainer> updatedDataSources = new LinkedHashSet<>();
        Set<DBPDataSourceContainer> addedDataSources = new LinkedHashSet<>();
        Set<DataSourceFolder> addedFolders = new LinkedHashSet<>();
        Set<DataSourceFolder> updatedFolders = new LinkedHashSet<>();
    }

    private class DisconnectTask implements DBRRunnableWithProgress {
        boolean disconnected;

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            monitor = new ProxyProgressMonitor(monitor) {
                @Override
                public boolean isCanceled() {
                    // It is never canceled because we call DisconnectTask on shutdown when all tasks are canceled
                    return false;
                }
            };
            List<DataSourceDescriptor> dsSnapshot;
            synchronized (dataSources) {
                dsSnapshot = CommonUtils.copyList(dataSources.values());
            }
            monitor.beginTask("Disconnect all databases", dsSnapshot.size());
            try {
                for (DataSourceDescriptor dataSource : dsSnapshot) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    if (dataSource.isConnected()) {
                        try {
                            // Disconnect
                            monitor.subTask("Disconnect from [" + dataSource.getName() + "]");
                            disconnected = dataSource.disconnect(monitor);
                        } catch (Exception ex) {
                            log.error("Can't shutdown data source '" + dataSource.getName() + "'", ex);
                        }
                    }
                    monitor.worked(1);
                }
            } finally {
                monitor.done();
            }
        }
    }

    private class ConfigSaver extends AbstractJob {
        ConfigSaver() {
            super("Datasource configuration save");
        }
        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            synchronized (DataSourceRegistry.this) {
                //log.debug("Save column config " + System.currentTimeMillis());
                saveDataSources(monitor);
            }
            return Status.OK_STATUS;
        }
    }

}
