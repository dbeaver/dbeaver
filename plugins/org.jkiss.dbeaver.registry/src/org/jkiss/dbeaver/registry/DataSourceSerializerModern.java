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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionEventType;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.runtime.encode.EncryptionException;
import org.jkiss.dbeaver.runtime.encode.PasswordEncrypter;
import org.jkiss.dbeaver.runtime.encode.SimpleStringEncrypter;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

class DataSourceSerializerModern implements DataSourceSerializer
{
    private static final Log log = Log.getLog(DataSourceSerializerModern.class);

    private static PasswordEncrypter ENCRYPTOR = new SimpleStringEncrypter();

    private static Gson CONFIG_GSON = new GsonBuilder()
        .setLenient()
        .serializeNulls()
        .setPrettyPrinting()
        .create();

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
                    jsonWriter.beginArray();
                    // Folders (only for default origin)
                    for (DataSourceFolder folder : dataSourceFolders) {
                        saveFolder(jsonWriter, folder);
                    }
                    jsonWriter.endArray();
                }

                {
                    // Save connections
                    jsonWriter.name("connections");
                    jsonWriter.beginArray();
                    for (DataSourceDescriptor dataSource : localDataSources) {
                        // Skip temporary
                        if (!dataSource.isTemporary()) {
                            saveDataSource(jsonWriter, dataSource);
                        }
                    }
                    jsonWriter.endArray();
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
        throw new DBException("Not implemented yet");
    }

    private static void saveFolder(JsonWriter json, DataSourceFolder folder)
        throws IOException
    {
        json.beginObject();
        if (folder.getParent() != null) {
            json.name(RegistryConstants.ATTR_PARENT);
            json.value(folder.getParent().getFolderPath());
        }

        json.name(RegistryConstants.ATTR_NAME);
        json.value(folder.getName());
        if (!CommonUtils.isEmpty(folder.getDescription())) {
            json.name(RegistryConstants.ATTR_DESCRIPTION);
            json.value(folder.getDescription());
        }

        json.endObject();
    }

    static void saveDataSource(JsonWriter json, DataSourceDescriptor dataSource)
        throws IOException
    {
        json.beginObject();
        JSONUtils.field(json, RegistryConstants.ATTR_ID, dataSource.getId());
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

        {
            // Connection info
            DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
            json.name(RegistryConstants.TAG_CONNECTION);
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
            serializeProperties(json, RegistryConstants.TAG_PROPERTIES, connectionInfo.getProperties());
            serializeProperties(json, RegistryConstants.TAG_PROVIDER_PROPERTIES, connectionInfo.getProviderProperties());

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
                json.beginArray();
                for (DBWHandlerConfiguration configuration : connectionInfo.getDeclaredHandlers()) {
                    json.beginObject();
                    JSONUtils.field(json, RegistryConstants.ATTR_TYPE, configuration.getType().name());
                    JSONUtils.field(json, RegistryConstants.ATTR_ID, CommonUtils.notEmpty(configuration.getId()));
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
                    serializeProperties(json, RegistryConstants.TAG_PROPERTIES, configuration.getProperties());
                    json.endObject();
                }
                json.endArray();
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
                    serializeList(json, RegistryConstants.TAG_QUERY, bootstrap.getInitQueries());
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

/*
        // Virtual model
        if (dataSource.getVirtualModel().hasValuableData()) {
            xml.startElement(RegistryConstants.TAG_VIRTUAL_META_DATA);
            dataSource.getVirtualModel().serialize(xml);
            xml.endElement();
        }
*/
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
                serializeProperties(json, RegistryConstants.TAG_CUSTOM_PROPERTIES, props);
            }
        }


        json.endObject();
    }

    private static void serializeProperties(JsonWriter json, String tagName, Map<String, String> properties) throws IOException {
        if (!CommonUtils.isEmpty(properties)) {
            json.name(tagName);
            json.beginObject();
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                JSONUtils.field(json, entry.getKey(), entry.getValue());
            }
            json.endObject();
        }
    }

    private static void saveObjectFiler(JsonWriter json, String typeName, String objectID, DBSObjectFilter filter) throws IOException
    {
        json.beginObject();
        if (typeName != null) {
            JSONUtils.field(json, RegistryConstants.ATTR_TYPE, typeName);
        }
        if (objectID != null) {
            JSONUtils.field(json, RegistryConstants.ATTR_ID, objectID);
        }
        if (!CommonUtils.isEmpty(filter.getName())) {
            JSONUtils.field(json, RegistryConstants.ATTR_NAME, filter.getName());
        }
        if (!CommonUtils.isEmpty(filter.getDescription())) {
            JSONUtils.field(json, RegistryConstants.ATTR_DESCRIPTION, filter.getDescription());
        }
        if (!filter.isEnabled()) {
            JSONUtils.field(json, RegistryConstants.ATTR_ENABLED, false);
        }
        serializeList(json, RegistryConstants.TAG_INCLUDE, filter.getInclude());
        serializeList(json, RegistryConstants.TAG_EXCLUDE, filter.getExclude());
        json.endObject();
    }

    private static void serializeList(JsonWriter json, String tagName, List<String> list) throws IOException {
        if (!CommonUtils.isEmpty(list)) {
            json.name(tagName);
            json.beginArray();
            for (String include : CommonUtils.safeCollection(list)) {
                json.value(include);
            }
            json.endArray();
        }
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

}
