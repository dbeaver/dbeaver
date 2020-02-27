/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.virtual.DBVModel;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.resource.DBeaverNature;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

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
    private final ProjectMetadata project;

    private final Map<IFile, DataSourceOrigin> origins = new LinkedHashMap<>();
    private final List<DataSourceDescriptor> dataSources = new ArrayList<>();
    private final List<DBPEventListener> dataSourceListeners = new ArrayList<>();
    private final List<DataSourceFolder> dataSourceFolders = new ArrayList<>();
    private final List<DBSObjectFilter> savedFilters = new ArrayList<>();
    private final List<DBWNetworkProfile> networkProfiles = new ArrayList<>();
    private volatile boolean saveInProgress = false;

    private final DBVModel.ModelChangeListener modelChangeListener = new DBVModel.ModelChangeListener();
    private volatile ConfigSaver configSaver;

    public DataSourceRegistry(DBPPlatform platform, ProjectMetadata project) {
        this(platform, project, true);
    }

    public DataSourceRegistry(DBPPlatform platform, ProjectMetadata project, boolean loadAllDataSources) {
        this.platform = platform;
        this.project = project;

        loadDataSources(true);
        DataSourceProviderRegistry.getInstance().fireRegistryChange(this, true);

        addDataSourceListener(modelChangeListener);
    }

    /**
     * Create copy
     */
    public DataSourceRegistry(DataSourceRegistry source, ProjectMetadata project, boolean copyDataSources) {
        this.platform = source.platform;
        this.project = project;
        if (copyDataSources) {
            for (DataSourceDescriptor ds : source.dataSources) {
                dataSources.add(new DataSourceDescriptor(ds, this));
            }
        }

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
            for (DataSourceDescriptor dataSourceDescriptor : this.dataSources) {
                dataSourceDescriptor.dispose();
            }
            this.dataSources.clear();
        }
    }

    private void closeConnections(long waitTime) {
        boolean hasConnections = false;
        synchronized (dataSources) {
            for (DataSourceDescriptor dataSource : dataSources) {
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

    DataSourceOrigin getDefaultOrigin() {
        synchronized (origins) {
            for (DataSourceOrigin origin : origins.values()) {
                if (origin.isDefault()) {
                    return origin;
                }
            }
            IFile defFile = getModernConfigFile();
            if (!defFile.exists()) {
                IFile legacyFile = getLegacyConfigFile();
                if (legacyFile.exists()) {
                    defFile = legacyFile;
                }
            }
            DataSourceOrigin origin = new DataSourceOrigin(defFile, true);
            origins.put(defFile, origin);
            return origin;
        }
    }

    private IFile getLegacyConfigFile() {
        return project
            .getEclipseProject()
            .getFile(LEGACY_CONFIG_FILE_NAME);
    }

    private IFile getModernConfigFile() {
        return project
            .getMetadataFolder(false)
            .getFile(MODERN_CONFIG_FILE_NAME);
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
            for (DataSourceDescriptor dsd : dataSources) {
                if (dsd.getId().equals(id)) {
                    return dsd;
                }
            }
        }
        return null;
    }

    @Nullable
    @Override
    public DataSourceDescriptor getDataSource(DBPDataSource dataSource) {
        synchronized (dataSources) {
            for (DataSourceDescriptor dsd : dataSources) {
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
            for (DataSourceDescriptor dsd : dataSources) {
                if (dsd.getName().equals(name)) {
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
            dsCopy = CommonUtils.copyList(dataSources);
        }
        dsCopy.removeIf(ds -> !CommonUtils.equalObjects(ds.getConnectionConfiguration().getUserProfileName(), profile.getProfileName()));
        return dsCopy;
    }

    @NotNull
    @Override
    public List<DataSourceDescriptor> getDataSources() {
        List<DataSourceDescriptor> dsCopy;
        synchronized (dataSources) {
            dsCopy = CommonUtils.copyList(dataSources);
        }
        dsCopy.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        return dsCopy;
    }

    @NotNull
    @Override
    public DBPDataSourceContainer createDataSource(DBPDriver driver, DBPConnectionConfiguration connConfig) {
        return new DataSourceDescriptor(this, DataSourceDescriptor.generateNewId(driver), (DriverDescriptor) driver, connConfig);
    }

    @NotNull
    @Override
    public DBPDataSourceContainer createDataSource(DBPDataSourceContainer source) {
        DataSourceDescriptor newDS = new DataSourceDescriptor((DataSourceDescriptor) source);
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
        for (DataSourceDescriptor ds : dataSources) {
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

    @Override
    public DBPDataSourceRegistry createCopy(DBPProject project, boolean copyDataSources) {
        return new DataSourceRegistry(this, (ProjectMetadata) project, copyDataSources);
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
        for (DBWNetworkProfile profile : networkProfiles) {
            if (CommonUtils.equalObjects(profile.getProfileName(), name)) {
                return profile;
            }
        }
        return null;
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

    void addNetworkProfile(DBWNetworkProfile profile) {
        networkProfiles.add(profile);
    }

    ////////////////////////////////////////////////////
    // Data sources

    public void addDataSource(@NotNull DBPDataSourceContainer dataSource) {
        final DataSourceDescriptor descriptor = (DataSourceDescriptor) dataSource;
        addDataSourceToList(descriptor);
        if (!dataSource.isTemporary()) {
            this.saveDataSources();
        }
        notifyDataSourceListeners(new DBPEvent(DBPEvent.Action.OBJECT_ADD, descriptor, true));
    }

    void addDataSourceToList(@NotNull DataSourceDescriptor descriptor) {
        synchronized (dataSources) {
            this.dataSources.add(descriptor);
        }
    }

    public void removeDataSource(@NotNull DBPDataSourceContainer dataSource) {
        final DataSourceDescriptor descriptor = (DataSourceDescriptor) dataSource;
        synchronized (dataSources) {
            this.dataSources.remove(descriptor);
        }
        if (!dataSource.isTemporary()) {
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
        if (!dataSources.contains(dataSource)) {
            addDataSource(dataSource);
        } else {
            if (!dataSource.isTemporary()) {
                this.saveDataSources();
            }
            this.fireDataSourceEvent(DBPEvent.Action.OBJECT_UPDATE, dataSource);
        }
    }

    @Override
    public void flushConfig() {
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

    @Override
    public void loadDataSourcesFromFile(@NotNull DBPDataSourceConfigurationStorage configurationStorage, @NotNull IFile fromFile) {
        loadDataSources(fromFile, false, true, new ParseResults(), configurationStorage);
    }

    private void loadDataSources(boolean refresh) {
        if (!project.isOpen()) {
            return;
        }
        // Clear filters before reload
        savedFilters.clear();

        // Parse datasources
        ParseResults parseResults = new ParseResults();
        try {
            // Modern way - search json configs in metadata folder
            boolean modernFormat = false;
            IFolder metadataFolder = project.getMetadataFolder(false);
            if (metadataFolder.exists()) {
                if (refresh) {
                    metadataFolder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
                }
                for (IResource res : metadataFolder.members(IContainer.INCLUDE_HIDDEN | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS)) {
                    if (res instanceof IFile && res.exists() &&
                        res.getName().startsWith(MODERN_CONFIG_FILE_PREFIX) && res.getName().endsWith(MODERN_CONFIG_FILE_EXT)) {
                        loadDataSources((IFile) res, refresh, true, parseResults);
                        modernFormat = true;
                    }
                }
            }
            if (!modernFormat) {
                // Logacy way (search config.xml in project folder)
                for (IResource res : project.getEclipseProject().members(IContainer.INCLUDE_HIDDEN)) {
                    if (res instanceof IFile) {
                        IFile file = (IFile) res;
                        if (res.getName().startsWith(LEGACY_CONFIG_FILE_PREFIX) && res.getName().endsWith(LEGACY_CONFIG_FILE_EXT)) {
                            if (file.exists()) {
                                if (file.exists()) {
                                    loadDataSources(file, refresh, false, parseResults);
                                }
                            }
                        }
                    }
                }
                if (!origins.isEmpty()) {
                    // Save config immediately in the new format
                    flushConfig();
                }
            }

            {
                // Call external configurations
                Map<String, Object> searchOptions = new LinkedHashMap<>();
                for (DataSourceConfigurationStorageDescriptor cfd : DataSourceProviderRegistry.getInstance().getDataSourceConfigurationStorages()) {
                    try {
                        cfd.getInstance().loadDataSources(this, searchOptions);
                    } catch (Exception e) {
                        log.error("Error loading data sources from storage '" + cfd.getName() + "'", e);
                    }
                }
            }
        } catch (CoreException e) {
            log.error("Error reading data sources configuration", e);
        }

        // Reflect changes
        if (refresh) {
            for (DataSourceDescriptor ds : parseResults.updatedDataSources) {
                fireDataSourceEvent(DBPEvent.Action.OBJECT_UPDATE, ds);
            }
            for (DataSourceDescriptor ds : parseResults.addedDataSources) {
                fireDataSourceEvent(DBPEvent.Action.OBJECT_ADD, ds);
            }

            List<DataSourceDescriptor> removedDataSource = new ArrayList<>();
            for (DataSourceDescriptor ds : dataSources) {
                if (!parseResults.addedDataSources.contains(ds) && !parseResults.updatedDataSources.contains(ds)) {
                    removedDataSource.add(ds);
                }
            }
            for (DataSourceDescriptor ds : removedDataSource) {
                this.dataSources.remove(ds);
                this.fireDataSourceEvent(DBPEvent.Action.OBJECT_REMOVE, ds);
                ds.dispose();
            }
        }
    }

    private void loadDataSources(@NotNull IFile fromFile, boolean refresh, boolean modern, @NotNull ParseResults parseResults) {
        boolean extraConfig = !fromFile.getName().equalsIgnoreCase(modern ? MODERN_CONFIG_FILE_NAME : LEGACY_CONFIG_FILE_NAME);
        DataSourceOrigin origin;
        synchronized (origins) {
            origin = origins.get(fromFile);
            if (origin == null) {
                origin = new DataSourceOrigin(fromFile, !extraConfig);
                origins.put(fromFile, origin);
            }
        }
        loadDataSources(fromFile, refresh, modern, parseResults, origin);
    }

    private void loadDataSources(@NotNull IFile fromFile, boolean refresh, boolean modern, @NotNull ParseResults parseResults, @NotNull DBPDataSourceConfigurationStorage configurationStorage) {
        if (!fromFile.exists()) {
            return;
        }

        try {
            DataSourceSerializer serializer = modern ? new DataSourceSerializerModern(this) : new DataSourceSerializerLegacy(this);
            serializer.parseDataSources(fromFile, configurationStorage, refresh, parseResults);
            updateProjectNature();
        } catch (Exception ex) {
            log.error("Error loading datasource config from " + fromFile.getFullPath(), ex);
        }
    }

    private void saveDataSources() {
        updateProjectNature();
        final DBRProgressMonitor monitor = new VoidProgressMonitor();
        saveInProgress = true;
        try {
            for (DataSourceOrigin origin : origins.values()) {
                List<DataSourceDescriptor> localDataSources = getDataSources(origin);

                IFile configFile = origin.getSourceFile();

                try {
                    configFile.getParent().refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
                } catch (Exception e) {
                    log.debug("Error refreshing config directory", e);
                }

                if (origin.isDefault()) {
                    if (project.getFormat() == ProjectMetadata.ProjectFormat.MODERN) {
                        configFile = getModernConfigFile();
                    } else {
                        configFile = getLegacyConfigFile();
                    }
                } else {
                    if (configFile.getName().startsWith(LEGACY_CONFIG_FILE_PREFIX) && "xml".equals(configFile.getFileExtension())) {
                        // Legacy configuration - move to metadata folder as json
                        String newFileName = MODERN_CONFIG_FILE_PREFIX + configFile.getName().substring(LEGACY_CONFIG_FILE_PREFIX.length());
                        int divPos = newFileName.lastIndexOf(".");
                        newFileName = newFileName.substring(0, divPos) + ".json";
                        configFile = project.getMetadataFolder(false).getFile(newFileName);
                    }
                }
                try {
                    ContentUtils.makeFileBackup(configFile);

                    if (localDataSources.isEmpty()) {
                        if (configFile.exists()) {
                            configFile.delete(true, false, monitor.getNestedMonitor());
                        }
                    } else {
                        DataSourceSerializer serializer;
                        if (project.getFormat() == ProjectMetadata.ProjectFormat.LEGACY) {
                            serializer = new DataSourceSerializerLegacy(this);
                        } else {
                            serializer = new DataSourceSerializerModern(this);
                        }
                        project.getMetadataFolder(true);
                        serializer.saveDataSources(
                            monitor,
                            origin,
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

    private List<DataSourceDescriptor> getDataSources(DataSourceOrigin origin) {
        List<DataSourceDescriptor> result = new ArrayList<>();
        synchronized (dataSources) {
            for (DataSourceDescriptor ds : dataSources) {
                if (ds.getOrigin() == origin) {
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

    /**
     * Save secure config in protected storage.
     * @return true on success (if protected storage is available and configured)
     */
    static boolean saveCredentialsInSecuredStorage(
        @NotNull DBPProject project,
        @Nullable DataSourceDescriptor dataSource,
        @Nullable String subNode,
        @Nullable String userName,
        @Nullable String password)
    {
        final DBASecureStorage secureStorage = project.getSecureStorage();
        {
            try {
                ISecurePreferences prefNode = dataSource == null ?
                    project.getSecureStorage().getSecurePreferences() :
                    dataSource.getSecurePreferences();
                if (!secureStorage.useSecurePreferences()) {
                    prefNode.removeNode();
                } else {
                    if (subNode != null) {
                        for (String nodeName : subNode.split("/")) {
                            prefNode = prefNode.node(nodeName);
                        }
                    }
                    prefNode.put("name", dataSource != null ? dataSource.getName() : project.getName(), false);

                    if (!CommonUtils.isEmpty(userName)) {
                        prefNode.put(RegistryConstants.ATTR_USER, userName, true);
                    } else {
                        prefNode.remove(RegistryConstants.ATTR_USER);
                    }
                    if (!CommonUtils.isEmpty(password)) {
                        prefNode.put(RegistryConstants.ATTR_PASSWORD, password, true);
                    } else {
                        prefNode.remove(RegistryConstants.ATTR_PASSWORD);
                    }
                    return true;
                }
            } catch (Throwable e) {
                log.error("Can't save password in secure storage", e);
            }
        }
        return false;
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
        Set<DataSourceDescriptor> updatedDataSources = new LinkedHashSet<>();
        Set<DataSourceDescriptor> addedDataSources = new LinkedHashSet<>();
    }

    private class DisconnectTask implements DBRRunnableWithProgress {
        boolean disconnected;

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            List<DataSourceDescriptor> dsSnapshot;
            synchronized (dataSources) {
                dsSnapshot = CommonUtils.copyList(dataSources);
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
