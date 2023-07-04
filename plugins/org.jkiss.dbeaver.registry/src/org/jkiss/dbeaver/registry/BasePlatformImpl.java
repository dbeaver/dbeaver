/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConfigurationController;
import org.jkiss.dbeaver.model.DBFileController;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.app.DBPApplicationConfigurator;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderRegistry;
import org.jkiss.dbeaver.model.data.DBDRegistry;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.fs.DBFRegistry;
import org.jkiss.dbeaver.model.impl.preferences.AbstractPreferenceStore;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.model.task.DBTTaskController;
import org.jkiss.dbeaver.registry.datatype.DataTypeProviderRegistry;
import org.jkiss.dbeaver.registry.fs.FileSystemProviderRegistry;
import org.jkiss.dbeaver.runtime.IPluginService;
import org.jkiss.dbeaver.runtime.jobs.DataSourceMonitorJob;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BaseWorkspaceImpl.
 *
 * Base implementation of DBeaver platform
 */
public abstract class BasePlatformImpl implements DBPPlatform, DBPApplicationConfigurator {

    private static final Log log = Log.getLog(BasePlatformImpl.class);

    private static final String APP_CONFIG_FILE = "dbeaver.ini";
    private static final String ECLIPSE_CONFIG_FILE = "eclipse.ini";

    public static final String CONFIG_FOLDER = ".config";
    public static final String FILES_FOLDER = ".files";

    protected OSDescriptor localSystem;

    private DBNModel navigatorModel;

    private final List<IPluginService> activatedServices = new ArrayList<>();
    private DBFileController localFileController;
    private DBTTaskController localTaskController;
    
    private DBConfigurationController defaultConfigurationController;
    private final Map<Bundle, DBConfigurationController> configurationControllerByPlugin = new HashMap<>();

    protected void initialize() {
        log.debug("Initialize base platform...");

        DBPPreferenceStore prefsStore = getPreferenceStore();
        // Global pref events forwarder
        prefsStore.addPropertyChangeListener(event -> {
            // Forward event to all data source preferences
            for (DBPDataSourceContainer ds : DataSourceRegistry.getAllDataSources()) {
                ((AbstractPreferenceStore)ds.getPreferenceStore()).firePropertyChangeEvent(prefsStore, event.getProperty(), event.getOldValue(), event.getNewValue());
            }
        });

        this.localSystem = new OSDescriptor(Platform.getOS(), Platform.getOSArch());

        // Navigator model
        this.navigatorModel = new DBNModel(this, null);
        this.navigatorModel.setModelAuthContext(getWorkspace().getAuthContext());
        this.navigatorModel.initialize();

        if (!getApplication().isExclusiveMode()) {
            // Activate plugin services
            for (IPluginService pluginService : PluginServiceRegistry.getInstance().getServices()) {
                try {
                    pluginService.activateService();
                    activatedServices.add(pluginService);
                } catch (Throwable e) {
                    log.error("Error activating plugin service", e);
                }
            }

            // Connections monitoring job
            new DataSourceMonitorJob(this).scheduleMonitor();
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

        // Dispose navigator model first
        // It is a part of UI
        if (this.navigatorModel != null) {
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
    public DBConfigurationController getPluginConfigurationController(@NotNull String pluginId) {
        return getConfigurationController(Platform.getBundle(pluginId));
    }
    
    private DBConfigurationController getConfigurationController(Bundle bundle) {
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
                getWorkspace().getMetadataFolder().resolve(CONFIG_FOLDER)
            );
            controller.setLegacyConfigFolder(getProductPlugin().getStateLocation().toFile().toPath());
            return controller;
        } else {
            return new LocalConfigurationController(
                Platform.getStateLocation(bundle).toFile().toPath()
            );
        }
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
    public Path getLocalConfigurationFile(String fileName) {
        return getProductPlugin().getStateLocation().toFile().toPath().resolve(fileName);
    }

    @NotNull
    @Override
    public DBTTaskController getTaskController() {
        if (localTaskController == null) {
            localTaskController = createTaskController();
        }
        return localTaskController;
    }

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
    public OSDescriptor getLocalSystem() {
        return localSystem;
    }

    @NotNull
    @Override
    public DBNModel getNavigatorModel() {
        return navigatorModel;
    }

    @NotNull
    @Override
    public DBPDataSourceProviderRegistry getDataSourceProviderRegistry() {
        return DataSourceProviderRegistry.getInstance();
    }
}
