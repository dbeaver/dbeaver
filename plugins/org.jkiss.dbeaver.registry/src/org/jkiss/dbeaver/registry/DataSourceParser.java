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

import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConfigurationProfile;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataSourceParser {

    private static final Log log = Log.getLog(DataSourceParser.class);

    static final String NODE_CONNECTION = "#connection"; //$NON-NLS-1$

    static void saveSecuredCredentials(
        @NotNull ContextParameters contextParameters,
        @Nullable DataSourceDescriptor dataSource,
        @Nullable DBPConfigurationProfile profile,
        @Nullable String subNode,
        @NotNull SecureCredentials credentials
    ) {
        assert dataSource != null|| profile != null;
        if (contextParameters.project() != null && contextParameters.project().isUseSecretStorage()) {
            return;
        }

        String topNodeId = profile != null ? "profile:" + profile.getProfileId() : dataSource.getId();
        if (subNode == null) subNode = NODE_CONNECTION;

        Map<String, Map<String, String>> nodeMap = contextParameters.secureProperties().computeIfAbsent(topNodeId, s -> new LinkedHashMap<>());
        Map<String, String> propMap = nodeMap.computeIfAbsent(subNode, s -> new LinkedHashMap<>());
        saveCredentialsToMap(propMap, credentials);
        if (propMap.isEmpty()) {
            nodeMap.remove(subNode);
        }
        if (nodeMap.isEmpty()) {
            contextParameters.secureProperties().remove(topNodeId);
        }
    }

    static void saveNetworkProfiles(
        @NotNull ContextParameters contextParameters,
        @NotNull JsonWriter jsonWriter,
        @NotNull List<DBWNetworkProfile> profiles
    ) throws IOException {
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
                        contextParameters,
                        jsonWriter,
                        null,
                        np,
                        configuration,
                        !contextParameters.isTrusted()
                    );
                }
            }
            jsonWriter.endObject();
            jsonWriter.endObject();
        }
        jsonWriter.endObject();
    }

    public record ContextParameters(
        @Nullable DBPProject project,
        @Nullable DataSourceConfigurationManager configurationManager,
        @NotNull Map<String, Map<String, Map<String, String>>> secureProperties
    ) {
        public boolean isTrusted() {
            return configurationManager == null || configurationManager.isTrusted();
        }
        public boolean isSecure() {
            return configurationManager != null && configurationManager.isSecure();
        }
    }

    static void saveNetworkHandlerConfiguration(
        @NotNull ContextParameters contextParameters,
        @NotNull JsonWriter json,
        @Nullable DataSourceDescriptor dataSource,
        @Nullable DBWNetworkProfile profile,
        @NotNull DBWHandlerConfiguration configuration,
        boolean referenceOnly
    ) throws IOException {
        json.name(CommonUtils.notEmpty(configuration.getId()));
        json.beginObject();
        JSONUtils.field(json, RegistryConstants.ATTR_TYPE, configuration.getType().name());
        JSONUtils.field(json, RegistryConstants.ATTR_ENABLED, configuration.isEnabled());
        if (!referenceOnly) {
            JSONUtils.field(json, RegistryConstants.ATTR_SAVE_PASSWORD, configuration.isSavePassword());
            if (!CommonUtils.isEmpty(configuration.getUserName()) ||
                !CommonUtils.isEmpty(configuration.getPassword()) ||
                !CommonUtils.isEmpty(configuration.getSecureProperties())
            ) {
                final SecureCredentials credentials = new SecureCredentials(configuration);
                credentials.setProperties(configuration.getSecureProperties());

                DBPProject project = dataSource != null ?
                    dataSource.getProject() : (profile != null ? profile.getProject() : null);

                if (contextParameters.isTrusted()) {
                    if (contextParameters.isSecure() ||
                        (project != null && project.isUseSecretStorage() && profile == null && dataSource.isSharedCredentials())) {
                        // For secured projects save only shared credentials
                        // Others are stored in secret storage
                        savePlainCredentials(json, credentials);
                    } else {
                        saveSecuredCredentials(
                            contextParameters,
                            dataSource,
                            profile,
                            "network/" + configuration.getId() + (profile == null ? "" : "/profile/" + profile.getProfileName()),
                            credentials
                        );
                    }
                }
            }
            JSONUtils.serializeProperties(json, RegistryConstants.TAG_PROPERTIES, configuration.getProperties(), true);
        }
        json.endObject();
    }

    @NotNull
    public static List<DBWNetworkProfile> parseProfiles(
        @NotNull ContextParameters parameters,
        @NotNull Map<String, Object> json
    ) {
        List<DBWNetworkProfile> profiles = new ArrayList<>();
        // Network profiles
        for (Map.Entry<String, Map<String, Object>> vmMap : JSONUtils.getNestedObjects(json, "network-profiles")) {
            String profileId = vmMap.getKey();
            Map<String, Object> profileMap = vmMap.getValue();
            DBWNetworkProfile profile = new DBWNetworkProfile(parameters.project);
            profile.setProfileName(profileId);
            profile.setProfileName(profileId);
            profile.setProperties(JSONUtils.deserializeStringMap(profileMap, "properties"));

            for (Map.Entry<String, Map<String, Object>> handlerMap : JSONUtils.getNestedObjects(profileMap, "handlers")) {
                DBWHandlerConfiguration configuration = parseNetworkHandlerConfig(parameters, null, profile, handlerMap);
                if (configuration != null) {
                    profile.updateConfiguration(configuration);
                }
            }
            profiles.add(profile);
        }
        return profiles;
    }

    @NotNull
    static SecureCredentials readSecuredCredentials(
        @NotNull ContextParameters contextParameters,
        @Nullable DataSourceDescriptor dataSource,
        @Nullable DBPConfigurationProfile profile,
        @Nullable String subNode)
    {
        assert dataSource != null || profile != null;

        SecureCredentials creds = new SecureCredentials();

        String topNodeId = profile != null ? "profile:" + profile.getProfileId() : dataSource.getId();
        if (subNode == null) subNode = NODE_CONNECTION;

        Map<String, Map<String, String>> subMap = contextParameters.secureProperties.get(topNodeId);
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


    @NotNull
    static SecureCredentials readPlainCredentials(@NotNull Map<String, Object> propMap) {
        Map<String, Object> credentialsMap = JSONUtils.getObject(propMap, "credentials");
        SecureCredentials creds = new SecureCredentials();

        for (Map.Entry<String, Object> entry : credentialsMap.entrySet()) {
            String value = CommonUtils.toString(entry.getValue(), null);
            switch (entry.getKey()) {
                case RegistryConstants.ATTR_USER:
                    creds.setUserName(value);
                    break;
                case RegistryConstants.ATTR_PASSWORD:
                    creds.setUserPassword(value);
                    break;
                default:
                    creds.setSecureProp(entry.getKey(), value);
                    break;
            }
        }

        return creds;
    }

    static void savePlainCredentials(
        @NotNull JsonWriter jsonWriter,
        @NotNull SecureCredentials credentials
    ) throws IOException {
        Map<String, String> propMap = new LinkedHashMap<>();
        saveCredentialsToMap(propMap, credentials);
        JSONUtils.serializeProperties(jsonWriter, "credentials", propMap, true);
    }

    static void saveCredentialsToMap(@NotNull Map<String, String> propMap, @NotNull SecureCredentials credentials) {
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

    @Nullable
    static DBWHandlerConfiguration parseNetworkHandlerConfig(
        @NotNull ContextParameters parameters,
        @Nullable DataSourceDescriptor dataSource,
        @Nullable DBWNetworkProfile profile,
        @NotNull Map.Entry<String, Map<String, Object>> handlerObject
    ) {
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
            {
                final SecureCredentials creds = parameters.isSecure() ?
                    readPlainCredentials(handlerCfg) :
                    readSecuredCredentials(
                        parameters,
                        dataSource,
                        profile,
                        "network/" + handlerId + (profile == null ? "" : "/profile/" + profile.getProfileName())
                    );
                curNetworkHandler.setUserName(creds.getUserName());
                if (curNetworkHandler.isSavePassword()) {
                    curNetworkHandler.setPassword(creds.getUserPassword());
                }
                if (creds.getProperties() != null) {
                    curNetworkHandler.setSecureProperties(creds.getProperties());
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
}

