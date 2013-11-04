/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.DBPApplication;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.net.DBWGlobalAuthenticator;
import org.jkiss.dbeaver.model.qm.QMController;
import org.jkiss.dbeaver.registry.*;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.registry.transfer.DataTransferRegistry;
import org.jkiss.dbeaver.runtime.qm.QMControllerImpl;
import org.jkiss.dbeaver.runtime.qm.QMLogFileWriter;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorAdapterFactory;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * DBeaverCore
 */
public class DBeaverCore implements DBPApplication {

    static final Log log = LogFactory.getLog(DBeaverCore.class);

    //private static final String AUTOSAVE_DIR = ".autosave";
    private static final String LOB_DIR = ".lob"; //$NON-NLS-1$
    public static final String TEMP_PROJECT_NAME = "org.jkiss.dbeaver.temp"; //$NON-NLS-1$

    private static DBeaverCore instance;
    private static boolean standalone = false;
    private static volatile boolean isClosing = false;

    private DatabaseEditorAdapterFactory editorsAdapter;
    //private DBeaverProgressProvider progressProvider;
    private IWorkspace workspace;
    private IProject tempProject;
    private OSDescriptor localSystem;

    private DataSourceProviderRegistry dataSourceProviderRegistry;
    private EntityEditorsRegistry editorsRegistry;
    private DataTransferRegistry dataTransferRegistry;
    private DataFormatterRegistry dataFormatterRegistry;
    private NetworkHandlerRegistry networkHandlerRegistry;

    private DBNModel navigatorModel;
    private QMControllerImpl queryManager;
    private QMLogFileWriter qmLogWriter;
    private ProjectRegistry projectRegistry;

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
            }
        }

        instance = new DBeaverCore();
        instance.initialize();
        return instance;
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
        return "DBeaver " + getVersion();
    }

    public static IPreferenceStore getGlobalPreferenceStore()
    {
        return DBeaverActivator.getInstance().getPreferenceStore();
    }

    DBeaverCore()
    {
    }

    private void initialize()
    {
        // Register properties adapter
        this.editorsAdapter = new DatabaseEditorAdapterFactory();
        IAdapterManager mgr = Platform.getAdapterManager();
        mgr.registerAdapters(editorsAdapter, IWorkbenchPart.class);

        this.workspace = ResourcesPlugin.getWorkspace();

        this.localSystem = new OSDescriptor(Platform.getOS(), Platform.getOSArch());

        DBeaverUI.getInstance();

        IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();

        // Init datasource registry
        this.dataSourceProviderRegistry = new DataSourceProviderRegistry();
        this.dataSourceProviderRegistry.loadExtensions(extensionRegistry);

        this.editorsRegistry = new EntityEditorsRegistry(extensionRegistry);
        this.dataTransferRegistry = new DataTransferRegistry(extensionRegistry);
        this.dataFormatterRegistry = new DataFormatterRegistry(extensionRegistry);
        this.networkHandlerRegistry = new NetworkHandlerRegistry(extensionRegistry);

        this.queryManager = new QMControllerImpl();
        this.qmLogWriter = new QMLogFileWriter();
        this.queryManager.registerMetaListener(qmLogWriter);

        // Init default network settings
        Authenticator.setDefault(DBWGlobalAuthenticator.getInstance());

        // Init project registry
        this.projectRegistry = new ProjectRegistry();
        this.projectRegistry.loadExtensions(extensionRegistry);

        initializeTempProject();

        // Navigator model
        this.navigatorModel = new DBNModel();
        this.navigatorModel.initialize();
    }

    private void initializeTempProject()
    {
        try {
            final IProgressMonitor monitor = new NullProgressMonitor();
            try {
                // Temp project
                tempProject = workspace.getRoot().getProject(TEMP_PROJECT_NAME);
                File systemTempFolder = new File(System.getProperty("java.io.tmpdir"));
                File dbeaverTempFolder = new File(
                    systemTempFolder,
                    TEMP_PROJECT_NAME + "." + CommonUtils.escapeIdentifier(workspace.getRoot().getLocation().toString()));
                if (tempProject.exists()) {
                    try {
                        tempProject.delete(true, true, monitor);
                    } catch (CoreException e) {
                        log.error("Can't delete temp project", e);
                    }
                }
                if (!dbeaverTempFolder.exists()) {
                    if (!dbeaverTempFolder.mkdirs()) {
                        log.error("Can't create directory '" + dbeaverTempFolder.getAbsolutePath() + "'");
                    }
                }
                IProjectDescription description = workspace.newProjectDescription(TEMP_PROJECT_NAME);
                description.setLocation(new Path(dbeaverTempFolder.getAbsolutePath()));
                description.setName(TEMP_PROJECT_NAME);
                description.setComment("Project for DBeaver temporary content");
                try {
                    tempProject.create(description, IProject.HIDDEN, monitor);
                } catch (CoreException e) {
                    log.error("Can't create temp project", e);
                }

                tempProject.open(monitor);
                //tempProject.setHidden(true);
            } catch (CoreException e) {
                log.error("Cannot open temp project", e); //$NON-NLS-1$
            }

            // Projects registry
            projectRegistry.loadProjects(workspace, monitor);
        } catch (Throwable e) {
            log.error(e);
        }
    }

    public synchronized void dispose()
    {
        log.debug("Shutdown initiated");
        long startTime = System.currentTimeMillis();

        DBeaverCore.setClosing(true);

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

        // Cleanup temp project
        IProgressMonitor monitor = new NullProgressMonitor();
        if (workspace != null) {
            if (tempProject != null && tempProject.exists()) {
                try {
                    tempProject.delete(true, true, monitor);
                } catch (CoreException e) {
                    log.warn("Can't cleanup temp project", e);
                }
            }
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
        if (this.networkHandlerRegistry != null) {
            this.networkHandlerRegistry.dispose();
            this.networkHandlerRegistry = null;
        }
        if (this.dataTransferRegistry != null) {
            this.dataTransferRegistry.dispose();
            this.dataTransferRegistry = null;
        }
        if (this.dataFormatterRegistry != null) {
            this.dataFormatterRegistry.dispose();
            this.dataFormatterRegistry = null;
        }
        if (this.editorsRegistry != null) {
            this.editorsRegistry.dispose();
            this.editorsRegistry = null;
        }
        if (this.dataSourceProviderRegistry != null) {
            this.dataSourceProviderRegistry.dispose();
            this.dataSourceProviderRegistry = null;
        }

        if (this.editorsAdapter != null) {
            // Unregister properties adapter
            Platform.getAdapterManager().unregisterAdapters(this.editorsAdapter);
            this.editorsAdapter = null;
        }

        if (isStandalone()) {
            try {
                workspace.save(true, monitor);
            } catch (CoreException ex) {
                log.error("Can't save workspace", ex); //$NON-NLS-1$
            }
        }

        DBeaverUI.disposeUI();

        DBeaverCore.instance = null;
        DBeaverCore.disposed = true;

        log.debug("Shutdown completed in " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public IWorkspace getWorkspace()
    {
        return workspace;
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

    @Override
    public DBNModel getNavigatorModel()
    {
        return navigatorModel;
    }

    public QMController getQueryManager()
    {
        return queryManager;
    }

    public DataSourceProviderRegistry getDataSourceProviderRegistry()
    {
        return this.dataSourceProviderRegistry;
    }

    public EntityEditorsRegistry getEditorsRegistry()
    {
        return editorsRegistry;
    }

    public DataTransferRegistry getDataTransferRegistry()
    {
        return dataTransferRegistry;
    }

    public DataFormatterRegistry getDataFormatterRegistry()
    {
        return dataFormatterRegistry;
    }

    public NetworkHandlerRegistry getNetworkHandlerRegistry()
    {
        return networkHandlerRegistry;
    }

    public ProjectRegistry getProjectRegistry()
    {
        return projectRegistry;
    }

    public IFolder getLobFolder(IProgressMonitor monitor)
        throws IOException
    {
        return getTempFolder(monitor, LOB_DIR);
    }

    private IFolder getTempFolder(IProgressMonitor monitor, String name)
        throws IOException
    {
        IPath tempPath = tempProject.getProjectRelativePath().append(name);
        IFolder tempFolder = tempProject.getFolder(tempPath);
        if (!tempFolder.exists()) {
            try {
                tempFolder.create(true, true, monitor);
                tempFolder.setHidden(true);
            } catch (CoreException ex) {
                throw new IOException(MessageFormat.format(CoreMessages.DBeaverCore_error_can_create_temp_dir, tempFolder.toString()), ex);
            }
        }
        return tempFolder;
    }

    public List<IProject> getLiveProjects()
    {
        List<IProject> result = new ArrayList<IProject>();
        for (IProject project : workspace.getRoot().getProjects()) {
            if (project.exists() && !project.isHidden()) {
                result.add(project);
            }
        }
        return result;
    }

}
