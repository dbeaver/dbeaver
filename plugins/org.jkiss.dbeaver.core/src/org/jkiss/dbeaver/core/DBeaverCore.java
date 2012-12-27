/*
 * Copyright (C) 2010-2012 Serge Rieder
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
import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jkiss.dbeaver.DBeaverConstants;
import org.jkiss.dbeaver.model.DBPApplication;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.net.DBWGlobalAuthenticator;
import org.jkiss.dbeaver.model.qm.QMController;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.*;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.qm.QMControllerImpl;
import org.jkiss.dbeaver.runtime.qm.QMLogFileWriter;
import org.jkiss.dbeaver.runtime.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.runtime.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorAdapterFactory;
import org.jkiss.dbeaver.ui.editors.binary.HexEditControl;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Authenticator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private DBeaverActivator plugin;
    private DatabaseEditorAdapterFactory editorsAdapter;
    //private DBeaverProgressProvider progressProvider;
    private IWorkspace workspace;
    private IProject tempProject;
    private OSDescriptor localSystem;

    private DataSourceProviderRegistry dataSourceProviderRegistry;
    private EntityEditorsRegistry editorsRegistry;
    private DataExportersRegistry dataExportersRegistry;
    private DataFormatterRegistry dataFormatterRegistry;
    private NetworkHandlerRegistry networkHandlerRegistry;

    private DBNModel navigatorModel;
    private QMControllerImpl queryManager;
    private QMLogFileWriter qmLogWriter;
    private ProjectRegistry projectRegistry;

    private boolean isClosing;
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
                    DBeaverCore.createInstance(DBeaverActivator.getInstance());
                }
            }
        }
        return instance;
    }

    private static DBeaverCore createInstance(DBeaverActivator plugin)
    {
        log.debug("Initializing DBeaver");
        log.debug("Host plugin: " + plugin.getBundle().getSymbolicName() + " " + plugin.getBundle().getVersion());

        instance = new DBeaverCore(plugin);
        instance.initialize();
        return instance;
    }

    DBeaverCore(DBeaverActivator plugin)
    {
        this.plugin = plugin;
    }

    public boolean isClosing()
    {
        return isClosing;
    }

    public void setClosing(boolean closing)
    {
        isClosing = closing;
    }

    public static boolean isStandalone()
    {
        return standalone;
    }

    public static void setStandalone(boolean flag)
    {
        standalone = flag;
    }

    public static Version getVersion()
    {
        return DBeaverActivator.getInstance().getBundle().getVersion();
    }

    public static String getProductTitle()
    {
        return "DBeaver " + getVersion();
    }

    private void initialize()
    {
        // Register properties adapter
        this.editorsAdapter = new DatabaseEditorAdapterFactory();
        IAdapterManager mgr = Platform.getAdapterManager();
        mgr.registerAdapters(editorsAdapter, IWorkbenchPart.class);

        this.workspace = ResourcesPlugin.getWorkspace();

        this.localSystem = new OSDescriptor(Platform.getOS(), Platform.getOSArch());

        DBeaverUI.initializeUI();

        IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();

        // Init datasource registry
        this.dataSourceProviderRegistry = new DataSourceProviderRegistry();
        this.dataSourceProviderRegistry.loadExtensions(extensionRegistry);

        this.editorsRegistry = new EntityEditorsRegistry(extensionRegistry);
        this.dataExportersRegistry = new DataExportersRegistry(extensionRegistry);
        this.dataFormatterRegistry = new DataFormatterRegistry(extensionRegistry);
        this.networkHandlerRegistry = new NetworkHandlerRegistry(extensionRegistry);

        this.queryManager = new QMControllerImpl(dataSourceProviderRegistry);
        this.qmLogWriter = new QMLogFileWriter();
        this.queryManager.registerMetaListener(qmLogWriter);

        // Init preferences
        initDefaultPreferences();

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
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    try {
                        try {
                            // Temp project
                            tempProject = workspace.getRoot().getProject(TEMP_PROJECT_NAME);
                            File systemTempFolder = new File(System.getProperty("java.io.tmpdir"));
                            File dbeaverTempFolder = new File(
                                systemTempFolder,
                                TEMP_PROJECT_NAME + "." + CommonUtils.escapeIdentifier(workspace.getRoot().getLocation().toString()));
                            if (tempProject.exists()) {
                                try {
                                    tempProject.delete(true, true, monitor.getNestedMonitor());
                                } catch (CoreException e) {
                                    log.error("Can't delete temp project", e);
                                }
                            }
                            if (!dbeaverTempFolder.exists()) {
                                if (!dbeaverTempFolder.mkdirs()) {
                                    log.error("Can't create directory '" + dbeaverTempFolder.getAbsolutePath() + "'");
                                }
                            }
                            ProjectDescription description = new ProjectDescription();
                            description.setLocation(new Path(dbeaverTempFolder.getAbsolutePath()));
                            description.setName(TEMP_PROJECT_NAME);
                            description.setComment("Project for DBeaver temporary content");
                            try {
                                tempProject.create(description, monitor.getNestedMonitor());
                            } catch (CoreException e) {
                                log.error("Can't create temp project", e);
                            }

                            tempProject.open(monitor.getNestedMonitor());
                            tempProject.setHidden(true);
                        } catch (CoreException e) {
                            log.error("Cannot open temp project", e); //$NON-NLS-1$
                        }

                        // Projects registry
                        projectRegistry.loadProjects(workspace, monitor.getNestedMonitor());
                    } catch (CoreException ex) {
                        throw new InvocationTargetException(ex);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            log.error(e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public synchronized void dispose()
    {
        IProgressMonitor monitor = new NullProgressMonitor();
        if (workspace != null) {
            if (tempProject != null && tempProject.exists()) {
                try {
                    tempProject.delete(true, true, monitor);
                } catch (CoreException e) {
                    log.warn("Can't cleanup temp project", e);
                }
            }
            if (isStandalone()) {
                for (IProject project : workspace.getRoot().getProjects()) {
                    try {
                        if (project.isOpen()) {
                            project.close(monitor);
                        }
                    } catch (CoreException ex) {
                        log.error("Can't close default project", ex); //$NON-NLS-1$
                    }
                }
                try {
                    workspace.save(true, monitor);
                } catch (CoreException ex) {
                    log.error("Can't save workspace", ex); //$NON-NLS-1$
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
        if (this.navigatorModel != null) {
            this.navigatorModel.dispose();
            //navigatorModel = null;
        }
        if (this.networkHandlerRegistry != null) {
            this.networkHandlerRegistry.dispose();
            this.networkHandlerRegistry = null;
        }
        if (this.dataExportersRegistry != null) {
            this.dataExportersRegistry.dispose();
            this.dataExportersRegistry = null;
        }
        if (this.dataFormatterRegistry != null) {
            this.dataFormatterRegistry.dispose();
            this.dataFormatterRegistry = null;
        }
        if (this.editorsRegistry != null) {
            this.editorsRegistry.dispose();
            this.editorsRegistry = null;
        }
        if (this.projectRegistry != null) {
            this.projectRegistry.dispose();
            this.projectRegistry = null;
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

        DBeaverUI.disposeUI();
        //progressProvider.shutdown();
        //progressProvider = null;

        DBeaverCore.instance = null;
        DBeaverCore.disposed = true;
    }

    public Plugin getPlugin()
    {
        return plugin;
    }

    public String getPluginID()
    {
        return plugin.getBundle().getSymbolicName();
    }

    public ILog getPluginLog()
    {
        return plugin.getLog();
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

    public DataExportersRegistry getDataExportersRegistry()
    {
        return dataExportersRegistry;
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

    public IPreferenceStore getGlobalPreferenceStore()
    {
        return plugin.getPreferenceStore();
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

    private void initDefaultPreferences()
    {
        IPreferenceStore store = getGlobalPreferenceStore();

        // Common
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.DEFAULT_AUTO_COMMIT, true);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.KEEP_STATEMENT_OPEN, false);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.QUERY_ROLLBACK_ON_ERROR, false);

        // SQL execution
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.SCRIPT_COMMIT_TYPE, SQLScriptCommitType.NO_COMMIT.name());
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.SCRIPT_COMMIT_LINES, 1000);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.SCRIPT_ERROR_HANDLING, SQLScriptErrorHandling.STOP_ROLLBACK.name());
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.SCRIPT_FETCH_RESULT_SETS, false);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.SCRIPT_AUTO_FOLDERS, false);

        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.STATEMENT_TIMEOUT, 10 * 1000);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.MEMORY_CONTENT_MAX_SIZE, 10000);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.READ_EXPENSIVE_PROPERTIES, true);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.META_CASE_SENSITIVE, false);

        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.RS_EDIT_MAX_TEXT_SIZE, 10 * 1000000);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.RS_EDIT_LONG_AS_LOB, true);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.CONTENT_HEX_ENCODING, ContentUtils.getDefaultFileEncoding());
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.RS_COMMIT_ON_EDIT_APPLY, false);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.RS_COMMIT_ON_CONTENT_APPLY, false);

        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.TEXT_EDIT_UNDO_LEVEL, 200);

        RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION, true);
        RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTO_ACTIVATION_DELAY, 500);
        RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO, true);
        RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROPOSAL_INSERT_CASE, SQLPreferenceConstants.PROPOSAL_CASE_DEFAULT);

        RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES, true);
        RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES, true);
        RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS, true);
        RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_COMMENTS, true);
        RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_BEGIN_END, true);

        // Text editor default preferences
        RuntimeUtils.setDefaultPreferenceValue(store, AbstractTextEditor.PREFERENCE_TEXT_DRAG_AND_DROP_ENABLED, true);

        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.HEX_FONT_NAME, HexEditControl.DEFAULT_FONT_NAME);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.HEX_FONT_SIZE, 10);

        // General UI
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.UI_AUTO_UPDATE_CHECK, true);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.UI_PROXY_HOST, "");
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.UI_PROXY_PORT, 0);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.UI_PROXY_USER, "");
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.UI_PROXY_PASSWORD, "");
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.UI_DRIVERS_HOME, "");
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.UI_DRIVERS_SOURCES, DBeaverConstants.DEFAULT_DRIVERS_SOURCE);

        // Network
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.NET_TUNNEL_PORT_MIN, 10000);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.NET_TUNNEL_PORT_MAX, 60000);

        // ResultSet
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.RESULT_SET_MAX_ROWS, 200);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.RESULT_SET_BINARY_SHOW_STRINGS, true);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.RESULT_SET_BINARY_STRING_MAX_LEN, 32);

        // QM
        queryManager.initDefaultPreferences(store);

        // Data formats
        DataFormatterProfile.initDefaultPreferences(store, Locale.getDefault());
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
