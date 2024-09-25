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

package org.jkiss.dbeaver.dpi.app;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.app.DBACertificateStorage;
import org.jkiss.dbeaver.model.impl.app.BaseApplicationImpl;
import org.jkiss.dbeaver.model.impl.app.DefaultCertificateStorage;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMRegistry;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.BasePlatformImpl;
import org.jkiss.dbeaver.runtime.qm.QMRegistryImpl;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * DPIPlatform
 */
public class DPIPlatform extends BasePlatformImpl {

    public static final String PLUGIN_ID = "org.jkiss.dbeaver.dpi.app"; //$NON-NLS-1$

    private static final Log log = Log.getLog(DPIPlatform.class);

    static DPIPlatform instance;

    private static volatile boolean isClosing = false;

    private Path tempFolder;
    private DPIWorkspace workspace;

    private QMRegistryImpl qmController;
    private DefaultCertificateStorage defaultCertificateStorage;

//    public static DPIPlatform getInstance() {
//        return instance;
//    }
//
//    static DPIPlatform createInstance() {
//        log.debug("Initializing " + GeneralUtils.getProductTitle());
//        try {
//            instance = new DPIPlatform();
//            instance.initialize();
//            return instance;
//        } catch (Throwable e) {
//            log.error("Error initializing DPI platform", e);
//            throw new IllegalStateException("Error initializing DPI platform", e);
//        }
//    }

    public static boolean isClosing() {
        return isClosing;
    }

    private static void setClosing(boolean closing) {
        isClosing = closing;
    }

    DPIPlatform() {
    }

    protected void initialize() {
        long startTime = System.currentTimeMillis();
        log.debug("Initialize DPI Platform...");

        try {
            Path installPath = RuntimeUtils.getLocalPathFromURL(Platform.getInstallLocation().getURL());

            this.tempFolder = installPath.resolve("temp");
            this.defaultCertificateStorage = new DefaultCertificateStorage(installPath.resolve(DBConstants.CERTIFICATE_STORAGE_FOLDER));
        } catch (IOException e) {
            log.debug(e);
        }

        // Register properties adapter
        try {
            Path workspacePath = Path.of(Platform.getInstanceLocation().getURL().toURI());
            this.workspace = new DPIWorkspace(this, workspacePath);
            this.workspace.initializeProjects();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize DPI workspace", e);
        }

        QMUtils.initApplication(this);
        this.qmController = new QMRegistryImpl();

        log.debug("DPI Platform initialized (" + (System.currentTimeMillis() - startTime) + "ms)");
    }

    public synchronized void dispose() {
        log.debug("Shutdown DPI...");
        DPIPlatform.setClosing(true);
        super.dispose();
        workspace.dispose();

        // Remove temp folder
        if (tempFolder != null) {
            if (!ContentUtils.deleteFileRecursive(tempFolder)) {
                log.warn("Can't delete temp folder '" + tempFolder + "'");
            }
            tempFolder = null;
        }

        DPIPlatform.instance = null;
    }

    @NotNull
    @Override
    public DPIWorkspace getWorkspace() {
        return workspace;
    }

    @NotNull
    @Override
    public DPIApplication getApplication() {
        return (DPIApplication) BaseApplicationImpl.getInstance();
    }

    @NotNull
    public QMRegistry getQueryManager() {
        return qmController;
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
    public Path getTempFolder(@NotNull DBRProgressMonitor monitor, @NotNull String name) {
        return tempFolder.resolve(name);
    }

    @Override
    protected Plugin getProductPlugin() {
        return DPIActivator.getInstance();
    }

    @Override
    public boolean isShuttingDown() {
        return isClosing();
    }

}
