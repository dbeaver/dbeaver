/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.eclipse.core.internal.registry.IRegistryConstants;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderRegistry;
import org.jkiss.dbeaver.model.data.DBDRegistry;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.fs.DBFRegistry;
import org.jkiss.dbeaver.model.impl.preferences.AbstractPreferenceStore;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.registry.datatype.DataTypeProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.formatter.DataFormatterRegistry;
import org.jkiss.dbeaver.registry.fs.FileSystemProviderRegistry;
import org.jkiss.dbeaver.registry.language.PlatformLanguageRegistry;
import org.jkiss.dbeaver.runtime.IPluginService;
import org.jkiss.dbeaver.runtime.jobs.DataSourceMonitorJob;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * BaseWorkspaceImpl.
 *
 * Base implementation of DBeaver platform
 */
public abstract class BasePlatformImpl implements DBPPlatform, DBPPlatformLanguageManager {

    private static final Log log = Log.getLog(BasePlatformImpl.class);

    private static final String APP_CONFIG_FILE = "dbeaver.ini";
    private static final String ECLIPSE_CONFIG_FILE = "eclipse.ini";
    private static final String CONFIG_FILE = "config.ini";

    private DBPPlatformLanguage language;
    private OSDescriptor localSystem;

    private DBNModel navigatorModel;

    private final List<IPluginService> activatedServices = new ArrayList<>();

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
        {
            this.language = PlatformLanguageRegistry.getInstance().getLanguage(Locale.getDefault());
            if (this.language == null) {
                log.debug("Language for locale '" + Locale.getDefault() + "' not found. Use default.");
                this.language = PlatformLanguageRegistry.getInstance().getLanguage(Locale.ENGLISH);
            }
        }

        // Navigator model
        this.navigatorModel = new DBNModel(this, null);
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

    @Override
    public DBPGlobalEventManager getGlobalEventManager() {
        return GlobalEventManagerImpl.getInstance();
    }

    @NotNull
    @Override
    public DBPDataFormatterRegistry getDataFormatterRegistry() {
        return DataFormatterRegistry.getInstance();
    }

    @NotNull
    @Override
    public File getApplicationConfiguration() {
        File configPath;
        try {
            configPath = RuntimeUtils.getLocalFileFromURL(Platform.getInstallLocation().getURL());
        } catch (IOException e) {
            throw new IllegalStateException("Can't detect application installation folder.", e);
        }
        File iniFile = new File(configPath, ECLIPSE_CONFIG_FILE);
        if (!iniFile.exists()) {
            iniFile = new File(configPath, APP_CONFIG_FILE);
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
    public DBPPlatformLanguage getLanguage() {
        return language;
    }

    @Override
    public boolean isLanguageChangeEnabled() {
        return true;
    }

    @Override
    public void setPlatformLanguage(@NotNull DBPPlatformLanguage language) throws DBException {
        if (CommonUtils.equalObjects(language, this.language)) {
            return;
        }

        try {
            final File config = new File(RuntimeUtils.getLocalFileFromURL(Platform.getConfigurationLocation().getURL()), CONFIG_FILE);
            final Properties properties = new Properties();

            if (config.exists()) {
                try (FileInputStream is = new FileInputStream(config)) {
                    properties.load(is);
                }
            }

            properties.put(IRegistryConstants.PROP_NL, language.getCode());

            try (FileOutputStream os = new FileOutputStream(config)) {
                properties.store(os, null);
            }

            this.language = language;
            // This property is fake. But we set it to trigger property change listener
            // which will ask to restart workbench.
            getPreferenceStore().setValue(ModelPreferences.PLATFORM_LANGUAGE, language.getCode());
        } catch (IOException e) {
            throw new DBException("Unexpected error while saving startup configuration", e);
        }
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

    @NotNull
    @Override
    public Path getCustomDriversHome() {
        return DriverDescriptor.getCustomDriversHome();
    }

    @Override
    public boolean isReadOnly() {
        return Platform.getInstanceLocation().isReadOnly();
    }

    // Patch config and add/update -nl parameter
    private void setConfigNLS(List<String> lines, String nl) {
        int vmArgsPos = -1, nlPos = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.equalsIgnoreCase("-nl")) {
                nlPos = i;
            } else if (line.equalsIgnoreCase("-vmargs")) {
                vmArgsPos = i;
                // Do not check the rest - they are VM args anyway
                break;
            }
        }
        if (nlPos >= 0 && lines.size() > nlPos + 1) {
            // Just change existing nl
            lines.set(nlPos + 1, nl);
        } else if (vmArgsPos >= 0) {
            // There is no nl but there are vmargs. Insert before them
            lines.add(vmArgsPos, nl);
            lines.add(vmArgsPos, "-nl");
        } else {
            lines.add("-nl");
            lines.add(nl);
        }
    }

}
