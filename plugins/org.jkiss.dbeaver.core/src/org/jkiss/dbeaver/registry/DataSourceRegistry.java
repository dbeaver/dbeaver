/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.registry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.StringConverter;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.registry.encode.EncryptionException;
import org.jkiss.dbeaver.registry.encode.PasswordEncrypter;
import org.jkiss.dbeaver.registry.encode.SimpleStringEncrypter;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;
import org.jkiss.dbeaver.utils.ContentUtils;
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
    static final Log log = Log.getLog(DataSourceRegistry.class);

    public static final String CONFIG_FILE_NAME = ".dbeaver-data-sources.xml"; //$NON-NLS-1$
    public static final String OLD_CONFIG_FILE_NAME = "data-sources.xml"; //$NON-NLS-1$

    private final IProject project;

    private final List<DataSourceDescriptor> dataSources = new ArrayList<DataSourceDescriptor>();
    private final List<DBPEventListener> dataSourceListeners = new ArrayList<DBPEventListener>();

    public DataSourceRegistry(IProject project)
    {
        this.project = project;
        IFile configFile = project.getFile(CONFIG_FILE_NAME);
        if (!configFile.exists()) {
            configFile = project.getFile(OLD_CONFIG_FILE_NAME);
        }
        if (configFile.exists()) {
            File dsFile = configFile.getLocation().toFile();
            if (dsFile.exists()) {
                loadDataSources(dsFile, new SimpleStringEncrypter());
            }
        }
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
        if (!RuntimeUtils.runTask(disconnectTask, waitTime)) {
            log.warn("Some data source connections wasn't closed on shutdown in " + waitTime + "ms. Probably network timeout occurred.");
        }
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

    public void addDataSource(DataSourceDescriptor dataSource)
    {
        synchronized (dataSources) {
            this.dataSources.add(dataSource);
        }
        this.saveDataSources();
        this.fireDataSourceEvent(DBPEvent.Action.OBJECT_ADD, dataSource);
    }

    public void removeDataSource(DataSourceDescriptor dataSource)
    {
        synchronized (dataSources) {
            this.dataSources.remove(dataSource);
        }
        this.saveDataSources();
        try {
            this.fireDataSourceEvent(DBPEvent.Action.OBJECT_REMOVE, dataSource);
        } finally {
            dataSource.dispose();
        }
    }

    public void updateDataSource(DataSourceDescriptor dataSource)
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

    public void fireDataSourceEvent(
        DBPEvent.Action action,
        DBSObject object,
        boolean enabled)
    {
        notifyDataSourceListeners(new DBPEvent(action, object, enabled));
    }

    public void fireDataSourceEvent(
        DBPEvent.Action action,
        DBSObject object,
        Object data)
    {
        notifyDataSourceListeners(new DBPEvent(action, object, data));
    }

    public void fireDataSourceEvent(DBPEvent event)
    {
        notifyDataSourceListeners(event);
    }

    private void notifyDataSourceListeners(
        final DBPEvent event)
    {
        if (dataSourceListeners.isEmpty()) {
            return;
        }
        final List<DBPEventListener> listeners;
        synchronized (dataSourceListeners) {
            listeners = new ArrayList<DBPEventListener>(dataSourceListeners);
        }
        for (DBPEventListener listener : listeners) {
            listener.handleDataSourceEvent(event);
        }
    }

    private void loadDataSources(File fromFile, PasswordEncrypter encrypter)
    {
        if (!fromFile.exists()) {
            return;
        }
        try {
            InputStream is = new FileInputStream(fromFile);
            try {
                try {
                    loadDataSources(is, encrypter);
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

    private void loadDataSources(InputStream is, PasswordEncrypter encrypter)
        throws DBException, IOException
    {
        SAXReader parser = new SAXReader(is);
        try {
            parser.parse(new DataSourcesParser(encrypter));
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
        IProgressMonitor progressMonitor = VoidProgressMonitor.INSTANCE.getNestedMonitor();
        PasswordEncrypter encrypter = new SimpleStringEncrypter();
        IFile configFile = getProject().getFile(CONFIG_FILE_NAME);
        try {
            if (localDataSources.isEmpty()) {
                configFile.delete(true, false, progressMonitor);
            } else {
                // Save in temp memory to be safe (any error during direct write will corrupt configuration)
                ByteArrayOutputStream tempStream = new ByteArrayOutputStream(10000);
                try {
                    XMLBuilder xml = new XMLBuilder(tempStream, ContentUtils.DEFAULT_FILE_CHARSET_NAME);
                    xml.setButify(true);
                    xml.startElement("data-sources");
                    for (DataSourceDescriptor dataSource : localDataSources) {
                        saveDataSource(xml, dataSource, encrypter);
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
        xml.addAttribute(RegistryConstants.ATTR_SHOW_SYSTEM_OBJECTS, dataSource.isShowSystemObjects());
        xml.addAttribute(RegistryConstants.ATTR_READ_ONLY, dataSource.isConnectionReadOnly());
        if (!CommonUtils.isEmpty(dataSource.getFolderPath())) {
            xml.addAttribute(RegistryConstants.ATTR_FOLDER, dataSource.getFolderPath());
        }

        {
            // Connection info
            DBPConnectionInfo connectionInfo = dataSource.getConnectionInfo();
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
                        log.error("Could not encrypt password. Save it as is", e);
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
                xml.addAttribute(RegistryConstants.ATTR_COLOR, StringConverter.asString(connectionInfo.getConnectionColor().getRGB()));
            }
            if (connectionInfo.getProperties() != null) {

                for (Map.Entry<Object, Object> entry : connectionInfo.getProperties().entrySet()) {
                    xml.startElement(RegistryConstants.TAG_PROPERTY);
                    xml.addAttribute(RegistryConstants.ATTR_NAME, CommonUtils.toString(entry.getKey()));
                    xml.addAttribute(RegistryConstants.ATTR_VALUE, CommonUtils.toString(entry.getValue()));
                    xml.endElement();
                }
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
                            log.error("Could not encrypt password. Save it as is", e);
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

            xml.endElement();
        }

        {
            // Filters
            Collection<DataSourceDescriptor.FilterMapping> filterMappings = dataSource.getObjectFilters();
            if (!CommonUtils.isEmpty(filterMappings)) {
                xml.startElement(RegistryConstants.TAG_FILTERS);
                for (DataSourceDescriptor.FilterMapping filter : filterMappings) {
                    if (filter.defaultFilter != null) {
                        saveObjectFiler(xml, filter.type, null, filter.defaultFilter);
                    }
                    for (Map.Entry<String,DBSObjectFilter> cf : filter.customFilters.entrySet()) {
                        saveObjectFiler(xml, filter.type, cf.getKey(), cf.getValue());
                    }
                }
                xml.endElement();
            }
        }

        // Preferences
        {
            // Save only properties who are differs from default values
            AbstractPreferenceStore prefStore = dataSource.getPreferenceStore();
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

        // Virtual model
        if (dataSource.getVirtualModel().hasValuableData()) {
            xml.startElement(RegistryConstants.TAG_VIRTUAL_META_DATA);
            dataSource.getVirtualModel().persist(xml);
            xml.endElement();
        }

        //xml.addText(CommonUtils.getString(dataSource.getDescription()));
        xml.endElement();
    }

    private void saveObjectFiler(XMLBuilder xml, Class<?> type, String objectID, DBSObjectFilter filter) throws IOException
    {
        xml.startElement(RegistryConstants.TAG_FILTER);
        xml.addAttribute(RegistryConstants.ATTR_TYPE, type.getName());
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

    private class DataSourcesParser implements SAXListener
    {
        DataSourceDescriptor curDataSource;
        PasswordEncrypter encrypter;
        boolean isDescription = false;
        DBRShellCommand curCommand = null;
        private DBWHandlerConfiguration curNetworkHandler;
        private DBSObjectFilter curFilter;

        private DataSourcesParser(PasswordEncrypter encrypter)
        {
            this.encrypter = encrypter;
        }

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException
        {
            isDescription = false;
            curCommand = null;
            if (localName.equals(RegistryConstants.TAG_DATA_SOURCE)) {
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
                curDataSource = new DataSourceDescriptor(
                    DataSourceRegistry.this,
                    id,
                    driver,
                    new DBPConnectionInfo());
                curDataSource.setName(name);
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
                curDataSource.setSavePassword(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_SAVE_PASSWORD)));
                curDataSource.setShowSystemObjects(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_SHOW_SYSTEM_OBJECTS)));
                curDataSource.setConnectionReadOnly(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_READ_ONLY)));
                curDataSource.setFolderPath(atts.getValue(RegistryConstants.ATTR_FOLDER));
                {
                    // Legacy filter settings
                    String legacyCatalogFilter = atts.getValue(RegistryConstants.ATTR_FILTER_CATALOG);
                    if (!CommonUtils.isEmpty(legacyCatalogFilter)) {
                        curDataSource.updateObjectFilter(DBSCatalog.class, null, new DBSObjectFilter(legacyCatalogFilter, null));
                    }
                    String legacySchemaFilter = atts.getValue(RegistryConstants.ATTR_FILTER_SCHEMA);
                    if (!CommonUtils.isEmpty(legacySchemaFilter)) {
                        curDataSource.updateObjectFilter(DBSSchema.class, null, new DBSObjectFilter(legacySchemaFilter, null));
                    }
                }

                dataSources.add(curDataSource);
            } else if (localName.equals(RegistryConstants.TAG_CONNECTION)) {
                if (curDataSource != null) {
                    DriverDescriptor driver = curDataSource.getDriver();
                    if (CommonUtils.isEmpty(driver.getName())) {
                        // Broken driver - seems to be just created
                        driver.setName(atts.getValue(RegistryConstants.ATTR_URL));
                        driver.setDriverClassName("java.sql.Driver");
                    }
                    curDataSource.getConnectionInfo().setHostName(atts.getValue(RegistryConstants.ATTR_HOST));
                    curDataSource.getConnectionInfo().setHostPort(atts.getValue(RegistryConstants.ATTR_PORT));
                    curDataSource.getConnectionInfo().setServerName(atts.getValue(RegistryConstants.ATTR_SERVER));
                    curDataSource.getConnectionInfo().setDatabaseName(atts.getValue(RegistryConstants.ATTR_DATABASE));
                    curDataSource.getConnectionInfo().setUrl(atts.getValue(RegistryConstants.ATTR_URL));
                    curDataSource.getConnectionInfo().setUserName(atts.getValue(RegistryConstants.ATTR_USER));
                    curDataSource.getConnectionInfo().setUserPassword(decryptPassword(atts.getValue(RegistryConstants.ATTR_PASSWORD)));
                    curDataSource.getConnectionInfo().setClientHomeId(atts.getValue(RegistryConstants.ATTR_HOME));
                    curDataSource.getConnectionInfo().setConnectionType(
                        DataSourceProviderRegistry.getInstance().getConnectionType(
                            CommonUtils.toString(atts.getValue(RegistryConstants.ATTR_TYPE)),
                            DBPConnectionType.DEFAULT_TYPE)
                        );
                    String colorValue = atts.getValue(RegistryConstants.ATTR_COLOR);
                    if (!CommonUtils.isEmpty(colorValue)) {
                        curDataSource.getConnectionInfo().setConnectionColor(
                            DBeaverUI.getSharedTextColors().getColor(
                                StringConverter.asRGB(colorValue)));
                    }
                    curDataSource.refreshConnectionInfo();
                }
            } else if (localName.equals(RegistryConstants.TAG_PROPERTY)) {
                if (curNetworkHandler != null) {
                    curNetworkHandler.getProperties().put(
                        atts.getValue(RegistryConstants.ATTR_NAME),
                        atts.getValue(RegistryConstants.ATTR_VALUE));
                } else if (curDataSource != null) {
                    curDataSource.getConnectionInfo().setProperty(
                        atts.getValue(RegistryConstants.ATTR_NAME),
                        atts.getValue(RegistryConstants.ATTR_VALUE));
                }
            } else if (localName.equals(RegistryConstants.TAG_EVENT)) {
                if (curDataSource != null) {
                    DBPConnectionEventType eventType = DBPConnectionEventType.valueOf(atts.getValue(RegistryConstants.ATTR_TYPE));
                    curCommand = new DBRShellCommand("");
                    curCommand.setEnabled(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_ENABLED)));
                    curCommand.setShowProcessPanel(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_SHOW_PANEL)));
                    curCommand.setWaitProcessFinish(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_WAIT_PROCESS)));
                    curCommand.setTerminateAtDisconnect(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_TERMINATE_AT_DISCONNECT)));
                    curDataSource.getConnectionInfo().setEvent(eventType, curCommand);
                }
            } else if (localName.equals(RegistryConstants.TAG_CUSTOM_PROPERTY)) {
                if (curDataSource != null) {
                    curDataSource.getPreferenceStore().getProperties().put(
                        atts.getValue(RegistryConstants.ATTR_NAME),
                        atts.getValue(RegistryConstants.ATTR_VALUE));
                }
            } else if (localName.equals(RegistryConstants.TAG_NETWORK_HANDLER)) {
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
                    curDataSource.getConnectionInfo().addHandler(curNetworkHandler);
                }
            } else if (localName.equals(RegistryConstants.TAG_FILTER)) {
                if (curDataSource != null) {
                    String typeName = atts.getValue(RegistryConstants.ATTR_TYPE);
                    String objectID = atts.getValue(RegistryConstants.ATTR_ID);
                    Class<? extends DBSObject> objectClass = curDataSource.getDriver().getObjectClass(typeName, DBSObject.class);
                    if (objectClass != null) {
                        curFilter = new DBSObjectFilter();
                        curFilter.setName(atts.getValue(RegistryConstants.ATTR_NAME));
                        curFilter.setDescription(atts.getValue(RegistryConstants.ATTR_DESCRIPTION));
                        curFilter.setEnabled(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_ENABLED), true));
                        curDataSource.updateObjectFilter(objectClass, objectID, curFilter);

                    }
                }
            } else if (localName.equals(RegistryConstants.TAG_INCLUDE)) {
                if (curFilter != null) {
                    curFilter.addInclude(CommonUtils.notEmpty(atts.getValue(RegistryConstants.ATTR_NAME)));
                }
            } else if (localName.equals(RegistryConstants.TAG_EXCLUDE)) {
                if (curFilter != null) {
                    curFilter.addExclude(CommonUtils.notEmpty(atts.getValue(RegistryConstants.ATTR_NAME)));
                }
            } else if (localName.equals(RegistryConstants.TAG_DESCRIPTION)) {
                isDescription = true;
            } else if (localName.equals(RegistryConstants.TAG_VIRTUAL_META_DATA)) {
                if (curDataSource != null) {
                    reader.setListener(curDataSource.getVirtualModel().getModelParser());
                }
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
            }
        }

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName)
            throws XMLException
        {
            if (localName.equals(RegistryConstants.TAG_DATA_SOURCE)) {
                curDataSource = null;
            } else if (localName.equals(RegistryConstants.TAG_NETWORK_HANDLER)) {
                curNetworkHandler = null;
            } else if (localName.equals(RegistryConstants.TAG_FILTER)) {
                curFilter = null;
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
