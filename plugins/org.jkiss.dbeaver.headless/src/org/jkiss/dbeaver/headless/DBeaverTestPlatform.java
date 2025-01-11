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

package org.jkiss.dbeaver.headless;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Plugin;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPExternalFileManager;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.model.impl.app.BaseApplicationImpl;
import org.jkiss.dbeaver.model.impl.app.DefaultCertificateStorage;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMRegistry;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.BasePlatformImpl;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.GlobalEventManagerImpl;
import org.jkiss.dbeaver.registry.language.PlatformLanguageRegistry;
import org.jkiss.dbeaver.runtime.qm.QMRegistryImpl;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * DBeaverTestPlatform
 */
public class DBeaverTestPlatform extends BasePlatformImpl implements DBPPlatformDesktop {

    public static final String PLUGIN_ID = "org.jkiss.dbeaver.headless"; //$NON-NLS-1$
    private static final String TEMP_PROJECT_NAME = ".dbeaver-temp"; //$NON-NLS-1$

    private static final Log log = Log.getLog(DBeaverTestPlatform.class);

    static DBeaverTestPlatform instance;

    private static volatile boolean isClosing = false;

    private File tempFolder;
    private DBeaverTestWorkspace workspace;

    private static boolean disposed = false;
    private QMRegistryImpl qmController;
    private DefaultCertificateStorage defaultCertificateStorage;

    public static String getCorePluginID() {
        return PLUGIN_ID;
    }

    public static boolean isStandalone() {
        return BaseApplicationImpl.getInstance().isStandalone();
    }

    public static boolean isClosing() {
        return isClosing;
    }

    private static void setClosing(boolean closing) {
        isClosing = closing;
    }

    DBeaverTestPlatform() {
    }

    protected void initialize() {
        long startTime = System.currentTimeMillis();
        log.debug("Initialize Test Platform...");

        this.defaultCertificateStorage = new DefaultCertificateStorage(
            DBeaverTestActivator.getConfigurationFile(DBConstants.CERTIFICATE_STORAGE_FOLDER).toPath());

        // Register properties adapter
        this.workspace = new DBeaverTestWorkspace(this, ResourcesPlugin.getWorkspace());
        this.workspace.initializeProjects();

        QMUtils.initApplication(this);
        this.qmController = new QMRegistryImpl();

        super.initialize();

        log.debug("Test Platform initialized (" + (System.currentTimeMillis() - startTime) + "ms)");
    }

    public synchronized void dispose() {
        long startTime = System.currentTimeMillis();
        log.debug("Shutdown Core...");

        DBeaverTestPlatform.setClosing(true);

        super.dispose();

        workspace.dispose();

        DataSourceProviderRegistry.dispose();

        // Remove temp folder
        if (tempFolder != null) {

            if (!ContentUtils.deleteFileRecursive(tempFolder)) {
                log.warn("Can't delete temp folder '" + tempFolder.getAbsolutePath() + "'");
            }
            tempFolder = null;
        }

        DBeaverTestPlatform.instance = null;
        DBeaverTestPlatform.disposed = true;
        System.gc();
        log.debug("Test platform shutdown completed in " + (System.currentTimeMillis() - startTime) + "ms");
    }

    @NotNull
    @Override
    public DBPWorkspaceDesktop getWorkspace() {
        return workspace;
    }

    @NotNull
    @Override
    public DBPPlatformLanguage getLanguage() {
        return PlatformLanguageRegistry.getInstance().getLanguage(Locale.ENGLISH);
    }

    @NotNull
    @Override
    public DBeaverHeadlessApplication getApplication() {
        return (DBeaverHeadlessApplication) BaseApplicationImpl.getInstance();
    }

    @NotNull
    public QMRegistry getQueryManager() {
        return qmController;
    }

    @NotNull
    @Override
    public DBPGlobalEventManager getGlobalEventManager() {
        return GlobalEventManagerImpl.getInstance();
    }

    @NotNull
    @Override
    public DBPPreferenceStore getPreferenceStore() {
        return getApplication().getPreferenceStore();
    }

    @NotNull
    @Override
    public DBACertificateStorage getCertificateStorage() {
        return defaultCertificateStorage;
    }

    @NotNull
    @Override
    public DBPExternalFileManager getExternalFileManager() {
        return workspace;
    }

    @NotNull
    public Path getTempFolder(@NotNull DBRProgressMonitor monitor, @NotNull String name) {
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
                    if (!tempFolder.mkdirs()) {
                        final String sysUserFolder = System.getProperty(StandardConstants.ENV_USER_HOME);
                        if (!CommonUtils.isEmpty(sysUserFolder)) {
                            tempFolder = new File(sysUserFolder, TEMP_PROJECT_NAME);
                            if (!tempFolder.mkdirs()) {
                                tempFolder = new File(TEMP_PROJECT_NAME);
                            }
                        }

                    }
                }
            }
        }
        if (!tempFolder.exists() && !tempFolder.mkdirs()) {
            log.error("Can't create temp directory " + tempFolder.getAbsolutePath());
        }
        return tempFolder.toPath();
    }

    @Override
    protected Plugin getProductPlugin() {
        return DBeaverTestActivator.getInstance();
    }

    @Override
    public boolean isShuttingDown() {
        return isClosing();
    }

    @Override
    public boolean isUnitTestMode() {
        return true;
    }
}
