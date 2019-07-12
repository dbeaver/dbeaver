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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBASecureStorage;
import org.jkiss.dbeaver.model.connection.*;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.virtual.DBVModel;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.encode.EncryptionException;
import org.jkiss.dbeaver.runtime.encode.PasswordEncrypter;
import org.jkiss.dbeaver.runtime.encode.SimpleStringEncrypter;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

class DataSourceSerializerModern implements DataSourceSerializer
{
    private static final Log log = Log.getLog(DataSourceSerializerModern.class);

    private static PasswordEncrypter ENCRYPTOR = new SimpleStringEncrypter();

    private static Gson CONFIG_GSON = new GsonBuilder()
        .setLenient()
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    private boolean passwordReadCanceled = false;

    @Override
    public void saveDataSources(
        DBRProgressMonitor monitor,
        boolean primaryConfig,
        List<DataSourceFolder> dataSourceFolders,
        List<DataSourceDescriptor> localDataSources,
        List<DBSObjectFilter> savedFilters,
        IFile configFile) throws CoreException
    {
        ByteArrayOutputStream tempStream = new ByteArrayOutputStream(10000);
        try (OutputStreamWriter osw = new OutputStreamWriter(tempStream, StandardCharsets.UTF_8)) {
            try (JsonWriter jsonWriter = CONFIG_GSON.newJsonWriter(osw)) {
                jsonWriter.setIndent("\t");
                jsonWriter.beginObject();

                // Save folders
                if (primaryConfig) {
                    jsonWriter.name("folders");
                    jsonWriter.beginObject();
                    // Folders (only for default origin)
                    for (DataSourceFolder folder : dataSourceFolders) {
                        saveFolder(jsonWriter, folder);
                    }
                    jsonWriter.endObject();
                }

                Map<String, DBVModel> virtualModels = new LinkedHashMap<>();
                Map<String, DBPConnectionType> connectionTypes = new LinkedHashMap<>();
                Map<String, Map<String, DBPDriver>> drivers = new LinkedHashMap<>();
                {
                    // Save connections
                    jsonWriter.name("connections");
                    jsonWriter.beginObject();
                    for (DataSourceDescriptor dataSource : localDataSources) {
                        // Skip temporary
                        if (!dataSource.isTemporary()) {
                            saveDataSource(jsonWriter, dataSource);
                            if (dataSource.getVirtualModel().hasValuableData()) {
                                virtualModels.put(dataSource.getVirtualModel().getId(), dataSource.getVirtualModel());
                            }
                            DBPConnectionType connectionType = dataSource.getConnectionConfiguration().getConnectionType();
                            if (!connectionType.isPredefined()) {
                                connectionTypes.put(connectionType.getId(), connectionType);
                            }
                            DriverDescriptor driver = dataSource.getDriver();
                            if (driver.isCustom() && !driver.getProviderDescriptor().isTemporary()) {
                                Map<String, DBPDriver> driverMap = drivers.computeIfAbsent(driver.getProviderId(), s -> new LinkedHashMap<>());
                                driverMap.put(driver.getId(), driver);
                            }
                        }
                    }
                    jsonWriter.endObject();
                }

                if (primaryConfig) {
                    if (!virtualModels.isEmpty()) {
                        // Save virtual models
                        jsonWriter.name("virtual-models");
                        jsonWriter.beginObject();
                        for (DBVModel model : virtualModels.values()) {
                            model.serialize(jsonWriter);
                        }
                        jsonWriter.endObject();
                    }

                    // Filters
                    if (!CommonUtils.isEmpty(savedFilters)) {
                        jsonWriter.name("saved-filters");
                        jsonWriter.beginArray();
                        for (DBSObjectFilter cf : savedFilters) {
                            if (!cf.isEmpty()) {
                                saveObjectFiler(jsonWriter, null, null, cf);
                            }
                        }
                        jsonWriter.endArray();
                    }
                    // Connection types
                    if (!CommonUtils.isEmpty(connectionTypes)) {
                        jsonWriter.name("connection-types");
                        jsonWriter.beginObject();
                        for (DBPConnectionType ct : connectionTypes.values()) {
                            jsonWriter.name(ct.getId());
                            jsonWriter.beginObject();
                            JSONUtils.fieldNE(jsonWriter, RegistryConstants.ATTR_NAME, ct.getName());
                            JSONUtils.fieldNE(jsonWriter, RegistryConstants.ATTR_COLOR, ct.getColor());
                            JSONUtils.fieldNE(jsonWriter, RegistryConstants.ATTR_DESCRIPTION, ct.getDescription());
                            JSONUtils.field(jsonWriter, "auto-commit", ct.isAutocommit());
                            JSONUtils.field(jsonWriter, "confirm-execute", ct.isConfirmExecute());
                            JSONUtils.field(jsonWriter, "confirm-data-change", ct.isConfirmDataChange());
                            jsonWriter.endObject();
                        }
                        jsonWriter.endObject();
                    }

                    // Drivers
                    if (!CommonUtils.isEmpty(drivers)) {
                        jsonWriter.name("drivers");
                        jsonWriter.beginObject();
                        for (Map.Entry<String, Map<String, DBPDriver>> dmap : drivers.entrySet()) {
                            jsonWriter.name(dmap.getKey());
                            jsonWriter.beginObject();
                            for (DBPDriver driver : dmap.getValue().values()) {
                                ((DriverDescriptor) driver).serialize(jsonWriter, true);
                            }
                            jsonWriter.endObject();
                        }
                        jsonWriter.endObject();
                    }
                }

                jsonWriter.endObject();
                jsonWriter.flush();
            }
        } catch (IOException e) {
            log.error("IO error while saving datasources json", e);
        }
        InputStream ifs = new ByteArrayInputStream(tempStream.toByteArray());
        if (!configFile.exists()) {
            configFile.create(ifs, true, monitor.getNestedMonitor());
            configFile.setHidden(true);
        } else {
            configFile.setContents(ifs, true, false, monitor.getNestedMonitor());
        }
    }

    @Override
    public void parseDataSources(DataSourceRegistry registry, InputStream is, DataSourceOrigin origin, boolean refresh, DataSourceRegistry.ParseResults parseResults) throws DBException, IOException {
        try (Reader configReader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            Map<String, Object> jsonMap = JSONUtils.parseMap(CONFIG_GSON, configReader);

            // Folders
            for (Map.Entry<String, Map<String, Object>> folderMap : JSONUtils.getNestedObjects(jsonMap, "folders")) {
                String name = folderMap.getKey();
                String description = JSONUtils.getObjectProperty(folderMap.getValue(), RegistryConstants.ATTR_DESCRIPTION);
                String parentFolder = JSONUtils.getObjectProperty(folderMap.getValue(), RegistryConstants.ATTR_PARENT);
                DataSourceFolder parent = parentFolder == null ? null : registry.findFolderByPath(parentFolder, true);
                DataSourceFolder folder = parent == null ? registry.findFolderByPath(name, true) : parent.getChild(name);
                if (folder == null) {
                    folder = new DataSourceFolder(registry, parent, name, description);
                    registry.addDataSourceFolder(folder);
                } else {
                    folder.setDescription(description);
                }
            }

            // Connection types
            for (Map.Entry<String, Map<String, Object>> ctMap : JSONUtils.getNestedObjects(jsonMap, "connection-types")) {
                String id = ctMap.getKey();
                String name = JSONUtils.getObjectProperty(ctMap.getValue(), RegistryConstants.ATTR_NAME);
                String description = JSONUtils.getObjectProperty(ctMap.getValue(), RegistryConstants.ATTR_DESCRIPTION);
                String color = JSONUtils.getObjectProperty(ctMap.getValue(), RegistryConstants.ATTR_COLOR);
                Boolean autoCommit = JSONUtils.getObjectProperty(ctMap.getValue(), "auto-commit");
                Boolean confirmExecute = JSONUtils.getObjectProperty(ctMap.getValue(), "confirm-execute");
                Boolean confirmDataChange = JSONUtils.getObjectProperty(ctMap.getValue(), "confirm-data-change");
                DBPConnectionType ct = DBWorkbench.getPlatform().getDataSourceProviderRegistry().getConnectionType(id, null);
                if (ct == null) {
                    ct = new DBPConnectionType(id, name, color, description, CommonUtils.toBoolean(autoCommit), CommonUtils.toBoolean(confirmExecute), CommonUtils.toBoolean(confirmDataChange));
                    DBWorkbench.getPlatform().getDataSourceProviderRegistry().addConnectionType(ct);
                }
            }

            // Drivers

            // Virtual models
            Map<String, DBVModel> modelMap = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Object>> vmMap : JSONUtils.getNestedObjects(jsonMap, "virtual-models")) {
                String id = vmMap.getKey();
                DBVModel model = new DBVModel(id, vmMap.getValue());
                modelMap.put(id, model);
            }

            // Connections
            for (Map.Entry<String, Map<String, Object>> conMap : JSONUtils.getNestedObjects(jsonMap, "connections")) {
                Map<String, Object> conObject = conMap.getValue();

                // Primary settings
                String id = conMap.getKey();
                String dsProviderID = CommonUtils.toString(conObject.get(RegistryConstants.ATTR_PROVIDER));
                if (CommonUtils.isEmpty(dsProviderID)) {
                    log.warn("Empty datasource provider for datasource '" + id + "'");
                    continue;
                }
                DataSourceProviderDescriptor provider = DataSourceProviderRegistry.getInstance().getDataSourceProvider(
                    dsProviderID);
                if (provider == null) {
                    log.warn("Can't find datasource provider " + dsProviderID + " for datasource '" + id + "'");
                    provider = (DataSourceProviderDescriptor) DataSourceProviderRegistry.getInstance().makeFakeProvider(dsProviderID);
                    return;
                }
                String driverId = CommonUtils.toString(conObject.get(RegistryConstants.ATTR_DRIVER));
                DriverDescriptor driver = provider.getDriver(driverId);
                if (driver == null) {
                    log.warn("Can't find driver " + driverId + " in datasource provider " + provider.getId() + " for datasource '" + id + "'. Create new driver");
                    driver = provider.createDriver(driverId);
                    driver.setName(driverId);
                    driver.setDescription("Missing driver " + driverId);
                    driver.setDriverClassName("java.sql.Driver");
                    driver.setTemporary(true);
                    provider.addDriver(driver);
                }

                DataSourceDescriptor dataSource = registry.getDataSource(id);
                boolean newDataSource = (dataSource == null);
                if (newDataSource) {
                    dataSource = new DataSourceDescriptor(
                        registry,
                        origin,
                        id,
                        driver,
                        new DBPConnectionConfiguration());
                } else {
                    // Clean settings - they have to be loaded later by parser
                    dataSource.getConnectionConfiguration().setProperties(Collections.emptyMap());
                    dataSource.getConnectionConfiguration().setHandlers(Collections.emptyList());
                    dataSource.clearFilters();
                }
                dataSource.setName(JSONUtils.getString(conObject, RegistryConstants.ATTR_NAME));
                dataSource.setSavePassword(JSONUtils.getBoolean(conObject, RegistryConstants.ATTR_SAVE_PASSWORD));
                dataSource.setShowSystemObjects(JSONUtils.getBoolean(conObject, RegistryConstants.ATTR_SHOW_SYSTEM_OBJECTS));
                dataSource.setShowUtilityObjects(JSONUtils.getBoolean(conObject, RegistryConstants.ATTR_SHOW_UTIL_OBJECTS));
                dataSource.setConnectionReadOnly(JSONUtils.getBoolean(conObject, RegistryConstants.ATTR_READ_ONLY));
                final String folderPath = JSONUtils.getString(conObject, RegistryConstants.ATTR_FOLDER);
                if (folderPath != null) {
                    dataSource.setFolder(registry.findFolderByPath(folderPath, true));
                }
                dataSource.setLockPasswordHash(CommonUtils.toString(conObject.get(RegistryConstants.ATTR_LOCK_PASSWORD)));

                // Connection settings
                {
                    Map<String, Object> cfgObject = JSONUtils.getObject(conObject, "configuration");
                    DBPConnectionConfiguration config = dataSource.getConnectionConfiguration();
                    config.setHostName(JSONUtils.getString(cfgObject, RegistryConstants.ATTR_HOST));
                    config.setHostPort(JSONUtils.getString(cfgObject, RegistryConstants.ATTR_PORT));
                    config.setServerName(JSONUtils.getString(cfgObject, RegistryConstants.ATTR_SERVER));
                    config.setDatabaseName(JSONUtils.getString(cfgObject, RegistryConstants.ATTR_DATABASE));
                    config.setUrl(JSONUtils.getString(cfgObject, RegistryConstants.ATTR_URL));
                    if (!passwordReadCanceled) {
                        final String[] creds = readSecuredCredentials(cfgObject, dataSource, null);
                        config.setUserName(creds[0]);
                        if (dataSource.isSavePassword()) {
                            config.setUserPassword(creds[1]);
                        }
                    }
                    config.setClientHomeId(JSONUtils.getString(cfgObject, RegistryConstants.ATTR_HOME));
                    config.setConnectionType(
                        DataSourceProviderRegistry.getInstance().getConnectionType(
                            JSONUtils.getString(cfgObject, RegistryConstants.ATTR_TYPE), DBPConnectionType.DEFAULT_TYPE));
                    String colorValue = JSONUtils.getString(cfgObject, RegistryConstants.ATTR_COLOR);
                    if (!CommonUtils.isEmpty(colorValue)) {
                        config.setConnectionColor(colorValue);
                    }
                    String keepAlive = JSONUtils.getString(cfgObject, RegistryConstants.ATTR_KEEP_ALIVE);
                    if (!CommonUtils.isEmpty(keepAlive)) {
                        try {
                            config.setKeepAliveInterval(Integer.parseInt(keepAlive));
                        } catch (NumberFormatException e) {
                            log.warn("Bad keep-alive interval value", e);
                        }
                    }
                    config.setProperties(JSONUtils.deserializeProperties(cfgObject, RegistryConstants.TAG_PROPERTIES));
                    config.setProviderProperties(JSONUtils.deserializeProperties(cfgObject, RegistryConstants.TAG_PROVIDER_PROPERTIES));
                }

                // Virtual model
                String vmID = CommonUtils.toString(conObject.get("virtual-model-id"), id);
                DBVModel dbvModel = modelMap.get(vmID);
                if (dbvModel != null) {
                    dataSource.setVirtualModel(dbvModel);
                }

                // Add to the list
                if (newDataSource) {
                    registry.addDataSourceToList(dataSource);
                    parseResults.addedDataSources.add(dataSource);
                } else {
                    parseResults.updatedDataSources.add(dataSource);
                }
            }
        }

    }

    private String[] readSecuredCredentials(Map<String, Object> map, DataSourceDescriptor dataSource, String subNode) {
        String[] creds = new String[2];
        final DBASecureStorage secureStorage = DBWorkbench.getPlatform().getSecureStorage();
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
            creds[0] = JSONUtils.getString(map, RegistryConstants.ATTR_USER);
        }
        if (CommonUtils.isEmpty(creds[1])) {
            final String encPassword = JSONUtils.getString(map, RegistryConstants.ATTR_PASSWORD);
            creds[1] = CommonUtils.isEmpty(encPassword) ? null : decryptPassword(encPassword);
        }
        return creds;
    }

    private static void saveFolder(JsonWriter json, DataSourceFolder folder)
        throws IOException
    {
        json.name(folder.getName());

        json.beginObject();
        if (folder.getParent() != null) {
            JSONUtils.field(json, RegistryConstants.ATTR_PARENT, folder.getParent().getFolderPath());
        }
        JSONUtils.fieldNE(json, RegistryConstants.ATTR_DESCRIPTION, folder.getDescription());

        json.endObject();
    }

    static void saveDataSource(JsonWriter json, DataSourceDescriptor dataSource)
        throws IOException
    {
        json.name(dataSource.getId());
        json.beginObject();
        JSONUtils.field(json, RegistryConstants.ATTR_PROVIDER, dataSource.getDriver().getProviderDescriptor().getId());
        JSONUtils.field(json, RegistryConstants.ATTR_DRIVER, dataSource.getDriver().getId());
        JSONUtils.field(json, RegistryConstants.ATTR_NAME, dataSource.getName());
        JSONUtils.fieldNE(json, RegistryConstants.TAG_DESCRIPTION, dataSource.getDescription());
        JSONUtils.field(json, RegistryConstants.ATTR_SAVE_PASSWORD, dataSource.isSavePassword());

        if (dataSource.isShowSystemObjects()) {
            JSONUtils.field(json, RegistryConstants.ATTR_SHOW_SYSTEM_OBJECTS, dataSource.isShowSystemObjects());
        }
        if (dataSource.isShowUtilityObjects()) {
            JSONUtils.field(json, RegistryConstants.ATTR_SHOW_UTIL_OBJECTS, dataSource.isShowUtilityObjects());
        }
        JSONUtils.field(json, RegistryConstants.ATTR_READ_ONLY, dataSource.isConnectionReadOnly());

        if (dataSource.getFolder() != null) {
            JSONUtils.field(json, RegistryConstants.ATTR_FOLDER, dataSource.getFolder().getFolderPath());
        }
        final String lockPasswordHash = dataSource.getLockPasswordHash();
        if (!CommonUtils.isEmpty(lockPasswordHash)) {
            JSONUtils.field(json, RegistryConstants.ATTR_LOCK_PASSWORD, lockPasswordHash);
        }
        if (dataSource.hasSharedVirtualModel()) {
            JSONUtils.field(json, "virtual-model-id", dataSource.getVirtualModel().getId());
        }

        {
            // Connection info
            DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
            json.name("configuration");
            json.beginObject();
            JSONUtils.fieldNE(json, RegistryConstants.ATTR_HOST, connectionInfo.getHostName());
            JSONUtils.fieldNE(json, RegistryConstants.ATTR_PORT, connectionInfo.getHostPort());
            JSONUtils.fieldNE(json, RegistryConstants.ATTR_SERVER, connectionInfo.getServerName());
            JSONUtils.fieldNE(json, RegistryConstants.ATTR_DATABASE, connectionInfo.getDatabaseName());
            JSONUtils.fieldNE(json, RegistryConstants.ATTR_URL, connectionInfo.getUrl());

            saveSecuredCredentials(json,
                dataSource,
                null,
                connectionInfo.getUserName(),
                dataSource.isSavePassword() ? connectionInfo.getUserPassword() : null);

            JSONUtils.fieldNE(json, RegistryConstants.ATTR_HOME, connectionInfo.getClientHomeId());
            if (connectionInfo.getConnectionType() != null) {
                JSONUtils.field(json, RegistryConstants.ATTR_TYPE, connectionInfo.getConnectionType().getId());
            }
            JSONUtils.fieldNE(json, RegistryConstants.ATTR_COLOR, connectionInfo.getConnectionColor());
            // Save other
            if (connectionInfo.getKeepAliveInterval() > 0) {
                JSONUtils.field(json, RegistryConstants.ATTR_KEEP_ALIVE, connectionInfo.getKeepAliveInterval());
            }
            JSONUtils.serializeProperties(json, RegistryConstants.TAG_PROPERTIES, connectionInfo.getProperties());
            JSONUtils.serializeProperties(json, RegistryConstants.TAG_PROVIDER_PROPERTIES, connectionInfo.getProviderProperties());

            // Save events
            if (!ArrayUtils.isEmpty(connectionInfo.getDeclaredEvents())) {
                json.name(RegistryConstants.TAG_EVENTS);
                json.beginArray();
                for (DBPConnectionEventType eventType : connectionInfo.getDeclaredEvents()) {
                    DBRShellCommand command = connectionInfo.getEvent(eventType);
                    json.beginObject();
                    JSONUtils.field(json, RegistryConstants.ATTR_TYPE, eventType.name());
                    JSONUtils.field(json, RegistryConstants.ATTR_ENABLED, command.isEnabled());
                    JSONUtils.field(json, RegistryConstants.ATTR_SHOW_PANEL, command.isShowProcessPanel());
                    JSONUtils.field(json, RegistryConstants.ATTR_WAIT_PROCESS, command.isWaitProcessFinish());
                    if (command.isWaitProcessFinish()) {
                        JSONUtils.field(json, RegistryConstants.ATTR_WAIT_PROCESS_TIMEOUT, command.getWaitProcessTimeoutMs());
                    }
                    JSONUtils.field(json, RegistryConstants.ATTR_TERMINATE_AT_DISCONNECT, command.isTerminateAtDisconnect());
                    JSONUtils.field(json, RegistryConstants.ATTR_PAUSE_AFTER_EXECUTE, command.getPauseAfterExecute());
                    JSONUtils.fieldNE(json, RegistryConstants.ATTR_WORKING_DIRECTORY, command.getWorkingDirectory());
                    JSONUtils.fieldNE(json, RegistryConstants.ATTR_COMMAND, command.getCommand());
                    json.endObject();
                }
                json.endArray();
            }

            // Save network handlers' configurations
            if (!CommonUtils.isEmpty(connectionInfo.getDeclaredHandlers())) {
                json.name(RegistryConstants.TAG_HANDLERS);
                json.beginObject();
                for (DBWHandlerConfiguration configuration : connectionInfo.getDeclaredHandlers()) {
                    json.name(CommonUtils.notEmpty(configuration.getId()));
                    json.beginObject();
                    JSONUtils.field(json, RegistryConstants.ATTR_TYPE, configuration.getType().name());
                    JSONUtils.field(json, RegistryConstants.ATTR_ENABLED, configuration.isEnabled());
                    JSONUtils.field(json, RegistryConstants.ATTR_SAVE_PASSWORD, configuration.isSavePassword());
                    if (!CommonUtils.isEmpty(configuration.getUserName())) {
                        saveSecuredCredentials(
                            json,
                            dataSource,
                            "network/" + configuration.getId(),
                            configuration.getUserName(),
                            configuration.isSavePassword() ? configuration.getPassword() : null);
                    }
                    JSONUtils.serializeProperties(json, RegistryConstants.TAG_PROPERTIES, configuration.getProperties());
                    json.endObject();
                }
                json.endObject();
            }

            // Save bootstrap info
            {
                DBPConnectionBootstrap bootstrap = connectionInfo.getBootstrap();
                if (bootstrap.hasData()) {
                    json.name(RegistryConstants.TAG_BOOTSTRAP);
                    json.beginObject();
                    if (bootstrap.getDefaultAutoCommit() != null) {
                        JSONUtils.field(json, RegistryConstants.ATTR_AUTOCOMMIT, bootstrap.getDefaultAutoCommit());
                    }
                    if (bootstrap.getDefaultTransactionIsolation() != null) {
                        JSONUtils.field(json, RegistryConstants.ATTR_TXN_ISOLATION, bootstrap.getDefaultTransactionIsolation());
                    }
                    JSONUtils.fieldNE(json, RegistryConstants.ATTR_DEFAULT_OBJECT, bootstrap.getDefaultObjectName());
                    if (bootstrap.isIgnoreErrors()) {
                        JSONUtils.field(json, RegistryConstants.ATTR_IGNORE_ERRORS, true);
                    }
                    JSONUtils.serializeStringList(json, RegistryConstants.TAG_QUERY, bootstrap.getInitQueries());
                    json.endObject();
                }
            }

            json.endObject();
        }

        {
            // Filters
            Collection<FilterMapping> filterMappings = dataSource.getObjectFilters();
            if (!CommonUtils.isEmpty(filterMappings)) {
                json.name(RegistryConstants.TAG_FILTERS);
                json.beginArray();
                for (FilterMapping filter : filterMappings) {
                    if (filter.defaultFilter != null && !filter.defaultFilter.isEmpty()) {
                        saveObjectFiler(json, filter.typeName, null, filter.defaultFilter);
                    }
                    for (Map.Entry<String, DBSObjectFilter> cf : filter.customFilters.entrySet()) {
                        if (!cf.getValue().isEmpty()) {
                            saveObjectFiler(json, filter.typeName, cf.getKey(), cf.getValue());
                        }
                    }
                }
                json.endArray();
            }
        }

        // Preferences
        {
            // Save only properties who are differs from default values
            SimplePreferenceStore prefStore = dataSource.getPreferenceStore();
            Map<String, String> props = new TreeMap<>();
            for (String propName : prefStore.preferenceNames()) {
                String propValue = prefStore.getString(propName);
                String defValue = prefStore.getDefaultString(propName);
                if (propValue != null && !CommonUtils.equalObjects(propValue, defValue)) {
                    props.put(propName, propValue);
                }
            }
            if (!props.isEmpty()) {
                JSONUtils.serializeProperties(json, RegistryConstants.TAG_CUSTOM_PROPERTIES, props);
            }
        }


        json.endObject();
    }

    private static void saveObjectFiler(JsonWriter json, String typeName, String objectID, DBSObjectFilter filter) throws IOException
    {
        json.beginObject();
        JSONUtils.fieldNE(json, RegistryConstants.ATTR_ID, objectID);
        JSONUtils.fieldNE(json, RegistryConstants.ATTR_TYPE, typeName);
        JSONUtils.fieldNE(json, RegistryConstants.ATTR_NAME, filter.getName());
        JSONUtils.fieldNE(json, RegistryConstants.ATTR_DESCRIPTION, filter.getDescription());

        if (!filter.isEnabled()) {
            JSONUtils.field(json, RegistryConstants.ATTR_ENABLED, false);
        }
        JSONUtils.serializeStringList(json, RegistryConstants.TAG_INCLUDE, filter.getInclude());
        JSONUtils.serializeStringList(json, RegistryConstants.TAG_EXCLUDE, filter.getExclude());
        json.endObject();
    }

    private static void saveSecuredCredentials(JsonWriter json, DataSourceDescriptor dataSource, String subNode, String userName, String password) throws IOException {
        boolean saved = DataSourceRegistry.saveSecuredCredentials(dataSource, subNode, userName, password);
        if (!saved) {
            try {
                if (!CommonUtils.isEmpty(userName)) {
                    JSONUtils.field(json, RegistryConstants.ATTR_USER, CommonUtils.notEmpty(userName));
                }
                if (!CommonUtils.isEmpty(password)) {
                    JSONUtils.field(json, RegistryConstants.ATTR_PASSWORD, ENCRYPTOR.encrypt(password));
                }
            } catch (EncryptionException e) {
                log.error("Error encrypting password", e);
            }
        }
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

}
