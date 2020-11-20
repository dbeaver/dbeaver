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

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.DBPInformationProvider;
import org.jkiss.dbeaver.model.app.DBASecureStorage;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Connection spec utils.
 */
public class DataSourceUtils {

    public static final String PARAM_ID = "id";
    public static final String PARAM_DRIVER = "driver";
    public static final String PARAM_NAME = "name";
    public static final String PARAM_URL = "url";
    public static final String PARAM_HOST = "host";
    public static final String PARAM_PORT = "port";
    public static final String PARAM_SERVER = "server";
    public static final String PARAM_DATABASE = "database";
    public static final String PARAM_USER = "user";

    private static final String PARAM_PASSWORD = "password";
    private static final String PARAM_SAVE_PASSWORD = "savePassword";
    private static final String PARAM_AUTH_MODEL = "auth";
    private static final String PARAM_SHOW_SYSTEM_OBJECTS = "showSystemObjects";
    private static final String PARAM_SHOW_UTILITY_OBJECTS = "showUtilityObjects";
    private static final String PARAM_SHOW_ONLY_ENTITIES = "showOnlyEntities";
    private static final String PARAM_HIDE_FOLDERS = "hideFolders";
    private static final String PARAM_HIDE_SCHEMAS = "hideSchemas";
    private static final String PARAM_MERGE_ENTITIES = "mergeEntities";
    private static final String PARAM_FOLDER = "folder";
    private static final String PARAM_AUTO_COMMIT = "autoCommit";

    private static final String PREFIX_HANDLER = "handler.";
    private static final String PREFIX_PROP = "prop.";
    private static final String PREFIX_AUTH_PROP = "authProp.";

    private static final Log log = Log.getLog(DataSourceUtils.class);

    public static DBPDataSourceContainer getDataSourceBySpec(
        @NotNull DBPProject project,
        @NotNull String connectionSpec,
        @Nullable GeneralUtils.IParameterHandler parameterHandler,
        boolean searchByParameters,
        boolean createNewDataSource)
    {
        String driverName = null, url = null, host = null, port = null,
            server = null, database = null,
            user = null, password = null, authModelId = null;
        boolean
            showSystemObjects = false,
            showUtilityObjects = false,
            showOnlyEntities = false,
            hideFolders = false,
            hideSchemas = false,
            mergeEntities = false,
            savePassword = true;
        Boolean autoCommit = null;
        Map<String, String> conProperties = new HashMap<>();
        Map<String, Map<String, String>> handlerProps = new HashMap<>();
        Map<String, String> authProperties = new HashMap<>();
        DBPDataSourceFolder folder = null;
        String dsId = null, dsName = null;

        DBPDataSourceRegistry dsRegistry = project == null ? null : project.getDataSourceRegistry();
        if (dsRegistry == null) {
            log.debug("No datasource registry for project '" + project.getName() + "'");
            return null;
        }

        String[] conParams = connectionSpec.split("\\|");
        for (String cp : conParams) {
            int divPos = cp.indexOf('=');
            if (divPos == -1) {
                continue;
            }
            String paramName = cp.substring(0, divPos);
            String paramValue = cp.substring(divPos + 1);
            switch (paramName) {
                case PARAM_ID:
                    dsId = paramValue;
                    break;
                case PARAM_DRIVER:
                    driverName = paramValue;
                    break;
                case PARAM_NAME:
                    dsName = paramValue;
                    break;
                case PARAM_URL:
                    url = paramValue;
                    break;
                case PARAM_HOST:
                    host = paramValue;
                    break;
                case PARAM_PORT:
                    port = paramValue;
                    break;
                case PARAM_SERVER:
                    server = paramValue;
                    break;
                case PARAM_DATABASE:
                    database = paramValue;
                    break;
                case PARAM_USER:
                    user = paramValue;
                    break;
                case PARAM_PASSWORD:
                    password = paramValue;
                    break;
                case PARAM_AUTH_MODEL:
                    authModelId = paramValue;
                    break;
                case PARAM_SAVE_PASSWORD:
                    savePassword = CommonUtils.toBoolean(paramValue);
                    break;
                case PARAM_SHOW_SYSTEM_OBJECTS:
                    showSystemObjects = CommonUtils.toBoolean(paramValue);
                    break;
                case PARAM_SHOW_UTILITY_OBJECTS:
                    showUtilityObjects = CommonUtils.toBoolean(paramValue);
                    break;
                case PARAM_SHOW_ONLY_ENTITIES:
                    showOnlyEntities = CommonUtils.toBoolean(paramValue);
                    break;
                case PARAM_HIDE_FOLDERS:
                    hideFolders = CommonUtils.toBoolean(paramValue);
                    break;
                case PARAM_HIDE_SCHEMAS:
                    hideSchemas = CommonUtils.toBoolean(paramValue);
                    break;
                case PARAM_MERGE_ENTITIES:
                    mergeEntities = CommonUtils.toBoolean(paramValue);
                    break;
                case PARAM_FOLDER:
                    folder = dsRegistry.getFolder(paramValue);
                    break;
                case PARAM_AUTO_COMMIT:
                    autoCommit = CommonUtils.toBoolean(paramValue);
                    break;
                default:
                    boolean handled = false;
                    if (paramName.length() > PREFIX_PROP.length() && paramName.startsWith(PREFIX_PROP)) {
                        paramName = paramName.substring(PREFIX_PROP.length());
                        conProperties.put(paramName, paramValue);
                        handled = true;
                    } else if (paramName.length() > PREFIX_AUTH_PROP.length() && paramName.startsWith(PREFIX_AUTH_PROP)) {
                        paramName = paramName.substring(PREFIX_AUTH_PROP.length());
                        authProperties.put(paramName, paramValue);
                        handled = true;
                    } else if (paramName.length() > PREFIX_HANDLER.length() && paramName.startsWith(PREFIX_HANDLER)) {
                        // network handler prop
                        paramName = paramName.substring(PREFIX_HANDLER.length());
                        divPos = paramName.indexOf('.');
                        if (divPos == -1) {
                            log.debug("Wrong handler parameter: '" + paramName + "'");
                            continue;
                        }
                        String handlerId = paramName.substring(0, divPos);
                        paramName = paramName.substring(divPos + 1);
                        Map<String, String> handlerPopMap = handlerProps.computeIfAbsent(handlerId, k -> new HashMap<>());
                        handlerPopMap.put(paramName, paramValue);
                        handled = true;
                    } else if (parameterHandler != null) {
                        handled = parameterHandler.setParameter(paramName, paramValue);
                    }
                    if (!handled) {
                        log.debug("Unknown connection parameter '" + paramName + "'");
                    }
            }
        }

        DBPDataSourceContainer dataSource = null;

        if (dsId != null) {
            dataSource = dsRegistry.getDataSource(dsId);
        }

        if (dsName != null) {
            dataSource = dsRegistry.findDataSourceByName(dsName);
        }

        if (dataSource != null) {
            DBPConnectionConfiguration connConfig = dataSource.getConnectionConfiguration();
            if (!CommonUtils.isEmpty(database)) connConfig.setDatabaseName(database);
            if (!CommonUtils.isEmpty(user)) connConfig.setUserName(user);
            if (!CommonUtils.isEmpty(password)) connConfig.setUserPassword(password);
            if (!CommonUtils.isEmpty(conProperties)) connConfig.setProperties(conProperties);
            if (!CommonUtils.isEmpty(authProperties)) connConfig.setAuthProperties(authProperties);
            if (!CommonUtils.isEmpty(authModelId)) connConfig.setAuthModelId(authModelId);

            return dataSource;
        }

        if (searchByParameters) {
            // Try to find by parameters / handler props
            if (url != null) {
                for (DBPDataSourceContainer ds : dsRegistry.getDataSources()) {
                    if (url.equals(ds.getConnectionConfiguration().getUrl())) {
                        if (user == null || user.equals(ds.getConnectionConfiguration().getUserName())) {
                            return ds;
                        }
                    }
                }
            } else {
                for (DBPDataSourceContainer ds : dsRegistry.getDataSources()) {
                    DBPConnectionConfiguration cfg = ds.getConnectionConfiguration();
                    if (server != null && !server.equals(cfg.getServerName()) ||
                        host != null && !host.equals(cfg.getHostName()) ||
                        port != null && !port.equals(cfg.getHostPort()) ||
                        database != null && !database.equals(cfg.getDatabaseName()) ||
                        user != null && !user.equals(cfg.getUserName()))
                    {
                        continue;
                    }
                    boolean matched = true;
                    if (!conProperties.isEmpty()) {
                        for (Map.Entry<String, String> prop : conProperties.entrySet()) {
                            if (!CommonUtils.equalObjects(cfg.getProperty(prop.getKey()), prop.getValue())) {
                                matched = false;
                                break;
                            }
                        }
                        if (!matched) {
                            continue;
                        }
                    }
                    if (!handlerProps.isEmpty()) {
                        for (Map.Entry<String, Map<String, String>> handlerProp : handlerProps.entrySet()) {
                            DBWHandlerConfiguration handler = cfg.getHandler(handlerProp.getKey());
                            if (handler == null) {
                                matched = false;
                                break;
                            }
                            for (Map.Entry<String, String> prop : handlerProp.getValue().entrySet()) {
                                if (!CommonUtils.equalObjects(handler.getProperty(prop.getKey()), prop.getValue())) {
                                    matched = false;
                                    break;
                                }

                            }
                            if (!matched) {
                                break;
                            }
                        }
                        if (!matched) {
                            continue;
                        }
                    }
                    return ds;
                }
            }
        }

        if (!createNewDataSource) {
            return null;
        }

        if (driverName == null) {
            log.error("Driver name not specified - can't create new datasource");
            return null;
        }
        DBPDriver driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver(driverName);
        if (driver == null) {
            log.error("Driver '" + driverName + "' not found");
            return null;
        }

        // Create new datasource with specified parameters
        if (dsName == null) {
            dsName = "Ext: " + driver.getName();
            if (database != null) {
                dsName += " - " + database;
            } else if (server != null) {
                dsName += " - " + server;
            }
        }

        DBPConnectionConfiguration connConfig = new DBPConnectionConfiguration();
        connConfig.setUrl(url);
        connConfig.setHostName(host);
        connConfig.setHostPort(port);
        connConfig.setServerName(server);
        connConfig.setDatabaseName(database);
        connConfig.setUserName(user);
        connConfig.setUserPassword(password);
        connConfig.setProperties(conProperties);
        if (!CommonUtils.isEmpty(authProperties)) {
            connConfig.setAuthProperties(authProperties);
        }
        if (!CommonUtils.isEmpty(authModelId)) {
            connConfig.setAuthModelId(authModelId);
        }

        if (autoCommit != null) {
            connConfig.getBootstrap().setDefaultAutoCommit(autoCommit);
        }

        DBPDataSourceContainer newDS = dsRegistry.createDataSource(driver, connConfig);
        newDS.setName(dsName);
        ((DataSourceDescriptor)newDS).setTemporary(true);
        if (savePassword) {
            newDS.setSavePassword(true);
        }
        if (folder != null) {
            newDS.setFolder(folder);
        }
        DataSourceNavigatorSettings navSettings = ((DataSourceDescriptor)newDS).getNavigatorSettings();
        navSettings.setShowSystemObjects(showSystemObjects);
        navSettings.setShowUtilityObjects(showUtilityObjects);
        navSettings.setShowOnlyEntities(showOnlyEntities);
        navSettings.setHideSchemas(hideSchemas);
        navSettings.setHideFolders(hideFolders);
        navSettings.setMergeEntities(mergeEntities);

        //ds.set
        dsRegistry.addDataSource(newDS);
        return newDS;
    }

    /**
     * Save secure config in protected storage.
     * @return true on success (if protected storage is available and configured)
     */
    static boolean saveCredentialsInSecuredStorage(
        @NotNull DBPProject project,
        @Nullable DataSourceDescriptor dataSource,
        @Nullable String subNode,
        @NotNull SecureCredentials credentials)
    {
        final DBASecureStorage secureStorage = project.getSecureStorage();
        {
            try {
                ISecurePreferences prefNode = dataSource == null ?
                    project.getSecureStorage().getSecurePreferences() :
                    dataSource.getSecurePreferences();
                if (!secureStorage.useSecurePreferences()) {
                    prefNode.removeNode();
                } else {
                    if (subNode != null) {
                        for (String nodeName : subNode.split("/")) {
                            prefNode = prefNode.node(nodeName);
                        }
                    }
                    prefNode.put("name", dataSource != null ? dataSource.getName() : project.getName(), false);

                    if (!CommonUtils.isEmpty(credentials.getUserName())) {
                        prefNode.put(RegistryConstants.ATTR_USER, credentials.getUserName(), true);
                    } else {
                        prefNode.remove(RegistryConstants.ATTR_USER);
                    }
                    if (!CommonUtils.isEmpty(credentials.getUserPassword())) {
                        prefNode.put(RegistryConstants.ATTR_PASSWORD, credentials.getUserPassword(), true);
                    } else {
                        prefNode.remove(RegistryConstants.ATTR_PASSWORD);
                    }
                    if (!CommonUtils.isEmpty(credentials.getProperties())) {
                        for (Map.Entry<String, String> prop : credentials.getProperties().entrySet()) {
                            prefNode.put(prop.getKey(), prop.getValue(), true);
                        }
                    }
                    return true;
                }
            } catch (Throwable e) {
                log.error("Can't save credentials in secure storage", e);
            }
        }
        return false;
    }

    @NotNull
    public static String getDataSourceAddressText(DBPDataSourceContainer dataSourceContainer) {
        DBPDataSourceProvider dataSourceProvider = dataSourceContainer.getDriver().getDataSourceProvider();
        if (dataSourceProvider instanceof DBPInformationProvider) {
            String objectInformation = ((DBPInformationProvider) dataSourceProvider).getObjectInformation(dataSourceContainer, DBPInformationProvider.INFO_TARGET_ADDRESS);
            if (!CommonUtils.isEmpty(objectInformation)) {
                return objectInformation;
            }
        }
        DBPConnectionConfiguration cfg = dataSourceContainer.getConnectionConfiguration();
        String hostText = getTargetTunnelHostName(cfg);
        String hostPort = cfg.getHostPort();
        if (!CommonUtils.isEmpty(hostPort)) {
            return hostText + ":" + hostPort;
        }
        return hostText;
    }

    @NotNull
    public static String getTargetTunnelHostName(DBPConnectionConfiguration cfg) {
        String hostText = cfg.getHostName();
        // For localhost ry to get real host name from tunnel configuration
        if (CommonUtils.isEmpty(hostText) || hostText.equals("localhost") || hostText.equals("127.0.0.1")) {
            for (DBWHandlerConfiguration hc : cfg.getHandlers()) {
                if (hc.isEnabled() && hc.getType() == DBWHandlerType.TUNNEL) {
                    String tunnelHost = hc.getStringProperty(DBWHandlerConfiguration.PROP_HOST);
                    if (!CommonUtils.isEmpty(tunnelHost)) {
                        hostText = tunnelHost;
                        break;
                    }
                }
            }
        }
        return CommonUtils.notEmpty(hostText);
    }
}
