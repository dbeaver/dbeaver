/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPApplication;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.DBPProjectManager;
import org.jkiss.dbeaver.model.data.DBDValueHandlerRegistry;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.qm.QMController;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataTypeProviderRegistry;
import org.jkiss.dbeaver.registry.PluginServiceRegistry;
import org.jkiss.dbeaver.registry.ProjectRegistry;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.runtime.IPluginService;
import org.jkiss.dbeaver.runtime.net.GlobalProxyAuthenticator;
import org.jkiss.dbeaver.runtime.net.GlobalProxySelector;
import org.jkiss.dbeaver.runtime.qm.QMControllerImpl;
import org.jkiss.dbeaver.runtime.qm.QMLogFileWriter;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.ProxySelector;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * DBeaverCore
 */
public class DBeaverCore implements DBPApplication {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.jkiss.dbeaver.core"; //$NON-NLS-1$

    static final Log log = Log.getLog(DBeaverCore.class);

    public static final String TEMP_PROJECT_NAME = ".dbeaver-temp"; //$NON-NLS-1$

    static DBeaverCore instance;
    private static boolean standalone = false;
    private static volatile boolean isClosing = false;

    private File tempFolder;
    private IWorkspace workspace;
    private OSDescriptor localSystem;

    private DBNModel navigatorModel;
    private QMControllerImpl queryManager;
    private QMLogFileWriter qmLogWriter;
    private ProjectRegistry projectRegistry;

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
        log.debug("Initializing " + getProductTitle());
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
        return standalone;
    }

    public static void setStandalone(boolean flag)
    {
        standalone = flag;
    }

    public static boolean isClosing()
    {
        return isClosing;
    }

    public static void setClosing(boolean closing)
    {
        isClosing = closing;
    }

    public static Version getVersion()
    {
        return DBeaverActivator.getInstance().getBundle().getVersion();
    }

    public static String getProductTitle()
    {
        return Platform.getProduct().getName() + " " + getVersion();
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

        // Temp folder
        {
            try {
                final java.nio.file.Path tempDirectory = Files.createTempDirectory(TEMP_PROJECT_NAME);
                tempFolder = tempDirectory.toFile();
            } catch (IOException e) {
                final String sysTempFolder = System.getProperty("java.io.tmpdir");
                if (!CommonUtils.isEmpty(sysTempFolder)) {
                    tempFolder = new File(sysTempFolder, TEMP_PROJECT_NAME);
                    if (!tempFolder.mkdirs()){
                        final String sysUserFolder = System.getProperty("user.home");
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

        // Register properties adapter
        this.workspace = ResourcesPlugin.getWorkspace();

        this.localSystem = new OSDescriptor(Platform.getOS(), Platform.getOSArch());

        QMUtils.initApplication(this);
        this.queryManager = new QMControllerImpl();

        this.qmLogWriter = new QMLogFileWriter();
        this.queryManager.registerMetaListener(qmLogWriter);

        // Init default network settings
        Authenticator.setDefault(new GlobalProxyAuthenticator());
        ProxySelector.setDefault(new GlobalProxySelector(ProxySelector.getDefault()));

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
            if (!tempFolder.delete()) {
                log.warn("Can't delete temp folder '" + tempFolder.getAbsolutePath() + "'");
            }
            tempFolder = null;
        }

        DBeaverCore.instance = null;
        DBeaverCore.disposed = true;

        log.debug("Shutdown completed in " + (System.currentTimeMillis() - startTime) + "ms");
    }

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

    /**
     * Returns configuration file
     */
    public File getConfigurationFile(String fileName, boolean read)
    {
        File configFile = new File(DBeaverActivator.getInstance().getStateLocation().toFile(), fileName);
        if (!configFile.exists() && read) {
            // [Compatibility with DBeaver 1.x]
            configFile = new File(Platform.getLocation().toFile(), fileName);
        }
        return configFile;
    }

    public OSDescriptor getLocalSystem()
    {
        return localSystem;
    }

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
    public DBDValueHandlerRegistry getValueHandlerRegistry() {
        return DataTypeProviderRegistry.getInstance();
    }

    @NotNull
    @Override
    public DBERegistry getEditorsRegistry() {
        return EntityEditorsRegistry.getInstance();
    }

    @NotNull
    @Override
    public DBPPreferenceStore getPreferenceStore() {
        return getGlobalPreferenceStore();
    }

    public ProjectRegistry getProjectRegistry()
    {
        return projectRegistry;
    }

    @NotNull
    public File getTempFolder(DBRProgressMonitor monitor, String name) {
        return tempFolder;
    }

    @NotNull
    @Override
    public DBRRunnableContext getRunnableContext() {
        return DBeaverUI.getDefaultRunnableContext();
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
