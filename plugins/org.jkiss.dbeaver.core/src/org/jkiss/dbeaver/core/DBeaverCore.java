/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPExternalFileManager;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.model.data.DBDRegistry;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.impl.app.DefaultCertificateStorage;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMController;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterRegistry;
import org.jkiss.dbeaver.registry.*;
import org.jkiss.dbeaver.registry.datatype.DataTypeProviderRegistry;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.registry.language.PlatformLanguageRegistry;
import org.jkiss.dbeaver.registry.sql.SQLFormatterConfigurationRegistry;
import org.jkiss.dbeaver.runtime.IPluginService;
import org.jkiss.dbeaver.runtime.jobs.KeepAliveJob;
import org.jkiss.dbeaver.runtime.net.GlobalProxySelector;
import org.jkiss.dbeaver.runtime.qm.QMControllerImpl;
import org.jkiss.dbeaver.runtime.qm.QMLogFileWriter;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;
import org.osgi.framework.Bundle;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.ProxySelector;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * DBeaverCore
 */
public class DBeaverCore implements DBPPlatform {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.jkiss.dbeaver.core"; //$NON-NLS-1$
    public static final String APP_CONFIG_FILE = "dbeaver.ini";
    public static final String ECLIPSE_CONFIG_FILE = "eclipse.ini";
    public static final String TEMP_PROJECT_NAME = ".dbeaver-temp"; //$NON-NLS-1$

    private static final Log log = Log.getLog(DBeaverCore.class);

    private static final DBPApplication DEFAULT_APPLICATION = new EclipseApplication();

    static DBeaverCore instance;

    @NotNull
    private static DBPApplication application = DEFAULT_APPLICATION;
    private static volatile boolean isClosing = false;

    private File tempFolder;
    private IWorkspace workspace;
    private DBPPlatformLanguage language;
    private OSDescriptor localSystem;

    private DBNModel navigatorModel;
    private QMControllerImpl queryManager;
    private QMLogFileWriter qmLogWriter;
    private ProjectRegistry projectRegistry;
    private DBACertificateStorage certificateStorage;

    private final List<IPluginService> activatedServices = new ArrayList<>();

    private static boolean disposed = false;

    public static DBeaverCore getInstance()
    {
        if (instance == null) {
            synchronized (DBeaverCore.class) {
                if (disposed) {
                    throw new IllegalStateException("DBeaver core already disposed");
                }
                if (instance == null) {
                    // Initialize DBeaver Core
                    DBeaverCore.createInstance();
                }
            }
        }
        return instance;
    }

    private static DBeaverCore createInstance()
    {
        log.debug("Initializing " + GeneralUtils.getProductTitle());
        if (Platform.getProduct() != null) {
            Bundle definingBundle = Platform.getProduct().getDefiningBundle();
            if (definingBundle != null) {
                log.debug("Host plugin: " + definingBundle.getSymbolicName() + " " + definingBundle.getVersion());
            } else {
                log.debug("!!! No product bundle found");
            }
        }

        try {
            instance = new DBeaverCore();
            instance.initialize();
            return instance;
        } catch (Throwable e) {
            log.error("Error initializing DBeaverCore", e);
            throw new IllegalStateException("Error initializing DBeaverCore", e);
        }
    }

    public static String getCorePluginID()
    {
        return DBeaverActivator.getInstance().getBundle().getSymbolicName();
    }

    public static boolean isStandalone()
    {
        return application.isStandalone();
    }

    public static void setApplication(@NotNull DBPApplication app)
    {
        application = app;
    }

    public static boolean isClosing()
    {
        if (isClosing) {
            return true;
        }
        IWorkbench workbench = PlatformUI.getWorkbench();
        return workbench == null || workbench.isClosing();
    }

    public static void setClosing(boolean closing)
    {
        isClosing = closing;
    }

    public static DBPPreferenceStore getGlobalPreferenceStore()
    {
        return DBeaverActivator.getInstance().getPreferences();
    }

    DBeaverCore()
    {
    }

    private void initialize()
    {
        long startTime = System.currentTimeMillis();
        log.debug("Initialize Core...");

        DBPPreferenceStore prefsStore = getGlobalPreferenceStore();
        //' Global pref events forwarder
        prefsStore.addPropertyChangeListener(new DBPPreferenceListener() {
            @Override
            public void preferenceChange(PreferenceChangeEvent event) {
                // Forward event to all data source preferences
                for (DataSourceDescriptor ds : DataSourceRegistry.getAllDataSources()) {
                    ds.getPreferenceStore().firePropertyChangeEvent(event.getProperty(), event.getOldValue(), event.getNewValue());
                }
            }
        });

        // Register properties adapter
        this.workspace = ResourcesPlugin.getWorkspace();

        this.localSystem = new OSDescriptor(Platform.getOS(), Platform.getOSArch());
        {
            this.language = PlatformLanguageRegistry.getInstance().getLanguage(Locale.getDefault());
            if (this.language == null) {
                log.debug("Language for locale '" + Locale.getDefault() + "' not found. Use default.");
                this.language = PlatformLanguageRegistry.getInstance().getLanguage(Locale.ENGLISH);
            }
        }

        QMUtils.initApplication(this);
        this.queryManager = new QMControllerImpl();

        this.qmLogWriter = new QMLogFileWriter();
        this.queryManager.registerMetaListener(qmLogWriter);

        // Init default network settings
        Authenticator.setDefault(new GlobalProxyAuthenticator());
        ProxySelector.setDefault(new GlobalProxySelector(ProxySelector.getDefault()));

        this.certificateStorage = new DefaultCertificateStorage(
            new File(DBeaverActivator.getInstance().getStateLocation().toFile(), "security"));

        // Init project registry
        this.projectRegistry = new ProjectRegistry(workspace);

        // Projects registry
        initializeProjects();

        // Navigator model
        this.navigatorModel = new DBNModel(this);
        this.navigatorModel.initialize();

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
        new KeepAliveJob(this).scheduleMonitor();

        log.debug("Core initialized (" + (System.currentTimeMillis() - startTime) + "ms)");
    }

    private void initializeProjects()
    {
        final IProgressMonitor monitor = new NullProgressMonitor();
        try {
            projectRegistry.loadProjects(monitor);
        } catch (DBException e) {
            log.error("Error loading projects", e);
        }
    }

    public synchronized void dispose()
    {
        long startTime = System.currentTimeMillis();
        log.debug("Shutdown Core...");

        DBeaverCore.setClosing(true);

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

        // Dispose project registry
        // It will close all open connections
        if (this.projectRegistry != null) {
            this.projectRegistry.dispose();
            this.projectRegistry = null;
        }

        if (this.qmLogWriter != null) {
            this.queryManager.unregisterMetaListener(qmLogWriter);
            this.qmLogWriter.dispose();
            this.qmLogWriter = null;
        }
        if (this.queryManager != null) {
            this.queryManager.dispose();
            //queryManager = null;
        }
        DataSourceProviderRegistry.getInstance().dispose();

        if (isStandalone() && workspace != null) {
            try {
                IProgressMonitor monitor = new NullProgressMonitor();
                workspace.save(true, monitor);
            } catch (CoreException ex) {
                log.error("Can't save workspace", ex); //$NON-NLS-1$
            }
        }

        // Remove temp folder
        if (tempFolder != null) {

            if (!ContentUtils.deleteFileRecursive(tempFolder)) {
                log.warn("Can't delete temp folder '" + tempFolder.getAbsolutePath() + "'");
            }
            tempFolder = null;
        }

        DBeaverCore.application = DEFAULT_APPLICATION;
        DBeaverCore.instance = null;
        DBeaverCore.disposed = true;
        System.gc();
        log.debug("Shutdown completed in " + (System.currentTimeMillis() - startTime) + "ms");
    }

    @NotNull
    @Override
    public IWorkspace getWorkspace()
    {
        return workspace;
    }

    @NotNull
    @Override
    public DBPProjectManager getProjectManager() {
        return getProjectRegistry();
    }

    public OSDescriptor getLocalSystem()
    {
        return localSystem;
    }

    @NotNull
    @Override
    public DBPApplication getApplication() {
        return application;
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

        File configPath;
        try {
            configPath = RuntimeUtils.getLocalFileFromURL(Platform.getInstallLocation().getURL());
        } catch (IOException e) {
            throw new DBException("Can't detect application installation folder.", e);
        }
        File iniFile = new File(configPath, ECLIPSE_CONFIG_FILE);
        if (!iniFile.exists()) {
            iniFile = new File(configPath, APP_CONFIG_FILE);
        }
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
            getGlobalPreferenceStore().setValue(DBeaverPreferences.PLATFORM_LANGUAGE, language.getCode());
        } catch (AccessDeniedException e) {
            throw new DBException("Can't save startup configuration - access denied.\n" +
                "You could try to change national locale manually in '" + iniFile.getAbsolutePath() + "'. Refer to readme.txt file for details.", e);
        } catch (Exception e) {
            throw new DBException("Unexpected error while saving startup configuration", e);
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

/*
    private void changeRuntimeLocale(@NotNull DBPPlatformLanguage runtimeLocale) {
        // Check locale
        ILocaleChangeService localeService = PlatformUI.getWorkbench().getService(ILocaleChangeService.class);
        if (localeService != null) {
            localeService.changeApplicationLocale(runtimeLocale.getCode());
        } else {
            log.warn("Can't resolve locale change service");
        }

        Locale locale = Locale.forLanguageTag(runtimeLocale.getCode());
        if (locale != null) {
            Locale.setDefault(locale);
        }
    }
*/

    @NotNull
    @Override
    public DBNModel getNavigatorModel()
    {
        return navigatorModel;
    }

    @NotNull
    public QMController getQueryManager()
    {
        return queryManager;
    }

    @NotNull
    @Override
    public DBDRegistry getValueHandlerRegistry() {
        return DataTypeProviderRegistry.getInstance();
    }

    @NotNull
    @Override
    public DBERegistry getEditorsRegistry() {
        return EntityEditorsRegistry.getInstance();
    }

    @Override
    public SQLFormatterRegistry getSQLFormatterRegistry() {
        return SQLFormatterConfigurationRegistry.getInstance();
    }

    @NotNull
    @Override
    public DBPPreferenceStore getPreferenceStore() {
        return getGlobalPreferenceStore();
    }

    @NotNull
    @Override
    public DBACertificateStorage getCertificateStorage() {
        return certificateStorage;
    }

    @NotNull
    @Override
    public DBASecureStorage getSecureStorage() {
        return application.getSecureStorage();
    }

    public ProjectRegistry getProjectRegistry()
    {
        return projectRegistry;
    }

    public DBPExternalFileManager getExternalFileManager() {
        return projectRegistry;
    }

    @NotNull
    public File getTempFolder(DBRProgressMonitor monitor, String name) {
        if (tempFolder == null) {
            // Make temp folder
            monitor.subTask("Create temp folder");
            try {
                final java.nio.file.Path tempDirectory = Files.createTempDirectory(TEMP_PROJECT_NAME);
                tempFolder = tempDirectory.toFile();
            } catch (IOException e) {
                final String sysTempFolder = System.getProperty(StandardConstants.ENV_TMP_DIR);
                if (!CommonUtils.isEmpty(sysTempFolder)) {
                    tempFolder = new File(sysTempFolder, TEMP_PROJECT_NAME);
                    if (!tempFolder.mkdirs()){
                        final String sysUserFolder = System.getProperty(StandardConstants.ENV_USER_HOME);
                        if (!CommonUtils.isEmpty(sysUserFolder)) {
                            tempFolder = new File(sysUserFolder, TEMP_PROJECT_NAME);
                            if (!tempFolder.mkdirs()){
                                tempFolder = new File(TEMP_PROJECT_NAME);
                                if (!tempFolder.mkdirs()){
                                    log.error("Can't create temp directory!");
                                }
                            }
                        }

                    }
                }
            }
        }
        return tempFolder;
    }

    @Override
    public boolean isShuttingDown() {
        return isClosing();
    }

    @NotNull
    public List<IProject> getLiveProjects()
    {
        List<IProject> result = new ArrayList<>();
        for (IProject project : workspace.getRoot().getProjects()) {
            if (project.exists() && !project.isHidden()) {
                result.add(project);
            }
        }
        return result;
    }

}
