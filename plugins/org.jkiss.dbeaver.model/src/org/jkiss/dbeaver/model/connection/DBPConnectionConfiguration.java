/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.connection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.access.DBAAuthCredentials;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.IVariableResolver;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Connection configuration.
 */
public class DBPConnectionConfiguration implements DBPObject {
    // Variables
    public static final String VARIABLE_HOST = "host";
    public static final String VARIABLE_HOST_TUNNEL = "host.tunnel";
    public static final String VARIABLE_PORT = "port";
    public static final String VARIABLE_SERVER = "server";
    public static final String VARIABLE_DATABASE = "database";
    public static final String VARIABLE_USER = "user";
    public static final String VARIABLE_PASSWORD = "password";
    public static final String VARIABLE_URL = "url";
    public static final String VARIABLE_CONN_TYPE = "connection.type";
    public static final String VARIABLE_CONN_TYPE_LEGACY = "connectionType";
    public static final String VARIABLE_DATASOURCE = "datasource";
    public static final String VAR_PROJECT_PATH = "project.path";
    public static final String VAR_PROJECT_NAME = "project.name";
    public static final String VAR_HOST_OR_DATABASE = "host_or_database";
    public static final String VARIABLE_PREFIX_PROPERTIES = "properties.";
    public static final String VARIABLE_PREFIX_AUTH = "auth.";
    public static final String VARIABLE_PREFIX_ORIGIN = "origin.";
    public static final String VARIABLE_PREFIX_TAG = "tag.";

    public static final String VARIABLE_DATE = "date";
    public static final String VARIABLE_TIME = "time";

    public static final String[][] CONNECT_VARIABLES = new String[][]{
        {VARIABLE_HOST, "target database host"},
        {VARIABLE_HOST_TUNNEL, "tunnel host name"},
        {VARIABLE_PORT, "target database port"},
        {VARIABLE_SERVER, "target server name"},
        {VARIABLE_DATABASE, "target database name"},
        {VARIABLE_USER, "database user name"},
        {VARIABLE_URL, "connection URL"},
        {VARIABLE_CONN_TYPE, "connection type"},
        {VARIABLE_DATASOURCE, "datasource"},
        {VAR_PROJECT_PATH, "project path"},
        {VAR_PROJECT_NAME, "project name"},
        {VARIABLE_DATE, "current date"},

        {SystemVariablesResolver.VAR_WORKSPACE, "workspace path"},
        {SystemVariablesResolver.VAR_HOME, "OS user home path"},
        {SystemVariablesResolver.VAR_DBEAVER_HOME, "application install path"},
        {SystemVariablesResolver.VAR_APP_PATH, "application install path"},
        {SystemVariablesResolver.VAR_APP_NAME, "application name"},
        {SystemVariablesResolver.VAR_APP_VERSION, "application version"},
        {SystemVariablesResolver.VAR_LOCAL_IP, "local IP address"},
    };

    public static final String[][] INTERNAL_CONNECT_VARIABLES = ArrayUtils.concatArrays(
        CONNECT_VARIABLES,
        new String[][]{
            // {VARIABLE_PASSWORD, "database password (plain)"},  see dbeaver/pro#1861
        });

    private static final Log log = Log.getLog(DBPConnectionConfiguration.class);

    private String hostName;
    private String hostPort;
    private String serverName;
    private String databaseName;
    private String userName;
    private String userPassword;
    private String url;
    private String clientHomeId;

    private String configProfileSource;
    private String configProfileName;

    @NotNull
    private final Map<String, String> properties;
    @NotNull
    private final Map<String, String> providerProperties;
    @NotNull
    private final Map<String, Object> runtimeAttributes;
    @NotNull
    private final Map<DBPConnectionEventType, DBRShellCommand> events;
    @NotNull
    private final List<DBWHandlerConfiguration> handlers;
    private final DBPConnectionBootstrap bootstrap;
    private DBPConnectionType connectionType;
    private DBPDriverConfigurationType configurationType;
    private String connectionColor;
    private int keepAliveInterval;
    private boolean closeIdleConnection;
    private int closeIdleInterval;

    private String authModelId;
    private Map<String, String> authProperties;

    public DBPConnectionConfiguration() {
        this.connectionType = DBPConnectionType.DEFAULT_TYPE;
        this.configurationType = DBPDriverConfigurationType.MANUAL;
        this.properties = new LinkedHashMap<>();
        this.providerProperties = new LinkedHashMap<>();
        this.events = new LinkedHashMap<>();
        this.runtimeAttributes = new HashMap<>();
        this.handlers = new ArrayList<>();
        this.bootstrap = new DBPConnectionBootstrap();
        this.keepAliveInterval = 0;
        this.closeIdleConnection = true;
        this.closeIdleInterval = 0;
    }

    public DBPConnectionConfiguration(@NotNull DBPConnectionConfiguration info) {
        this.hostName = info.hostName;
        this.hostPort = info.hostPort;
        this.serverName = info.serverName;
        this.databaseName = info.databaseName;
        this.userName = info.userName;
        this.userPassword = info.userPassword;
        this.url = info.url;
        this.clientHomeId = info.clientHomeId;
        this.configProfileSource = info.configProfileSource;
        this.configProfileName = info.configProfileName;
        this.authModelId = info.authModelId;
        this.authProperties = info.authProperties == null ? null : new LinkedHashMap<>(info.authProperties);
        this.connectionType = info.connectionType;
        this.configurationType = info.configurationType;
        this.properties = new LinkedHashMap<>(info.properties);
        this.providerProperties = new LinkedHashMap<>(info.providerProperties);
        this.runtimeAttributes = new HashMap<>(info.runtimeAttributes);
        this.events = new LinkedHashMap<>(info.events.size());
        for (Map.Entry<DBPConnectionEventType, DBRShellCommand> entry : info.events.entrySet()) {
            this.events.put(entry.getKey(), new DBRShellCommand(entry.getValue()));
        }
        this.handlers = new ArrayList<>(info.handlers.size());
        for (DBWHandlerConfiguration handler : info.handlers) {
            this.handlers.add(new DBWHandlerConfiguration(handler));
        }
        this.bootstrap = new DBPConnectionBootstrap(info.bootstrap);
        this.connectionColor = info.connectionColor;
        this.keepAliveInterval = info.keepAliveInterval;
        this.closeIdleConnection = info.closeIdleConnection;
        this.closeIdleInterval = info.closeIdleInterval;
    }

    public String getClientHomeId() {
        return clientHomeId;
    }

    public void setClientHomeId(String clientHomeId) {
        this.clientHomeId = clientHomeId;
    }

    @Nullable
    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getHostPort() {
        return hostPort;
    }

    public void setHostPort(String hostPort) {
        this.hostPort = hostPort;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(@Nullable String userPassword) {
        this.userPassword = userPassword;
    }

    ////////////////////////////////////////////////////
    // Properties (connection properties, usually used by driver)

    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    public String getProperty(String name) {
        return properties.get(name);
    }

    public void setProperty(String name, String value) {
        properties.put(name, value);
    }

    public void removeProperty(String name) {
        properties.remove(name);
    }
    
    @NotNull
    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(@NotNull Map<String, String> properties) {
        this.properties.clear();
        this.properties.putAll(properties);
    }

    ////////////////////////////////////////////////////
    // Provider properties (extra configuration parameters)

    public String getProviderProperty(String name) {
        return providerProperties.get(name);
    }

    public void setProviderProperty(String name, String value) {
        providerProperties.put(name, value);
    }

    public void removeProviderProperty(String name) {
        providerProperties.remove(name);
    }

    @NotNull
    public Map<String, String> getProviderProperties() {
        return providerProperties;
    }

    public void setProviderProperties(@NotNull Map<String, String> properties) {
        this.providerProperties.clear();
        this.providerProperties.putAll(properties);
    }

    ////////////////////////////////////////////////////
    // Runtime attributes

    public Object getRuntimeAttribute(String name) {
        return runtimeAttributes.get(name);
    }

    public void setRuntimeAttribute(String name, Object value) {
        runtimeAttributes.put(name, value);
    }

    @NotNull
    public Map<String, Object> getRuntimeAttribute() {
        return runtimeAttributes;
    }

    ////////////////////////////////////////////////////
    // Events

    public DBRShellCommand getEvent(DBPConnectionEventType eventType) {
        return events.get(eventType);
    }

    public void setEvent(DBPConnectionEventType eventType, DBRShellCommand command) {
        if (command == null) {
            events.remove(eventType);
        } else {
            events.put(eventType, command);
        }
    }

    public DBPConnectionEventType[] getDeclaredEvents() {
        Set<DBPConnectionEventType> eventTypes = events.keySet();
        return eventTypes.toArray(new DBPConnectionEventType[0]);
    }

    ////////////////////////////////////////////////////
    // Network handlers

    @NotNull
    public List<DBWHandlerConfiguration> getHandlers() {
        return handlers;
    }

    public void setHandlers(@NotNull List<DBWHandlerConfiguration> handlers) {
        synchronized (this.handlers) {
            this.handlers.clear();
            this.handlers.addAll(handlers);
        }
    }

    public void updateHandler(DBWHandlerConfiguration handler) {
        synchronized (handlers) {
            for (int i = 0; i < handlers.size(); i++) {
                if (handlers.get(i).getId().equals(handler.getId())) {
                    handlers.set(i, handler);
                    return;
                }
            }
            this.handlers.add(handler);
        }
    }

    @Nullable
    public DBWHandlerConfiguration getHandler(String id) {
        synchronized (handlers) {
            for (DBWHandlerConfiguration cfg : handlers) {
                if (cfg.getId().equals(id)) {
                    return cfg;
                }
            }
            return null;
        }
    }

    ////////////////////////////////////////////////////
    // Misc

    public DBPConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(DBPConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    @NotNull
    public DBPDriverConfigurationType getConfigurationType() {
        return configurationType;
    }

    public void setConfigurationType(@NotNull DBPDriverConfigurationType configurationType) {
        this.configurationType = configurationType;
    }

    /**
     * Color in RGB format
     *
     * @return RGB color or null
     */
    public String getConnectionColor() {
        return connectionColor;
    }

    public void setConnectionColor(String color) {
        this.connectionColor = color;
    }

    @NotNull
    public DBPConnectionBootstrap getBootstrap() {
        return bootstrap;
    }

    /**
     * Keep-Alive interval (seconds).
     * Zero or negative means no keep-alive.
     */
    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public void setKeepAliveInterval(int keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    public boolean isCloseIdleConnection() {
        return closeIdleConnection;
    }

    public void setCloseIdleConnection(boolean closeIdleConnection) {
        this.closeIdleConnection = closeIdleConnection;
    }

    public int getCloseIdleInterval() {
        return closeIdleInterval;
    }

    public void setCloseIdleInterval(int closeIdleInterval) {
        this.closeIdleInterval = closeIdleInterval;
    }

    public String getConfigProfileSource() {
        return configProfileSource;
    }

    public void setConfigProfileSource(String configProfileSource) {
        this.configProfileSource = configProfileSource;
    }

    public String getConfigProfileName() {
        return configProfileName;
    }

    public void setConfigProfileName(String configProfileName) {
        this.configProfileName = configProfileName;
    }

    public void setConfigProfile(DBWNetworkProfile profile) {
        if (profile == null) {
            configProfileName = null;
        } else {
            configProfileSource = profile.getProfileSource();
            configProfileName = profile.getProfileId();
            for (DBWHandlerConfiguration handlerConfig : profile.getConfigurations()) {
                if (handlerConfig.isEnabled()) {
                    updateHandler(new DBWHandlerConfiguration(handlerConfig));
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////
    // Authentication

    @Nullable
    public String getAuthModelId() {
        return authModelId;
    }

    @NotNull
    public <T extends DBAAuthCredentials> DBAAuthModel<T> getAuthModel() {
        if (!CommonUtils.isEmpty(authModelId)) {
            DBPAuthModelDescriptor authModelDesc = getAuthModelDescriptor(authModelId);
            if (authModelDesc != null) {
                return authModelDesc.getInstance();
            } else {
                log.error("Authentication model '" + authModelId + "' not found. Use default.");
            }
        }
        return AuthModelDatabaseNative.INSTANCE;
    }

    @NotNull
    public DBPAuthModelDescriptor getAuthModelDescriptor() {
        if (!CommonUtils.isEmpty(authModelId)) {
            DBPAuthModelDescriptor authModelDesc = getAuthModelDescriptor(authModelId);
            if (authModelDesc != null) {
                return authModelDesc;
            } else {
                log.error("Authentication model '" + authModelId + "' not found. Use default.");
            }
        }
        return getAuthModelDescriptor(AuthModelDatabaseNative.ID);
    }

    private DBPAuthModelDescriptor getAuthModelDescriptor(String id) {
        return DBWorkbench.getPlatform().getDataSourceProviderRegistry().getAuthModel(id);
    }

    public void setAuthModelId(String authModelId) {
        this.authModelId = authModelId;
    }

    public String getAuthProperty(String name) {
        return authProperties == null ? null : authProperties.get(name);
    }

    public Map<String, String> getAuthProperties() {
        return authProperties;
    }

    public void setAuthProperties(@Nullable Map<String, String> authProperties) {
        if (authProperties == null) {
            this.authProperties = null;
        } else {
            this.authProperties = new LinkedHashMap<>(authProperties);
        }
    }

    public void setAuthProperty(@NotNull String name, @Nullable String value) {
        if (authProperties == null) {
            authProperties = new LinkedHashMap<>();
        }
        if (value == null) {
            this.authProperties.remove(name);
        } else {
            this.authProperties.put(name, value);
        }
    }

    ///////////////////////////////////////////////////////////
    // Misc

    @Override
    public String toString() {
        return "ConnectionConfiguration: " + (url == null ? databaseName : url);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DBPConnectionConfiguration)) {
            return false;
        }
        DBPConnectionConfiguration source = (DBPConnectionConfiguration) obj;
        return
            CommonUtils.equalOrEmptyStrings(this.hostName, source.hostName) &&
                CommonUtils.equalOrEmptyStrings(this.hostPort, source.hostPort) &&
                CommonUtils.equalOrEmptyStrings(this.serverName, source.serverName) &&
                CommonUtils.equalOrEmptyStrings(this.databaseName, source.databaseName) &&
                CommonUtils.equalOrEmptyStrings(this.userName, source.userName) &&
                CommonUtils.equalOrEmptyStrings(this.userPassword, source.userPassword) &&
                CommonUtils.equalOrEmptyStrings(this.url, source.url) &&
                CommonUtils.equalObjects(this.configurationType, source.configurationType) &&
                CommonUtils.equalObjects(this.clientHomeId, source.clientHomeId) &&
                CommonUtils.equalObjects(this.configProfileSource, source.configProfileSource) &&
                CommonUtils.equalObjects(this.configProfileName, source.configProfileName) &&
                CommonUtils.equalObjects(this.authModelId, source.authModelId) &&
                CommonUtils.equalObjects(this.authProperties, source.authProperties) &&
                CommonUtils.equalObjects(this.connectionType, source.connectionType) &&
                CommonUtils.equalObjects(this.properties, source.properties) &&
                CommonUtils.equalObjects(this.providerProperties, source.providerProperties) &&
                CommonUtils.equalObjects(this.events, source.events) &&
                CommonUtils.equalObjects(this.handlers, source.handlers) &&
                CommonUtils.equalObjects(this.bootstrap, source.bootstrap) &&
                this.keepAliveInterval == source.keepAliveInterval &&
                this.closeIdleConnection == source.closeIdleConnection &&
                this.closeIdleInterval == source.closeIdleInterval;
    }

    public void resolveDynamicVariables(IVariableResolver variableResolver) {
        hostName = GeneralUtils.replaceVariables(hostName, variableResolver);
        hostPort = GeneralUtils.replaceVariables(hostPort, variableResolver);
        serverName = GeneralUtils.replaceVariables(serverName, variableResolver);
        databaseName = GeneralUtils.replaceVariables(databaseName, variableResolver);
        userName = GeneralUtils.replaceVariables(userName, variableResolver);
        userPassword = GeneralUtils.replaceVariables(userPassword, variableResolver);
        url = GeneralUtils.replaceVariables(url, variableResolver);

        resolveDynamicVariablesInMap(this.properties, variableResolver);
        resolveDynamicVariablesInMap(this.authProperties, variableResolver);
        resolveDynamicVariablesInMap(this.providerProperties, variableResolver);
        for (DBWHandlerConfiguration handler : handlers) {
            if (handler.isEnabled()) {
                handler.resolveDynamicVariables(variableResolver);
            }
        }
        bootstrap.resolveDynamicVariables(variableResolver);
    }

    private void resolveDynamicVariablesInMap(Map<String, String> map, IVariableResolver variableResolver) {
        if (map == null) {
            return;
        }
        for (Map.Entry<String, String> prop : map.entrySet()) {
            prop.setValue(GeneralUtils.replaceVariables(prop.getValue(), variableResolver));
        }
    }

}
