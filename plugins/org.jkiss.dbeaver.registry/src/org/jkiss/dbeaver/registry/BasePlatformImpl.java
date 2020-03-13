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

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataFormatterRegistry;
import org.jkiss.dbeaver.model.app.DBPGlobalEventManager;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPPlatformLanguage;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderRegistry;
import org.jkiss.dbeaver.model.data.DBDRegistry;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.registry.datatype.DataTypeProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.formatter.DataFormatterRegistry;
import org.jkiss.dbeaver.registry.language.PlatformLanguageRegistry;
import org.jkiss.dbeaver.runtime.IPluginService;
import org.jkiss.dbeaver.runtime.jobs.KeepAliveListenerJob;
import org.jkiss.dbeaver.runtime.net.GlobalProxySelector;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.net.ProxySelector;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * BaseWorkspaceImpl.
 *
 * Base implementation of DBeaver platform
 */
public abstract class BasePlatformImpl implements DBPPlatform {

    private static final Log log = Log.getLog(BasePlatformImpl.class);

    private static final String APP_CONFIG_FILE = "dbeaver.ini";
    private static final String ECLIPSE_CONFIG_FILE = "eclipse.ini";

    private DBPPlatformLanguage language;
    private OSDescriptor localSystem;

    private DBNModel navigatorModel;

    private final List<IPluginService> activatedServices = new ArrayList<>();

    protected void initialize() {
        log.debug("Initialize base platform...");

        DBPPreferenceStore prefsStore = getPreferenceStore();
        //' Global pref events forwarder
        prefsStore.addPropertyChangeListener(event -> {
            // Forward event to all data source preferences
            for (DBPDataSourceContainer ds : DataSourceRegistry.getAllDataSources()) {
                ds.getPreferenceStore().firePropertyChangeEvent(event.getProperty(), event.getOldValue(), event.getNewValue());
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
        this.navigatorModel = new DBNModel(this, true);
        this.navigatorModel.initialize();

        // Activate proxy service
        activateProxyService();

        // Activate plugin services
        for (IPluginService pluginService : PluginServiceRegistry.getInstance().getServices()) {
            try {
                pluginService.activateService();
                activatedServices.add(pluginService);
            } catch (Throwable e) {
                log.error("Error activating plugin service", e);
            }
        }

        // Keep-alive job
        new KeepAliveListenerJob(this).scheduleMonitor();
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

    protected void installProxySelector() {
        // Init default network settings
        ProxySelector defProxySelector = GeneralUtils.adapt(this, ProxySelector.class);
        if (defProxySelector == null) {
            defProxySelector = new GlobalProxySelector(ProxySelector.getDefault());
        }
        ProxySelector.setDefault(defProxySelector);
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

    public void setPlatformLanguage(@NotNull DBPPlatformLanguage language) throws DBException {
        if (CommonUtils.equalObjects(language, this.language)) {
            return;
        }

        File iniFile = getApplicationConfiguration();
        if (!iniFile.exists()) {
            throw new DBException("Application configuration file (" + iniFile.getAbsolutePath() + ") not found. Default language cannot be changed.");
        }
        try {
            List<String> configLines = Files.readAllLines(iniFile.toPath());
            setConfigNLS(configLines, language.getCode());
            Files.write(iniFile.toPath(), configLines, StandardOpenOption.WRITE);

            this.language = language;
            // This property is fake. But we set it to trigger property change listener
            // which will ask to restart workbench.
            getPreferenceStore().setValue(ModelPreferences.PLATFORM_LANGUAGE, language.getCode());
        } catch (AccessDeniedException e) {
            throw new DBException("Can't save startup configuration - access denied.\n" +
                "You could try to change national locale manually in '" + iniFile.getAbsolutePath() + "'. Refer to readme.txt file for details.", e);
        } catch (Exception e) {
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
    public File getCustomDriversHome() {
        return DriverDescriptor.getCustomDriversHome();
    }

    private void activateProxyService() {
        try {
            log.debug("Proxy service '" + IProxyService.class.getName() + "' loaded");
        } catch (Throwable e) {
            log.debug("Proxy service not found");
        }
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
