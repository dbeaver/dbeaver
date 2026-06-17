/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import com.google.gson.Strictness;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.access.DBAAuthProfile;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.connection.*;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.model.secret.DBSValueEncryptor;
import org.jkiss.dbeaver.model.security.SMObjectType;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.virtual.DBVModel;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptorSerializerModern;
import org.jkiss.dbeaver.registry.internal.RegistryMessages;
import org.jkiss.dbeaver.runtime.DBInterruptedException;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class DataSourceSerializerModern<T extends DataSourceDescriptor> implements DataSourceSerializer<T> {

    private static final String ATTR_ORIGINAL_PROVIDER = "original-provider"; //$NON-NLS-1$
    private static final String ATTR_ORIGINAL_DRIVER = "original-driver"; //$NON-NLS-1$
    private static final String ATTR_DRIVER_SUBSTITUTION = "driver-substitution"; //$NON-NLS-1$

    public static final String TAG_ORIGIN = "origin"; //$NON-NLS-1$
    private static final String ATTR_ORIGIN_TYPE = "$type"; //$NON-NLS-1$
    private static final String ATTR_ORIGIN_CONFIGURATION = "$configuration";
    public static final String ATTR_DPI_ENABLED = "dpi-enabled";

    private static final Log log = Log.getLog(DataSourceSerializerModern.class);
    private static final String USE_PROJECT_PASSWORD = "useProjectPassword"; //$NON-NLS-1$
    private static final String CONFIGURATION_FOLDERS = "folders"; //$NON-NLS-1$
    private static final String ENCRYPTED_CONFIGURATION = "secureProject"; //$NON-NLS-1$

    protected static final Gson CONFIG_GSON = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .serializeNulls()
        .create();

    protected final FilterSerializer<T> filterSerializer = new FilterSerializer<>();

    @NotNull
    private final DataSourceRegistry<T> registry;
    // Secure props.
    //  0 level: datasource ID
    //  1 level: object type (connection or handler id)
    //  2 level: map of secured properties
    private final Map<String, Map<String, Map<String, String>>> secureProperties = new LinkedHashMap<>();
    private final boolean isDetachedProcess = DBWorkbench.getPlatform().getApplication().isDetachedProcess();

    protected DataSourceSerializerModern(@NotNull DataSourceRegistry<T> registry) {
        this.registry = registry;
    }

    @Override
    public void saveDataSources(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DataSourceConfigurationManager configurationManager,
        @NotNull DBPDataSourceConfigurationStorage configurationStorage,
        @NotNull List<T> localDataSources
    ) throws DBException, IOException {
        DataSourceParser.ContextParameters contextParameters = new DataSourceParser.ContextParameters(
            registry.getProject(),
            configurationManager,
            secureProperties
        );

        ByteArrayOutputStream dsConfigBuffer = new ByteArrayOutputStream(10000);
        try (OutputStreamWriter osw = new OutputStreamWriter(dsConfigBuffer, StandardCharsets.UTF_8)) {
            try (JsonWriter jsonWriter = CONFIG_GSON.newJsonWriter(osw)) {
                jsonWriter.setIndent(JSONUtils.DEFAULT_INDENT);
                jsonWriter.beginObject();

                // Save folders
                if (configurationStorage.isDefault()) {
                    jsonWriter.name(CONFIGURATION_FOLDERS);
                    jsonWriter.beginObject();
                    // Folders (only for default storage)
                    Set<DBPDataSourceFolder> globalFolders = registry.getTemporaryFolders();
                    for (DataSourceFolder folder : registry.getAllFolders()) {
                        if (!globalFolders.contains(folder)) {
                            saveFolder(jsonWriter, folder);
                        }
                    }
                    jsonWriter.endObject();
                }

                Map<String, DBVModel> virtualModels = new LinkedHashMap<>();
                Map<String, DBPConnectionType> connectionTypes = new LinkedHashMap<>();
                Map<String, Map<String, DBPDriver>> drivers = new LinkedHashMap<>();
                Map<String, DBPExternalConfiguration> externalConfigurations = new LinkedHashMap<>();
                {
                    // Save connections
                    jsonWriter.name("connections");
                    jsonWriter.beginObject();
                    for (T dataSource : localDataSources) {
                        // Skip temporary
                        if (!dataSource.isDetached()) {
                            saveDataSource(contextParameters, jsonWriter, dataSource, externalConfigurations);
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
                        jsonWriter.setIndent(JSONUtils.EMPTY_INDENT);
                        for (DBVModel model : virtualModels.values()) {
                            model.serialize(monitor, jsonWriter);
                        }
                        jsonWriter.endObject();
                        jsonWriter.setIndent(JSONUtils.DEFAULT_INDENT);
                    }
                    // Network profiles
                    List<DBWNetworkProfile> profiles = registry.getNetworkProfiles().getProfiles();
                    if (!CommonUtils.isEmpty(profiles)) {
                        DataSourceParser.saveNetworkProfiles(contextParameters, jsonWriter, profiles);
                    }
                    // Auth profiles
                    List<DBAAuthProfile> authProfiles = registry.getAllAuthProfiles();
                    if (!CommonUtils.isEmpty(authProfiles)) {
                        saveAuthProfiles(contextParameters, jsonWriter, authProfiles);
                    }
                    // Filters
                    List<DBSObjectFilter> savedFilters = registry.getSavedFilters();
                    if (!CommonUtils.isEmpty(savedFilters)) {
                        jsonWriter.name("saved-filters");
                        jsonWriter.beginArray();
                        for (DBSObjectFilter cf : savedFilters) {
                            if (!cf.isEmpty()) {
                                filterSerializer.saveObjectFilter(jsonWriter, null, null, cf);
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
                            JSONUtils.field(jsonWriter, "smart-commit", ct.isSmartCommit());
                            JSONUtils.field(jsonWriter, "smart-commit-recover", ct.isSmartCommitRecover());
                            JSONUtils.field(jsonWriter, "auto-close-transactions", ct.isAutoCloseTransactions());
                            JSONUtils.field(jsonWriter, "close-transactions-period", ct.getCloseIdleTransactionPeriod());
                            JSONUtils.field(jsonWriter, "auto-close-connections", ct.isAutoCloseConnections());
                            JSONUtils.field(jsonWriter, "close-connections-period", ct.getCloseIdleConnectionPeriod());
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
                                new DriverDescriptorSerializerModern().serializeDriver(
                                    jsonWriter, (DriverDescriptor) driver, true);
                            }
                            jsonWriter.endObject();
                        }
                        jsonWriter.endObject();
                    }

                    // External configurations
                    if (!DBWorkbench.isDistributed() && !DBWorkbench.getPlatform().getApplication().isMultiuser() && !CommonUtils.isEmpty(externalConfigurations)) {
                        jsonWriter.name("external-configurations");
                        jsonWriter.beginObject();
                        for (Map.Entry<String, DBPExternalConfiguration> ecfg : externalConfigurations.entrySet()) {
                            jsonWriter.name(ecfg.getKey());
                            JSONUtils.serializeMap(jsonWriter, ecfg.getValue().getProperties());
                        }
                        jsonWriter.endObject();
                    }
                }

                jsonWriter.endObject();
                jsonWriter.flush();
            }
        } catch (IOException e) {
            log.error("IO error while saving datasources configuration", e);
        }

        String jsonString = dsConfigBuffer.toString(StandardCharsets.UTF_8);
        saveConfigFile(
            configurationManager,
            configurationStorage.getStorageName(),
            jsonString,
            //don't encrypt data for read only configuration manager
            registry.getProject().isEncryptedProject() && !configurationManager.isReadOnly()
        );

        if (!configurationManager.isSecure()) {
            saveSecureCredentialsFile(configurationManager, configurationStorage);
        }
    }

    private void saveAuthProfiles(
        @NotNull DataSourceParser.ContextParameters contextParameters,
        @NotNull JsonWriter jsonWriter,
        @NotNull List<DBAAuthProfile> authProfiles
    ) throws IOException {
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
            if (contextParameters.configurationManager().isTrusted()) {
                SecureCredentials credentials = new SecureCredentials(authProfile);
                if (contextParameters.configurationManager().isSecure()) {
                    DataSourceParser.savePlainCredentials(jsonWriter, credentials);
                } else {
                    // Save all auth properties in secure storage
                    DataSourceParser.saveSecuredCredentials(contextParameters, null, authProfile, null, credentials);
                }
            }
            jsonWriter.endObject();
        }
        jsonWriter.endObject();
    }

    private String loadConfigFile(@NotNull InputStream stream, boolean decrypt) throws DBException, IOException {
        ByteArrayOutputStream credBuffer = new ByteArrayOutputStream();
        try {
            IOUtils.copyStream(stream, credBuffer);
        } catch (Exception e) {
            log.error("Error reading secure credentials file", e);
        }
        if (!decrypt) {
            return credBuffer.toString(StandardCharsets.UTF_8);
        } else {
            DBSValueEncryptor encryptor = registry.getProject().getValueEncryptor();
            try {
                return new String(encryptor.decryptValue(credBuffer.toByteArray()), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new IOException("Error decrypting encrypted file", e);
            }
        }
    }

    private void saveConfigFile(
        DataSourceConfigurationManager configurationManager,
        String name,
        String contents,
        boolean encrypt
    ) throws DBException, IOException {
        byte[] binaryContents = null;
        if (contents != null) {
            if (encrypt) {
                // Serialize and encrypt
                DBSValueEncryptor valueEncryptor = registry.getProject().getValueEncryptor();
                binaryContents = valueEncryptor.encryptValue(contents.getBytes(StandardCharsets.UTF_8));
            } else {
                binaryContents = contents.getBytes(StandardCharsets.UTF_8);
            }
        }

        // Save result to file
        configurationManager.writeConfiguration(name, binaryContents);
    }

    void saveSecureCredentialsFile(
        @NotNull DataSourceConfigurationManager configurationManager,
        @NotNull DBPDataSourceConfigurationStorage storage
    ) {
        String credFile = DBPDataSourceRegistry.CREDENTIALS_CONFIG_FILE_PREFIX + storage.getStorageSubId() + DBPDataSourceRegistry.CREDENTIALS_CONFIG_FILE_EXT;
        try {
            if (secureProperties.isEmpty()) {
                saveConfigFile(configurationManager, credFile, null, true);
            } else {
                // Serialize and encrypt
                String jsonString = CONFIG_GSON.toJson(secureProperties, Map.class);
                saveConfigFile(configurationManager, credFile, jsonString, true);
            }
        } catch (Exception e) {
            log.error("Error saving secure credentials", e);
        }
    }

    @Override
    public boolean parseDataSources(
        @NotNull DBPDataSourceConfigurationStorage configurationStorage,
        @NotNull DataSourceConfigurationManager configurationManager,
        @NotNull DataSourceParseResults parseResults,
        @Nullable Collection<String> dataSourceIds
    ) throws DBException, IOException {
        var connectionConfigurationChanged = false;

        // Read in this particular order to handle configuration reading errors first, but process in reverse order later
        Map<String, Map<String, Map<String, String>>> secureCredentialsMap = null;
        Map<String, Object> configurationMap = null;

        // process projectConfiguration
        if (!DBWorkbench.getPlatform().getApplication().isHeadlessMode()
            && DBWorkbench.getPlatform().getApplication().isCommunity()
            && CommonUtils.toBoolean(registry.getProject().getProjectProperty(ENCRYPTED_CONFIGURATION))
        ) {
            DBWorkbench.getPlatformUI().showWarningMessageBox(
                RegistryMessages.project_open_cannot_read_configuration_title,
                NLS.bind(RegistryMessages.project_open_cannot_read_configuration_message,
                    registry.getProject().getName()));
            throw new DBInterruptedException("Project secure credentials read canceled by user.");
        }
        try {
            configurationMap = readConfiguration(configurationStorage, configurationManager, dataSourceIds);
        } catch (DBInterruptedException e) {
            throw e;
        } catch (DBException e) {
            log.error(e);
        }
        // process project credential
        if (!DBWorkbench.getPlatform().getApplication().isHeadlessMode()
            && DBWorkbench.getPlatform().getApplication().isCommunity() &&
            CommonUtils.toBoolean(registry.getProject().getProjectProperty(USE_PROJECT_PASSWORD))
        ) {
            if (Boolean.parseBoolean(registry.getProject().getRuntimeProperty(RuntimeProjectPropertiesConstant.IS_USER_DECLINE_PROJECT_DECRYPTION))) {
                throw new DBInterruptedException("Project secure credentials read canceled by user.");
            }
            if (DBWorkbench.getPlatformUI().confirmAction(
                RegistryMessages.project_open_cannot_read_credentials_title,
                NLS.bind(RegistryMessages.project_open_cannot_read_credentials_message,
                    registry.getProject().getName()),
                RegistryMessages.project_open_cannot_read_credentials_button_text, true)) {
                // in case of user agreed lost project credentials - proceed opening
                log.info("The user agreed lost project credentials.");
                registry.getProject().setRuntimeProperty(RuntimeProjectPropertiesConstant.IS_USER_DECLINE_PROJECT_DECRYPTION, Boolean.FALSE.toString());

            } else {
                // in case of canceling erase credentials intercept original exception
                registry.getProject().setRuntimeProperty(RuntimeProjectPropertiesConstant.IS_USER_DECLINE_PROJECT_DECRYPTION, Boolean.TRUE.toString());
                throw new DBInterruptedException("Project secure credentials read canceled by user.");
            }
        }
        try {
            secureCredentialsMap = readSecureCredentials(configurationStorage, configurationManager, dataSourceIds);
        } catch (DBInterruptedException e) {
            throw e;
        } catch (DBException e) {
            log.error(e);
        }
        if (secureCredentialsMap != null) {
            secureProperties.putAll(secureCredentialsMap);
        }

        DataSourceParser.ContextParameters contextParameters = new DataSourceParser.ContextParameters(
            registry.getProject(),
            configurationManager,
            secureProperties
        );

        if (configurationMap != null) {
            // Folders
            for (Map.Entry<String, Map<String, Object>> folderMap : JSONUtils.getNestedObjects(configurationMap, CONFIGURATION_FOLDERS)) {
                String name = folderMap.getKey();
                String description = JSONUtils.getObjectProperty(folderMap.getValue(), RegistryConstants.ATTR_DESCRIPTION);
                String parentFolder = JSONUtils.getObjectProperty(folderMap.getValue(), RegistryConstants.ATTR_PARENT);
                DataSourceFolder parent = parentFolder == null ? null : registry.findFolderByPath(parentFolder, true, parseResults);
                DataSourceFolder folder = parent == null ? registry.findFolderByPath(name, true, parseResults) : parent.getChild(name);
                if (folder == null) {
                    folder = new DataSourceFolder(registry, parent, name, description);
                    parseResults.addedFolders.add(folder);
                } else {
                    folder.setDescription(description);
                    parseResults.updatedFolders.add(folder);
                }
            }

            // Connection types
            for (Map.Entry<String, Map<String, Object>> ctMap : JSONUtils.getNestedObjects(configurationMap, "connection-types")) {
                String id = ctMap.getKey();
                Map<String, Object> ctConfig = ctMap.getValue();
                String name = JSONUtils.getObjectProperty(ctConfig, RegistryConstants.ATTR_NAME);
                String description = JSONUtils.getObjectProperty(ctConfig, RegistryConstants.ATTR_DESCRIPTION);
                String color = JSONUtils.getObjectProperty(ctConfig, RegistryConstants.ATTR_COLOR);
                Boolean autoCommit = JSONUtils.getObjectProperty(ctConfig, "auto-commit");
                Boolean confirmExecute = JSONUtils.getObjectProperty(ctConfig, "confirm-execute");
                Boolean confirmDataChange = JSONUtils.getObjectProperty(ctConfig, "confirm-data-change");
                Boolean smartCommit = JSONUtils.getObjectProperty(ctConfig, "smart-commit");
                Boolean smartCommitRecover = JSONUtils.getObjectProperty(ctConfig, "smart-commit-recover");
                Boolean autoCloseTransactions = JSONUtils.getObjectProperty(ctConfig, "auto-close-transactions");
                Object closeTransactionsPeriod = JSONUtils.getObjectProperty(ctConfig, "close-transactions-period");
                Boolean autoCloseConnections = JSONUtils.getObjectProperty(ctConfig, "auto-close-connections");
                Object closeConnectionsPeriod = JSONUtils.getObjectProperty(ctConfig, "close-connections-period");
                DBPConnectionType ct = DBWorkbench.getPlatform().getDataSourceProviderRegistry().getConnectionType(id, null);
                if (ct == null) {
                    ct = new DBPConnectionType(
                        id,
                        name,
                        color,
                        description,
                        CommonUtils.toBoolean(autoCommit),
                        CommonUtils.toBoolean(confirmExecute),
                        CommonUtils.toBoolean(confirmDataChange),
                        CommonUtils.toBoolean(smartCommit),
                        CommonUtils.toBoolean(smartCommitRecover),
                        CommonUtils.toBoolean(autoCloseTransactions),
                        CommonUtils.toInt(closeTransactionsPeriod),
                        CommonUtils.toBoolean(autoCloseConnections),
                        CommonUtils.toInt(closeConnectionsPeriod));
                    DBWorkbench.getPlatform().getDataSourceProviderRegistry().addConnectionType(ct);
                }
                deserializeModifyPermissions(ctConfig, ct);
            }

            // Drivers
            // TODO: load drivers config

            // External configurations
            Map<String, DBPExternalConfiguration> externalConfigurations = new LinkedHashMap<>();
            if (!DBWorkbench.isDistributed()) {
                // External configurations not used in distributed mode
                for (Map.Entry<String, Map<String, Object>> ctMap : JSONUtils.getNestedObjects(configurationMap, "external-configurations")) {
                    String id = ctMap.getKey();
                    Map<String, Object> configMap = ctMap.getValue();
                    externalConfigurations.put(id, new DBPExternalConfiguration(id, () -> configMap));
                }
            }

            // Virtual models
            Map<String, DBVModel> modelMap = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Object>> vmMap : JSONUtils.getNestedObjects(configurationMap, "virtual-models")) {
                String id = vmMap.getKey();
                DBVModel model = new DBVModel(id, vmMap.getValue());
                modelMap.put(id, model);
            }

            // Network profiles
            for (Map.Entry<String, Map<String, Object>> vmMap : JSONUtils.getNestedObjects(configurationMap, "network-profiles")) {
                String profileId = vmMap.getKey();
                Map<String, Object> profileMap = vmMap.getValue();
                DBWNetworkProfile profile = new DBWNetworkProfile(registry.getProject());
                profile.setProfileName(profileId);
                profile.setProfileName(profileId);
                profile.setProperties(JSONUtils.deserializeStringMap(profileMap, "properties"));

                for (Map.Entry<String, Map<String, Object>> handlerMap : JSONUtils.getNestedObjects(profileMap, "handlers")) {
                    DBWHandlerConfiguration configuration = DataSourceParser.parseNetworkHandlerConfig(contextParameters, null, profile, handlerMap);
                    if (configuration != null) {
                        profile.updateConfiguration(configuration);
                    }
                }

                registry.getNetworkProfiles().addOrUpdateProfile(profile);
            }

            // Auth profiles
            List<DBAAuthProfile> profiles = new ArrayList<>();
            for (Map.Entry<String, Map<String, Object>> vmMap : JSONUtils.getNestedObjects(configurationMap, "auth-profiles")) {
                String profileId = vmMap.getKey();
                Map<String, Object> profileMap = vmMap.getValue();
                DBAAuthProfile profile = new DBAAuthProfile(registry.getProject());
                profile.setProfileId(profileId);
                profile.setProfileName(JSONUtils.getString(profileMap, RegistryConstants.ATTR_NAME));
                profile.setAuthModelId(JSONUtils.getString(profileMap, RegistryConstants.ATTR_AUTH_MODEL));
                profile.setSavePassword(JSONUtils.getBoolean(profileMap, RegistryConstants.ATTR_SAVE_PASSWORD));

                SecureCredentials authCreds = configurationManager.isSecure() ?
                    DataSourceParser.readPlainCredentials(profileMap) :
                    DataSourceParser.readSecuredCredentials(contextParameters, null, profile, null);
                profile.setUserName(authCreds.getUserName());
                profile.setUserPassword(authCreds.getUserPassword());
                profile.setProperties(authCreds.getProperties());
                profiles.add(profile);
            }
            registry.setAuthProfiles(profiles);

            // Connections
            for (Map.Entry<String, Map<String, Object>> conMap : JSONUtils.getNestedObjects(configurationMap, "connections")) {
                String id = conMap.getKey();
                Map<String, Object> conObject = conMap.getValue();

                final String originalProviderId = CommonUtils.toString(conObject.get(ATTR_ORIGINAL_PROVIDER));
                final String originalDriverId = CommonUtils.toString(conObject.get(ATTR_ORIGINAL_DRIVER));
                final String substitutedProviderId = CommonUtils.toString(conObject.get(RegistryConstants.ATTR_PROVIDER));
                final String substitutedDriverId = CommonUtils.toString(conObject.get(RegistryConstants.ATTR_DRIVER));

                DBPDriver originalDriver;
                DBPDriver substitutedDriver;

                if (CommonUtils.isEmpty(originalProviderId) || CommonUtils.isEmpty(originalDriverId)) {
                    originalDriver = parseOrCreateDriver(id, substitutedProviderId, substitutedDriverId, !isDetachedProcess);
                    substitutedDriver = originalDriver;
                } else {
                    originalDriver = parseOrCreateDriver(id, originalProviderId, originalDriverId, !isDetachedProcess);
                    substitutedDriver = parseOrCreateDriver(id, substitutedProviderId, substitutedDriverId, false);
                }
                if (originalDriver == null) {
                    continue;
                }
                if (substitutedDriver == null || substitutedDriver.isTemporary()) {
                    substitutedDriver = originalDriver;
                }

                if (getReplacementDriver(substitutedDriver) == originalDriver) {
                    final DBPDriver original = originalDriver;
                    originalDriver = substitutedDriver;
                    substitutedDriver = original;
                }

                substitutedDriver = getReplacementDriver(substitutedDriver);

                T dataSource = registry.getDataSource(id);
                boolean newDataSource = (dataSource == null);
                T oldDataSource = null;
                if (newDataSource) {
                    DBPDataSourceOrigin origin;
                    Map<String, Object> originProperties = JSONUtils.deserializeProperties(conObject, TAG_ORIGIN);
                    if (CommonUtils.isEmpty(originProperties) || !originProperties.containsKey(ATTR_ORIGIN_TYPE)) {
                        origin = DataSourceOriginLocal.INSTANCE;
                    } else {
                        String originID = CommonUtils.toString(originProperties.remove(ATTR_ORIGIN_TYPE));
                        String extConfigID = CommonUtils.toString(originProperties.remove(ATTR_ORIGIN_CONFIGURATION));
                        DBPExternalConfiguration extConfig = null;
                        if (!CommonUtils.isEmpty(extConfigID)) {
                            extConfig = externalConfigurations.get(extConfigID);
                        }
                        origin = new DataSourceOriginLazy(originID, originProperties, extConfig);
                    }
                    dataSource = (T) registry.createDataSource(
                        configurationStorage.isVirtual() ? registry.getDefaultStorage() : configurationStorage,
                        origin,
                        id,
                        originalDriver,
                        substitutedDriver,
                        new DBPConnectionConfiguration());
                } else {
                    oldDataSource = (T) registry.createDataSource(dataSource);
                    oldDataSource.setId(id);
                    // Clean settings - they have to be loaded later by parser
                    dataSource.getConnectionConfiguration().setProperties(Collections.emptyMap());
                    dataSource.getConnectionConfiguration().setHandlers(Collections.emptyList());
                    dataSource.clearFilters();
                }
                deserializeDataSource(parseResults, dataSource, conObject);

                // Connection settings
                {
                    Map<String, Object> cfgObject = JSONUtils.getObject(conObject, "configuration");
                    DBPConnectionConfiguration config = dataSource.getConnectionConfiguration();
                    config.setHostName(JSONUtils.getString(cfgObject, RegistryConstants.ATTR_HOST));
                    config.setHostPort(JSONUtils.getString(cfgObject, RegistryConstants.ATTR_PORT));
                    config.setServerName(JSONUtils.getString(cfgObject, RegistryConstants.ATTR_SERVER));
                    config.setDatabaseName(JSONUtils.getString(cfgObject, RegistryConstants.ATTR_DATABASE));
                    config.setUrl(JSONUtils.getString(cfgObject, RegistryConstants.ATTR_URL));

                        final SecureCredentials creds = configurationManager.isSecure() ?
                            DataSourceParser.readPlainCredentials(cfgObject) :
                            DataSourceParser.readSecuredCredentials(contextParameters, dataSource, null, null);
                    if (shouldUpdateCreds(creds)) {
                        config.setUserName(creds.getUserName());
                        if (dataSource.isSavePassword() || !CommonUtils.isEmpty(creds.getUserPassword())) {
                            config.setUserPassword(creds.getUserPassword());
                        } else {
                            config.setUserPassword(null);
                        }
                        boolean savePasswordApplicable = (!dataSource.getProject()
                            .isUseSecretStorage() || dataSource.isSharedCredentials());
                        if (savePasswordApplicable && !CommonUtils.isEmpty(creds.getUserPassword())) {
                            dataSource.setSavePassword(true);
                        }
                        dataSource.getConnectionConfiguration().setAuthProperties(creds.getProperties());
                        dataSource.resetAllSecrets();
                    }
                    {
                        // Still try to read credentials directly from configuration (#6564)
                        String userName = JSONUtils.getString(cfgObject, RegistryConstants.ATTR_USER);
                        if (!CommonUtils.isEmpty(userName)) config.setUserName(userName);
                        String userPassword = JSONUtils.getString(cfgObject, RegistryConstants.ATTR_PASSWORD);
                        if (!CommonUtils.isEmpty(userPassword)) config.setUserPassword(userPassword);
                    }

                    config.setClientHomeId(JSONUtils.getString(cfgObject, RegistryConstants.ATTR_HOME));
                    config.setConfigProfileSource(JSONUtils.getString(cfgObject, "config-profile-source"));
                    config.setConfigProfileName(JSONUtils.getString(cfgObject, "config-profile"));
                    config.setConnectionType(
                        DataSourceProviderRegistry.getInstance().getConnectionType(
                            CommonUtils.notEmpty(JSONUtils.getString(cfgObject, RegistryConstants.ATTR_TYPE)),
                            DBPConnectionType.DEFAULT_TYPE));
                    String configurationType = JSONUtils.getString(cfgObject, RegistryConstants.ATTR_CONFIGURATION_TYPE);
                    if (!CommonUtils.isEmpty(configurationType)) {
                        config.setConfigurationType(
                            CommonUtils.valueOf(DBPDriverConfigurationType.class, configurationType, DBPDriverConfigurationType.MANUAL));
                    }
                    String colorValue = JSONUtils.getString(cfgObject, RegistryConstants.ATTR_COLOR);
                    if (!CommonUtils.isEmpty(colorValue)) {
                        config.setConnectionColor(colorValue);
                    }
                    int keepAlive = JSONUtils.getInteger(cfgObject, RegistryConstants.ATTR_KEEP_ALIVE);
                    if (keepAlive > 0) {
                        config.setKeepAliveInterval(keepAlive);
                    }
                    boolean closeIdleEnabled = JSONUtils.getBoolean(cfgObject, RegistryConstants.ATTR_CLOSE_IDLE_ENABLED);
                    config.setCloseIdleConnection(closeIdleEnabled);
                    int closeIdle = JSONUtils.getInteger(cfgObject, RegistryConstants.ATTR_CLOSE_IDLE);
                    if (closeIdle > 0) {
                        config.setCloseIdleInterval(closeIdle);
                    }

                    config.setProperties(JSONUtils.deserializeStringMap(cfgObject, RegistryConstants.TAG_PROPERTIES));
                    config.setProviderProperties(JSONUtils.deserializeStringMap(cfgObject, RegistryConstants.TAG_PROVIDER_PROPERTIES));
                    config.setAuthModelId(JSONUtils.getString(cfgObject, RegistryConstants.ATTR_AUTH_MODEL));
                    //backward compatibility
                    //in the current version the configuration should not contain auth-properties, they should be in secrets
                    if (cfgObject.containsKey(RegistryConstants.TAG_AUTH_PROPERTIES)) {
                        config.setAuthProperties(JSONUtils.deserializeStringMapOrNull(cfgObject, RegistryConstants.TAG_AUTH_PROPERTIES));
                    }

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
                        DBWHandlerConfiguration configuration = DataSourceParser.parseNetworkHandlerConfig(contextParameters, dataSource, null, handlerObject);
                        if (configuration != null) {
                            dataSource.getConnectionConfiguration().updateHandler(configuration);
                        }
                    }

                    // Bootstrap
                    Map<String, Object> bootstrapCfg = JSONUtils.getObject(cfgObject, RegistryConstants.TAG_BOOTSTRAP);
                    if (!bootstrapCfg.isEmpty()) {
                        DBPConnectionBootstrap bootstrap = config.getBootstrap();
                        if (bootstrapCfg.containsKey(RegistryConstants.ATTR_AUTOCOMMIT)) {
                            bootstrap.setDefaultAutoCommit(JSONUtils.getBoolean(bootstrapCfg, RegistryConstants.ATTR_AUTOCOMMIT));
                        }
                        if (bootstrapCfg.containsKey(RegistryConstants.ATTR_TXN_ISOLATION)) {
                            bootstrap.setDefaultTransactionIsolation(JSONUtils.getInteger(
                                bootstrapCfg,
                                RegistryConstants.ATTR_TXN_ISOLATION
                            ));
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

                    if (originalDriver != substitutedDriver) {
                        if (substitutedDriver.getProviderDescriptor().supportsDriverMigration()) {
                            final DBPDataSourceProvider dataSourceProvider = substitutedDriver.getDataSourceProvider();
                            if (dataSourceProvider instanceof DBPConnectionConfigurationMigrator migrator) {
                                if (migrator.migrationRequired(config)) {
                                    final DBPConnectionConfiguration migrated = new DBPConnectionConfiguration(config);
                                    try {
                                        migrator.migrateConfiguration(config, migrated);
                                        dataSource.setConnectionInfo(migrated);
                                        log.debug("Connection configuration for data source '" + id + "' was migrated successfully");
                                    } catch (DBException e) {
                                        log.error("Unable to migrate connection configuration for data source '" + id + "'", e);
                                    }
                                }
                            }
                        }
                    }
                }

                // Permissions
                {
                    deserializeModifyPermissions(conObject, dataSource);
                }

                // Filters
                for (Map<String, Object> filterCfg : JSONUtils.getObjectList(conObject, RegistryConstants.TAG_FILTERS)) {
                    var filterConfiguration = filterSerializer.deserializeObjectFilterConfig(filterCfg);
                    if (filterConfiguration.typeNamePresent()) {
                        dataSource.setObjectFilter(
                            filterConfiguration.typeName(),
                            filterConfiguration.objectID(),
                            filterConfiguration.filter()
                        );
                    }
                }

                // Preferences
                DataSourcePreferenceStore preferenceStore = dataSource.getPreferenceStore();
                preferenceStore.clear();
                Map<String, String> customProperties = JSONUtils.deserializeStringMap(conObject, RegistryConstants.TAG_CUSTOM_PROPERTIES);
                customProperties.forEach(preferenceStore::setValue);

                setCurrentUserSettings(dataSource, conObject);

                dataSource.setTags(
                    JSONUtils.deserializeStringMap(conObject, RegistryConstants.TAG_TAGS));

                {
                    // Extensions
                    Map<String, Object> extensions = null;
                    if (conObject.containsKey(RegistryConstants.TAG_PROPERTIES)) {
                        // Backward compatibility
                        extensions = JSONUtils.deserializeProperties(conObject, RegistryConstants.TAG_PROPERTIES);
                    } else if (conObject.containsKey(RegistryConstants.TAG_EXTENSIONS)) {
                        extensions = JSONUtils.deserializeProperties(conObject, RegistryConstants.TAG_EXTENSIONS);
                    }
                    if (extensions == null) {
                        extensions = new LinkedHashMap<>();
                    }
                    dataSource.setExtensions(extensions);
                }

                // Virtual model
                String vmID = CommonUtils.toString(conObject.get("virtual-model-id"), id);
                DBVModel dbvModel = modelMap.get(vmID);
                if (dbvModel != null) {
                    dataSource.setVirtualModel(dbvModel);
                }

                deserializeAdditionalProperties(dataSource, conObject);
                // Add to the list
                if (newDataSource) {
                    parseResults.addedDataSources.add(dataSource);
                    connectionConfigurationChanged = true;
                } else {
                    parseResults.updatedDataSources.add(dataSource);
                    if (!dataSource.equalSettings(oldDataSource)) {
                        connectionConfigurationChanged = true;
                    }
                }
            }

            // Saved filters
            for (Map<String, Object> ctMap : JSONUtils.getObjectList(configurationMap, "saved-filters")) {
                DBSObjectFilter filter = filterSerializer.deserializeObjectFilter(ctMap);
                registry.addSavedFilter(filter);
            }
        }
        return connectionConfigurationChanged;

    }

    protected void deserializeDataSource(
        @NotNull DataSourceParseResults parseResults,
        @NotNull T dataSource,
        @NotNull Map<String, Object> conObject
    ) {
        dataSource.setName(CommonUtils.notNull(JSONUtils.getString(conObject, RegistryConstants.ATTR_NAME), "?"));
        dataSource.setDescription(JSONUtils.getString(conObject, RegistryConstants.TAG_DESCRIPTION));
        dataSource.forceSetSharedCredentials(JSONUtils.getBoolean(
            conObject,
            RegistryConstants.ATTR_SHARED_CREDENTIALS));
        dataSource.setSavePassword(JSONUtils.getBoolean(conObject, RegistryConstants.ATTR_SAVE_PASSWORD));
        dataSource.setDriverSubstitution(DataSourceProviderRegistry.getInstance()
            .getDriverSubstitution(CommonUtils.notEmpty(JSONUtils.getString(conObject, ATTR_DRIVER_SUBSTITUTION))));

        dataSource.setConnectionReadOnly(JSONUtils.getBoolean(conObject, RegistryConstants.ATTR_READ_ONLY));
        final String folderPath = JSONUtils.getString(conObject, RegistryConstants.ATTR_FOLDER);
        dataSource.setFolder(folderPath == null ? null : registry.findFolderByPath(folderPath, true, parseResults));
        dataSource.setLockPasswordHash(CommonUtils.toString(conObject.get(RegistryConstants.ATTR_LOCK_PASSWORD)));
    }


    private void setCurrentUserSettings(@NotNull T dataSource, @NotNull Map<String, Object> conObject) {
        DBPObjectSettingsProvider settingsProvider = DBUtils.getAdapter(DBPObjectSettingsProvider.class, dataSource.getProject());
        Map<String, String> userSettings = null;
        if (settingsProvider != null) {
            try {
                userSettings = settingsProvider.getObjectSettings(SMObjectType.datasource, dataSource.getId());
            } catch (Exception e) {
                log.warn("Error reading user datasource settings", e);
            }
        }

        DataSourcePreferenceStore preferenceStore = dataSource.getPreferenceStore();
        if (userSettings != null) {

            Map<String, String> datasourceUserSettings = userSettings.entrySet().stream()
                .filter(setting -> !DataSourceNavigatorSettings.NAVIGATOR_SETTINGS.contains(setting.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            preferenceStore.putUserSettings(datasourceUserSettings);
        }

        dataSource.getNavigatorSettings().reset();

        // Navigator settings
        if (!CommonUtils.isEmpty(userSettings) && userSettings.keySet().stream().anyMatch(
            DataSourceNavigatorSettings.NAVIGATOR_SETTINGS::contains)
        ) {
            // There are custom navigator settings
            DataSourceNavigatorSettingsUtils.loadSettingsFromMap(dataSource.getNavigatorSettings(), userSettings);
            dataSource.getNavigatorSettings().setUserSettings(true);
            DataSourceNavigatorSettings originalSettings = new DataSourceNavigatorSettings();
            DataSourceNavigatorSettingsUtils.loadSettingsFromMap(originalSettings, conObject);
            dataSource.getNavigatorSettings().setOriginalSettings(originalSettings);
        } else {
            DataSourceNavigatorSettingsUtils.loadSettingsFromMap(dataSource.getNavigatorSettings(), conObject);
        }

        if (!CommonUtils.isEmpty(userSettings)) {
            UserDBSObjectFilterUtils.setUserObjectFilters(dataSource, userSettings);
        }
    }

    private boolean shouldUpdateCreds(@NotNull SecureCredentials creds) {
        boolean isCredsResolved = creds.getUserName() != null && creds.getUserPassword() != null;
        return isCredsResolved
            // in TE secrets must be resolved by dataSource itself, if not present here
            || !DBWorkbench.isDistributed();
    }


    /**
     * Deserialize additional datasource properties
     * @param dataSource - deserializable datasource
     * @param conObject - full datasource config
     */
    protected void deserializeAdditionalProperties(@NotNull T dataSource, @NotNull Map<String, Object> conObject) {

    }

    @Nullable
    private Map<String, Map<String, Map<String, String>>> readSecureCredentials(
        @NotNull DBPDataSourceConfigurationStorage configurationStorage,
        @NotNull DataSourceConfigurationManager configurationManager,
        @Nullable Collection<String> dataSourceIds
    ) throws DBException {
        if (configurationManager.isSecure()) {
            return null;
        }
        final String name = DBPDataSourceRegistry.CREDENTIALS_CONFIG_FILE_PREFIX
                + configurationStorage.getStorageSubId() + DBPDataSourceRegistry.CREDENTIALS_CONFIG_FILE_EXT;
        try (InputStream is = configurationManager.readConfiguration(name, dataSourceIds)) {
            if (is == null) {
                return null;
            }
            final String data = loadConfigFile(is, true);
            return CONFIG_GSON.fromJson(data, new TypeToken<Map<String, Map<String, Map<String, String>>>>() {
            }.getType());
        } catch (IOException e) {
            // here we catch any exceptions that happens for secure credential
            // reading
            throw new DBException("Project secure credentials can not be read", e);
        }
    }

    @Nullable
    private Map<String, Object> readConfiguration(
        @NotNull DBPDataSourceConfigurationStorage configurationStorage,
        @NotNull DataSourceConfigurationManager configurationManager,
        @Nullable Collection<String> dataSourceIds
    ) throws DBException, IOException {
        final InputStream is;
        if (configurationStorage instanceof DataSourceMemoryStorage) {
            is = ((DataSourceMemoryStorage) configurationStorage).getInputStream();
        } else {
            is = configurationManager.readConfiguration(configurationStorage.getStorageName(), dataSourceIds);
        }
        if (is == null) {
            return null;
        }
        try (is) {
            final String data = loadConfigFile(is, CommonUtils.toBoolean(registry.getProject().isEncryptedProject()));
            return JSONUtils.parseMap(CONFIG_GSON, new StringReader(data));
        } catch (DBInterruptedException e) {
            // happens only if user cancelled entering password
            // not a community level
            throw e;
        } catch (IOException e) {
            // intercept exceptions for crypted configuration
            // for community provide a dialog
            throw new DBException(e.getMessage(), e);
        }
    }

    @Nullable
    private static DBPDriver parseOrCreateDriver(
        @NotNull String id,
        @NotNull String providerId,
        @NotNull String driverId,
        boolean createIfAbsent
    ) {
        if (CommonUtils.isEmpty(providerId)) {
            log.debug("Empty datasource provider for datasource '" + id + "'");
            return null;
        }

        if (CommonUtils.isEmpty(driverId)) {
            log.debug("Empty driver for datasource '" + id + "'");
            return null;
        }

        DataSourceProviderDescriptor provider = DataSourceProviderRegistry.getInstance().getDataSourceProvider(providerId);
        if (provider == null) {
            if (createIfAbsent) {
                log.debug("Can't find datasource provider " + providerId + " for datasource '" + id + "'");
                provider = (DataSourceProviderDescriptor) DataSourceProviderRegistry.getInstance().makeFakeProvider(providerId);
            } else {
                return null;
            }
        }

        DBPDriver curDriver = provider.getOriginalDriver(driverId);
        if (curDriver == null) {
            if (createIfAbsent) {
                log.debug("Can't find driver " + driverId + " in datasource provider "
                    + provider.getId() + " for datasource '" + id + "'. Create new driver");
                DriverDescriptor driver = provider.createDriver(driverId);
                driver.setName(driverId);
                driver.setDescription("Missing driver " + driverId);
                driver.setTemporary(true);
                provider.addDriver(driver);
                curDriver = driver;
            }
        }
        return curDriver;
    }

    private void deserializeModifyPermissions(
        @Nullable Map<String, Object> conObject,
        @NotNull DBPDataSourcePermissionOwner permissionOwner
    ) {
        if (conObject == null) {
            return;
        }
        Map<String, Object> securityCfg = JSONUtils.getObject(conObject, "security");
        List<DBPDataSourcePermission> permissions = new ArrayList<>();
        if (!CommonUtils.isEmpty(securityCfg)) {
            List<String> permissionRestrictions = JSONUtils.deserializeStringList(securityCfg, "permission-restrictions");
            if (!CommonUtils.isEmpty(permissionRestrictions)) {
                for (String perm : permissionRestrictions) {
                    try {
                        permissions.add(DBPDataSourcePermission.getById(perm));
                    } catch (IllegalArgumentException e) {
                        log.debug(e);
                    }
                }
            }
        }
        permissionOwner.setModifyPermissions(permissions);
    }

    private static void saveFolder(@NotNull JsonWriter json, @NotNull DataSourceFolder folder) throws IOException {
        json.name(folder.getName());

        json.beginObject();
        if (folder.getParent() != null) {
            JSONUtils.field(json, RegistryConstants.ATTR_PARENT, folder.getParent().getFolderPath());
        }
        JSONUtils.fieldNE(json, RegistryConstants.ATTR_DESCRIPTION, folder.getDescription());

        json.endObject();
    }

    protected void saveDataSource(
        @NotNull DataSourceParser.ContextParameters contextParameters,
        @NotNull JsonWriter json,
        @NotNull T dataSource,
        @NotNull Map<String, DBPExternalConfiguration> externalConfigurations
    ) throws IOException {
        json.name(dataSource.getId());
        json.beginObject();
        serializeDataSource(contextParameters, json, dataSource, externalConfigurations);
        json.endObject();
    }

    protected void serializeDataSource(
        @NotNull DataSourceParser.ContextParameters contextParameters,
        @NotNull JsonWriter json,
        @NotNull T dataSource,
        @NotNull Map<String, DBPExternalConfiguration> externalConfigurations
    ) throws IOException {
        JSONUtils.field(json, RegistryConstants.ATTR_PROVIDER, dataSource.getDriver().getProviderDescriptor().getId());
        JSONUtils.field(json, RegistryConstants.ATTR_DRIVER, dataSource.getDriver().getId());
        if (dataSource.getDriver() != dataSource.getOriginalDriver()) {
            JSONUtils.field(json, ATTR_ORIGINAL_PROVIDER, dataSource.getOriginalDriver().getProviderDescriptor().getId());
            JSONUtils.field(json, ATTR_ORIGINAL_DRIVER, dataSource.getOriginalDriver().getId());
        }
        if (dataSource.getDriverSubstitution() != null) {
            JSONUtils.field(json, ATTR_DRIVER_SUBSTITUTION, dataSource.getDriverSubstitution().getId());
        }
        DBPDataSourceOrigin origin = dataSource.getOriginSource();
        if (origin != DataSourceOriginLocal.INSTANCE) {
            Map<String, Object> originProps = new LinkedHashMap<>();
            originProps.put(ATTR_ORIGIN_TYPE, origin.getType());
            if (origin instanceof DBPDataSourceOriginExternal) {
                DBPExternalConfiguration externalConfiguration = ((DBPDataSourceOriginExternal) origin).getExternalConfiguration();
                if (externalConfiguration != null) {
                    originProps.put(ATTR_ORIGIN_CONFIGURATION, externalConfiguration.getId());
                    externalConfigurations.put(externalConfiguration.getId(), externalConfiguration);
                }
            }
            originProps.putAll(origin.getDataSourceConfiguration());
            JSONUtils.serializeProperties(json, TAG_ORIGIN, originProps);
        }
        JSONUtils.field(json, RegistryConstants.ATTR_NAME, dataSource.getName());
        JSONUtils.fieldNE(json, RegistryConstants.TAG_DESCRIPTION, dataSource.getDescription());
        if (dataSource.isSavePassword()) JSONUtils.field(json, RegistryConstants.ATTR_SAVE_PASSWORD, true);
        if (dataSource.isSharedCredentials()) JSONUtils.field(json, RegistryConstants.ATTR_SHARED_CREDENTIALS, true);

        DataSourceNavigatorSettings.saveSettingsToMap(json, dataSource.getOriginalNavigatorSettings());

        if (dataSource.isConnectionReadOnly()) JSONUtils.field(json, RegistryConstants.ATTR_READ_ONLY, true);

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

        DataSourceConfigurationManager configurationManager = contextParameters.configurationManager();
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
            JSONUtils.fieldNE(json, RegistryConstants.ATTR_CONFIGURATION_TYPE, connectionInfo.getConfigurationType().toString());

            if (dataSource.getProject().isUseSecretStorage()) {
                // should be stored in secrets
            } else if (configurationManager.isTrusted()) {
                if (configurationManager.isSecure()) {
                    // Secure manager == save to buffer
                    DataSourceParser.savePlainCredentials(json, new SecureCredentials(dataSource));
                } else {
                    DataSourceParser.saveSecuredCredentials(
                        contextParameters,
                        dataSource,
                        null,
                        null,
                        new SecureCredentials(dataSource)
                    );
                }
            }

            JSONUtils.fieldNE(json, RegistryConstants.ATTR_HOME, connectionInfo.getClientHomeId());
            if (connectionInfo.getConnectionType() != null) {
                JSONUtils.field(json, RegistryConstants.ATTR_TYPE, connectionInfo.getConnectionType().getId());
            }
            JSONUtils.fieldNE(json, RegistryConstants.ATTR_COLOR, connectionInfo.getConnectionColor());
            // Save other
            if (connectionInfo.getKeepAliveInterval() > 0) {
                JSONUtils.field(json, RegistryConstants.ATTR_KEEP_ALIVE, connectionInfo.getKeepAliveInterval());
            }
            JSONUtils.field(json, RegistryConstants.ATTR_CLOSE_IDLE_ENABLED, connectionInfo.isCloseIdleConnection());
            if (connectionInfo.getCloseIdleInterval() > 0) {
                JSONUtils.field(json, RegistryConstants.ATTR_CLOSE_IDLE, connectionInfo.getCloseIdleInterval());
            }
            JSONUtils.fieldNE(json, "config-profile-source", connectionInfo.getConfigProfileSource());
            JSONUtils.fieldNE(json, "config-profile", connectionInfo.getConfigProfileName());
            JSONUtils.serializeProperties(json, RegistryConstants.TAG_PROPERTIES, connectionInfo.getProperties(), true);
            JSONUtils.serializeProperties(json, RegistryConstants.TAG_PROVIDER_PROPERTIES, connectionInfo.getProviderProperties(), true);
            JSONUtils.fieldNE(json, RegistryConstants.ATTR_AUTH_MODEL, connectionInfo.getAuthModelId());

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

            {
                // Save network handlers' configurations
                if (!CommonUtils.isEmpty(connectionInfo.getHandlers())) {
                    json.name(RegistryConstants.TAG_HANDLERS);
                    json.beginObject();

                    String configProfileName = connectionInfo.getConfigProfileName();
                    DBWNetworkProfile networkProfile = CommonUtils.isEmpty(configProfileName) ? null :
                        registry.getNetworkProfiles().getProfile(connectionInfo.getConfigProfileSource(), configProfileName);
                    for (DBWHandlerConfiguration configuration : connectionInfo.getHandlers()) {
                        if (configuration.isEnabled()) {
                            DBWHandlerConfiguration profileConfig = networkProfile == null ? null :
                                networkProfile.getConfiguration(configuration.getHandlerDescriptor());
                            DataSourceParser.saveNetworkHandlerConfiguration(
                                contextParameters,
                                json,
                                dataSource,
                                null,
                                configuration,
                                profileConfig != null && !configurationManager.isTrusted()
                            );
                        }
                    }
                    json.endObject();
                }
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

        // Filters
        filterSerializer.saveObjectFilters(json, RegistryConstants.TAG_FILTERS, dataSource, false);

        // Tags
        JSONUtils.serializeProperties(json, RegistryConstants.TAG_TAGS, dataSource.getTags(), true);

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
                JSONUtils.serializeProperties(json, RegistryConstants.TAG_CUSTOM_PROPERTIES, props, true);
            }
        }

        // Extensions
        JSONUtils.serializeProperties(json, RegistryConstants.TAG_EXTENSIONS, dataSource.getExtensions(), true);
    }

    private void serializeModifyPermissions(
        @NotNull JsonWriter json,
        @NotNull DBPDataSourcePermissionOwner permissionOwner
    ) throws IOException {
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


    @NotNull
    private static DBPDriver getReplacementDriver(@NotNull DBPDriver driver) {
        DBPDriver replacement = driver;

        while (replacement.getReplacedBy() != null) {
            replacement = replacement.getReplacedBy();
        }

        return replacement;
    }
}
