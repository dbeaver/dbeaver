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
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.access.DBAAuthProfile;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.auth.DBAAuthCredentialsProvider;
import org.jkiss.dbeaver.model.connection.DBPAuthModelDescriptor;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderRegistry;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.model.runtime.*;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.virtual.DBVModel;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.resource.DBeaverNature;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class DataSourceRegistry implements DBPDataSourceRegistry {
    @Deprecated
    public static final String DEFAULT_AUTO_COMMIT = "default.autocommit"; //$NON-NLS-1$
    @Deprecated
    public static final String DEFAULT_ISOLATION = "default.isolation"; //$NON-NLS-1$
    @Deprecated
    public static final String DEFAULT_ACTIVE_OBJECT = "default.activeObject"; //$NON-NLS-1$

    private static final long DISCONNECT_ALL_TIMEOUT = 5000;

    private static final Log log = Log.getLog(DataSourceRegistry.class);

    public static final String OLD_CONFIG_FILE_NAME = "data-sources.xml"; //$NON-NLS-1$

    private final DBPPlatform platform;
    private final DBPProject project;

    private final Map<Path, DataSourceStorage> storages = new LinkedHashMap<>();
    private final Map<String, DataSourceDescriptor> dataSources = new LinkedHashMap<>();
    private final List<DBPEventListener> dataSourceListeners = new ArrayList<>();
    private final List<DataSourceFolder> dataSourceFolders = new ArrayList<>();
    private final List<DBSObjectFilter> savedFilters = new ArrayList<>();
    private final List<DBWNetworkProfile> networkProfiles = new ArrayList<>();
    private final Map<String, DBAAuthProfile> authProfiles = new LinkedHashMap<>();
    private volatile boolean saveInProgress = false;

    private final DBVModel.ModelChangeListener modelChangeListener = new DBVModel.ModelChangeListener();
    private volatile ConfigSaver configSaver;
    private DBAAuthCredentialsProvider authCredentialsProvider;
    private Throwable lastLoadError;

    public DataSourceRegistry(DBPPlatform platform, DBPProject project) {
        this.platform = platform;
        this.project = project;

        loadDataSources(true);
        DataSourceProviderRegistry.getInstance().fireRegistryChange(this, true);

        addDataSourceListener(modelChangeListener);
    }

    @Override
    public void dispose() {
        removeDataSourceListener(modelChangeListener);
        DataSourceProviderRegistry.getInstance().fireRegistryChange(this, false);
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

    DataSourceStorage getDefaultStorage() {
        synchronized (storages) {
            for (DataSourceStorage storage : storages.values()) {
                if (storage.isDefault()) {
                    return storage;
                }
            }
            Path defFile = getModernConfigFile();
            if (!Files.exists(defFile)) {
                Path legacyFile = getLegacyConfigFile();
                if (Files.exists(legacyFile)) {
                    defFile = legacyFile;
                }
            }
            DataSourceStorage storage = new DataSourceStorage(defFile, true);
            storages.put(defFile, storage);
            return storage;
        }
    }

    private Path getLegacyConfigFile() {
        return project.getAbsolutePath().resolve(LEGACY_CONFIG_FILE_NAME);
    }

    private Path getModernConfigFile() {
        return project.getMetadataFolder(false).resolve(MODERN_CONFIG_FILE_NAME);
    }

    @NotNull
    public DBPPlatform getPlatform() {
        return platform;
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

        for (DataSourceFolder child : folderImpl.getChildren()) {
            removeFolder(child, dropContents);
        }

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
        dataSourceFolders.remove(folderImpl);
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
        return findFolderByPath(path, true);
    }

    DataSourceFolder findFolderByPath(String path, boolean create) {
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
        }
        return parent;
    }

    void addDataSourceFolder(DataSourceFolder folder) {
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
        synchronized (authProfiles) {
            authProfiles.remove(profile.getProfileId());
        }
    }

    ////////////////////////////////////////////////////
    // Data sources

    public void addDataSource(@NotNull DBPDataSourceContainer dataSource) {
        final DataSourceDescriptor descriptor = (DataSourceDescriptor) dataSource;
        addDataSourceToList(descriptor);
        if (!descriptor.isDetached()) {
            this.saveDataSources();
        }
        notifyDataSourceListeners(new DBPEvent(DBPEvent.Action.OBJECT_ADD, descriptor, true));
    }

    void addDataSourceToList(@NotNull DataSourceDescriptor descriptor) {
        synchronized (dataSources) {
            this.dataSources.put(descriptor.getId(), descriptor);
        }
    }

    public void removeDataSource(@NotNull DBPDataSourceContainer dataSource) {
        final DataSourceDescriptor descriptor = (DataSourceDescriptor) dataSource;
        synchronized (dataSources) {
            this.dataSources.remove(descriptor.getId());
        }
        if (!descriptor.isDetached()) {
            this.saveDataSources();
        }
        try {
            this.fireDataSourceEvent(DBPEvent.Action.OBJECT_REMOVE, dataSource);
        } finally {
            descriptor.dispose();
        }
    }

    public void updateDataSource(@NotNull DBPDataSourceContainer dataSource) {
        if (!(dataSource instanceof DataSourceDescriptor)) {
            return;
        }
        if (!dataSources.containsKey(dataSource.getId())) {
            addDataSource(dataSource);
        } else {
            if (!((DataSourceDescriptor) dataSource).isDetached()) {
                this.saveDataSources();
            }
            this.fireDataSourceEvent(DBPEvent.Action.OBJECT_UPDATE, dataSource);
        }
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
    public Throwable getLastLoadError() {
        return lastLoadError;
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

    @Override
    @NotNull
    public ISecurePreferences getSecurePreferences() {
        return platform.getApplication().getSecureStorage().getSecurePreferences().node("datasources");
    }

    @Nullable
    @Override
    public DBAAuthCredentialsProvider getAuthCredentialsProvider() {
        return authCredentialsProvider;
    }

    public void setAuthCredentialsProvider(DBAAuthCredentialsProvider authCredentialsProvider) {
        this.authCredentialsProvider = authCredentialsProvider;
    }

    /**
     * @return true if there is at least one project which was initialized.
     */
    public static boolean isProjectsInitialized() {
        for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
            if (project.isRegistryLoaded()) {
                return true;
            }
        }
        return false;
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

    @NotNull
    @Override
    public List<? extends DBPDataSourceContainer> loadDataSourcesFromFile(@NotNull DBPDataSourceConfigurationStorage configurationStorage, @NotNull Path fromPath) {
        ParseResults parseResults = new ParseResults();
        loadDataSources(fromPath, false, true, parseResults, configurationStorage);
        return new ArrayList<>(parseResults.addedDataSources);
    }

    private void loadDataSources(boolean refresh) {
        if (!project.isOpen() || project.isInMemory()) {
            return;
        }
        // Clear filters before reload
        savedFilters.clear();

        // Parse datasources
        ParseResults parseResults = new ParseResults();

        // Modern way - search json configs in metadata folder
        boolean modernFormat = false;
        Path metadataFolder = project.getMetadataFolder(false);
        if (Files.exists(metadataFolder)) {
            try {
                List<Path> mdFiles = Files.list(metadataFolder)
                    .filter(path -> !Files.isDirectory(path) && Files.exists(path))
                    .collect(Collectors.toList());
                for (Path res : mdFiles) {
                    String fileName = res.getFileName().toString();
                    if (fileName.startsWith(MODERN_CONFIG_FILE_PREFIX) && fileName.endsWith(MODERN_CONFIG_FILE_EXT)) {
                        loadDataSources(res, refresh, true, parseResults);
                        modernFormat = true;
                    }
                }
            } catch (IOException e) {
                log.error("Error during project files read", e);
            }
        }
        if (!modernFormat) {
            if (Files.exists(project.getAbsolutePath())) {
                try {
                    // Logacy way (search config.xml in project folder)
                    List<Path> mdFiles = Files.list(project.getAbsolutePath())
                        .filter(path -> !Files.isDirectory(path) && Files.exists(path))
                        .collect(Collectors.toList());
                    for (Path res : mdFiles) {
                        String fileName = res.getFileName().toString();
                        if (fileName.startsWith(LEGACY_CONFIG_FILE_PREFIX) && fileName.endsWith(LEGACY_CONFIG_FILE_EXT)) {
                            loadDataSources(res, refresh, false, parseResults);
                        }
                    }
                } catch (IOException e) {
                    log.error("Error during legacy project files read", e);
                }
            }
            if (!storages.isEmpty()) {
                // Save config immediately in the new format
                flushConfig();
            }
        }

        {
            // Call external configurations
            Map<String, Object> searchOptions = new LinkedHashMap<>();
            for (DataSourceConfigurationStorageDescriptor cfd : DataSourceProviderRegistry.getInstance().getDataSourceConfigurationStorages()) {
                try {
                    List<? extends DBPDataSourceContainer> loadedDS = cfd.getInstance().loadDataSources(this, searchOptions);
                    if (!loadedDS.isEmpty()) {
                        parseResults.addedDataSources.addAll(loadedDS);
                    }
                } catch (Exception e) {
                    log.error("Error loading data sources from storage '" + cfd.getName() + "'", e);
                }
            }
        }

        // Reflect changes
        if (refresh) {
            for (DBPDataSourceContainer ds : parseResults.updatedDataSources) {
                fireDataSourceEvent(DBPEvent.Action.OBJECT_UPDATE, ds);
            }
            for (DBPDataSourceContainer ds : parseResults.addedDataSources) {
                fireDataSourceEvent(DBPEvent.Action.OBJECT_ADD, ds);
            }

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
        }
    }

    private void loadDataSources(@NotNull Path path, boolean refresh, boolean modern, @NotNull ParseResults parseResults) {
        boolean extraConfig = !path.getFileName().toString().equalsIgnoreCase(modern ? MODERN_CONFIG_FILE_NAME : LEGACY_CONFIG_FILE_NAME);
        DataSourceStorage storage;
        synchronized (storages) {
            storage = storages.get(path);
            if (storage == null) {
                storage = new DataSourceStorage(path, !extraConfig);
                storages.put(path, storage);
            }
        }
        loadDataSources(path, refresh, modern, parseResults, storage);
    }

    private void loadDataSources(@NotNull Path fromFile, boolean refresh, boolean modern, @NotNull ParseResults parseResults, @NotNull DBPDataSourceConfigurationStorage configurationStorage) {
        if (!Files.exists(fromFile)) {
            return;
        }

        try {
            DataSourceSerializer serializer = modern ? new DataSourceSerializerModern(this) : new DataSourceSerializerLegacy(this);
            serializer.parseDataSources(fromFile, configurationStorage, refresh, parseResults);
            updateProjectNature();

            lastLoadError = null;
        } catch (Exception ex) {
            lastLoadError = ex;
            log.error("Error loading datasource config from " + fromFile.toAbsolutePath(), ex);
        }
    }

    private void saveDataSources() {
        if (project.isInMemory()) {
            return;
        }

        updateProjectNature();
        final DBRProgressMonitor monitor = new VoidProgressMonitor();
        saveInProgress = true;
        try {
            for (DataSourceStorage storage : storages.values()) {
                List<DataSourceDescriptor> localDataSources = getDataSources(storage);

                Path configFile = storage.getSourceFile();

                if (storage.isDefault()) {
                    if (project.isModernProject()) {
                        configFile = getModernConfigFile();
                    } else {
                        configFile = getLegacyConfigFile();
                    }
                } else {
                    String configFileName = configFile.getFileName().toString();
                    if (configFileName.startsWith(LEGACY_CONFIG_FILE_PREFIX) && configFileName.endsWith(".xml")) {
                        // Legacy configuration - move to metadata folder as json
                        String newFileName = MODERN_CONFIG_FILE_PREFIX + configFileName.substring(LEGACY_CONFIG_FILE_PREFIX.length());
                        int divPos = newFileName.lastIndexOf(".");
                        newFileName = newFileName.substring(0, divPos) + ".json";
                        configFile = project.getMetadataFolder(false).resolve(newFileName);
                    }
                }
                try {
                    ContentUtils.makeFileBackup(configFile);

                    if (localDataSources.isEmpty()) {
                        if (Files.exists(configFile)) {
                            try {
                                Files.delete(configFile);
                            } catch (IOException e) {
                                log.error("Error deleting file '" + configFile.toAbsolutePath() + "'", e);
                            }
                        }
                    } else {
                        DataSourceSerializer serializer;
                        if (!project.isModernProject()) {
                            serializer = new DataSourceSerializerLegacy(this);
                        } else {
                            serializer = new DataSourceSerializerModern(this);
                        }
                        project.getMetadataFolder(true);
                        serializer.saveDataSources(
                            monitor,
                            storage,
                            localDataSources,
                            configFile);
                    }
                    try {
                        getSecurePreferences().flush();
                    } catch (Throwable e) {
                        log.error("Error saving secured preferences", e);
                    }
                } catch (Exception ex) {
                    log.error("Error saving datasources configuration", ex);
                }
            }
        } finally {
            saveInProgress = false;
        }
    }

    private List<DataSourceDescriptor> getDataSources(DataSourceStorage storage) {
        List<DataSourceDescriptor> result = new ArrayList<>();
        synchronized (dataSources) {
            for (DataSourceDescriptor ds : dataSources.values()) {
                if (ds.getStorage() == storage) {
                    result.add(ds);
                }
            }
        }

        return result;
    }

    private void updateProjectNature() {
        try {
            IProject eclipseProject = project.getEclipseProject();
            final IProjectDescription description = eclipseProject.getDescription();
            if (description != null) {
                String[] natureIds = description.getNatureIds();
                if (dataSources.isEmpty()) {
                    // Remove nature
                    if (ArrayUtils.contains(natureIds, DBeaverNature.NATURE_ID)) {
                        description.setNatureIds(ArrayUtils.remove(String.class, natureIds, DBeaverNature.NATURE_ID));
                        eclipseProject.setDescription(description, new NullProgressMonitor());
                    }

                } else {
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
        } catch (Exception e) {
            log.debug(e);
        }
    }

    private void clearSecuredPasswords(DataSourceDescriptor dataSource) {
        try {
            dataSource.getSecurePreferences().removeNode();
        } catch (Throwable e) {
            log.debug("Error clearing '" + dataSource.getId() + "' secure storage");
        }
    }

    @Override
    public DBPProject getProject() {
        return project;
    }

    @Override
    public String toString() {
        return project.getName() + " (" + getClass().getSimpleName() + ")";
    }

    static class ParseResults {
        Set<DBPDataSourceContainer> updatedDataSources = new LinkedHashSet<>();
        Set<DBPDataSourceContainer> addedDataSources = new LinkedHashSet<>();
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
                saveDataSources();
            }
            return Status.OK_STATUS;
        }
    }

}
