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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;
import org.jkiss.dbeaver.model.app.DBASecureStorage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionEventType;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.dbeaver.runtime.encode.EncryptionException;
import org.jkiss.dbeaver.runtime.encode.PasswordEncrypter;
import org.jkiss.dbeaver.runtime.encode.SimpleStringEncrypter;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.xml.sax.Attributes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Legacy datasource serialization (xml)
 */
@Deprecated
class DataSourceSerializerLegacy implements DataSourceSerializer
{
    private static final Log log = Log.getLog(DataSourceSerializerLegacy.class);

    private static PasswordEncrypter ENCRYPTOR = new SimpleStringEncrypter();

    private final DataSourceRegistry registry;

    public DataSourceSerializerLegacy(DataSourceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void saveDataSources(
        DBRProgressMonitor monitor,
        DBPDataSourceConfigurationStorage configurationStorage,
        List<DataSourceDescriptor> localDataSources,
        IFile configFile) throws DBException, IOException
    {
        // Save in temp memory to be safe (any error during direct write will corrupt configuration)
        ByteArrayOutputStream tempStream = new ByteArrayOutputStream(10000);
        try {
            XMLBuilder xml = new XMLBuilder(tempStream, GeneralUtils.UTF8_ENCODING);
            xml.setButify(true);
            try (XMLBuilder.Element el1 = xml.startElement("data-sources")) {
                if (configurationStorage.isDefault()) {
                    // Folders (only for default origin)
                    for (DataSourceFolder folder : registry.getAllFolders()) {
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
                if (configurationStorage.isDefault()) {
                    try (XMLBuilder.Element ignored = xml.startElement(RegistryConstants.TAG_FILTERS)) {
                        for (DBSObjectFilter cf : registry.getSavedFilters()) {
                            if (!cf.isEmpty()) {
                                saveObjectFiler(xml, null, null, cf);
                            }
                        }
                    }
                }

            }
            xml.flush();
        } catch (IOException ex) {
            log.error("IO error while saving datasources xml", ex);
        }
        InputStream ifs = new ByteArrayInputStream(tempStream.toByteArray());
        try {
            if (!configFile.exists()) {
                configFile.create(ifs, true, monitor.getNestedMonitor());
                configFile.setHidden(true);
            } else {
                configFile.setContents(ifs, true, false, monitor.getNestedMonitor());
            }
        } catch (CoreException e) {
            throw new IOException("Error saving configuration to a file " + configFile.getFullPath(), e);
        }
    }

    @Override
    public void parseDataSources(IFile configFile, DBPDataSourceConfigurationStorage configurationStorage, boolean refresh, DataSourceRegistry.ParseResults parseResults)
        throws DBException, IOException
    {
        try {
            SAXReader parser = new SAXReader(configFile.getContents());
            final DataSourcesParser dsp = new DataSourcesParser(registry, configurationStorage, refresh, parseResults);
            parser.parse(dsp);
        } catch (Exception ex) {
            throw new DBException("Datasource config parse error", ex);
        }
    }

    private static void saveFolder(XMLBuilder xml, DataSourceFolder folder)
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

    private static void saveDataSource(XMLBuilder xml, DataSourceDescriptor dataSource)
        throws IOException
    {
        xml.startElement(RegistryConstants.TAG_DATA_SOURCE);
        xml.addAttribute(RegistryConstants.ATTR_ID, dataSource.getId());
        xml.addAttribute(RegistryConstants.ATTR_PROVIDER, dataSource.getDriver().getProviderDescriptor().getId());
        xml.addAttribute(RegistryConstants.ATTR_DRIVER, dataSource.getDriver().getId());
        xml.addAttribute(RegistryConstants.ATTR_NAME, dataSource.getName());
        xml.addAttribute(RegistryConstants.ATTR_SAVE_PASSWORD, dataSource.isSavePassword());

        DataSourceNavigatorSettings navSettings = dataSource.getNavigatorSettings();
        if (navSettings.isShowSystemObjects()) xml.addAttribute(DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_SYSTEM_OBJECTS, navSettings.isShowSystemObjects());
        if (navSettings.isShowUtilityObjects()) xml.addAttribute(DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_UTIL_OBJECTS, navSettings.isShowUtilityObjects());

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
                dataSource.getRegistry().getProject(),
                dataSource,
                null,
                new SecureCredentials(dataSource));

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
            for (DBWHandlerConfiguration configuration : connectionInfo.getHandlers()) {
                xml.startElement(RegistryConstants.TAG_NETWORK_HANDLER);
                xml.addAttribute(RegistryConstants.ATTR_TYPE, configuration.getType().name());
                xml.addAttribute(RegistryConstants.ATTR_ID, CommonUtils.notEmpty(configuration.getId()));
                xml.addAttribute(RegistryConstants.ATTR_ENABLED, configuration.isEnabled());
                xml.addAttribute(RegistryConstants.ATTR_SAVE_PASSWORD, configuration.isSavePassword());
                if (!CommonUtils.isEmpty(configuration.getUserName())) {
                    saveSecuredCredentials(
                        xml,
                        dataSource.getRegistry().getProject(),
                        dataSource,
                        "network/" + configuration.getId(),
                        new SecureCredentials(configuration));
                }
                for (Map.Entry<String, Object> entry : configuration.getProperties().entrySet()) {
                    if (entry.getValue() == null) {
                        continue;
                    }
                    xml.startElement(RegistryConstants.TAG_PROPERTY);
                    xml.addAttribute(RegistryConstants.ATTR_NAME, entry.getKey());
                    xml.addAttribute(RegistryConstants.ATTR_VALUE, CommonUtils.notEmpty(entry.getValue().toString()));
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
                    if (!CommonUtils.isEmpty(bootstrap.getDefaultCatalogName())) {
                        xml.addAttribute(RegistryConstants.ATTR_DEFAULT_OBJECT, bootstrap.getDefaultCatalogName());
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

    private static void saveSecuredCredentials(@NotNull XMLBuilder xml, @NotNull DBPProject project, @Nullable DataSourceDescriptor dataSource, String subNode, SecureCredentials creds) throws IOException {
        boolean saved = DataSourceUtils.saveCredentialsInSecuredStorage(project, dataSource, subNode, creds);
        if (!saved) {
            try {
                if (!CommonUtils.isEmpty(creds.getUserName())) {
                    xml.addAttribute(RegistryConstants.ATTR_USER, creds.getUserName());
                }
                if (!CommonUtils.isEmpty(creds.getUserPassword())) {
                    xml.addAttribute(RegistryConstants.ATTR_PASSWORD, ENCRYPTOR.encrypt(creds.getUserPassword()));
                }
            } catch (EncryptionException e) {
                log.error("Error encrypting password", e);
            }
        }
    }

    private static void saveObjectFiler(XMLBuilder xml, String typeName, String objectID, DBSObjectFilter filter) throws IOException
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

    @Nullable
    private static String decryptPassword(String encPassword) {
        if (!CommonUtils.isEmpty(encPassword)) {
            try {
                encPassword = ENCRYPTOR.decrypt(encPassword);
            } catch (Throwable e) {
                // could not decrypt - use as is
                encPassword = null;
            }
        }
        return encPassword;
    }

    private class DataSourcesParser implements SAXListener {
        DataSourceRegistry registry;
        DataSourceDescriptor curDataSource;
        DBPDataSourceConfigurationStorage origin;
        boolean refresh;
        boolean isDescription = false;
        DBRShellCommand curCommand = null;
        private DBWHandlerConfiguration curNetworkHandler;
        private DBSObjectFilter curFilter;
        private StringBuilder curQuery;
        private DataSourceRegistry.ParseResults parseResults;
        private boolean passwordReadCanceled = false;

        private DataSourcesParser(DataSourceRegistry registry, DBPDataSourceConfigurationStorage origin, boolean refresh, DataSourceRegistry.ParseResults parseResults) {
            this.registry = registry;
            this.origin = origin;
            this.refresh = refresh;
            this.parseResults = parseResults;
        }

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts) {
            isDescription = false;
            curCommand = null;
            switch (localName) {
                case RegistryConstants.TAG_FOLDER: {
                    String name = atts.getValue(RegistryConstants.ATTR_NAME);
                    String description = atts.getValue(RegistryConstants.ATTR_DESCRIPTION);
                    String parentFolder = atts.getValue(RegistryConstants.ATTR_PARENT);
                    DataSourceFolder parent = parentFolder == null ? null : registry.findFolderByPath(parentFolder, true);
                    DataSourceFolder folder = parent == null ? registry.findFolderByPath(name, true) : parent.getChild(name);
                    if (folder == null) {
                        folder = new DataSourceFolder(registry, parent, name, description);
                        registry.addDataSourceFolder(folder);
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
                    curDataSource = registry.getDataSource(id);
                    boolean newDataSource = (curDataSource == null);
                    if (newDataSource) {
                        curDataSource = new DataSourceDescriptor(
                            registry,
                            origin,
                            id,
                            driver,
                            new DBPConnectionConfiguration());
                    } else {
                        // Clean settings - they have to be loaded later by parser
                        curDataSource.getConnectionConfiguration().setProperties(Collections.emptyMap());
                        curDataSource.getConnectionConfiguration().setHandlers(Collections.emptyList());
                        curDataSource.clearFilters();
                    }
                    curDataSource.setName(name);
                    curDataSource.setSavePassword(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_SAVE_PASSWORD)));

                    DataSourceNavigatorSettings navSettings = curDataSource.getNavigatorSettings();
                    navSettings.setShowSystemObjects(CommonUtils.getBoolean(atts.getValue(DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_SYSTEM_OBJECTS)));
                    navSettings.setShowUtilityObjects(CommonUtils.getBoolean(atts.getValue(DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_UTIL_OBJECTS)));
                    navSettings.setShowOnlyEntities(CommonUtils.getBoolean(atts.getValue(DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_ONLY_ENTITIES)));
                    navSettings.setHideFolders(CommonUtils.getBoolean(atts.getValue(DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_FOLDERS)));
                    navSettings.setHideSchemas(CommonUtils.getBoolean(atts.getValue(DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_SCHEMAS)));
                    navSettings.setHideVirtualModel(CommonUtils.getBoolean(atts.getValue(DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_VIRTUAL)));
                    navSettings.setMergeEntities(CommonUtils.getBoolean(atts.getValue(DataSourceSerializerModern.ATTR_NAVIGATOR_MERGE_ENTITIES)));

                    curDataSource.setConnectionReadOnly(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_READ_ONLY)));
                    final String folderPath = atts.getValue(RegistryConstants.ATTR_FOLDER);
                    if (folderPath != null) {
                        curDataSource.setFolder(registry.findFolderByPath(folderPath, true));
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
                        registry.addDataSourceToList(curDataSource);
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
                            config.getBootstrap().setDefaultCatalogName(atts.getValue(RegistryConstants.ATTR_DEFAULT_OBJECT));
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
                        curNetworkHandler.setProperty(
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
                        switch (propName) {
                            case DataSourceRegistry.DEFAULT_AUTO_COMMIT:
                                curDataSource.getConnectionConfiguration().getBootstrap().setDefaultAutoCommit(CommonUtils.toBoolean(propValue));
                                break;
                            case DataSourceRegistry.DEFAULT_ISOLATION:
                                curDataSource.getConnectionConfiguration().getBootstrap().setDefaultTransactionIsolation(CommonUtils.toInt(propValue));
                                break;
                            case DataSourceRegistry.DEFAULT_ACTIVE_OBJECT:
                                if (!CommonUtils.isEmpty(propValue)) {
                                    curDataSource.getConnectionConfiguration().getBootstrap().setDefaultCatalogName(propValue);
                                }
                                break;
                            default:
                                curDataSource.getPreferenceStore().getProperties().put(propName, propValue);
                                break;
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
                        curNetworkHandler = new DBWHandlerConfiguration(handlerDescriptor, curDataSource);
                        curNetworkHandler.setEnabled(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_ENABLED)));
                        curNetworkHandler.setSavePassword(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_SAVE_PASSWORD)));
                        if (!passwordReadCanceled) {
                            final String[] creds = readSecuredCredentials(atts, curDataSource, "network/" + handlerId);
                            curNetworkHandler.setUserName(creds[0]);
                            if (curNetworkHandler.isSavePassword()) {
                                curNetworkHandler.setPassword(creds[1]);
                            }
                        }

                        curDataSource.getConnectionConfiguration().updateHandler(curNetworkHandler);
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
                        registry.addSavedFilter(curFilter);
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
        public void saxText(SAXReader reader, String data) {
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
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName) {
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
            final DBASecureStorage secureStorage = dataSource.getProject().getSecureStorage();
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

}
