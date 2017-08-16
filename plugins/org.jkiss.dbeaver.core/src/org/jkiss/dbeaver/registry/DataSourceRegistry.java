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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverNature;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBASecureStorage;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionEventType;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.encode.EncryptionException;
import org.jkiss.dbeaver.registry.encode.PasswordEncrypter;
import org.jkiss.dbeaver.registry.encode.SimpleStringEncrypter;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DataSourceRegistry implements DBPDataSourceRegistry
{
    @Deprecated
    public static final String DEFAULT_AUTO_COMMIT = "default.autocommit"; //$NON-NLS-1$
    @Deprecated
    public static final String DEFAULT_ISOLATION = "default.isolation"; //$NON-NLS-1$
    @Deprecated
    public static final String DEFAULT_ACTIVE_OBJECT = "default.activeObject"; //$NON-NLS-1$

    private static final long DISCONNECT_ALL_TIMEOUT = 5000;

    private static final Log log = Log.getLog(DataSourceRegistry.class);

    public static final String OLD_CONFIG_FILE_NAME = "data-sources.xml"; //$NON-NLS-1$

    private static PasswordEncrypter ENCRYPTOR = new SimpleStringEncrypter();

    private final DBPPlatform platform;
    private final IProject project;

    private final Map<IFile, DataSourceOrigin> origins = new LinkedHashMap<>();
    private final List<DataSourceDescriptor> dataSources = new ArrayList<>();
    private final List<DBPEventListener> dataSourceListeners = new ArrayList<>();
    private final List<DataSourceFolder> dataSourceFolders = new ArrayList<>();
    private final List<DBSObjectFilter> savedFilters = new ArrayList<>();
    private volatile boolean saveInProgress = false;

    public DataSourceRegistry(DBPPlatform platform, IProject project)
    {
        this.platform = platform;
        this.project = project;
        loadDataSources(false);
        DataSourceProviderRegistry.getInstance().fireRegistryChange(this, true);
    }

    public void dispose()
    {
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
//        if (getProject().isOpen()) {
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

    private void closeConnections(long waitTime)
    {
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
            IFile defFile = project.getFile(CONFIG_FILE_NAME);
            DataSourceOrigin origin = new DataSourceOrigin(defFile, true);
            origins.put(defFile, origin);
            return origin;
        }
    }

    @NotNull
    public DBPPlatform getPlatform() {
        return platform;
    }

    ////////////////////////////////////////////////////
    // Data sources

    @Nullable
    @Override
    public DataSourceDescriptor getDataSource(String id)
    {
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
    public DataSourceDescriptor getDataSource(DBPDataSource dataSource)
    {
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
    public DataSourceDescriptor findDataSourceByName(String name)
    {
        synchronized (dataSources) {
            for (DataSourceDescriptor dsd : dataSources) {
                if (dsd.getName().equals(name)) {
                    return dsd;
                }
            }
        }
        return null;
    }

    @Override
    public List<DataSourceDescriptor> getDataSources()
    {
        List<DataSourceDescriptor> dsCopy;
        synchronized (dataSources) {
            dsCopy = CommonUtils.copyList(dataSources);
        }
        Collections.sort(dsCopy, new Comparator<DataSourceDescriptor>() {
            @Override
            public int compare(DataSourceDescriptor o1, DataSourceDescriptor o2)
            {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        return dsCopy;
    }

    @Override
    public List<? extends DBPDataSourceFolder> getAllFolders() {
        return dataSourceFolders;
    }

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

    private DataSourceFolder findRootFolder(String name) {
        for (DataSourceFolder root : getRootFolders()) {
            if (root.getName().equals(name)) {
                return root;
            }
        }
        return null;
    }

    public DBPDataSourceFolder getFolder(String path) {
        return findFolderByPath(path, true);
    }

    private DataSourceFolder findFolderByPath(String path, boolean create) {
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

    ////////////////////////////////////////////////////
    // Data sources

    public void addDataSource(DBPDataSourceContainer dataSource)
    {
        final DataSourceDescriptor descriptor = (DataSourceDescriptor) dataSource;
        synchronized (dataSources) {
            this.dataSources.add(descriptor);
        }
        if (!dataSource.isTemporary()) {
            this.saveDataSources();
        }
        notifyDataSourceListeners(new DBPEvent(DBPEvent.Action.OBJECT_ADD, descriptor, true));
    }

    public void removeDataSource(DBPDataSourceContainer dataSource)
    {
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

    public void updateDataSource(DBPDataSourceContainer dataSource)
    {
        if (!dataSource.isTemporary()) {
            this.saveDataSources();
        }
        this.fireDataSourceEvent(DBPEvent.Action.OBJECT_UPDATE, dataSource);
    }

    @Override
    public void flushConfig()
    {
        this.saveDataSources();
    }

    @Override
    public void refreshConfig() {
        if (!saveInProgress) {
            this.loadDataSources(true);
        }
    }

    @Override
    public void addDataSourceListener(DBPEventListener listener)
    {
        synchronized (dataSourceListeners) {
            dataSourceListeners.add(listener);
        }
    }

    @Override
    public boolean removeDataSourceListener(DBPEventListener listener)
    {
        synchronized (dataSourceListeners) {
            return dataSourceListeners.remove(listener);
        }
    }

    private void fireDataSourceEvent(
        DBPEvent.Action action,
        DBSObject object)
    {
        notifyDataSourceListeners(new DBPEvent(action, object));
    }

    public void notifyDataSourceListeners(final DBPEvent event)
    {
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
        return SecurePreferencesFactory.getDefault().node("dbeaver").node("datasources");
    }

    public static List<DataSourceRegistry> getAllRegistries() {
        List<DataSourceRegistry> result = new ArrayList<>();
        for (IProject project : DBeaverCore.getInstance().getLiveProjects()) {
            if (project.isOpen()) {
                DataSourceRegistry registry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(project);
                if (registry != null) {
                    result.add(registry);
                }
            }
        }
        return result;
    }

    public static List<DataSourceDescriptor> getAllDataSources() {
        List<DataSourceDescriptor> result = new ArrayList<>();
        for (IProject project : DBeaverCore.getInstance().getLiveProjects()) {
            if (project.isOpen()) {
                DataSourceRegistry registry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(project);
                if (registry != null) {
                    result.addAll(registry.getDataSources());
                }
            }
        }
        return result;
    }


    /**
     * Find data source in all available registries
     */
    public static DataSourceDescriptor findDataSource(String dataSourceId) {
        ProjectRegistry projectRegistry = DBeaverCore.getInstance().getProjectRegistry();
        for (IProject project : DBeaverCore.getInstance().getLiveProjects()) {
            DataSourceRegistry dataSourceRegistry = projectRegistry.getDataSourceRegistry(project);
            if (dataSourceRegistry != null) {
                DataSourceDescriptor dataSourceContainer = dataSourceRegistry.getDataSource(dataSourceId);
                if (dataSourceContainer != null) {
                    return dataSourceContainer;
                }
            }
        }
        return null;
    }

    private void loadDataSources(boolean refresh) {
        if (!project.isOpen()) {
            return;
        }
        // Clear filters before reload
        savedFilters.clear();
        // Parse with SAX
        ParseResults parseResults = new ParseResults();
        try {
            for (IResource res : project.members(IContainer.INCLUDE_HIDDEN)) {
                if (res instanceof IFile) {
                    IFile file = (IFile) res;
                    if (res.getName().startsWith(CONFIG_FILE_PREFIX) && res.getName().endsWith(CONFIG_FILE_EXT)) {
                        if (file.exists()) {
                            if (file.exists()) {
                                loadDataSources(file, refresh, parseResults);
                            }
                        }
                    }
                }
            }
        } catch (CoreException e) {
            log.error("Error reading datasources configuration", e);
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

    private void loadDataSources(IFile fromFile, boolean refresh, ParseResults parseResults)
    {
        boolean extraConfig = !fromFile.getName().equalsIgnoreCase(CONFIG_FILE_NAME);
        DataSourceOrigin origin;
        synchronized (origins) {
            origin = origins.get(fromFile);
            if (origin == null) {
                origin = new DataSourceOrigin(fromFile, !extraConfig);
                origins.put(fromFile, origin);
            }
        }
        if (!fromFile.exists()) {
            return;
        }
        try (InputStream is = fromFile.getContents()) {
            loadDataSources(is, origin, refresh, parseResults);
        } catch (DBException ex) {
            log.warn("Error loading datasource config from " + fromFile.getFullPath(), ex);
        } catch (IOException ex) {
            log.warn("IO error", ex);
        } catch (CoreException e) {
            log.warn("Resource error", e);
        }
    }

    private void loadDataSources(InputStream is, DataSourceOrigin origin, boolean refresh, ParseResults parseResults)
        throws DBException, IOException
    {
        SAXReader parser = new SAXReader(is);
        try {
            final DataSourcesParser dsp = new DataSourcesParser(origin, refresh, parseResults);
            parser.parse(dsp);
        }
        catch (XMLException ex) {
            throw new DBException("Datasource config parse error", ex);
        }
        updateProjectNature();
    }

    private void saveDataSources()
    {
        updateProjectNature();
        final IProgressMonitor progressMonitor = new NullProgressMonitor();
        saveInProgress = true;
        try {
            for (DataSourceOrigin origin : origins.values()) {
                List<DataSourceDescriptor> localDataSources = getDataSources(origin);
                IFile configFile = origin.getSourceFile();
                try {
                    if (localDataSources.isEmpty()) {
                        configFile.delete(true, false, progressMonitor);
                    } else {
                        // Save in temp memory to be safe (any error during direct write will corrupt configuration)
                        ByteArrayOutputStream tempStream = new ByteArrayOutputStream(10000);
                        try {
                            XMLBuilder xml = new XMLBuilder(tempStream, GeneralUtils.UTF8_ENCODING);
                            xml.setButify(true);
                            try (XMLBuilder.Element el1 = xml.startElement("data-sources")) {
                                if (origin.isDefault()) {
                                    // Folders (only for default origin)
                                    for (DataSourceFolder folder : dataSourceFolders) {
                                        saveFolder(xml, folder);
                                    }
                                }

                                // Datasources
                                for (DataSourceDescriptor dataSource : localDataSources) {
                                    // Skip temporary
                                    if (!dataSource.isTemporary()) {
                                        saveDataSource(xml, dataSource);
                                    }
                                }

                                // Filters
                                if (origin.isDefault()) {
                                    try (XMLBuilder.Element el2 = xml.startElement(RegistryConstants.TAG_FILTERS)) {
                                        for (DBSObjectFilter cf : savedFilters) {
                                            if (!cf.isEmpty()) {
                                                saveObjectFiler(xml, null, null, cf);
                                            }
                                        }
                                    }
                                }

                            }
                            xml.flush();
                        } catch (IOException ex) {
                            log.warn("IO error while saving datasources", ex);
                        }
                        InputStream ifs = new ByteArrayInputStream(tempStream.toByteArray());
                        if (!configFile.exists()) {
                            configFile.create(ifs, true, progressMonitor);
                            configFile.setHidden(true);
                        } else {
                            configFile.setContents(ifs, true, false, progressMonitor);
                        }
                    }
                    try {
                        getSecurePreferences().flush();
                    } catch (Throwable e) {
                        log.error("Error saving secured preferences", e);
                    }
                } catch (CoreException ex) {
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
            final IProjectDescription description = project.getDescription();
            if (description != null) {
                String[] natureIds = description.getNatureIds();
                if (dataSources.isEmpty()) {
                    // Remove nature
                    if (ArrayUtils.contains(natureIds, DBeaverNature.NATURE_ID)) {
                        description.setNatureIds(ArrayUtils.remove(String.class, natureIds, DBeaverNature.NATURE_ID));
                        project.setDescription(description, new NullProgressMonitor());
                    }

                } else {
                    // Add nature
                    if (!ArrayUtils.contains(natureIds, DBeaverNature.NATURE_ID)) {
                        description.setNatureIds(ArrayUtils.add(String.class, natureIds, DBeaverNature.NATURE_ID));
                        try {
                            project.setDescription(description, new NullProgressMonitor());
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

    private void saveFolder(XMLBuilder xml, DataSourceFolder folder)
        throws IOException
    {
        xml.startElement(RegistryConstants.TAG_FOLDER);
        if (folder.getParent() != null) {
            xml.addAttribute(RegistryConstants.ATTR_PARENT, folder.getParent().getFolderPath());
        }
        xml.addAttribute(RegistryConstants.ATTR_NAME, folder.getName());
        if (!CommonUtils.isEmpty(folder.getDescription())) {
            xml.addAttribute(RegistryConstants.ATTR_DESCRIPTION, folder.getDescription());
        }
        xml.endElement();
    }

    private void saveDataSource(XMLBuilder xml, DataSourceDescriptor dataSource)
        throws IOException
    {
        xml.startElement(RegistryConstants.TAG_DATA_SOURCE);
        xml.addAttribute(RegistryConstants.ATTR_ID, dataSource.getId());
        xml.addAttribute(RegistryConstants.ATTR_PROVIDER, dataSource.getDriver().getProviderDescriptor().getId());
        xml.addAttribute(RegistryConstants.ATTR_DRIVER, dataSource.getDriver().getId());
        xml.addAttribute(RegistryConstants.ATTR_NAME, dataSource.getName());
        xml.addAttribute(RegistryConstants.ATTR_SAVE_PASSWORD, dataSource.isSavePassword());
        if (dataSource.isShowSystemObjects()) {
            xml.addAttribute(RegistryConstants.ATTR_SHOW_SYSTEM_OBJECTS, dataSource.isShowSystemObjects());
        }
        if (dataSource.isShowUtilityObjects()) {
            xml.addAttribute(RegistryConstants.ATTR_SHOW_UTIL_OBJECTS, dataSource.isShowUtilityObjects());
        }
        xml.addAttribute(RegistryConstants.ATTR_READ_ONLY, dataSource.isConnectionReadOnly());
        if (dataSource.getFolder() != null) {
            xml.addAttribute(RegistryConstants.ATTR_FOLDER, dataSource.getFolder().getFolderPath());
        }
        final String lockPasswordHash = dataSource.getLockPasswordHash();
        if (!CommonUtils.isEmpty(lockPasswordHash)) {
            xml.addAttribute(RegistryConstants.ATTR_LOCK_PASSWORD, lockPasswordHash);
        }

        {
            // Connection info
            DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
            xml.startElement(RegistryConstants.TAG_CONNECTION);
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                xml.addAttribute(RegistryConstants.ATTR_HOST, connectionInfo.getHostName());
            }
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                xml.addAttribute(RegistryConstants.ATTR_PORT, connectionInfo.getHostPort());
            }
            xml.addAttribute(RegistryConstants.ATTR_SERVER, CommonUtils.notEmpty(connectionInfo.getServerName()));
            xml.addAttribute(RegistryConstants.ATTR_DATABASE, CommonUtils.notEmpty(connectionInfo.getDatabaseName()));
            xml.addAttribute(RegistryConstants.ATTR_URL, CommonUtils.notEmpty(connectionInfo.getUrl()));

            saveSecuredCredentials(xml,
                dataSource,
                null,
                connectionInfo.getUserName(),
                dataSource.isSavePassword() ? connectionInfo.getUserPassword() : null);

            if (!CommonUtils.isEmpty(connectionInfo.getClientHomeId())) {
                xml.addAttribute(RegistryConstants.ATTR_HOME, connectionInfo.getClientHomeId());
            }
            if (connectionInfo.getConnectionType() != null) {
                xml.addAttribute(RegistryConstants.ATTR_TYPE, connectionInfo.getConnectionType().getId());
            }
            if (connectionInfo.getConnectionColor() != null) {
                xml.addAttribute(RegistryConstants.ATTR_COLOR, connectionInfo.getConnectionColor());
            }
            // Save other
            if (connectionInfo.getKeepAliveInterval() > 0) {
                xml.addAttribute(RegistryConstants.ATTR_KEEP_ALIVE, connectionInfo.getKeepAliveInterval());
            }

            for (Map.Entry<String, String> entry : connectionInfo.getProperties().entrySet()) {
                xml.startElement(RegistryConstants.TAG_PROPERTY);
                xml.addAttribute(RegistryConstants.ATTR_NAME, CommonUtils.toString(entry.getKey()));
                xml.addAttribute(RegistryConstants.ATTR_VALUE, CommonUtils.toString(entry.getValue()));
                xml.endElement();
            }
            for (Map.Entry<String, String> entry : connectionInfo.getProviderProperties().entrySet()) {
                xml.startElement(RegistryConstants.TAG_PROVIDER_PROPERTY);
                xml.addAttribute(RegistryConstants.ATTR_NAME, CommonUtils.toString(entry.getKey()));
                xml.addAttribute(RegistryConstants.ATTR_VALUE, CommonUtils.toString(entry.getValue()));
                xml.endElement();
            }

            // Save events
            for (DBPConnectionEventType eventType : connectionInfo.getDeclaredEvents()) {
                DBRShellCommand command = connectionInfo.getEvent(eventType);
                xml.startElement(RegistryConstants.TAG_EVENT);
                xml.addAttribute(RegistryConstants.ATTR_TYPE, eventType.name());
                xml.addAttribute(RegistryConstants.ATTR_ENABLED, command.isEnabled());
                xml.addAttribute(RegistryConstants.ATTR_SHOW_PANEL, command.isShowProcessPanel());
                xml.addAttribute(RegistryConstants.ATTR_WAIT_PROCESS, command.isWaitProcessFinish());
                if (command.isWaitProcessFinish()) {
                    xml.addAttribute(RegistryConstants.ATTR_WAIT_PROCESS_TIMEOUT, command.getWaitProcessTimeoutMs());
                }
                xml.addAttribute(RegistryConstants.ATTR_TERMINATE_AT_DISCONNECT, command.isTerminateAtDisconnect());
                xml.addAttribute(RegistryConstants.ATTR_PAUSE_AFTER_EXECUTE, command.getPauseAfterExecute());
                if (!CommonUtils.isEmpty(command.getWorkingDirectory())) {
                    xml.addAttribute(RegistryConstants.ATTR_WORKING_DIRECTORY, command.getWorkingDirectory());
                }
                xml.addText(command.getCommand());
                xml.endElement();
            }
            // Save network handlers' configurations
            for (DBWHandlerConfiguration configuration : connectionInfo.getDeclaredHandlers()) {
                xml.startElement(RegistryConstants.TAG_NETWORK_HANDLER);
                xml.addAttribute(RegistryConstants.ATTR_TYPE, configuration.getType().name());
                xml.addAttribute(RegistryConstants.ATTR_ID, CommonUtils.notEmpty(configuration.getId()));
                xml.addAttribute(RegistryConstants.ATTR_ENABLED, configuration.isEnabled());
                xml.addAttribute(RegistryConstants.ATTR_SAVE_PASSWORD, configuration.isSavePassword());
                if (!CommonUtils.isEmpty(configuration.getUserName())) {
                    saveSecuredCredentials(
                        xml,
                        dataSource,
                        "network/" + configuration.getId(),
                        configuration.getUserName(),
                        configuration.isSavePassword() ? configuration.getPassword() : null);
                }
                for (Map.Entry<String, String> entry : configuration.getProperties().entrySet()) {
                    if (CommonUtils.isEmpty(entry.getValue())) {
                        continue;
                    }
                    xml.startElement(RegistryConstants.TAG_PROPERTY);
                    xml.addAttribute(RegistryConstants.ATTR_NAME, entry.getKey());
                    xml.addAttribute(RegistryConstants.ATTR_VALUE, CommonUtils.notEmpty(entry.getValue()));
                    xml.endElement();
                }
                xml.endElement();
            }

            // Save bootstrap info
            {
                DBPConnectionBootstrap bootstrap = connectionInfo.getBootstrap();
                if (bootstrap.hasData()) {
                    xml.startElement(RegistryConstants.TAG_BOOTSTRAP);
                    if (bootstrap.getDefaultAutoCommit() != null) {
                        xml.addAttribute(RegistryConstants.ATTR_AUTOCOMMIT, bootstrap.getDefaultAutoCommit());
                    }
                    if (bootstrap.getDefaultTransactionIsolation() != null) {
                        xml.addAttribute(RegistryConstants.ATTR_TXN_ISOLATION, bootstrap.getDefaultTransactionIsolation());
                    }
                    if (!CommonUtils.isEmpty(bootstrap.getDefaultObjectName())) {
                        xml.addAttribute(RegistryConstants.ATTR_DEFAULT_OBJECT, bootstrap.getDefaultObjectName());
                    }
                    if (bootstrap.isIgnoreErrors()) {
                        xml.addAttribute(RegistryConstants.ATTR_IGNORE_ERRORS, true);
                    }
                    for (String query : bootstrap.getInitQueries()) {
                        xml.startElement(RegistryConstants.TAG_QUERY);
                        xml.addText(query);
                        xml.endElement();
                    }
                    xml.endElement();
                }
            }

            xml.endElement();
        }

        {
            // Filters
            Collection<FilterMapping> filterMappings = dataSource.getObjectFilters();
            if (!CommonUtils.isEmpty(filterMappings)) {
                xml.startElement(RegistryConstants.TAG_FILTERS);
                for (FilterMapping filter : filterMappings) {
                    if (filter.defaultFilter != null && !filter.defaultFilter.isEmpty()) {
                        saveObjectFiler(xml, filter.typeName, null, filter.defaultFilter);
                    }
                    for (Map.Entry<String,DBSObjectFilter> cf : filter.customFilters.entrySet()) {
                        if (!cf.getValue().isEmpty()) {
                            saveObjectFiler(xml, filter.typeName, cf.getKey(), cf.getValue());
                        }
                    }
                }
                xml.endElement();
            }
        }

        // Virtual model
        if (dataSource.getVirtualModel().hasValuableData()) {
            xml.startElement(RegistryConstants.TAG_VIRTUAL_META_DATA);
            dataSource.getVirtualModel().serialize(xml);
            xml.endElement();
        }

        // Preferences
        {
            // Save only properties who are differs from default values
            SimplePreferenceStore prefStore = dataSource.getPreferenceStore();
            for (String propName : prefStore.preferenceNames()) {
                String propValue = prefStore.getString(propName);
                String defValue = prefStore.getDefaultString(propName);
                if (propValue == null || CommonUtils.equalObjects(propValue, defValue)) {
                    continue;
                }
                xml.startElement(RegistryConstants.TAG_CUSTOM_PROPERTY);
                xml.addAttribute(RegistryConstants.ATTR_NAME, propName);
                xml.addAttribute(RegistryConstants.ATTR_VALUE, propValue);
                xml.endElement();
            }
        }

        if (!CommonUtils.isEmpty(dataSource.getDescription())) {
            xml.startElement(RegistryConstants.TAG_DESCRIPTION);
            xml.addText(dataSource.getDescription());
            xml.endElement();
        }
        xml.endElement();
    }

    private void saveSecuredCredentials(XMLBuilder xml, DataSourceDescriptor dataSource, String subNode, String userName, String password) throws IOException {
        boolean saved = false;
        final DBASecureStorage secureStorage = getPlatform().getSecureStorage();
        {
            try {
                ISecurePreferences prefNode = dataSource.getSecurePreferences();
                if (!secureStorage.useSecurePreferences()) {
                    prefNode.removeNode();
                } else {
                    if (subNode != null) {
                        for (String nodeName : subNode.split("/")) {
                            prefNode = prefNode.node(nodeName);
                        }
                    }
                    prefNode.put("name", dataSource.getName(), false);

                    if (!CommonUtils.isEmpty(userName)) {
                        prefNode.put(RegistryConstants.ATTR_USER, userName, true);
                        saved = true;
                    } else {
                        prefNode.remove(RegistryConstants.ATTR_USER);
                    }
                    if (!CommonUtils.isEmpty(password)) {
                        prefNode.put(RegistryConstants.ATTR_PASSWORD, password, true);
                        saved = true;
                    } else {
                        prefNode.remove(RegistryConstants.ATTR_PASSWORD);
                    }
                }
            } catch (Throwable e) {
                log.error("Can't save password in secure storage", e);
            }
        }
        if (!saved) {
            try {
                if (!CommonUtils.isEmpty(userName)) {
                    xml.addAttribute(RegistryConstants.ATTR_USER, CommonUtils.notEmpty(userName));
                }
                if (!CommonUtils.isEmpty(password)) {
                    xml.addAttribute(RegistryConstants.ATTR_PASSWORD, ENCRYPTOR.encrypt(password));
                }
            } catch (EncryptionException e) {
                log.error("Error encrypting password", e);
            }
        }
    }

    private void clearSecuredPasswords(DataSourceDescriptor dataSource) {
        try {
            dataSource.getSecurePreferences().removeNode();
        } catch (Throwable e) {
            log.debug("Error clearing '" + dataSource.getId() + "' secure storage");
        }
    }

    @Nullable
    private static String decryptPassword(String encPassword)
    {
        if (!CommonUtils.isEmpty(encPassword)) {
            try {
                encPassword = ENCRYPTOR.decrypt(encPassword);
            }
            catch (Throwable e) {
                // could not decrypt - use as is
                encPassword = null;
            }
        }
        return encPassword;
    }

    private void saveObjectFiler(XMLBuilder xml, String typeName, String objectID, DBSObjectFilter filter) throws IOException
    {
        xml.startElement(RegistryConstants.TAG_FILTER);
        if (typeName != null) {
            xml.addAttribute(RegistryConstants.ATTR_TYPE, typeName);
        }
        if (objectID != null) {
            xml.addAttribute(RegistryConstants.ATTR_ID, objectID);
        }
        if (!CommonUtils.isEmpty(filter.getName())) {
            xml.addAttribute(RegistryConstants.ATTR_NAME, filter.getName());
        }
        if (!CommonUtils.isEmpty(filter.getDescription())) {
            xml.addAttribute(RegistryConstants.ATTR_DESCRIPTION, filter.getDescription());
        }
        if (!filter.isEnabled()) {
            xml.addAttribute(RegistryConstants.ATTR_ENABLED, false);
        }
        for (String include : CommonUtils.safeCollection(filter.getInclude())) {
            xml.startElement(RegistryConstants.TAG_INCLUDE);
            xml.addAttribute(RegistryConstants.ATTR_NAME, include);
            xml.endElement();
        }
        for (String exclude : CommonUtils.safeCollection(filter.getExclude())) {
            xml.startElement(RegistryConstants.TAG_EXCLUDE);
            xml.addAttribute(RegistryConstants.ATTR_NAME, exclude);
            xml.endElement();
        }
        xml.endElement();
    }

    @Override
    public IProject getProject()
    {
        return project;
    }

    private static class ParseResults {
        Set<DataSourceDescriptor> updatedDataSources = new HashSet<>();
        Set<DataSourceDescriptor> addedDataSources = new HashSet<>();
    }

    private class DataSourcesParser implements SAXListener
    {
        DataSourceDescriptor curDataSource;
        DataSourceOrigin origin;
        boolean refresh;
        boolean isDescription = false;
        DBRShellCommand curCommand = null;
        private DBWHandlerConfiguration curNetworkHandler;
        private DBSObjectFilter curFilter;
        private StringBuilder curQuery;
        private ParseResults parseResults;
        private boolean passwordReadCanceled = false;

        private DataSourcesParser(DataSourceOrigin origin, boolean refresh, ParseResults parseResults)
        {
            this.origin = origin;
            this.refresh = refresh;
            this.parseResults = parseResults;
        }

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException
        {
            isDescription = false;
            curCommand = null;
            switch (localName) {
                case RegistryConstants.TAG_FOLDER: {
                    String name = atts.getValue(RegistryConstants.ATTR_NAME);
                    String description = atts.getValue(RegistryConstants.ATTR_DESCRIPTION);
                    String parentFolder = atts.getValue(RegistryConstants.ATTR_PARENT);
                    DataSourceFolder parent = parentFolder == null ? null : findFolderByPath(parentFolder, true);
                    DataSourceFolder folder = parent == null ? findFolderByPath(name, true) : parent.getChild(name);
                    if (folder == null) {
                        folder = new DataSourceFolder(DataSourceRegistry.this, parent, name, description);
                        dataSourceFolders.add(folder);
                    } else {
                        folder.setDescription(description);
                    }
                    break;
                }
                case RegistryConstants.TAG_DATA_SOURCE: {
                    String name = atts.getValue(RegistryConstants.ATTR_NAME);
                    String id = atts.getValue(RegistryConstants.ATTR_ID);
                    if (id == null) {
                        // Support of old version without ID
                        id = name;
                    }
                    String providerId = atts.getValue(RegistryConstants.ATTR_PROVIDER);
                    DataSourceProviderDescriptor provider = DataSourceProviderRegistry.getInstance().getDataSourceProvider(providerId);
                    if (provider == null) {
                        log.warn("Can't find datasource provider " + providerId + " for datasource '" + name + "'");
                        curDataSource = null;
                        reader.setListener(EMPTY_LISTENER);
                        return;
                    }
                    String driverId = atts.getValue(RegistryConstants.ATTR_DRIVER);
                    DriverDescriptor driver = provider.getDriver(driverId);
                    if (driver == null) {
                        log.warn("Can't find driver " + driverId + " in datasource provider " + provider.getId() + " for datasource '" + name + "'. Create new driver");
                        driver = provider.createDriver(driverId);
                        provider.addDriver(driver);
                    }
                    curDataSource = getDataSource(id);
                    boolean newDataSource = (curDataSource == null);
                    if (newDataSource) {
                        curDataSource = new DataSourceDescriptor(
                            DataSourceRegistry.this,
                            origin,
                            id,
                            driver,
                            new DBPConnectionConfiguration());
                    } else {
                        // Clean settings - they have to be loaded later by parser
                        curDataSource.getConnectionConfiguration().setProperties(Collections.<String, String>emptyMap());
                        curDataSource.getConnectionConfiguration().setHandlers(Collections.<DBWHandlerConfiguration>emptyList());
                        curDataSource.clearFilters();
                    }
                    curDataSource.setName(name);
                    curDataSource.setSavePassword(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_SAVE_PASSWORD)));
                    curDataSource.setShowSystemObjects(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_SHOW_SYSTEM_OBJECTS)));
                    curDataSource.setShowUtilityObjects(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_SHOW_UTIL_OBJECTS)));
                    curDataSource.setConnectionReadOnly(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_READ_ONLY)));
                    final String folderPath = atts.getValue(RegistryConstants.ATTR_FOLDER);
                    if (folderPath != null) {
                        curDataSource.setFolder(findFolderByPath(folderPath, true));
                    }
                    curDataSource.setLockPasswordHash(atts.getValue(RegistryConstants.ATTR_LOCK_PASSWORD));
                    {
                        // Legacy filter settings
                        String legacyCatalogFilter = atts.getValue(RegistryConstants.ATTR_FILTER_CATALOG);
                        if (!CommonUtils.isEmpty(legacyCatalogFilter)) {
                            curDataSource.updateObjectFilter(DBSCatalog.class.getName(), null, new DBSObjectFilter(legacyCatalogFilter, null));
                        }
                        String legacySchemaFilter = atts.getValue(RegistryConstants.ATTR_FILTER_SCHEMA);
                        if (!CommonUtils.isEmpty(legacySchemaFilter)) {
                            curDataSource.updateObjectFilter(DBSSchema.class.getName(), null, new DBSObjectFilter(legacySchemaFilter, null));
                        }
                    }
                    if (newDataSource) {
                        dataSources.add(curDataSource);
                        parseResults.addedDataSources.add(curDataSource);
                    } else {
                        parseResults.updatedDataSources.add(curDataSource);
                    }
                    break;
                }
                case RegistryConstants.TAG_CONNECTION:
                    if (curDataSource != null) {
                        DriverDescriptor driver = curDataSource.getDriver();
                        if (CommonUtils.isEmpty(driver.getName())) {
                            // Broken driver - seems to be just created
                            driver.setName(atts.getValue(RegistryConstants.ATTR_URL));
                            driver.setDriverClassName("java.sql.Driver");
                        }
                        DBPConnectionConfiguration config = curDataSource.getConnectionConfiguration();
                        config.setHostName(atts.getValue(RegistryConstants.ATTR_HOST));
                        config.setHostPort(atts.getValue(RegistryConstants.ATTR_PORT));
                        config.setServerName(atts.getValue(RegistryConstants.ATTR_SERVER));
                        config.setDatabaseName(atts.getValue(RegistryConstants.ATTR_DATABASE));
                        config.setUrl(atts.getValue(RegistryConstants.ATTR_URL));
                        if (!passwordReadCanceled) {
                            final String[] creds = readSecuredCredentials(atts, curDataSource, null);
                            config.setUserName(creds[0]);
                            if (curDataSource.isSavePassword()) {
                                config.setUserPassword(creds[1]);
                            }
                        }
                        config.setClientHomeId(atts.getValue(RegistryConstants.ATTR_HOME));
                        config.setConnectionType(
                            DataSourceProviderRegistry.getInstance().getConnectionType(
                                CommonUtils.toString(atts.getValue(RegistryConstants.ATTR_TYPE)),
                                DBPConnectionType.DEFAULT_TYPE)
                        );
                        String colorValue = atts.getValue(RegistryConstants.ATTR_COLOR);
                        if (!CommonUtils.isEmpty(colorValue)) {
                            config.setConnectionColor(colorValue);
                        }
                        String keepAlive = atts.getValue(RegistryConstants.ATTR_KEEP_ALIVE);
                        if (!CommonUtils.isEmpty(keepAlive)) {
                            try {
                                config.setKeepAliveInterval(Integer.parseInt(keepAlive));
                            } catch (NumberFormatException e) {
                                log.warn("Bad keep-alive interval value", e);
                            }
                        }
                    }
                    break;
                case RegistryConstants.TAG_BOOTSTRAP:
                    if (curDataSource != null) {
                        DBPConnectionConfiguration config = curDataSource.getConnectionConfiguration();
                        if (atts.getValue(RegistryConstants.ATTR_AUTOCOMMIT) != null) {
                            config.getBootstrap().setDefaultAutoCommit(CommonUtils.toBoolean(atts.getValue(RegistryConstants.ATTR_AUTOCOMMIT)));
                        }
                        if (atts.getValue(RegistryConstants.ATTR_TXN_ISOLATION) != null) {
                            config.getBootstrap().setDefaultTransactionIsolation(CommonUtils.toInt(atts.getValue(RegistryConstants.ATTR_TXN_ISOLATION)));
                        }
                        if (!CommonUtils.isEmpty(atts.getValue(RegistryConstants.ATTR_DEFAULT_OBJECT))) {
                            config.getBootstrap().setDefaultObjectName(atts.getValue(RegistryConstants.ATTR_DEFAULT_OBJECT));
                        }
                        if (atts.getValue(RegistryConstants.ATTR_IGNORE_ERRORS) != null) {
                            config.getBootstrap().setIgnoreErrors(CommonUtils.toBoolean(atts.getValue(RegistryConstants.ATTR_IGNORE_ERRORS)));
                        }
                    }
                    break;
                case RegistryConstants.TAG_QUERY:
                    curQuery = new StringBuilder();
                    break;
                case RegistryConstants.TAG_PROPERTY:
                    if (curNetworkHandler != null) {
                        curNetworkHandler.getProperties().put(
                            atts.getValue(RegistryConstants.ATTR_NAME),
                            atts.getValue(RegistryConstants.ATTR_VALUE));
                    } else if (curDataSource != null) {
                        final String propName = atts.getValue(RegistryConstants.ATTR_NAME);
                        final String propValue = atts.getValue(RegistryConstants.ATTR_VALUE);
                        if (propName != null) {
                            if (propName.startsWith(DBConstants.INTERNAL_PROP_PREFIX)) {
                                // Backward compatibility - internal properties are provider properties
                                curDataSource.getConnectionConfiguration().setProviderProperty(propName, propValue);
                            } else {
                                curDataSource.getConnectionConfiguration().setProperty(propName, propValue);
                            }
                        }
                    }
                    break;
                case RegistryConstants.TAG_PROVIDER_PROPERTY:
                    if (curDataSource != null) {
                        curDataSource.getConnectionConfiguration().setProviderProperty(
                            atts.getValue(RegistryConstants.ATTR_NAME),
                            atts.getValue(RegistryConstants.ATTR_VALUE));
                    }
                    break;
                case RegistryConstants.TAG_EVENT:
                    if (curDataSource != null) {
                        DBPConnectionEventType eventType = DBPConnectionEventType.valueOf(atts.getValue(RegistryConstants.ATTR_TYPE));
                        curCommand = new DBRShellCommand("");
                        curCommand.setEnabled(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_ENABLED)));
                        curCommand.setShowProcessPanel(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_SHOW_PANEL)));
                        curCommand.setWaitProcessFinish(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_WAIT_PROCESS)));
                        if (curCommand.isWaitProcessFinish()) {
                            String timeoutString = atts.getValue(RegistryConstants.ATTR_WAIT_PROCESS_TIMEOUT);
                            int timeoutMs = CommonUtils.toInt(timeoutString, DBRShellCommand.WAIT_PROCESS_TIMEOUT_FOREVER);
                            curCommand.setWaitProcessTimeoutMs(timeoutMs);
                        }
                        curCommand.setTerminateAtDisconnect(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_TERMINATE_AT_DISCONNECT)));
                        curCommand.setPauseAfterExecute(CommonUtils.toInt(atts.getValue(RegistryConstants.ATTR_PAUSE_AFTER_EXECUTE)));
                        curCommand.setWorkingDirectory(atts.getValue(RegistryConstants.ATTR_WORKING_DIRECTORY));
                        curDataSource.getConnectionConfiguration().setEvent(eventType, curCommand);
                    }
                    break;
                case RegistryConstants.TAG_CUSTOM_PROPERTY:
                    if (curDataSource != null) {
                        String propName = atts.getValue(RegistryConstants.ATTR_NAME);
                        String propValue = atts.getValue(RegistryConstants.ATTR_VALUE);
                        // TODO: remove bootstrap preferences later. PResent for config backward compatibility
                        if (propName.equals(DEFAULT_AUTO_COMMIT)) {
                            curDataSource.getConnectionConfiguration().getBootstrap().setDefaultAutoCommit(CommonUtils.toBoolean(propValue));
                        } else if (propName.equals(DEFAULT_ISOLATION)) {
                            curDataSource.getConnectionConfiguration().getBootstrap().setDefaultTransactionIsolation(CommonUtils.toInt(propValue));
                        } else if (propName.equals(DEFAULT_ACTIVE_OBJECT)) {
                            if (!CommonUtils.isEmpty(propValue)) {
                                curDataSource.getConnectionConfiguration().getBootstrap().setDefaultObjectName(propValue);
                            }
                        } else {
                            curDataSource.getPreferenceStore().getProperties().put(propName, propValue);
                        }
                    }
                    break;
                case RegistryConstants.TAG_NETWORK_HANDLER:
                    if (curDataSource != null) {
                        String handlerId = atts.getValue(RegistryConstants.ATTR_ID);
                        NetworkHandlerDescriptor handlerDescriptor = NetworkHandlerRegistry.getInstance().getDescriptor(handlerId);
                        if (handlerDescriptor == null) {
                            log.warn("Can't find network handler '" + handlerId + "'");
                            reader.setListener(EMPTY_LISTENER);
                            return;
                        }
                        curNetworkHandler = new DBWHandlerConfiguration(handlerDescriptor, curDataSource.getDriver());
                        curNetworkHandler.setEnabled(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_ENABLED)));
                        curNetworkHandler.setSavePassword(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_SAVE_PASSWORD)));
                        if (!passwordReadCanceled) {
                            final String[] creds = readSecuredCredentials(atts, curDataSource, "network/" + handlerId);
                            curNetworkHandler.setUserName(creds[0]);
                            if (curNetworkHandler.isSavePassword()) {
                                curNetworkHandler.setPassword(creds[1]);
                            }
                        }

                        curDataSource.getConnectionConfiguration().addHandler(curNetworkHandler);
                    }
                    break;
                case RegistryConstants.TAG_FILTER:
                    if (curDataSource != null) {
                        String typeName = atts.getValue(RegistryConstants.ATTR_TYPE);
                        String objectID = atts.getValue(RegistryConstants.ATTR_ID);
                        if (typeName != null) {
                            curFilter = new DBSObjectFilter();
                            curFilter.setName(atts.getValue(RegistryConstants.ATTR_NAME));
                            curFilter.setDescription(atts.getValue(RegistryConstants.ATTR_DESCRIPTION));
                            curFilter.setEnabled(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_ENABLED), true));
                            curDataSource.updateObjectFilter(typeName, objectID, curFilter);

                        }
                    } else {
                        curFilter = new DBSObjectFilter();
                        curFilter.setName(atts.getValue(RegistryConstants.ATTR_NAME));
                        curFilter.setDescription(atts.getValue(RegistryConstants.ATTR_DESCRIPTION));
                        curFilter.setEnabled(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_ENABLED), true));
                        savedFilters.add(curFilter);
                    }
                    break;
                case RegistryConstants.TAG_INCLUDE:
                    if (curFilter != null) {
                        curFilter.addInclude(CommonUtils.notEmpty(atts.getValue(RegistryConstants.ATTR_NAME)));
                    }
                    break;
                case RegistryConstants.TAG_EXCLUDE:
                    if (curFilter != null) {
                        curFilter.addExclude(CommonUtils.notEmpty(atts.getValue(RegistryConstants.ATTR_NAME)));
                    }
                    break;
                case RegistryConstants.TAG_DESCRIPTION:
                    isDescription = true;
                    break;
                case RegistryConstants.TAG_VIRTUAL_META_DATA:
                    if (curDataSource != null) {
                        reader.setListener(curDataSource.getVirtualModel().getModelParser());
                    }
                    break;
            }
        }

        @Override
        public void saxText(SAXReader reader, String data)
            throws XMLException
        {
            if (isDescription && curDataSource != null) {
                curDataSource.setDescription(data);
            } else if (curCommand != null) {
                curCommand.setCommand(data);
                curCommand = null;
            } else if (curQuery != null) {
                curQuery.append(data);
            }
        }

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName)
            throws XMLException
        {
            switch (localName) {
                case RegistryConstants.TAG_DATA_SOURCE:
                    curDataSource = null;
                    break;
                case RegistryConstants.TAG_NETWORK_HANDLER:
                    curNetworkHandler = null;
                    break;
                case RegistryConstants.TAG_FILTER:
                    curFilter = null;
                    break;
                case RegistryConstants.TAG_QUERY:
                    if (curDataSource != null && curQuery != null && curQuery.length() > 0) {
                        curDataSource.getConnectionConfiguration().getBootstrap().getInitQueries().add(curQuery.toString());
                        curQuery = null;
                    }
                    break;
            }
            isDescription = false;
        }

        private String[] readSecuredCredentials(Attributes xmlAttrs, DataSourceDescriptor dataSource, String subNode) {
            String[] creds = new String[2];
            final DBASecureStorage secureStorage = getPlatform().getSecureStorage();
            {
                try {
                    if (secureStorage.useSecurePreferences()) {
                        ISecurePreferences prefNode = dataSource.getSecurePreferences();
                        if (subNode != null) {
                            for (String nodeName : subNode.split("/")) {
                                prefNode = prefNode.node(nodeName);
                            }
                        }
                        creds[0] = prefNode.get(RegistryConstants.ATTR_USER, null);
                        creds[1] = prefNode.get(RegistryConstants.ATTR_PASSWORD, null);
                    }
                } catch (Throwable e) {
                    // Most likely user canceled master password enter of failed by some other reason.
                    // Anyhow we won't try it again
                    log.error("Can't read password from secure storage", e);
                    passwordReadCanceled = true;
                }
            }
            if (CommonUtils.isEmpty(creds[0])) {
                creds[0] = xmlAttrs.getValue(RegistryConstants.ATTR_USER);
            }
            if (CommonUtils.isEmpty(creds[1])) {
                final String encPassword = xmlAttrs.getValue(RegistryConstants.ATTR_PASSWORD);
                creds[1] = CommonUtils.isEmpty(encPassword) ? null : decryptPassword(encPassword);
            }
            return creds;
        }

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

}
