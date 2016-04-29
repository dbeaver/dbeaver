/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.registry;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
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
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.*;
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

    private static final Log log = Log.getLog(DataSourceRegistry.class);

    public static final String OLD_CONFIG_FILE_NAME = "data-sources.xml"; //$NON-NLS-1$

    private final DBPApplication application;
    private final IProject project;

    private final List<DataSourceDescriptor> dataSources = new ArrayList<>();
    private final List<DBPEventListener> dataSourceListeners = new ArrayList<>();
    private volatile boolean saveInProgress = false;

    public DataSourceRegistry(DBPApplication application, IProject project)
    {
        this.application = application;
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
        // Disconnect in 2 seconds or die
        closeConnections(DBConstants.DISCONNECT_TIMEOUT);
        // Do not save config on shutdown.
        // Some data source might be broken due to misconfiguration
        // and we don't want to loose their config just after restart
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

    public void closeConnections(long waitTime)
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

    @NotNull
    public DBPApplication getApplication() {
        return application;
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

    public void addDataSource(DBPDataSourceContainer dataSource)
    {
        synchronized (dataSources) {
            this.dataSources.add((DataSourceDescriptor) dataSource);
        }
        this.saveDataSources();
        this.fireDataSourceEvent(DBPEvent.Action.OBJECT_ADD, dataSource);
    }

    public void removeDataSource(DBPDataSourceContainer dataSource)
    {
        synchronized (dataSources) {
            this.dataSources.remove(dataSource);
        }
        this.saveDataSources();
        try {
            this.fireDataSourceEvent(DBPEvent.Action.OBJECT_REMOVE, dataSource);
        } finally {
            ((DataSourceDescriptor)dataSource).dispose();
        }
    }

    public void updateDataSource(DBPDataSourceContainer dataSource)
    {
        this.saveDataSources();
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

    public void fireDataSourceEvent(
        DBPEvent.Action action,
        DBSObject object)
    {
        notifyDataSourceListeners(new DBPEvent(action, object));
    }

    public void notifyDataSourceListeners(final DBPEvent event)
    {
        if (dataSourceListeners.isEmpty()) {
            return;
        }
        final List<DBPEventListener> listeners;
        synchronized (dataSourceListeners) {
            listeners = new ArrayList<>(dataSourceListeners);
        }
        for (DBPEventListener listener : listeners) {
            listener.handleDataSourceEvent(event);
        }
    }

    private void loadDataSources(boolean refresh) {
        if (!project.isOpen()) {
            return;
        }
        ParseResults parseResults = new ParseResults();
        try {
            for (IResource res : project.members(IContainer.INCLUDE_HIDDEN)) {
                if (res instanceof IFile) {
                    IFile file = (IFile) res;
                    if (res.getName().startsWith(CONFIG_FILE_PREFIX) && res.getName().endsWith(CONFIG_FILE_EXT)) {
                        if (file.exists()) {
                            File dsFile = file.getLocation().toFile();
                            if (dsFile.exists()) {
                                loadDataSources(dsFile, new SimpleStringEncrypter(), refresh, parseResults);
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

    private void loadDataSources(File fromFile, PasswordEncrypter encrypter, boolean refresh, ParseResults parseResults)
    {
        if (!fromFile.exists()) {
            return;
        }
        boolean extraConfig = !fromFile.getName().equalsIgnoreCase(CONFIG_FILE_NAME);
        try {
            InputStream is = new FileInputStream(fromFile);
            try {
                try {
                    loadDataSources(is, encrypter, extraConfig, refresh, parseResults);
                } catch (DBException ex) {
                    log.warn("Error loading datasource config from " + fromFile.getAbsolutePath(), ex);
                }
                finally {
                    is.close();
                }
            }
            catch (IOException ex) {
                log.warn("IO error", ex);
            }
            finally {
                ContentUtils.close(is);
            }
        } catch (IOException e) {
            log.warn("Can't load config file " + fromFile.getAbsolutePath(), e);
        }
    }

    private void loadDataSources(InputStream is, PasswordEncrypter encrypter, boolean extraConfig, boolean refresh, ParseResults parseResults)
        throws DBException, IOException
    {
        SAXReader parser = new SAXReader(is);
        try {
            final DataSourcesParser dsp = new DataSourcesParser(extraConfig, refresh, parseResults, encrypter);
            parser.parse(dsp);
        }
        catch (XMLException ex) {
            throw new DBException("Datasource config parse error", ex);
        }
    }

    void saveDataSources()
    {
        List<DataSourceDescriptor> localDataSources;
        synchronized (dataSources) {
            localDataSources = CommonUtils.copyList(dataSources);
        }
        final IProgressMonitor progressMonitor = new NullProgressMonitor();
        PasswordEncrypter encrypter = new SimpleStringEncrypter();
        IFile configFile = getProject().getFile(CONFIG_FILE_NAME);
        saveInProgress = true;
        try {
            if (localDataSources.isEmpty()) {
                configFile.delete(true, false, progressMonitor);
            } else {
                // Save in temp memory to be safe (any error during direct write will corrupt configuration)
                ByteArrayOutputStream tempStream = new ByteArrayOutputStream(10000);
                try {
                    XMLBuilder xml = new XMLBuilder(tempStream, GeneralUtils.DEFAULT_FILE_CHARSET_NAME);
                    xml.setButify(true);
                    xml.startElement("data-sources");
                    for (DataSourceDescriptor dataSource : localDataSources) {
                        if (!dataSource.isProvided()) {
                            saveDataSource(xml, dataSource, encrypter);
                        }
                    }
                    xml.endElement();
                    xml.flush();
                }
                catch (IOException ex) {
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
        } catch (CoreException ex) {
            log.error("Error saving datasources configuration", ex);
        } finally {
            saveInProgress = false;
        }
    }

    private void saveDataSource(XMLBuilder xml, DataSourceDescriptor dataSource, PasswordEncrypter encrypter)
        throws IOException
    {
        xml.startElement(RegistryConstants.TAG_DATA_SOURCE);
        xml.addAttribute(RegistryConstants.ATTR_ID, dataSource.getId());
        xml.addAttribute(RegistryConstants.ATTR_PROVIDER, dataSource.getDriver().getProviderDescriptor().getId());
        xml.addAttribute(RegistryConstants.ATTR_DRIVER, dataSource.getDriver().getId());
        xml.addAttribute(RegistryConstants.ATTR_NAME, dataSource.getName());
        xml.addAttribute(RegistryConstants.ATTR_CREATE_DATE, dataSource.getCreateDate().getTime());
        if (dataSource.getUpdateDate() != null) {
            xml.addAttribute(RegistryConstants.ATTR_UPDATE_DATE, dataSource.getUpdateDate().getTime());
        }
        if (dataSource.getLoginDate() != null) {
            xml.addAttribute(RegistryConstants.ATTR_LOGIN_DATE, dataSource.getLoginDate().getTime());
        }
        xml.addAttribute(RegistryConstants.ATTR_SAVE_PASSWORD, dataSource.isSavePassword());
        if (dataSource.isShowSystemObjects()) {
            xml.addAttribute(RegistryConstants.ATTR_SHOW_SYSTEM_OBJECTS, dataSource.isShowSystemObjects());
        }
        if (dataSource.isShowUtilityObjects()) {
            xml.addAttribute(RegistryConstants.ATTR_SHOW_UTIL_OBJECTS, dataSource.isShowUtilityObjects());
        }
        xml.addAttribute(RegistryConstants.ATTR_READ_ONLY, dataSource.isConnectionReadOnly());
        if (!CommonUtils.isEmpty(dataSource.getFolderPath())) {
            xml.addAttribute(RegistryConstants.ATTR_FOLDER, dataSource.getFolderPath());
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
            xml.addAttribute(RegistryConstants.ATTR_USER, CommonUtils.notEmpty(connectionInfo.getUserName()));
            if (dataSource.isSavePassword() && !CommonUtils.isEmpty(connectionInfo.getUserPassword())) {
                String encPassword = connectionInfo.getUserPassword();
                if (!CommonUtils.isEmpty(encPassword)) {
                    try {
                        encPassword = encrypter.encrypt(encPassword);
                    }
                    catch (EncryptionException e) {
                        log.error("Can't encrypt password. Save it as is", e);
                    }
                }
                xml.addAttribute(RegistryConstants.ATTR_PASSWORD, encPassword);
            }
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

            for (Map.Entry<Object, Object> entry : connectionInfo.getProperties().entrySet()) {
                xml.startElement(RegistryConstants.TAG_PROPERTY);
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
                xml.addAttribute(RegistryConstants.ATTR_TERMINATE_AT_DISCONNECT, command.isTerminateAtDisconnect());
                xml.addText(command.getCommand());
                xml.endElement();
            }
            // Save network handlers' configurations
            for (DBWHandlerConfiguration configuration : connectionInfo.getDeclaredHandlers()) {
                xml.startElement(RegistryConstants.TAG_NETWORK_HANDLER);
                xml.addAttribute(RegistryConstants.ATTR_TYPE, configuration.getType().name());
                xml.addAttribute(RegistryConstants.ATTR_ID, CommonUtils.notEmpty(configuration.getId()));
                xml.addAttribute(RegistryConstants.ATTR_ENABLED, configuration.isEnabled());
                xml.addAttribute(RegistryConstants.ATTR_USER, CommonUtils.notEmpty(configuration.getUserName()));
                xml.addAttribute(RegistryConstants.ATTR_SAVE_PASSWORD, configuration.isSavePassword());
                if (configuration.isSavePassword() && !CommonUtils.isEmpty(configuration.getPassword())) {
                    String encPassword = configuration.getPassword();
                    if (!CommonUtils.isEmpty(encPassword)) {
                        try {
                            encPassword = encrypter.encrypt(encPassword);
                        }
                        catch (EncryptionException e) {
                            log.error("Can't encrypt password. Save it as is", e);
                        }
                    }
                    xml.addAttribute(RegistryConstants.ATTR_PASSWORD, encPassword);
                }
                for (Map.Entry<String, String> entry : configuration.getProperties().entrySet()) {
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


            xml.endElement();
        }

        {
            // Filters
            Collection<DataSourceDescriptor.FilterMapping> filterMappings = dataSource.getObjectFilters();
            if (!CommonUtils.isEmpty(filterMappings)) {
                xml.startElement(RegistryConstants.TAG_FILTERS);
                for (DataSourceDescriptor.FilterMapping filter : filterMappings) {
                    if (filter.defaultFilter != null) {
                        saveObjectFiler(xml, filter.typeName, null, filter.defaultFilter);
                    }
                    for (Map.Entry<String,DBSObjectFilter> cf : filter.customFilters.entrySet()) {
                        saveObjectFiler(xml, filter.typeName, cf.getKey(), cf.getValue());
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
                if (propValue == null) {
                    continue;
                }
                xml.startElement(RegistryConstants.TAG_CUSTOM_PROPERTY);
                xml.addAttribute(RegistryConstants.ATTR_NAME, propName);
                xml.addAttribute(RegistryConstants.ATTR_VALUE, propValue);
                xml.endElement();
            }
        }

        //xml.addText(CommonUtils.getString(dataSource.getDescription()));
        xml.endElement();
    }

    private void saveObjectFiler(XMLBuilder xml, String typeName, String objectID, DBSObjectFilter filter) throws IOException
    {
        xml.startElement(RegistryConstants.TAG_FILTER);
        xml.addAttribute(RegistryConstants.ATTR_TYPE, typeName);
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
        boolean extraConfig;
        boolean refresh;
        PasswordEncrypter encrypter;
        boolean isDescription = false;
        DBRShellCommand curCommand = null;
        private DBWHandlerConfiguration curNetworkHandler;
        private DBSObjectFilter curFilter;
        private StringBuilder curQuery;
        private ParseResults parseResults;

        private DataSourcesParser(boolean extraConfig, boolean refresh, ParseResults parseResults, PasswordEncrypter encrypter)
        {
            this.extraConfig = extraConfig;
            this.refresh = refresh;
            this.parseResults = parseResults;
            this.encrypter = encrypter;
        }

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException
        {
            isDescription = false;
            curCommand = null;
            switch (localName) {
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
                            id,
                            driver,
                            new DBPConnectionConfiguration());
                    } else {
                        // Clean settings - they have to be loaded later by parser
                        curDataSource.getConnectionConfiguration().setProperties(Collections.emptyMap());
                        curDataSource.getConnectionConfiguration().setHandlers(Collections.<DBWHandlerConfiguration>emptyList());
                        curDataSource.clearFilters();
                    }
                    if (extraConfig) {
                        curDataSource.setProvided(true);
                    }
                    curDataSource.setName(name);
                    try {
                        String createDate = atts.getValue(RegistryConstants.ATTR_CREATE_DATE);
                        if (!CommonUtils.isEmpty(createDate)) {
                            curDataSource.setCreateDate(new Date(Long.parseLong(createDate)));
                        }
                        String updateDate = atts.getValue(RegistryConstants.ATTR_UPDATE_DATE);
                        if (!CommonUtils.isEmpty(updateDate)) {
                            curDataSource.setUpdateDate(new Date(Long.parseLong(updateDate)));
                        }
                        String loginDate = atts.getValue(RegistryConstants.ATTR_LOGIN_DATE);
                        if (!CommonUtils.isEmpty(loginDate)) {
                            curDataSource.setLoginDate(new Date(Long.parseLong(loginDate)));
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Bad date value", e);
                    }
                    curDataSource.setSavePassword(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_SAVE_PASSWORD)));
                    curDataSource.setShowSystemObjects(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_SHOW_SYSTEM_OBJECTS)));
                    curDataSource.setShowUtilityObjects(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_SHOW_UTIL_OBJECTS)));
                    curDataSource.setConnectionReadOnly(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_READ_ONLY)));
                    curDataSource.setFolderPath(atts.getValue(RegistryConstants.ATTR_FOLDER));
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
                        config.setUserName(atts.getValue(RegistryConstants.ATTR_USER));
                        config.setUserPassword(decryptPassword(atts.getValue(RegistryConstants.ATTR_PASSWORD)));
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
                        curDataSource.getConnectionConfiguration().setProperty(
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
                        curCommand.setTerminateAtDisconnect(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_TERMINATE_AT_DISCONNECT)));
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
                        curNetworkHandler.setUserName(CommonUtils.notEmpty(atts.getValue(RegistryConstants.ATTR_USER)));
                        curNetworkHandler.setSavePassword(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_SAVE_PASSWORD)));
                        curNetworkHandler.setPassword(decryptPassword(atts.getValue(RegistryConstants.ATTR_PASSWORD)));
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

        @Nullable
        private String decryptPassword(String encPassword)
        {
            if (!CommonUtils.isEmpty(encPassword)) {
                try {
                    encPassword = encrypter.decrypt(encPassword);
                }
                catch (Throwable e) {
                    // could not decrypt - use as is
                    encPassword = null;
                }
            }
            return encPassword;
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
