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
import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;
import org.jkiss.dbeaver.model.DBPDataSourcePermission;
import org.jkiss.dbeaver.model.DBPDataSourcePermissionOwner;
import org.jkiss.dbeaver.model.access.DBAAuthProfile;
import org.jkiss.dbeaver.model.app.DBASecureStorage;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
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
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

class DataSourceSerializerModern implements DataSourceSerializer
{
    // Navigator settings
    static final String ATTR_NAVIGATOR_SHOW_SYSTEM_OBJECTS = "show-system-objects"; //$NON-NLS-1$
    static final String ATTR_NAVIGATOR_SHOW_UTIL_OBJECTS = "show-util-objects"; //$NON-NLS-1$
    static final String ATTR_NAVIGATOR_SHOW_ONLY_ENTITIES = "navigator-show-only-entities"; //$NON-NLS-1$
    static final String ATTR_NAVIGATOR_HIDE_FOLDERS = "navigator-hide-folders"; //$NON-NLS-1$
    static final String ATTR_NAVIGATOR_HIDE_SCHEMAS = "navigator-hide-schemas"; //$NON-NLS-1$
    static final String ATTR_NAVIGATOR_HIDE_VIRTUAL = "navigator-hide-virtual"; //$NON-NLS-1$
    static final String ATTR_NAVIGATOR_MERGE_ENTITIES = "navigator-merge-entities"; //$NON-NLS-1$

    private static final Log log = Log.getLog(DataSourceSerializerModern.class);
    private static final String NODE_CONNECTION = "#connection";

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
    private boolean passwordWriteCanceled = false;

    private final DataSourceRegistry registry;
    // Secure props.
    //  0 level: datasource ID
    //  1 level: object type (connection or handler id)
    //  2 level: map of secured properties
    private Map<String, Map<String, Map<String, String>>> secureProperties = new LinkedHashMap<>();

    DataSourceSerializerModern(DataSourceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void saveDataSources(
        DBRProgressMonitor monitor,
        DBPDataSourceConfigurationStorage configurationStorage,
        List<DataSourceDescriptor> localDataSources,
        IFile configFile) throws DBException
    {
        ByteArrayOutputStream dsConfigBuffer = new ByteArrayOutputStream(10000);
        try (OutputStreamWriter osw = new OutputStreamWriter(dsConfigBuffer, StandardCharsets.UTF_8)) {
            try (JsonWriter jsonWriter = CONFIG_GSON.newJsonWriter(osw)) {
                jsonWriter.setIndent("\t");
                jsonWriter.beginObject();

                // Save folders
                if (configurationStorage.isDefault()) {
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
                        if (!dataSource.isDetached()) {
                            saveDataSource(jsonWriter, dataSource);
                            if (dataSource.getVirtualModel().hasValuableData()) {
                                virtualModels.put(dataSource.getVirtualModel().getId(), dataSource.getVirtualModel());
                            }
                            DBPConnectionType connectionType = dataSource.getConnectionConfiguration().getConnectionType();
                            /*if (!connectionType.isPredefined()) */{
                                connectionTypes.put(connectionType.getId(), connectionType);
                            }
                            DBPDriver driver = dataSource.getDriver();
                            if (driver.isCustom() && !driver.getProviderDescriptor().isTemporary()) {
                                Map<String, DBPDriver> driverMap = drivers.computeIfAbsent(driver.getProviderId(), s -> new LinkedHashMap<>());
                                driverMap.put(driver.getId(), driver);
                            }
                        }
                    }
                    jsonWriter.endObject();
                }

                if (configurationStorage.isDefault()) {
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
                            jsonWriter.name(np.getProfileId());
                            jsonWriter.beginObject();
                            JSONUtils.fieldNE(jsonWriter, RegistryConstants.ATTR_NAME, np.getProfileName());
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
                    // Auth profiles
                    List<DBAAuthProfile> authProfiles = registry.getAllAuthProfiles();
                    if (!CommonUtils.isEmpty(authProfiles)) {
                        jsonWriter.name("auth-profiles");
                        jsonWriter.beginObject();
                        for (DBAAuthProfile authProfile : authProfiles) {
                            jsonWriter.name(authProfile.getProfileId());
                            jsonWriter.beginObject();
                            JSONUtils.fieldNE(jsonWriter, RegistryConstants.ATTR_NAME, authProfile.getProfileName());
                            JSONUtils.fieldNE(jsonWriter, RegistryConstants.ATTR_DESCRIPTION, authProfile.getProfileDescription());
                            JSONUtils.fieldNE(jsonWriter, RegistryConstants.ATTR_AUTH_MODEL, authProfile.getAuthModelId());
                            if (authProfile.isSavePassword()) {
                                JSONUtils.field(jsonWriter, RegistryConstants.ATTR_SAVE_PASSWORD, authProfile.isSavePassword());
                            }
                            // Save all auth properties in secure storage
                            saveSecuredCredentials(null, authProfile, null, new SecureCredentials(authProfile));
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
                            serializeModifyPermissions(jsonWriter, ct);
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

        String jsonString = new String(dsConfigBuffer.toByteArray(), StandardCharsets.UTF_8);
        boolean encryptProject = CommonUtils.toBoolean(registry.getProject().getProjectProperty(DBPProject.PROP_SECURE_PROJECT));
        saveConfigFile(monitor.getNestedMonitor(), configFile, jsonString, false, encryptProject);
        try {
            configFile.setHidden(true);
        } catch (CoreException e) {
            log.debug(e);
        }

        {
            saveSecureCredentialsFile(
                monitor.getNestedMonitor(),
                (IFolder) configFile.getParent(),
                configurationStorage);
        }
    }

    private String loadConfigFile(IFile file, boolean decrypt) throws IOException {
        ByteArrayOutputStream credBuffer = new ByteArrayOutputStream();
        try (InputStream crdStream = file.getContents()) {
            IOUtils.copyStream(crdStream, credBuffer);
        } catch (Exception e) {
            log.error("Error reading secure credentials file", e);
        }
        if (!decrypt) {
            return new String(credBuffer.toByteArray(), StandardCharsets.UTF_8);
        } else {
            ContentEncrypter encrypter = new ContentEncrypter(registry.getProject().getSecureStorage().getLocalSecretKey());
            try {
                return encrypter.decrypt(credBuffer.toByteArray());
            } catch (Exception e) {
                throw new IOException("Error decrypting encrypted file", e);
            }
        }
    }

    private void saveConfigFile(IProgressMonitor monitor, IFile configFile, String contents, boolean teamPrivate, boolean encrypt) {
        try {
            byte[] binaryContents;
            if (encrypt) {
                // Serialize and encrypt
                ContentEncrypter encrypter = new ContentEncrypter(registry.getProject().getSecureStorage().getLocalSecretKey());
                binaryContents = encrypter.encrypt(contents);
            } else {
                binaryContents = contents.getBytes(StandardCharsets.UTF_8);
            }

            // Save result to file
            InputStream ifs = new ByteArrayInputStream(binaryContents);

            if (!configFile.exists()) {
                int updateFlags = IResource.FORCE | IResource.HIDDEN;
                if (teamPrivate) updateFlags |= IResource.TEAM_PRIVATE;
                configFile.create(ifs, updateFlags, monitor);
            } else {
                configFile.setContents(ifs, true, false, monitor);
            }
        } catch (Exception e) {
            log.error("Error saving configuration file " + configFile.getLocation().toFile().getAbsolutePath(), e);
        }
    }

    private void saveSecureCredentialsFile(IProgressMonitor monitor, IFolder parent, DBPDataSourceConfigurationStorage origin) {
        IFile credFile = parent.getFile(DBPDataSourceRegistry.CREDENTIALS_CONFIG_FILE_PREFIX + origin.getConfigurationFileSuffix() + DBPDataSourceRegistry.CREDENTIALS_CONFIG_FILE_EXT);
        try {
            ContentUtils.makeFileBackup(credFile);
            if (secureProperties.isEmpty()) {
                credFile.delete(true, false, monitor);
            } else {
                // Serialize and encrypt
                String jsonString = SECURE_GSON.toJson(secureProperties, Map.class);
                saveConfigFile(monitor, credFile, jsonString, true, true);
            }
        } catch (Exception e) {
            log.error("Error saving secure credentials", e);
        }
    }

    @Override
    public void parseDataSources(IFile configFile, DBPDataSourceConfigurationStorage configurationStorage, boolean refresh, DataSourceRegistry.ParseResults parseResults) throws IOException {
        // Read secured creds file
        IFolder mdFolder = registry.getProject().getMetadataFolder(false);
        if (mdFolder.exists()) {
            IFile credFile = mdFolder.getFile(DBPDataSourceRegistry.CREDENTIALS_CONFIG_FILE_PREFIX + configurationStorage.getConfigurationFileSuffix() + DBPDataSourceRegistry.CREDENTIALS_CONFIG_FILE_EXT);
            if (credFile.exists()) {
                try {
                    String credJson = loadConfigFile(credFile, true);
                    Map<String, Map<String, Map<String, String>>> res = CONFIG_GSON.fromJson(
                        credJson,
                        new TypeToken<Map<String, Map<String, Map<String, String>>>>(){}.getType());
                    secureProperties.putAll(res);
                } catch (Exception e) {
                    log.error("Error decrypting secure credentials", e);
                }
            }
        }

        boolean decryptProject = CommonUtils.toBoolean(registry.getProject().getProjectProperty(DBPProject.PROP_SECURE_PROJECT));
        String configJson = loadConfigFile(configFile, decryptProject);
        {
            Map<String, Object> jsonMap = JSONUtils.parseMap(CONFIG_GSON, new StringReader(configJson));

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
                Map<String, Object> ctConfig = ctMap.getValue();
                String name = JSONUtils.getObjectProperty(ctConfig, RegistryConstants.ATTR_NAME);
                String description = JSONUtils.getObjectProperty(ctConfig, RegistryConstants.ATTR_DESCRIPTION);
                String color = JSONUtils.getObjectProperty(ctConfig, RegistryConstants.ATTR_COLOR);
                Boolean autoCommit = JSONUtils.getObjectProperty(ctConfig, "auto-commit");
                Boolean confirmExecute = JSONUtils.getObjectProperty(ctConfig, "confirm-execute");
                Boolean confirmDataChange = JSONUtils.getObjectProperty(ctConfig, "confirm-data-change");
                DBPConnectionType ct = DBWorkbench.getPlatform().getDataSourceProviderRegistry().getConnectionType(id, null);
                if (ct == null) {
                    ct = new DBPConnectionType(id, name, color, description, CommonUtils.toBoolean(autoCommit), CommonUtils.toBoolean(confirmExecute), CommonUtils.toBoolean(confirmDataChange));
                    DBWorkbench.getPlatform().getDataSourceProviderRegistry().addConnectionType(ct);
                }
                deserializeModifyPermissions(ctConfig, ct);
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
                String profileId = vmMap.getKey();
                Map<String, Object> profileMap = vmMap.getValue();
                DBWNetworkProfile profile = new DBWNetworkProfile();
                profile.setProfileName(profileId);
                profile.setProfileName(profileId);
                profile.setProperties(JSONUtils.deserializeStringMap(profileMap, "properties"));

                for (Map.Entry<String, Map<String, Object>> handlerMap : JSONUtils.getNestedObjects(profileMap, "handlers")) {
                    DBWHandlerConfiguration configuration = parseNetworkHandlerConfig(null, profile, handlerMap);
                    if (configuration != null) {
                        profile.updateConfiguration(configuration);
                    }
                }

                registry.updateNetworkProfile(profile);
            }

            // Auth profiles
            for (Map.Entry<String, Map<String, Object>> vmMap : JSONUtils.getNestedObjects(jsonMap, "auth-profiles")) {
                String profileId = vmMap.getKey();
                Map<String, Object> profileMap = vmMap.getValue();
                DBAAuthProfile profile = new DBAAuthProfile();
                profile.setProfileId(profileId);
                profile.setProfileName(JSONUtils.getString(profileMap, RegistryConstants.ATTR_NAME));
                profile.setAuthModelId(JSONUtils.getString(profileMap, RegistryConstants.ATTR_AUTH_MODEL));
                profile.setSavePassword(JSONUtils.getBoolean(profileMap, RegistryConstants.ATTR_SAVE_PASSWORD));

                SecureCredentials authCreds = readSecuredCredentials(null, profile, null);
                profile.setUserName(authCreds.getUserName());
                profile.setUserPassword(authCreds.getUserPassword());
                profile.setProperties(authCreds.getProperties());

                registry.updateAuthProfile(profile);
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
                        configurationStorage,
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
                dataSource.setDescription(JSONUtils.getString(conObject, RegistryConstants.TAG_DESCRIPTION));
                dataSource.setSavePassword(JSONUtils.getBoolean(conObject, RegistryConstants.ATTR_SAVE_PASSWORD));

                DataSourceNavigatorSettings navSettings = dataSource.getNavigatorSettings();
                navSettings.setShowSystemObjects(JSONUtils.getBoolean(conObject, DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_SYSTEM_OBJECTS));
                navSettings.setShowUtilityObjects(JSONUtils.getBoolean(conObject, DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_UTIL_OBJECTS));
                navSettings.setShowOnlyEntities(JSONUtils.getBoolean(conObject, DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_ONLY_ENTITIES));
                navSettings.setHideFolders(JSONUtils.getBoolean(conObject, DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_FOLDERS));
                navSettings.setHideSchemas(JSONUtils.getBoolean(conObject, DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_SCHEMAS));
                navSettings.setHideVirtualModel(JSONUtils.getBoolean(conObject, DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_VIRTUAL));
                navSettings.setMergeEntities(JSONUtils.getBoolean(conObject, DataSourceSerializerModern.ATTR_NAVIGATOR_MERGE_ENTITIES));

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
                        final SecureCredentials creds = readSecuredCredentials(dataSource, null, null);
                        config.setUserName(creds.getUserName());
                        if (dataSource.isSavePassword()) {
                            config.setUserPassword(creds.getUserPassword());
                        }
                    }
                    {
                        // Still try to read credentials directly from configuration (#6564)
                        String userName = JSONUtils.getString(cfgObject, RegistryConstants.ATTR_USER);
                        if (!CommonUtils.isEmpty(userName)) config.setUserName(userName);
                        String userPassword = JSONUtils.getString(cfgObject, RegistryConstants.ATTR_PASSWORD);
                        if (!CommonUtils.isEmpty(userPassword)) config.setUserPassword(userPassword);
                    }

                    config.setClientHomeId(JSONUtils.getString(cfgObject, RegistryConstants.ATTR_HOME));
                    config.setConfigProfileName(JSONUtils.getString(cfgObject, "config-profile"));
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
                    config.setAuthModelId(JSONUtils.getString(cfgObject, RegistryConstants.ATTR_AUTH_MODEL));
                    config.setAuthProperties(JSONUtils.deserializeStringMapOrNull(cfgObject, "auth-properties"));

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
                    DBPConnectionBootstrap bootstrap = config.getBootstrap();
                    if (bootstrapCfg.containsKey(RegistryConstants.ATTR_AUTOCOMMIT)) {
                        bootstrap.setDefaultAutoCommit(JSONUtils.getBoolean(bootstrapCfg, RegistryConstants.ATTR_AUTOCOMMIT));
                    }
                    if (bootstrapCfg.containsKey(RegistryConstants.ATTR_TXN_ISOLATION)) {
                        bootstrap.setDefaultTransactionIsolation(JSONUtils.getInteger(bootstrapCfg, RegistryConstants.ATTR_TXN_ISOLATION));
                    }
                    bootstrap.setDefaultCatalogName(JSONUtils.getString(bootstrapCfg, RegistryConstants.ATTR_DEFAULT_CATALOG));
                    bootstrap.setDefaultSchemaName(JSONUtils.getString(bootstrapCfg, RegistryConstants.ATTR_DEFAULT_SCHEMA));
                    String defObjectName = JSONUtils.getString(bootstrapCfg, RegistryConstants.ATTR_DEFAULT_OBJECT);
                    if (!CommonUtils.isEmpty(defObjectName) && CommonUtils.isEmpty(bootstrap.getDefaultSchemaName())) {
                        bootstrap.setDefaultSchemaName(JSONUtils.getString(bootstrapCfg, defObjectName));
                    }

                    if (bootstrapCfg.containsKey(RegistryConstants.ATTR_IGNORE_ERRORS)) {
                        bootstrap.setIgnoreErrors(JSONUtils.getBoolean(bootstrapCfg, RegistryConstants.ATTR_IGNORE_ERRORS));
                    }
                    bootstrap.setInitQueries(JSONUtils.deserializeStringList(bootstrapCfg, RegistryConstants.TAG_QUERY));
                }

                // Permissions
                {
                    deserializeModifyPermissions(conObject, dataSource);
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

    private void deserializeModifyPermissions(Map<String, Object> conObject, DBPDataSourcePermissionOwner permissionOwner) {
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
                    permissionOwner.setModifyPermissions(permissions);
                }
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
            DBWHandlerConfiguration curNetworkHandler = new DBWHandlerConfiguration(handlerDescriptor, dataSource);
            curNetworkHandler.setEnabled(JSONUtils.getBoolean(handlerCfg, RegistryConstants.ATTR_ENABLED));
            curNetworkHandler.setSavePassword(JSONUtils.getBoolean(handlerCfg, RegistryConstants.ATTR_SAVE_PASSWORD));
            if (!passwordReadCanceled) {
                final SecureCredentials creds = readSecuredCredentials(dataSource, profile,
                    "network/" + handlerId + (profile == null ? "" : "/profile/" + profile.getProfileName()));
                curNetworkHandler.setUserName(creds.getUserName());
                if (curNetworkHandler.isSavePassword()) {
                    curNetworkHandler.setPassword(creds.getUserPassword());
                }
            }
            {
                // Still try to read credentials directly from configuration (#6564)
                String userName = JSONUtils.getString(handlerCfg, RegistryConstants.ATTR_USER);
                if (!CommonUtils.isEmpty(userName)) curNetworkHandler.setUserName(userName);
                String userPassword = JSONUtils.getString(handlerCfg, RegistryConstants.ATTR_PASSWORD);
                if (!CommonUtils.isEmpty(userPassword)) curNetworkHandler.setPassword(userPassword);
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

        DataSourceNavigatorSettings navSettings = dataSource.getNavigatorSettings();
        if (navSettings.isShowSystemObjects()) JSONUtils.field(json, ATTR_NAVIGATOR_SHOW_SYSTEM_OBJECTS, true);
        if (navSettings.isShowUtilityObjects()) JSONUtils.field(json, ATTR_NAVIGATOR_SHOW_UTIL_OBJECTS, true);
        if (navSettings.isShowOnlyEntities()) JSONUtils.field(json, ATTR_NAVIGATOR_SHOW_ONLY_ENTITIES, true);
        if (navSettings.isHideFolders()) JSONUtils.field(json, ATTR_NAVIGATOR_HIDE_FOLDERS, true);
        if (navSettings.isHideSchemas()) JSONUtils.field(json, ATTR_NAVIGATOR_HIDE_SCHEMAS, true);
        if (navSettings.isHideVirtualModel()) JSONUtils.field(json, ATTR_NAVIGATOR_HIDE_VIRTUAL, true);
        if (navSettings.isMergeEntities()) JSONUtils.field(json, ATTR_NAVIGATOR_MERGE_ENTITIES, true);

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
                new SecureCredentials(dataSource));

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
            JSONUtils.serializeProperties(json, RegistryConstants.TAG_PROPERTIES, connectionInfo.getProperties());
            JSONUtils.serializeProperties(json, RegistryConstants.TAG_PROVIDER_PROPERTIES, connectionInfo.getProviderProperties());
            JSONUtils.fieldNE(json, RegistryConstants.ATTR_AUTH_MODEL, connectionInfo.getAuthModelId());
            JSONUtils.serializeProperties(json, "auth-properties", connectionInfo.getAuthProperties());

            // Save events
            if (!ArrayUtils.isEmpty(connectionInfo.getDeclaredEvents())) {
                json.name(RegistryConstants.TAG_EVENTS);
                json.beginObject();
                for (DBPConnectionEventType eventType : connectionInfo.getDeclaredEvents()) {
                    DBRShellCommand command = connectionInfo.getEvent(eventType);
                    if (!command.isEnabled()) {
                        continue;
                    }
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
                    if (configuration.isEnabled()) {
                        saveNetworkHandlerConfiguration(json, dataSource, null, configuration);
                    }
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
                    JSONUtils.fieldNE(json, RegistryConstants.ATTR_DEFAULT_CATALOG, bootstrap.getDefaultCatalogName());
                    JSONUtils.fieldNE(json, RegistryConstants.ATTR_DEFAULT_SCHEMA, bootstrap.getDefaultSchemaName());
                    if (bootstrap.isIgnoreErrors()) {
                        JSONUtils.field(json, RegistryConstants.ATTR_IGNORE_ERRORS, true);
                    }
                    JSONUtils.serializeStringList(json, RegistryConstants.TAG_QUERY, bootstrap.getInitQueries());
                    json.endObject();
                }
            }

            json.endObject();
        }

        // Permissions
        serializeModifyPermissions(json, dataSource);

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

    private void serializeModifyPermissions(@NotNull JsonWriter json, DBPDataSourcePermissionOwner permissionOwner) throws IOException {
        List<DBPDataSourcePermission> permissions = permissionOwner.getModifyPermission();
        if (!CommonUtils.isEmpty(permissions)) {
            json.name("security");
            json.beginObject();
            List<String> permIds = new ArrayList<>(permissions.size());
            for (DBPDataSourcePermission perm : permissions) permIds.add(perm.getId());
            JSONUtils.serializeStringList(json, "permission-restrictions", permIds);
            json.endObject();
        }
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
                "network/" + configuration.getId() + (profile == null ? "" : "/profile/" + profile.getProfileName()),
                new SecureCredentials(configuration));
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
        @Nullable DBPConfigurationProfile profile,
        @Nullable String subNode,
        @NotNull SecureCredentials credentials) {
        assert dataSource != null|| profile != null;
        boolean saved = !passwordWriteCanceled && DataSourceUtils.saveCredentialsInSecuredStorage(
            registry.getProject(), dataSource, subNode, credentials);
        if (!saved) {
            passwordWriteCanceled = true;

            String topNodeId = profile != null ? "profile:" + profile.getProfileId() : dataSource.getId();
            if (subNode == null) subNode = NODE_CONNECTION;

            Map<String, Map<String, String>> nodeMap = secureProperties.computeIfAbsent(topNodeId, s -> new LinkedHashMap<>());
            Map<String, String> propMap = nodeMap.computeIfAbsent(subNode, s -> new LinkedHashMap<>());
            if (!CommonUtils.isEmpty(credentials.getUserName())) {
                propMap.put(RegistryConstants.ATTR_USER, credentials.getUserName());
            }
            if (!CommonUtils.isEmpty(credentials.getUserPassword())) {
                propMap.put(RegistryConstants.ATTR_PASSWORD, credentials.getUserPassword());
            }
            if (!CommonUtils.isEmpty(credentials.getProperties())) {
                propMap.putAll(credentials.getProperties());
            }
        }
    }

    private SecureCredentials readSecuredCredentials(
        @Nullable DataSourceDescriptor dataSource,
        @Nullable DBPConfigurationProfile profile,
        @Nullable String subNode)
    {
        assert dataSource != null || profile != null;
        SecureCredentials creds = new SecureCredentials();
        final DBASecureStorage secureStorage = dataSource == null ? registry.getProject().getSecureStorage() : dataSource.getProject().getSecureStorage();
        {
            try {
                if (secureStorage.useSecurePreferences()) {
                    ISecurePreferences prefNode = dataSource == null ? secureStorage.getSecurePreferences() : dataSource.getSecurePreferences();
                    if (subNode != null) {
                        for (String nodeName : subNode.split("/")) {
                            prefNode = prefNode.node(nodeName);
                        }
                    }
                    for (String key : prefNode.keys()) {
                        switch (key) {
                            case RegistryConstants.ATTR_USER:
                                creds.setUserName(prefNode.get(key, null));
                                break;
                            case RegistryConstants.ATTR_PASSWORD:
                                creds.setUserPassword(prefNode.get(key, null));
                                break;
                            default:
                                creds.setSecureProp(key, prefNode.get(key, null));
                                break;
                        }
                    }
                }
            } catch (Throwable e) {
                // Most likely user canceled master password enter of failed by some other reason.
                // Anyhow we won't try it again
                log.error("Can't read password from secure storage", e);
                passwordReadCanceled = true;
            }
        }
        String topNodeId = profile != null ? "profile:" + profile.getProfileId() : dataSource.getId();
        if (subNode == null) subNode = NODE_CONNECTION;

        Map<String, Map<String, String>> subMap = secureProperties.get(topNodeId);
        if (subMap != null) {
            Map<String, String> propMap = subMap.get(subNode);
            if (propMap != null) {
                for (Map.Entry<String, String> prop : propMap.entrySet()) {
                    switch (prop.getKey()) {
                        case RegistryConstants.ATTR_USER:
                            creds.setUserName(prop.getValue());
                            break;
                        case RegistryConstants.ATTR_PASSWORD:
                            creds.setUserPassword(prop.getValue());
                            break;
                        default:
                            creds.setSecureProp(prop.getKey(), prop.getValue());
                            break;
                    }
                }
            }
        }

        return creds;
    }

}
