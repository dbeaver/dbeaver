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

package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPExternalFileManager;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.model.impl.app.BaseApplicationImpl;
import org.jkiss.dbeaver.model.impl.app.DefaultCertificateStorage;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DesktopNavigatorModel;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMRegistry;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.runtime.features.DBRFeatureRegistry;
import org.jkiss.dbeaver.registry.BasePlatformImpl;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.GlobalEventManagerImpl;
import org.jkiss.dbeaver.runtime.SecurityProviderUtils;
import org.jkiss.dbeaver.runtime.qm.QMLogFileWriter;
import org.jkiss.dbeaver.runtime.qm.QMRegistryImpl;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
 * DesktopPlatform
 */
public class DesktopPlatform extends BasePlatformImpl implements DBPPlatformDesktop {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.jkiss.dbeaver.core"; //$NON-NLS-1$

    private static final String DBEAVER_CONFIG_FOLDER = "settings";

    private static final Log log = Log.getLog(DesktopPlatform.class);

    static DesktopPlatform instance;

    private static volatile boolean isClosing = false;

    private DBPWorkspaceDesktop workspace;
    private QMRegistryImpl queryManager;
    private QMLogFileWriter qmLogWriter;
    private DBACertificateStorage certificateStorage;
    private volatile boolean workbenchStarted;

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

    public DesktopPlatform() {
        instance = this;
    }

    protected void initialize() {
        long startTime = System.currentTimeMillis();
        log.debug("Initialize desktop platform...");

        if (getPreferenceStore().getBoolean(DBeaverPreferences.SECURITY_USE_BOUNCY_CASTLE)) {
            // Register BC security provider
            SecurityProviderUtils.registerSecurityProvider();
        }

        this.certificateStorage = new DefaultCertificateStorage(
            this,
            RuntimeUtils.getPluginStateLocation(DBeaverActivator.getInstance())
                .resolve(DBConstants.CERTIFICATE_STORAGE_FOLDER));

        // Create workspace
        getApplication().beforeWorkspaceInitialization();

        this.workspace = getApplication().createWorkspace(this);

        // Init workspace in UI because it may need some UI interactions to initialize
        this.workspace.initializeProjects();

        QMUtils.initApplication(this);
        this.queryManager = new QMRegistryImpl();

        this.qmLogWriter = new QMLogFileWriter();
        this.queryManager.registerMetaListener(qmLogWriter);

        super.initialize();

        DBRFeatureRegistry.getInstance().startTracking();

        log.debug("Platform initialized (" + (System.currentTimeMillis() - startTime) + "ms)");
    }

    @Override
    protected DBNModel createNavigatorModel() {
        return new DesktopNavigatorModel(this, null);
    }

    public synchronized void dispose() {
        long startTime = System.currentTimeMillis();
        log.debug("Shutdown desktop platform...");

        DesktopPlatform.setClosing(true);
        DBPApplication application = getApplication();
        if (application instanceof DBPApplicationController ac) {
            // Shutdown in headless mode
            ac.setHeadlessMode(true);
        }

        DBRFeatureRegistry.getInstance().endTracking();

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
        DataSourceProviderRegistry.dispose();

        if (isStandalone() && workspace != null && !application.isExclusiveMode()) {
            try {
                workspace.save(new LoggingProgressMonitor(log));
            } catch (DBException ex) {
                log.error("Can not save workspace", ex); //$NON-NLS-1$
            }
        }

        DesktopPlatform.instance = null;
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
    public DBPApplicationDesktop getApplication() {
        return (DBPApplicationDesktop) BaseApplicationImpl.getInstance();
    }

    @NotNull
    public QMRegistry getQueryManager() {
        return queryManager;
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
        return certificateStorage;
    }

    @NotNull
    @Override
    public DBPExternalFileManager getExternalFileManager() {
        return workspace;
    }

    @Override
    public boolean isWorkbenchStarted() {
        // In plugin mode it is always true
        // We don't have any specific security providers which are activated during startup so it is safe
        return workbenchStarted || !getApplication().isStandalone();
    }

    public void setWorkbenchStarted(boolean started) {
        this.workbenchStarted = started;
    }

    @Override
    public boolean isShuttingDown() {
        return isClosing();
    }

}
