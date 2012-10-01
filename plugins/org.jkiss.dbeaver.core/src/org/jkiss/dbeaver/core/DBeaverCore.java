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
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jkiss.dbeaver.model.DBPApplication;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.net.DBWGlobalAuthenticator;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.qm.QMController;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.*;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.qm.QMControllerImpl;
import org.jkiss.dbeaver.runtime.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.runtime.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.ui.SharedTextColors;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorAdapterFactory;
import org.jkiss.dbeaver.ui.editors.binary.HexEditControl;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.osgi.framework.Version;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Authenticator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * DBeaverCore
 */
public class DBeaverCore implements DBPApplication, DBRRunnableContext {

    static final Log log = LogFactory.getLog(DBeaverCore.class);

    //private static final String AUTOSAVE_DIR = ".autosave";
    private static final String LOB_DIR = ".lob"; //$NON-NLS-1$
    public static final String TEMP_PROJECT_NAME = "temp"; //$NON-NLS-1$

    private static DBeaverCore instance;
    private DBeaverActivator plugin;
    private DatabaseEditorAdapterFactory editorsAdapter;
    //private DBeaverProgressProvider progressProvider;
    private IWorkspace workspace;
    private IWorkbench workbench;
    private IPath rootPath;
    private IProject tempProject;
    private OSDescriptor localSystem;

    private DataSourceProviderRegistry dataSourceProviderRegistry;
    private EntityEditorsRegistry editorsRegistry;
    private DataExportersRegistry dataExportersRegistry;
    private DataFormatterRegistry dataFormatterRegistry;
    private NetworkHandlerRegistry networkHandlerRegistry;

    private DBNModel navigatorModel;
    private QMControllerImpl queryManager;
    private SharedTextColors sharedTextColors;
    private ProjectRegistry projectRegistry;

    private boolean standalone = false;
    private boolean isClosing;

    public static DBeaverCore getInstance()
    {
        if (instance == null) {
            synchronized (DBeaverCore.class) {
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
        instance.cleanupLobFiles(new NullProgressMonitor());
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

    void setClosing(boolean closing)
    {
        isClosing = closing;
    }

    public boolean isStandalone()
    {
        return standalone;
    }

    void setStandalone(boolean standalone)
    {
        this.standalone = standalone;
    }

    public Version getVersion()
    {
        return Platform.getProduct().getDefiningBundle().getVersion();
    }

    private void initialize()
    {
        //progressProvider = new DBeaverProgressProvider();
        this.sharedTextColors = new SharedTextColors();

        // Register properties adapter
        this.editorsAdapter = new DatabaseEditorAdapterFactory();
        IAdapterManager mgr = Platform.getAdapterManager();
        mgr.registerAdapters(editorsAdapter, IWorkbenchPart.class);

        DBeaverIcons.initRegistry(plugin.getBundle());

        this.workspace = ResourcesPlugin.getWorkspace();
        this.rootPath = Platform.getLocation();

        this.localSystem = new OSDescriptor(Platform.getOS(), Platform.getOSArch());

        IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();

        // Init datasource registry
        this.dataSourceProviderRegistry = new DataSourceProviderRegistry();
        this.dataSourceProviderRegistry.loadExtensions(extensionRegistry);

        this.editorsRegistry = new EntityEditorsRegistry(extensionRegistry);
        this.dataExportersRegistry = new DataExportersRegistry(extensionRegistry);
        this.dataFormatterRegistry = new DataFormatterRegistry(extensionRegistry);
        this.networkHandlerRegistry = new NetworkHandlerRegistry(extensionRegistry);

        this.queryManager = new QMControllerImpl(dataSourceProviderRegistry);

        // Init preferences
        initDefaultPreferences();

        // Init default network settings
        Authenticator.setDefault(DBWGlobalAuthenticator.getInstance());

        // Init project registry
        this.projectRegistry = new ProjectRegistry();
        this.projectRegistry.loadExtensions(extensionRegistry);

        try {
            PlatformUI.getWorkbench().getProgressService().run(false, false, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    try {
                        try {
// Temp project
                            tempProject = workspace.getRoot().getProject(TEMP_PROJECT_NAME);
                            if (!tempProject.exists()) {
                                tempProject.create(monitor);
                            }
                            tempProject.open(monitor);
                            tempProject.setHidden(true);
                        } catch (CoreException e) {
                            log.error("Cannot create temp project", e); //$NON-NLS-1$
                        }

                        // Projects registry
                        projectRegistry.loadProjects(workspace, monitor);
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

        // Navigator model
        this.navigatorModel = new DBNModel();
        this.navigatorModel.initialize();
    }

    public IProject getProject(String projectId)
    {
        IProject[] projects = this.workspace.getRoot().getProjects();
        for (IProject project : projects) {
            if (!project.isOpen()) {
                continue;
            }
            try {
                String id = project.getPersistentProperty(DBPResourceHandler.PROP_PROJECT_ID);
                if (id != null && id.equals(projectId)) {
                    return project;
                }
            } catch (CoreException e) {
                log.warn(e);
            }
        }
        return null;
    }

    public synchronized void dispose()
    {
        IProgressMonitor monitor = new NullProgressMonitor();
        if (workspace != null) {
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

        if (queryManager != null) {
            queryManager.dispose();
            //queryManager = null;
        }
        if (navigatorModel != null) {
            navigatorModel.dispose();
            //navigatorModel = null;
        }
        if (this.networkHandlerRegistry != null) {
            networkHandlerRegistry.dispose();
            networkHandlerRegistry = null;
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

        if (editorsAdapter != null) {
            // Unregister properties adapter
            Platform.getAdapterManager().unregisterAdapters(editorsAdapter);
            editorsAdapter = null;
        }

        if (this.sharedTextColors != null) {
            this.sharedTextColors.dispose();
            this.sharedTextColors = null;
        }
        //progressProvider.shutdown();
        //progressProvider = null;

        instance = null;
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

    public IPath getRootPath()
    {
        return rootPath;
    }

    public ISharedTextColors getSharedTextColors()
    {
        return sharedTextColors;
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

    public IWorkbench getWorkbench()
    {
        if (this.workbench == null) {
            this.workbench = PlatformUI.getWorkbench();
        }
        return this.workbench;
    }

    public IPreferenceStore getGlobalPreferenceStore()
    {
        return plugin.getPreferenceStore();
    }

    @Override
    public void runInProgressDialog(final DBRRunnableWithProgress runnable) throws InterruptedException, InvocationTargetException
    {
        try {
            IRunnableContext runnableContext;
            IWorkbenchWindow workbenchWindow = getWorkbench().getActiveWorkbenchWindow();
            if (workbenchWindow != null) {
                runnableContext = new ProgressMonitorDialog(getWorkbench().getActiveWorkbenchWindow().getShell());
            } else {
                runnableContext = this.getWorkbench().getProgressService();
            }
            runnableContext.run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    runnable.run(RuntimeUtils.makeMonitor(monitor));
                }
            });
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public void runInProgressService(final DBRRunnableWithProgress runnable)
        throws InvocationTargetException, InterruptedException
    {
        this.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                runnable.run(RuntimeUtils.makeMonitor(monitor));
            }
        });
    }

    public static AbstractUIJob runUIJob(String jobName, final DBRRunnableWithProgress runnableWithProgress)
    {
        AbstractUIJob job = new AbstractUIJob(jobName) {
            @Override
            public IStatus runInUIThread(DBRProgressMonitor monitor)
            {
                try {
                    runnableWithProgress.run(monitor);
                } catch (InvocationTargetException e) {
                    return RuntimeUtils.makeExceptionStatus(e);
                } catch (InterruptedException e) {
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
        return job;
    }

    public void runInUI(IRunnableContext context, final DBRRunnableWithProgress runnable)
    {
        try {
            this.getWorkbench().getProgressService().runInUI(context, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    runnable.run(RuntimeUtils.makeMonitor(monitor));
                }
            }, DBeaverActivator.getWorkspace().getRoot());
        } catch (InvocationTargetException e) {
            log.error(e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }
    }

/*
    public IFolder getAutosaveFolder(DBRProgressMonitor monitor)
        throws IOException
    {
        return getTempFolder(monitor, AUTOSAVE_DIR);
    }
*/

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

    public IFile makeTempFile(DBRProgressMonitor monitor, IFolder folder, String name, String extension)
        throws IOException
    {
        IFile tempFile = folder.getFile(name + "-" + System.currentTimeMillis() + "." + extension);  //$NON-NLS-1$ //$NON-NLS-2$
        try {
            InputStream contents = new ByteArrayInputStream(new byte[0]);
            tempFile.create(contents, true, monitor.getNestedMonitor());
        } catch (CoreException ex) {
            throw new IOException(MessageFormat.format(CoreMessages.DBeaverCore_error_can_create_temp_file, tempFile.toString(), folder.toString()), ex);
        }
        return tempFile;
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

        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.RESULT_SET_MAX_ROWS, 200);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.STATEMENT_TIMEOUT, 10 * 1000);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.MEMORY_CONTENT_MAX_SIZE, 10000);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.READ_EXPENSIVE_PROPERTIES, true);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.META_CASE_SENSITIVE, false);

        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.RS_EDIT_MAX_TEXT_SIZE, 10 * 1000000);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.RS_EDIT_LONG_AS_LOB, true);
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

        // Network
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.NET_TUNNEL_PORT_MIN, 10000);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.NET_TUNNEL_PORT_MAX, 60000);

        // QM
        queryManager.initDefaultPreferences(store);

        // Data formats
        DataFormatterProfile.initDefaultPreferences(store, Locale.getDefault());
    }

    public static IWorkbenchWindow getActiveWorkbenchWindow()
    {
        IWorkbenchWindow window = instance.plugin.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            return window;
        }
        IWorkbenchWindow[] windows = instance.plugin.getWorkbench().getWorkbenchWindows();
        if (windows.length > 0) {
            return windows[0];
        }
        return null;
    }

    public static Shell getActiveWorkbenchShell()
    {
        IWorkbenchWindow window = getActiveWorkbenchWindow();
        if (window != null) {
            return window.getShell();
        }
        IWorkbenchWindow[] windows = instance.plugin.getWorkbench().getWorkbenchWindows();
        if (windows.length > 0)
            return windows[0].getShell();
        return null;
    }

    public static Display getDisplay()
    {
        Shell shell = getActiveWorkbenchShell();
        if (shell != null)
            return shell.getDisplay();
        else
            return Display.getDefault();
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

    private void cleanupLobFiles(IProgressMonitor monitor)
    {
        IFolder tempFolder;
        try {
            tempFolder = getLobFolder(monitor);
        } catch (IOException e) {
            log.error(e);
            return;
        }
        try {
            IResource[] tempResources = tempFolder.members();
            for (IResource tempResource : tempResources) {
                if (tempResource instanceof IFile) {
                    IFile tempFile = (IFile) tempResource;
                    tempFile.delete(true, false, monitor);
                }
            }
        } catch (CoreException ex) {
            log.error("Error deleting temp lob file", ex); //$NON-NLS-1$
        }
    }

}
