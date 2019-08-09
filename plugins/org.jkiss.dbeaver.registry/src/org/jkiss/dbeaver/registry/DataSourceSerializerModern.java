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
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourcePermission;
import org.jkiss.dbeaver.model.app.DBASecureStorage;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.connection.*;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.virtual.DBVModel;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.encode.ContentEncrypter;
import org.jkiss.dbeaver.runtime.encode.PasswordEncrypter;
import org.jkiss.dbeaver.runtime.encode.SimpleStringEncrypter;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

class DataSourceSerializerModern implements DataSourceSerializer
{
    private static final Log log = Log.getLog(DataSourceSerializerModern.class);
    private static final String NODE_CONNECTION = "#connection";

    private static PasswordEncrypter ENCRYPTOR = new SimpleStringEncrypter();

    private static Gson CONFIG_GSON = new GsonBuilder()
        .setLenient()
        .serializeNulls()
        .setPrettyPrinting()
        .create();
    private static Gson SECURE_GSON = new GsonBuilder()
        .setLenient()
        .serializeNulls()
        .create();

    private boolean passwordReadCanceled = false;

    private final DataSourceRegistry registry;
    // Secure props.
    //  0 level: datasource ID
    //  1 level: object type (connection or handler id)
    //  2 level: map of secured properties
    private Map<String, Map<String, Map<String, String>>> secureProperties = new LinkedHashMap<>();

    public DataSourceSerializerModern(DataSourceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void saveDataSources(
        DBRProgressMonitor monitor,
        boolean primaryConfig,
        List<DataSourceDescriptor> localDataSources,
        IFile configFile) throws DBException, IOException {
        ByteArrayOutputStream dsConfigBuffer = new ByteArrayOutputStream(10000);
        try (OutputStreamWriter osw = new OutputStreamWriter(dsConfigBuffer, StandardCharsets.UTF_8)) {
            try (JsonWriter jsonWriter = CONFIG_GSON.newJsonWriter(osw)) {
                jsonWriter.setIndent("\t");
                jsonWriter.beginObject();

                // Save folders
                if (primaryConfig) {
                    jsonWriter.name("folders");
                    jsonWriter.beginObject();
                    // Folders (only for default origin)
                    for (DataSourceFolder folder : registry.getAllFolders()) {
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
                            model.serialize(monitor, jsonWriter);
                        }
                        jsonWriter.endObject();
                    }
                    // Network profiles
                    List<DBWNetworkProfile> profiles = registry.getNetworkProfiles();
                    if (!CommonUtils.isEmpty(profiles)) {
                        jsonWriter.name("network-profiles");
                        jsonWriter.beginObject();
                        for (DBWNetworkProfile np : profiles) {
                            jsonWriter.name(np.getProfileName());
                            jsonWriter.beginObject();
                            JSONUtils.fieldNE(jsonWriter, RegistryConstants.ATTR_DESCRIPTION, np.getProfileDescription());
                            jsonWriter.name("handlers");
                            jsonWriter.beginObject();
                            for (DBWHandlerConfiguration configuration : np.getConfigurations()) {
                                if (configuration.hasValuableInfo()) {
                                    saveNetworkHandlerConfiguration(
                                        jsonWriter,
                                        null,
                                        np,
                                        configuration);
                                }
                            }
                            jsonWriter.endObject();
                            jsonWriter.endObject();
                        }
                        jsonWriter.endObject();
                    }
                    // Filters
                    List<DBSObjectFilter> savedFilters = registry.getSavedFilters();
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

        InputStream ifs = new ByteArrayInputStream(dsConfigBuffer.toByteArray());
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

        saveSecureCredentialsFile(monitor.getNestedMonitor(), (IFolder) configFile.getParent());
    }

    private void saveSecureCredentialsFile(IProgressMonitor monitor, IFolder parent) {

        IFile credFile = parent.getFile(DBPDataSourceRegistry.CREDENTIALS_CONFIG_FILE_NAME);
        try {
            // Serialize and encrypt
            String jsonString = SECURE_GSON.toJson(secureProperties, Map.class);
            ContentEncrypter encrypter = new ContentEncrypter(registry.getPlatform().getApplication().getSecureStorage().getLocalSecretKey());
            byte[] credData = encrypter.encrypt(jsonString);

            // Save to file

            if (secureProperties.isEmpty()) {
                credFile.delete(true, false, monitor);
            } else {
                // Encrypt whole file

                // Save result to file
                InputStream ifs = new ByteArrayInputStream(credData);

                if (!credFile.exists()) {
                    credFile.create(ifs, IResource.FORCE | IResource.HIDDEN | IResource.TEAM_PRIVATE, monitor);
                } else {
                    credFile.setContents(ifs, true, false, monitor);
                }
            }
        } catch (Exception e) {
            log.error("Error saving secure credentials", e);
        }
    }

    @Override
    public void parseDataSources(InputStream is, DataSourceOrigin origin, boolean refresh, DataSourceRegistry.ParseResults parseResults) throws IOException {
        // Read secured creds file
        IFolder mdFolder = registry.getProject().getMetadataFolder(false);
        if (mdFolder.exists()) {
            IFile credFile = mdFolder.getFile(DBPDataSourceRegistry.CREDENTIALS_CONFIG_FILE_NAME);
            if (credFile.exists()) {
                ByteArrayOutputStream credBuffer = new ByteArrayOutputStream();
                try (InputStream crdStream = credFile.getContents()) {
                    IOUtils.copyStream(crdStream, credBuffer);
                } catch (Exception e) {
                    log.error("Error reading secure credentials file", e);
                }
                ContentEncrypter encrypter = new ContentEncrypter(registry.getPlatform().getApplication().getSecureStorage().getLocalSecretKey());
                try {
                    String credJson = encrypter.decrypt(credBuffer.toByteArray());
                    Map<String, Map<String, Map<String, String>>> res =
                        CONFIG_GSON.fromJson(
                            credJson,
                            new TypeToken<Map<String, Map<String, Map<String, String>>>>(){}.getType());
                    secureProperties.putAll(res);
                } catch (Exception e) {
                    log.error("Error decrypting secure credentials", e);
                }

            }
        }

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
            // TODO: add drivers deserialization

            // Virtual models
            Map<String, DBVModel> modelMap = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Object>> vmMap : JSONUtils.getNestedObjects(jsonMap, "virtual-models")) {
                String id = vmMap.getKey();
                DBVModel model = new DBVModel(id, vmMap.getValue());
                modelMap.put(id, model);
            }

            // Network profiles
            for (Map.Entry<String, Map<String, Object>> vmMap : JSONUtils.getNestedObjects(jsonMap, "network-profiles")) {
                String profileName = vmMap.getKey();
                Map<String, Object> profileMap = vmMap.getValue();
                DBWNetworkProfile profile = new DBWNetworkProfile();
                profile.setProfileName(profileName);
                profile.setProperties(JSONUtils.deserializeStringMap(profileMap, "properties"));

                for (Map.Entry<String, Map<String, Object>> handlerMap : JSONUtils.getNestedObjects(profileMap, "handlers")) {
                    DBWHandlerConfiguration configuration = parseNetworkHandlerConfig(null, profile, handlerMap);
                    if (configuration != null) {
                        profile.updateConfiguration(configuration);
                    }
                }

                registry.updateNetworkProfile(profile);
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
                        final String[] creds = readSecuredCredentials(cfgObject, dataSource, null, null);
                        config.setUserName(creds[0]);
                        if (dataSource.isSavePassword()) {
                            config.setUserPassword(creds[1]);
                        }
                    }
                    config.setClientHomeId(JSONUtils.getString(cfgObject, RegistryConstants.ATTR_HOME));
                    config.setConfigProfileName(JSONUtils.getString(cfgObject, "config-profile"));
                    config.setUserProfileName(JSONUtils.getString(cfgObject, "user-profile"));
                    config.setConnectionType(
                        DataSourceProviderRegistry.getInstance().getConnectionType(
                            JSONUtils.getString(cfgObject, RegistryConstants.ATTR_TYPE), DBPConnectionType.DEFAULT_TYPE));
                    String colorValue = JSONUtils.getString(cfgObject, RegistryConstants.ATTR_COLOR);
                    if (!CommonUtils.isEmpty(colorValue)) {
                        config.setConnectionColor(colorValue);
                    }
                    int keepAlive = JSONUtils.getInteger(cfgObject, RegistryConstants.ATTR_KEEP_ALIVE);
                    if (keepAlive > 0) {
                        config.setKeepAliveInterval(keepAlive);
                    }
                    config.setProperties(JSONUtils.deserializeStringMap(cfgObject, RegistryConstants.TAG_PROPERTIES));
                    config.setProviderProperties(JSONUtils.deserializeStringMap(cfgObject, RegistryConstants.TAG_PROVIDER_PROPERTIES));

                    // Events
                    for (Map.Entry<String, Map<String, Object>> eventObject : JSONUtils.getNestedObjects(cfgObject, RegistryConstants.TAG_EVENTS)) {
                        DBPConnectionEventType eventType = CommonUtils.valueOf(DBPConnectionEventType.class, eventObject.getKey(), DBPConnectionEventType.BEFORE_CONNECT);
                        Map<String, Object> eventCfg = eventObject.getValue();
                        DBRShellCommand command = new DBRShellCommand("");
                        command.setEnabled(JSONUtils.getBoolean(eventCfg, RegistryConstants.ATTR_ENABLED));
                        command.setShowProcessPanel(JSONUtils.getBoolean(eventCfg, RegistryConstants.ATTR_SHOW_PANEL));
                        command.setWaitProcessFinish(JSONUtils.getBoolean(eventCfg, RegistryConstants.ATTR_WAIT_PROCESS));
                        if (command.isWaitProcessFinish()) {
                            command.setWaitProcessTimeoutMs(JSONUtils.getInteger(eventCfg, RegistryConstants.ATTR_WAIT_PROCESS_TIMEOUT));
                        }
                        command.setTerminateAtDisconnect(JSONUtils.getBoolean(eventCfg, RegistryConstants.ATTR_TERMINATE_AT_DISCONNECT));
                        command.setPauseAfterExecute(JSONUtils.getInteger(eventCfg, RegistryConstants.ATTR_PAUSE_AFTER_EXECUTE));
                        command.setWorkingDirectory(JSONUtils.getString(eventCfg, RegistryConstants.ATTR_WORKING_DIRECTORY));
                        command.setCommand(JSONUtils.getString(eventCfg, RegistryConstants.ATTR_COMMAND));

                        config.setEvent(eventType, command);
                    }

                    // Handlers
                    for (Map.Entry<String, Map<String, Object>> handlerObject : JSONUtils.getNestedObjects(cfgObject, RegistryConstants.TAG_HANDLERS)) {
                        DBWHandlerConfiguration configuration = parseNetworkHandlerConfig(dataSource, null, handlerObject);
                        if (configuration != null) {
                            dataSource.getConnectionConfiguration().updateHandler(configuration);
                        }
                    }

                    // Bootstrap
                    Map<String, Object> bootstrapCfg = JSONUtils.getObject(cfgObject, RegistryConstants.TAG_BOOTSTRAP);
                    if (bootstrapCfg.containsKey(RegistryConstants.ATTR_AUTOCOMMIT)) {
                        config.getBootstrap().setDefaultAutoCommit(JSONUtils.getBoolean(bootstrapCfg, RegistryConstants.ATTR_AUTOCOMMIT));
                    }
                    if (bootstrapCfg.containsKey(RegistryConstants.ATTR_TXN_ISOLATION)) {
                        config.getBootstrap().setDefaultTransactionIsolation(JSONUtils.getInteger(bootstrapCfg, RegistryConstants.ATTR_TXN_ISOLATION));
                    }
                    config.getBootstrap().setDefaultObjectName(JSONUtils.getString(bootstrapCfg, RegistryConstants.ATTR_DEFAULT_OBJECT));
                    if (bootstrapCfg.containsKey(RegistryConstants.ATTR_IGNORE_ERRORS)) {
                        config.getBootstrap().setIgnoreErrors(JSONUtils.getBoolean(bootstrapCfg, RegistryConstants.ATTR_IGNORE_ERRORS));
                    }
                    config.getBootstrap().setInitQueries(JSONUtils.deserializeStringList(bootstrapCfg, RegistryConstants.TAG_QUERY));
                }

                // Permissions
                {
                    Map<String, Object> securityCfg = JSONUtils.getObject(conObject, "security");
                    if (!CommonUtils.isEmpty(securityCfg)) {
                        List<String> permissionRestrictions = JSONUtils.deserializeStringList(securityCfg, "permission-restrictions");
                        if (!CommonUtils.isEmpty(permissionRestrictions)) {
                            List<DBPDataSourcePermission> permissions = new ArrayList<>();
                            for (String perm : permissionRestrictions) {
                                try {
                                    DBPDataSourcePermission permission = DBPDataSourcePermission.getById(perm);
                                    if (permission != null) {
                                        permissions.add(permission);
                                    }
                                } catch (IllegalArgumentException e) {
                                    log.debug(e);
                                }
                            }
                            if (!permissions.isEmpty()) {
                                dataSource.setModifyPermissions(permissions);
                            }
                        }
                    }
                }

                // Filters
                for (Map<String, Object> filterCfg : JSONUtils.getObjectList(conObject, RegistryConstants.TAG_FILTERS)) {
                    String typeName = JSONUtils.getString(filterCfg, RegistryConstants.ATTR_TYPE);
                    String objectID = JSONUtils.getString(filterCfg, RegistryConstants.ATTR_ID);
                    if (!CommonUtils.isEmpty(typeName)) {
                        DBSObjectFilter filter = readObjectFiler(filterCfg);
                        dataSource.updateObjectFilter(typeName, objectID, filter);
                    }
                }

                // Preferences
                dataSource.getPreferenceStore().getProperties().putAll(
                    JSONUtils.deserializeStringMap(conObject, RegistryConstants.TAG_CUSTOM_PROPERTIES)
                );

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

            // Saved filters
            for (Map<String, Object> ctMap : JSONUtils.getObjectList(jsonMap, "saved-filters")) {
                DBSObjectFilter filter = readObjectFiler(ctMap);
                registry.addSavedFilter(filter);
            }
        }

    }

    @Nullable
    private DBWHandlerConfiguration parseNetworkHandlerConfig(
        @Nullable DataSourceDescriptor dataSource,
        @Nullable DBWNetworkProfile profile,
        @NotNull Map.Entry<String, Map<String, Object>> handlerObject)
    {
        String handlerId = handlerObject.getKey();
        Map<String, Object> handlerCfg = handlerObject.getValue();

        NetworkHandlerDescriptor handlerDescriptor = NetworkHandlerRegistry.getInstance().getDescriptor(handlerId);
        if (handlerDescriptor == null) {
            log.warn("Can't find network handler '" + handlerId + "'");
            return null;
        } else {
            DBWHandlerConfiguration curNetworkHandler = new DBWHandlerConfiguration(handlerDescriptor, dataSource == null ? null : dataSource.getDriver());
            curNetworkHandler.setEnabled(JSONUtils.getBoolean(handlerCfg, RegistryConstants.ATTR_ENABLED));
            curNetworkHandler.setSavePassword(JSONUtils.getBoolean(handlerCfg, RegistryConstants.ATTR_SAVE_PASSWORD));
            if (!passwordReadCanceled) {
                final String[] creds = readSecuredCredentials(handlerCfg, dataSource, profile, "network/" + handlerId);
                curNetworkHandler.setUserName(creds[0]);
                if (curNetworkHandler.isSavePassword()) {
                    curNetworkHandler.setPassword(creds[1]);
                }
            }
            Map<String, Object> properties = JSONUtils.deserializeProperties(handlerCfg, RegistryConstants.TAG_PROPERTIES);
            if (properties != null) {
                curNetworkHandler.setProperties(properties);
            }
            return curNetworkHandler;
        }
    }

    private static DBSObjectFilter readObjectFiler(Map<String, Object> map) {
        DBSObjectFilter filter = new DBSObjectFilter();
        filter.setName(JSONUtils.getString(map, RegistryConstants.ATTR_NAME));
        filter.setDescription(JSONUtils.getString(map, RegistryConstants.ATTR_DESCRIPTION));
        filter.setEnabled(JSONUtils.getBoolean(map, RegistryConstants.ATTR_ENABLED));
        filter.setInclude(JSONUtils.deserializeStringList(map, RegistryConstants.TAG_INCLUDE));
        filter.setExclude(JSONUtils.deserializeStringList(map, RegistryConstants.TAG_EXCLUDE));
        return filter;
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

    private void saveDataSource(@NotNull JsonWriter json, @NotNull DataSourceDescriptor dataSource)
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

            saveSecuredCredentials(
                dataSource,
                null,
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
            JSONUtils.fieldNE(json, "config-profile", connectionInfo.getConfigProfileName());
            JSONUtils.fieldNE(json, "user-profile", connectionInfo.getUserProfileName());
            JSONUtils.serializeProperties(json, RegistryConstants.TAG_PROPERTIES, connectionInfo.getProperties());
            JSONUtils.serializeProperties(json, RegistryConstants.TAG_PROVIDER_PROPERTIES, connectionInfo.getProviderProperties());

            // Save events
            if (!ArrayUtils.isEmpty(connectionInfo.getDeclaredEvents())) {
                json.name(RegistryConstants.TAG_EVENTS);
                json.beginObject();
                for (DBPConnectionEventType eventType : connectionInfo.getDeclaredEvents()) {
                    DBRShellCommand command = connectionInfo.getEvent(eventType);
                    json.name(eventType.name());
                    json.beginObject();
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
                json.endObject();
            }

            // Save network handlers' configurations
            if (!CommonUtils.isEmpty(connectionInfo.getHandlers())) {
                json.name(RegistryConstants.TAG_HANDLERS);
                json.beginObject();
                for (DBWHandlerConfiguration configuration : connectionInfo.getHandlers()) {
                    saveNetworkHandlerConfiguration(json, dataSource, null, configuration);
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
            // Permissions
            List<DBPDataSourcePermission> permissions = dataSource.getModifyPermission();
            if (!CommonUtils.isEmpty(permissions)) {
                json.name("security");
                json.beginObject();
                List<String> permIds = new ArrayList<>(permissions.size());
                for (DBPDataSourcePermission perm : permissions) permIds.add(perm.getId());
                JSONUtils.serializeStringList(json, "permission-restrictions", permIds);
                json.endObject();
            }
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

    private void saveNetworkHandlerConfiguration(
        @NotNull JsonWriter json,
        @Nullable DataSourceDescriptor dataSource,
        @Nullable DBWNetworkProfile  profile,
        @NotNull DBWHandlerConfiguration configuration) throws IOException
    {
        json.name(CommonUtils.notEmpty(configuration.getId()));
        json.beginObject();
        JSONUtils.field(json, RegistryConstants.ATTR_TYPE, configuration.getType().name());
        JSONUtils.field(json, RegistryConstants.ATTR_ENABLED, configuration.isEnabled());
        JSONUtils.field(json, RegistryConstants.ATTR_SAVE_PASSWORD, configuration.isSavePassword());
        if (!CommonUtils.isEmpty(configuration.getUserName()) || !CommonUtils.isEmpty(configuration.getPassword())) {
            saveSecuredCredentials(
                dataSource,
                profile,
                "network/" + configuration.getId(),
                configuration.getUserName(),
                configuration.isSavePassword() ? configuration.getPassword() : null);
        }
        JSONUtils.serializeProperties(json, RegistryConstants.TAG_PROPERTIES, configuration.getProperties());
        json.endObject();
    }

    private static void saveObjectFiler(JsonWriter json, String typeName, String objectID, DBSObjectFilter filter) throws IOException
    {
        json.beginObject();
        JSONUtils.fieldNE(json, RegistryConstants.ATTR_ID, objectID);
        JSONUtils.fieldNE(json, RegistryConstants.ATTR_TYPE, typeName);
        JSONUtils.fieldNE(json, RegistryConstants.ATTR_NAME, filter.getName());
        JSONUtils.fieldNE(json, RegistryConstants.ATTR_DESCRIPTION, filter.getDescription());
        JSONUtils.field(json, RegistryConstants.ATTR_ENABLED, filter.isEnabled());
        JSONUtils.serializeStringList(json, RegistryConstants.TAG_INCLUDE, filter.getInclude());
        JSONUtils.serializeStringList(json, RegistryConstants.TAG_EXCLUDE, filter.getExclude());
        json.endObject();
    }

    private void saveSecuredCredentials(
        @Nullable DataSourceDescriptor dataSource,
        @Nullable DBWNetworkProfile  profile,
        @Nullable String subNode,
        @Nullable String userName,
        @Nullable String password) {
        boolean saved = DataSourceRegistry.saveCredentialsInSecuredStorage(
            registry.getProject(), dataSource, subNode, userName, password);
        if (!saved) {
            String topNodeId = profile != null ? "profile:" + profile.getProfileName() : dataSource.getId();
            if (subNode == null) subNode = NODE_CONNECTION;

            Map<String, Map<String, String>> nodeMap = secureProperties.computeIfAbsent(topNodeId, s -> new LinkedHashMap<>());
            Map<String, String> propMap = nodeMap.computeIfAbsent(subNode, s -> new LinkedHashMap<>());
            if (!CommonUtils.isEmpty(userName)) {
                propMap.put(RegistryConstants.ATTR_USER, CommonUtils.notEmpty(userName));
            }
            if (!CommonUtils.isEmpty(password)) {
                propMap.put(RegistryConstants.ATTR_PASSWORD, password);
            }
        }
    }

    private String[] readSecuredCredentials(
        @NotNull Map<String, Object> map,
        @Nullable DataSourceDescriptor dataSource,
        @Nullable DBWNetworkProfile profile,
        @Nullable String subNode)
    {
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
        String topNodeId = profile != null ? "profile:" + profile.getProfileName() : dataSource.getId();
        if (subNode == null) subNode = NODE_CONNECTION;

        Map<String, Map<String, String>> subMap = secureProperties.get(topNodeId);
        if (subMap != null) {
            Map<String, String> propMap = subMap.get(subNode);
            if (propMap != null) {
                if (CommonUtils.isEmpty(creds[0])) {
                    creds[0] = propMap.get(RegistryConstants.ATTR_USER);
                }
                if (CommonUtils.isEmpty(creds[1])) {
                    creds[1] = propMap.get(RegistryConstants.ATTR_PASSWORD);
                }
            }
        }

        return creds;
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
