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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionEventType;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.dbeaver.runtime.encode.SimpleStringEncrypter;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Legacy datasource serialization (xml)
 */
class DataSourceSerializerLegacy<T extends DataSourceDescriptor> implements DataSourceSerializer<T> {
    private static final Log log = Log.getLog(DataSourceSerializerLegacy.class);

    private final DataSourceRegistry<T> registry;

    private static final String LEGACY_DEFAULT_AUTO_COMMIT = "default.autocommit"; //$NON-NLS-1$
    private static final String LEGACY_DEFAULT_ISOLATION = "default.isolation"; //$NON-NLS-1$
    private static final String LEGACY_DEFAULT_ACTIVE_OBJECT = "default.activeObject"; //$NON-NLS-1$

    DataSourceSerializerLegacy(DataSourceRegistry<T> registry) {
        this.registry = registry;
    }

    @Override
    public void saveDataSources(
        DBRProgressMonitor monitor,
        DataSourceConfigurationManager configurationManager,
        DBPDataSourceConfigurationStorage configurationStorage,
        List<T> localDataSources
    ) throws IOException {
        throw new IOException("Legacy serializer is deprecated, save not possible");
    }

    @Override
    public boolean parseDataSources(
        @NotNull DBPDataSourceConfigurationStorage configurationStorage,
        @NotNull DataSourceConfigurationManager configurationManager,
        @NotNull DataSourceRegistry.ParseResults parseResults,
        Collection<String> dataSourceIds
    ) throws DBException {
        try (InputStream is = configurationManager.readConfiguration(configurationStorage.getStorageName(), dataSourceIds)) {
            if (is != null) {
                SAXReader parser = new SAXReader(is);
                final DataSourcesParser dsp = new DataSourcesParser(registry, configurationStorage, parseResults);
                parser.parse(dsp);
            }
        } catch (Exception ex) {
            throw new DBException("Datasource config parse error", ex);
        }
        return false;
    }

    @Nullable
    private static String decryptPassword(String encPassword) {
        if (!CommonUtils.isEmpty(encPassword)) {
            try {
                encPassword = SimpleStringEncrypter.INSTANCE.decrypt(encPassword);
            } catch (Throwable e) {
                // could not decrypt - use as is
                encPassword = null;
            }
        }
        return encPassword;
    }

    private static class DataSourcesParser implements SAXListener {
        DataSourceRegistry registry;
        DataSourceDescriptor curDataSource;
        DBPDataSourceConfigurationStorage storage;
        boolean isDescription = false;
        DBRShellCommand curCommand = null;
        private DBWHandlerConfiguration curNetworkHandler;
        private DBSObjectFilter curFilter;
        private StringBuilder curQuery;
        private final DataSourceRegistry.ParseResults parseResults;
        private boolean passwordReadCanceled = false;

        private DataSourcesParser(DataSourceRegistry registry, DBPDataSourceConfigurationStorage storage, DataSourceRegistry.ParseResults parseResults) {
            this.registry = registry;
            this.storage = storage;
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
                    DataSourceFolder parent = parentFolder == null ? null : registry.findFolderByPath(parentFolder, true, parseResults);
                    DataSourceFolder folder = parent == null ? registry.findFolderByPath(name, true, parseResults) : parent.getChild(name);
                    if (folder == null) {
                        folder = new DataSourceFolder(registry, parent, name, description);
                        parseResults.addedFolders.add(folder);
                    } else {
                        folder.setDescription(description);
                        parseResults.updatedFolders.add(folder);
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
                            storage,
                            DataSourceOriginLocal.INSTANCE,
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
                        curDataSource.setFolder(registry.findFolderByPath(folderPath, true, parseResults));
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
                        parseResults.addedDataSources.add(curDataSource);
                    } else {
                        parseResults.updatedDataSources.add(curDataSource);
                    }
                    break;
                }
                case RegistryConstants.TAG_CONNECTION:
                    if (curDataSource != null) {
                        DBPDriver driver = curDataSource.getDriver();
                        if (CommonUtils.isEmpty(driver.getName())) {
                            if (driver instanceof DriverDescriptor) {
                                // Broken driver - seems to be just created
                                ((DriverDescriptor)driver).setName(atts.getValue(RegistryConstants.ATTR_URL));
                                ((DriverDescriptor)driver).setDriverClassName("java.sql.Driver");
                            }
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
                        // Backward compatibility
                        switch (propName) {
                            case LEGACY_DEFAULT_AUTO_COMMIT:
                                curDataSource.getConnectionConfiguration().getBootstrap().setDefaultAutoCommit(CommonUtils.toBoolean(propValue));
                                break;
                            case LEGACY_DEFAULT_ISOLATION:
                                curDataSource.getConnectionConfiguration().getBootstrap().setDefaultTransactionIsolation(CommonUtils.toInt(propValue));
                                break;
                            case LEGACY_DEFAULT_ACTIVE_OBJECT:
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
                    if (curDataSource != null && curQuery != null && !curQuery.isEmpty()) {
                        curDataSource.getConnectionConfiguration().getBootstrap().getInitQueries().add(curQuery.toString());
                        curQuery = null;
                    }
                    break;
            }
            isDescription = false;
        }

        private String[] readSecuredCredentials(Attributes xmlAttrs, DataSourceDescriptor dataSource, String subNode) {
            String[] creds = new String[2];
            DBPProject project = dataSource.getProject();
            {
                try {
                    if (project.isUseSecretStorage()) {
                        DBSSecretController secretController = DBSSecretController.getProjectSecretController(project);
                        String keyPrefix;
                        if (dataSource == null) {
                            keyPrefix = "projects/" + project.getId();
                        } else {
                            keyPrefix = "datasources/" + dataSource.getId();
                        }
                        Path itemPath = Path.of(keyPrefix).resolve(CommonUtils.notEmpty(subNode));

                        creds[0] = secretController.getPrivateSecretValue(itemPath.resolve(RegistryConstants.ATTR_USER)
                            .toString());
                        creds[1] = secretController.getPrivateSecretValue(itemPath.resolve(RegistryConstants.ATTR_PASSWORD)
                            .toString());
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
