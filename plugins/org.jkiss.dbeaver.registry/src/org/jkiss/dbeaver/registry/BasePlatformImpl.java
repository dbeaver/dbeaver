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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBConfigurationController;
import org.jkiss.dbeaver.model.DBFileController;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderRegistry;
import org.jkiss.dbeaver.model.data.DBDRegistry;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.fs.DBFRegistry;
import org.jkiss.dbeaver.model.impl.app.BaseApplicationImpl;
import org.jkiss.dbeaver.model.impl.preferences.AbstractPreferenceStore;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.net.DBWHandlerRegistry;
import org.jkiss.dbeaver.model.net.DBWNetworkProfileManager;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadataRegistry;
import org.jkiss.dbeaver.model.task.DBTTaskController;
import org.jkiss.dbeaver.registry.datatype.DataTypeProviderRegistry;
import org.jkiss.dbeaver.registry.formatter.DataFormatterRegistry;
import org.jkiss.dbeaver.registry.fs.FileSystemProviderRegistry;
import org.jkiss.dbeaver.registry.language.PlatformLanguageRegistry;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.dbeaver.registry.settings.GlobalSettings;
import org.jkiss.dbeaver.runtime.IPluginService;
import org.jkiss.dbeaver.runtime.jobs.DataSourceMonitorJob;
import org.jkiss.dbeaver.utils.*;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * BaseWorkspaceImpl.
 * Base implementation of DBeaver platform for all products
 */
public abstract class BasePlatformImpl implements DBPPlatform, DBPApplicationConfigurator, DBPPlatformLanguageManager {

    private static final Log log = Log.getLog(BasePlatformImpl.class);

    private static final String APP_CONFIG_FILE = "dbeaver.ini";
    private static final String ECLIPSE_CONFIG_FILE = "eclipse.ini";
    private static final String TEMP_PROJECT_NAME = ".dbeaver-temp"; //$NON-NLS-1$
    private static final String TEMP_SESSION_PREFIX = "session-"; //$NON-NLS-1$
    private static final String TEMP_SESSION_LOCK = ".lock"; //$NON-NLS-1$
    private static final long TEMP_SESSION_CREATION_GRACE_PERIOD = 60_000;
    private static final String SETTINGS_FOLDER = "settings";

    public static final String CONFIG_FOLDER = ".config";
    public static final String FILES_FOLDER = ".files";

    private static final String DBEAVER_PROP_LANGUAGE = "nl";

    protected OSDescriptor localSystem;

    private DBNModel navigatorModel;
    private final List<IPluginService> activatedServices = new ArrayList<>();
    private DBFileController localFileController;
    private DBTTaskController localTaskController;
    
    private DBConfigurationController defaultConfigurationController;
    private final Map<Bundle, DBConfigurationController> configurationControllerByPlugin = new HashMap<>();

    private SQLDialectMetadataRegistry sqlDialectRegistry;
    private final DBWNetworkProfileManager networkProfileManager;

    private DBPPlatformLanguage platformLanguage;

    protected Path tempFolder;
    private Path tempRootFolder;
    private FileMutex tempFolderLock;

    public BasePlatformImpl() {
        this.networkProfileManager = new GlobalNetworkProfileManager(this);
    }

    protected void initialize() throws DBException {
        log.debug("Initialize base platform...");

        DBPPreferenceStore prefStore = getPreferenceStore();
        // Global pref events forwarder
        prefStore.addPropertyChangeListener(event -> {
            // Forward event to all data source preferences
            for (DBPDataSourceContainer ds : DataSourceRegistry.getAllDataSources()) {
                ((AbstractPreferenceStore)ds.getPreferenceStore()).firePropertyChangeEvent(prefStore, event.getProperty(), event.getOldValue(), event.getNewValue());
            }
        });

        {
            this.platformLanguage = PlatformLanguageRegistry.getInstance().getLanguage(Locale.getDefault());
            if (this.platformLanguage == null) {
                log.debug("Language for locale '" + Locale.getDefault() + "' not found. Use default.");
                this.platformLanguage = PlatformLanguageRegistry.getInstance().getLanguage(Locale.ENGLISH);
            }
        }

        // Navigator model
        this.navigatorModel = createNavigatorModel();
        this.navigatorModel.setModelAuthContext(getWorkspace().getAuthContext());
        this.navigatorModel.initialize();

        DBPApplication application = getApplication();
        if (application.isHeadlessMode()) {
            postInitialize();
        }
    }

    public void postInitialize() {
        if (!getApplication().isExclusiveMode()) {
            // Activate plugin services
            activatePluginServices();

            if (!getApplication().isMultiuser()) {
                // Connections monitoring job
                new DataSourceMonitorJob(this).scheduleMonitor();
            }
        }
    }

    protected DBNModel createNavigatorModel() {
        return new DBNModel(this, null);
    }

    protected void activatePluginServices() {
        for (IPluginService pluginService : PluginServiceRegistry.getInstance().getServices()) {
            try {
                pluginService.activateService();
                activatedServices.add(pluginService);
            } catch (Throwable e) {
                log.error("Error activating plugin service", e);
            }
        }
    }

    public synchronized void dispose() {
        // Deactivate plugin services
        for (IPluginService pluginService : activatedServices) {
            try {
                pluginService.deactivateService();
            } catch (Exception e) {
                log.error("Error deactivating plugin service", e);
            }
        }
        activatedServices.clear();

        // Remove temp folder
        if (tempFolder != null) {
            if (tempFolderLock != null) {
                try {
                    tempFolderLock.close();
                } catch (IOException e) {
                    log.warn("Error releasing temp folder lock", e);
                }
                tempFolderLock = null;
            }
            if (!ContentUtils.deleteFileRecursive(tempFolder)) {
                log.warn("Can not delete temp folder '" + tempFolder + "'");
            }
            tempFolder = null;
            if (tempRootFolder != null) {
                try {
                    Files.deleteIfExists(tempRootFolder);
                } catch (DirectoryNotEmptyException ignored) {
                    // Another DBeaver instance is using the shared root.
                } catch (IOException e) {
                    log.debug("Can not delete temp root folder '" + tempRootFolder + "'", e);
                }
                tempRootFolder = null;
            }
        }
        // Dispose navigator model first
        // It is a part of UI
        disposeNavigatorModel();
    }

    public void disposeNavigatorModel() {
        if (this.navigatorModel != null && this.navigatorModel.getRoot() != null) {
            log.debug("Dispose navigator model");
            this.navigatorModel.dispose();
            //this.navigatorModel = null;
        }
    }

    @NotNull
    @Override
    public DBDRegistry getValueHandlerRegistry() {
        return DataTypeProviderRegistry.getInstance();
    }

    @NotNull
    @Override
    public DBERegistry getEditorsRegistry() {
        return ObjectManagerRegistry.getInstance();
    }

    @NotNull
    @Override
    public DBFRegistry getFileSystemRegistry() {
        return FileSystemProviderRegistry.getInstance();
    }

    @NotNull
    @Override
    public SQLDialectMetadataRegistry getSQLDialectRegistry() {
        if (sqlDialectRegistry == null) {
            BundleServiceRef<SQLDialectMetadataRegistry> registryRef = RuntimeUtils.getBundleService(
                SQLDialectMetadataRegistry.class,
                true
            );
            sqlDialectRegistry = registryRef.service();
            if (sqlDialectRegistry == null) {
                throw new IllegalStateException("Cannot determine SQL dialect registry for " + getClass());
            }
            registryRef.initializeService();
        }
        return sqlDialectRegistry;
    }

    @NotNull
    @Override
    public DBWHandlerRegistry getNetworkHandlerRegistry() {
        return NetworkHandlerRegistry.getInstance();
    }

    @NotNull
    @Override
    public DBWNetworkProfileManager getNetworkProfiles() {
        return networkProfileManager;
    }

    @NotNull
    @Override
    public DBConfigurationController getConfigurationController() {
        return getPluginConfigurationController(null);
    }
    
    @NotNull
    @Override
    public DBConfigurationController getProductConfigurationController() {
        return getConfigurationController(getProductPlugin().getBundle());
    }
    
    @NotNull
    @Override
    public DBConfigurationController getPluginConfigurationController(@Nullable String pluginId) {
        return getConfigurationController(CommonUtils.isEmpty(pluginId) ? null : Platform.getBundle(pluginId));
    }
    
    private DBConfigurationController getConfigurationController(@Nullable Bundle bundle) {
        DBConfigurationController controller = bundle == null ? defaultConfigurationController : configurationControllerByPlugin.get(bundle);
        if (controller == null) {
            controller = createConfigurationController(bundle);
            if (bundle == null) {
                defaultConfigurationController = controller;
            } else {
                configurationControllerByPlugin.put(bundle, controller);
            }
        }
        return controller;
    }

    @NotNull
    @Override
    public DBConfigurationController createConfigurationController(@Nullable String pluginId) {
        return createConfigurationController(pluginId == null ? null : Platform.getBundle(pluginId));
    }

    @NotNull
    private DBConfigurationController createConfigurationController(@Nullable Bundle bundle) {
        DBPApplication application = getApplication();
        if (application instanceof DBPApplicationConfigurator) {
            String pluginBundleName = bundle == null ? null : bundle.getSymbolicName();
            return ((DBPApplicationConfigurator) application).createConfigurationController(pluginBundleName);
        } else if (bundle == null) {
            LocalConfigurationController controller = new LocalConfigurationController(
                getLocalWorkspaceConfigFolder()
            );
            Plugin productPlugin = getProductPlugin();
            if (productPlugin != null) {
                Path pluginStateLocation = RuntimeUtils.getPluginStateLocation(productPlugin);
                if (Files.exists(pluginStateLocation)) {
                    controller.setLegacyConfigFolder(pluginStateLocation);
                }
            }
            return controller;
        } else {
            return new LocalConfigurationController(
                Platform.getStateLocation(bundle).toFile().toPath()
            );
        }
    }

    private @NotNull Path getLocalWorkspaceConfigFolder() {
        return getWorkspace().getMetadataFolder().resolve(CONFIG_FOLDER);
    }

    @NotNull
    @Override
    public DBFileController getFileController() {
        if (localFileController == null) {
            localFileController = createFileController();
        }
        return localFileController;
    }

    @Override
    @NotNull
    public DBFileController createFileController() {
        DBPApplication application = getApplication();
        if (application instanceof DBPApplicationConfigurator) {
            return ((DBPApplicationConfigurator) application).createFileController();
        }

        return new LocalFileController(
            getWorkspace().getMetadataFolder().resolve(FILES_FOLDER)
        );
    }

    @NotNull
    @Override
    public Path getLocalConfigurationFile(@NotNull String fileName) {
        Path productPluginPath = RuntimeUtils.getPluginStateLocation(getProductPlugin()).resolve(fileName);
        if (Files.exists(productPluginPath)) {
            return productPluginPath;
        }
        return getLocalWorkspaceConfigFolder().resolve(fileName);
    }

    @NotNull
    @Override
    public Path getGlobalConfigurationFile(@NotNull String fileName) {
        var root = RuntimeUtils.getWorkingDirectory(BaseApplicationImpl.DBEAVER_DATA_DIR);
        return Path.of(root, SETTINGS_FOLDER, fileName);
    }

    @NotNull
    @Override
    public DBTTaskController getTaskController() {
        if (localTaskController == null) {
            localTaskController = createTaskController();
        }
        return localTaskController;
    }

    @NotNull
    @Override
    public DBTTaskController createTaskController() {
        DBPApplication application = getApplication();
        if (application instanceof DBPApplicationConfigurator) {
            return ((DBPApplicationConfigurator) application).createTaskController();
        } else {
            return new LocalTaskController();
        }
    }

    protected abstract Plugin getProductPlugin();
    
    @NotNull
    @Override
    public Path getApplicationConfiguration() {
        Path configPath;
        try {
            configPath = RuntimeUtils.getLocalPathFromURL(Platform.getInstallLocation().getURL());
        } catch (IOException e) {
            throw new IllegalStateException("Can't detect application installation folder.", e);
        }
        Path iniFile = configPath.resolve(ECLIPSE_CONFIG_FILE);
        if (!Files.exists(iniFile)) {
            iniFile = configPath.resolve(APP_CONFIG_FILE);
        }
        return iniFile;
    }

    @NotNull
    @Override
    public DBPDataFormatterRegistry getDataFormatterRegistry() {
        return DataFormatterRegistry.getInstance();
    }

    @NotNull
    @Override
    public OSDescriptor getLocalSystem() {
        if (this.localSystem == null) {
            this.localSystem = new OSDescriptor(Platform.getOS(), Platform.getOSArch());
        }
        return this.localSystem;
    }

    @NotNull
    @Override
    public DBNModel getNavigatorModel() {
        return navigatorModel;
    }

    @NotNull
    @Override
    public String getDeploymentId() {
        return DeploymentId.get();
    }

    @NotNull
    @Override
    public DBPDataSourceProviderRegistry getDataSourceProviderRegistry() {
        return DataSourceProviderRegistry.getInstance();
    }

    @NotNull
    @Override
    public DBPPlatformLanguage getPlatformLanguage() {
        return platformLanguage;
    }

    @Override
    public void setPlatformLanguage(@NotNull DBPPlatformLanguage language) {
        if (CommonUtils.equalObjects(language, this.platformLanguage)) {
            return;
        }

        GlobalSettings.getInstance().setGlobalProperty(DBEAVER_PROP_LANGUAGE, language.getCode());
        this.platformLanguage = language;
        // This property is fake. But we set it to trigger property change listener
        // which will ask to restart workbench.
        getPreferenceStore().setValue(ModelPreferences.PLATFORM_LANGUAGE, language.getCode());
    }

    @NotNull
    public synchronized Path getTempFolder(@NotNull DBRProgressMonitor monitor, @NotNull String name) throws IOException {
        if (tempFolder == null) {
            initializeTempFolder();
        }
        Path localTemp = tempFolder.resolve(name);
        Files.createDirectories(localTemp);
        return localTemp;
    }

    private void initializeTempFolder() throws IOException {
        var candidates = new LinkedHashSet<Path>();
        String configuredPath = System.getProperty("dbeaver.io.tmpdir");
        if (!CommonUtils.isEmpty(configuredPath)) {
            candidates.add(Paths.get(GeneralUtils.replaceVariables(configuredPath, new SystemVariablesResolver())));
        }
        String systemTempPath = System.getProperty(StandardConstants.ENV_TMP_DIR);
        if (!CommonUtils.isEmpty(systemTempPath)) {
            candidates.add(Paths.get(systemTempPath));
        }
        String userHomePath = System.getProperty(StandardConstants.ENV_USER_HOME);
        if (!CommonUtils.isEmpty(userHomePath)) {
            candidates.add(Paths.get(userHomePath));
        }
        candidates.add(Path.of("."));

        IOException failure = null;
        for (Path candidate : candidates) {
            try {
                Path root = candidate.resolve(TEMP_PROJECT_NAME).toAbsolutePath().normalize();
                Files.createDirectories(root);

                Path session = Files.createTempDirectory(root, TEMP_SESSION_PREFIX);
                try {
                tempFolderLock = FileMutex.tryLock(session.resolve(TEMP_SESSION_LOCK));
                } catch (IOException e) {
                    ContentUtils.deleteFileRecursive(session);
                    throw e;
                }
                tempRootFolder = root;
                tempFolder = session;
                scheduleAbandonedTempFoldersCleanup(root, session);
                return;
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        throw new IOException("Can't create DBeaver temp folder", failure);
    }

    private static void scheduleAbandonedTempFoldersCleanup(@NotNull Path root, @NotNull Path currentSession) {
        var cleanupJob = new AbstractJob("Clean abandoned DBeaver temp folders") {
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                cleanupAbandonedTempFolders(root, currentSession);
                return Status.OK_STATUS;
            }
        };
        cleanupJob.setSystem(true);
        cleanupJob.schedule();
    }

    private static void cleanupAbandonedTempFolders(@NotNull Path root, @NotNull Path currentSession) {
        try (var children = Files.list(root)) {
            children.filter(child -> Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS) &&
                !child.equals(currentSession) &&
                child.getFileName().toString().startsWith(TEMP_SESSION_PREFIX)
            ).forEach(child -> {
                Path lockFile = child.resolve(TEMP_SESSION_LOCK);
                if (Files.exists(lockFile)) {
                    try (FileMutex ignored = FileMutex.tryLock(lockFile)) {
                        // The lock is available, so the process that owned this session is no longer running.
                    } catch (IOException e) {
                        return;
                    }
                } else {
                    try {
                        if (Files.getLastModifiedTime(child).toMillis() + TEMP_SESSION_CREATION_GRACE_PERIOD >=
                            System.currentTimeMillis()) {
                            return;
                        }
                    } catch (IOException e) {
                        return;
                    }
                }
                if (!ContentUtils.deleteFileRecursive(child)) {
                    log.warn("Can not delete abandoned temp folder '" + child + "'");
                }
            });
        } catch (IOException e) {
            log.warn("Error cleaning abandoned temp folders in '" + root + "'", e);
        }
    }

}
