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

package org.jkiss.dbeaver.core;

import org.eclipse.core.internal.registry.IRegistryConstants;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPExternalFileManager;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.model.impl.app.DefaultCertificateStorage;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMRegistry;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.registry.*;
import org.jkiss.dbeaver.registry.formatter.DataFormatterRegistry;
import org.jkiss.dbeaver.registry.language.PlatformLanguageRegistry;
import org.jkiss.dbeaver.runtime.SecurityProviderUtils;
import org.jkiss.dbeaver.runtime.qm.QMLogFileWriter;
import org.jkiss.dbeaver.runtime.qm.QMRegistryImpl;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;
import org.osgi.framework.Bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;

/**
 * DesktopPlatform
 */
public class DesktopPlatform extends BasePlatformImpl implements DBPPlatformDesktop, DBPPlatformLanguageManager {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.jkiss.dbeaver.core"; //$NON-NLS-1$

    private static final String TEMP_PROJECT_NAME = ".dbeaver-temp"; //$NON-NLS-1$
    private static final String OSGI_CONFIG_FILE = "config.ini";

    private static final Log log = Log.getLog(DesktopPlatform.class);

    static DesktopPlatform instance;

    private static volatile boolean isClosing = false;

    private Path tempFolder;
    private DesktopWorkspaceImpl workspace;
    private QMRegistryImpl queryManager;
    private QMLogFileWriter qmLogWriter;
    private DBACertificateStorage certificateStorage;
    private DBPPlatformLanguage language;

    private static boolean disposed = false;

    public static DesktopPlatform getInstance() {
        if (instance == null) {
            synchronized (DesktopPlatform.class) {
                if (disposed) {
                    throw new IllegalStateException("DBeaver core already disposed");
                }
                if (instance == null) {
                    // Initialize DBeaver Core
                    DesktopPlatform.createInstance();
                }
            }
        }
        return instance;
    }

    private static DesktopPlatform createInstance() {
        log.debug("Initializing " + GeneralUtils.getProductTitle());
        if (Platform.getProduct() != null) {
            Bundle definingBundle = Platform.getProduct().getDefiningBundle();
            if (definingBundle != null) {
                log.debug("Host plugin: " + definingBundle.getSymbolicName() + " " + definingBundle.getVersion());
            } else {
                log.debug("No product bundle found");
            }
        }

        try {
            instance = new DesktopPlatform();
            instance.initialize();
            return instance;
        } catch (Throwable e) {
            log.error("Error initializing desktop platform", e);
            throw new IllegalStateException("Error initializing desktop platform", e);
        }
    }

    public static String getCorePluginID() {
        return DBeaverActivator.getInstance().getBundle().getSymbolicName();
    }

    public static boolean isStandalone() {
        return BaseApplicationImpl.getInstance().isStandalone();
    }

    public static boolean isClosing() {
        if (isClosing) {
            return true;
        }
        if (!PlatformUI.isWorkbenchRunning()) {
            return false;
        }
        return false;
    }

    static void setClosing(boolean closing) {
        isClosing = closing;
    }

    public static DBPPreferenceStore getGlobalPreferenceStore() {
        return DBeaverActivator.getInstance().getPreferences();
    }

    private DesktopPlatform() {
    }

    protected void initialize() {
        long startTime = System.currentTimeMillis();
        log.debug("Initialize desktop platform...");

        {
            this.language = PlatformLanguageRegistry.getInstance().getLanguage(Locale.getDefault());
            if (this.language == null) {
                log.debug("Language for locale '" + Locale.getDefault() + "' not found. Use default.");
                this.language = PlatformLanguageRegistry.getInstance().getLanguage(Locale.ENGLISH);
            }
        }

        if (getPreferenceStore().getBoolean(DBeaverPreferences.SECURITY_USE_BOUNCY_CASTLE)) {
            // Register BC security provider
            SecurityProviderUtils.registerSecurityProvider();
        }

        this.certificateStorage = new DefaultCertificateStorage(
            DBeaverActivator.getInstance().getStateLocation().toFile().toPath().resolve("security"));

        // Create workspace
        getApplication().beforeWorkspaceInitialization();
        this.workspace = (DesktopWorkspaceImpl) getApplication().createWorkspace(this, ResourcesPlugin.getWorkspace());
        // Init workspace in UI because it may need some UI interactions to initialize
        this.workspace.initializeProjects();

        QMUtils.initApplication(this);
        this.queryManager = new QMRegistryImpl();

        this.qmLogWriter = new QMLogFileWriter();
        this.queryManager.registerMetaListener(qmLogWriter);

        super.initialize();

        log.debug("Platform initialized (" + (System.currentTimeMillis() - startTime) + "ms)");
    }

    public synchronized void dispose() {
        long startTime = System.currentTimeMillis();
        log.debug("Shutdown desktop platform...");

        DesktopPlatform.setClosing(true);
        DBPApplication application = getApplication();
        if (application instanceof DBPApplicationController) {
            // Shutdown in headless mode
            ((DBPApplicationController) application).setHeadlessMode(true);
        }

        super.dispose();

        if (workspace != null) {
            workspace.dispose();
            workspace = null;
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

        if (isStandalone() && workspace != null && !application.isExclusiveMode()) {
            try {
                workspace.save(new LoggingProgressMonitor(log));
            } catch (DBException ex) {
                log.error("Can not save workspace", ex); //$NON-NLS-1$
            }
        }

        // Remove temp folder
        if (tempFolder != null) {
            if (!ContentUtils.deleteFileRecursive(tempFolder)) {
                log.warn("Can not delete temp folder '" + tempFolder + "'");
            }
            tempFolder = null;
        }

        DesktopPlatform.instance = null;
        DesktopPlatform.disposed = true;
        System.gc();
        log.debug("Platform shutdown completed (" + (System.currentTimeMillis() - startTime) + "ms)");
        // Just in case do System.eis after pause
        new Thread(() -> {
            RuntimeUtils.pause(10000);
            System.out.println("App shutdown was halted. Force system shutdown!");
            System.exit(-2);
        }).start();
    }

    @Override
    protected Plugin getProductPlugin() {
        return DBeaverActivator.getInstance();
    }

    @NotNull
    @Override
    public DBPWorkspaceDesktop getWorkspace() {
        return workspace;
    }

    @NotNull
    @Override
    public DBPApplication getApplication() {
        return BaseApplicationImpl.getInstance();
    }

    @NotNull
    @Override
    public DBPPlatformLanguage getLanguage() {
        return language;
    }

    @Override
    public void setPlatformLanguage(@NotNull DBPPlatformLanguage language) throws DBException {
        if (CommonUtils.equalObjects(language, this.language)) {
            return;
        }

        try {
            final File config = new File(RuntimeUtils.getLocalFileFromURL(Platform.getConfigurationLocation().getURL()), OSGI_CONFIG_FILE);
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
    public QMRegistry getQueryManager() {
        return queryManager;
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
    public DBPPreferenceStore getPreferenceStore() {
        return DBeaverActivator.getInstance().getPreferences();
    }

    @NotNull
    @Override
    public DBACertificateStorage getCertificateStorage() {
        return certificateStorage;
    }

    @NotNull
    @Override
    public DBPExternalFileManager getExternalFileManager() {
        return workspace;
    }

    @NotNull
    public Path getTempFolder(DBRProgressMonitor monitor, String name) {
        if (tempFolder == null) {
            // Make temp folder
            monitor.subTask("Create temp folder");
            try {
                String tempFolderPath = System.getProperty("dbeaver.io.tmpdir");
                if (!CommonUtils.isEmpty(tempFolderPath)) {
                    tempFolderPath = GeneralUtils.replaceVariables(tempFolderPath, new SystemVariablesResolver());

                    File dbTempFolder = new File(tempFolderPath);
                    if (!dbTempFolder.mkdirs()) {
                        throw new IOException("Can't create temp directory '" + dbTempFolder.getAbsolutePath() + "'");
                    }
                } else {
                    tempFolderPath = System.getProperty(StandardConstants.ENV_TMP_DIR);
                }
                tempFolder = Files.createTempDirectory(
                    Paths.get(tempFolderPath),
                    TEMP_PROJECT_NAME);
            } catch (IOException e) {
                final String sysTempFolder = System.getProperty(StandardConstants.ENV_TMP_DIR);
                if (!CommonUtils.isEmpty(sysTempFolder)) {
                    tempFolder = Path.of(sysTempFolder).resolve(TEMP_PROJECT_NAME);
                    if (!Files.exists(tempFolder)) {
                        try {
                            Files.createDirectories(tempFolder);
                        } catch (IOException ex) {
                            final String sysUserFolder = System.getProperty(StandardConstants.ENV_USER_HOME);
                            if (!CommonUtils.isEmpty(sysUserFolder)) {
                                tempFolder = Path.of(sysUserFolder).resolve(TEMP_PROJECT_NAME);
                                if (!Files.exists(tempFolder)) {
                                    try {
                                        Files.createDirectories(tempFolder);
                                    } catch (IOException exc) {
                                        tempFolder = Path.of(TEMP_PROJECT_NAME);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Path localTemp = name == null ? tempFolder : tempFolder.resolve(name);
        if (!Files.exists(localTemp)) {
            try {
                Files.createDirectories(localTemp);
            } catch (IOException e) {
                log.error("Can't create temp directory " + localTemp, e);
            }
        }
        return localTemp;
    }

    @Override
    public boolean isShuttingDown() {
        return isClosing();
    }

}
