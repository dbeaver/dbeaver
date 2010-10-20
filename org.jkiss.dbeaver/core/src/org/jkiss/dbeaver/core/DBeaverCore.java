/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
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
import org.jkiss.dbeaver.model.qm.QMController;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.DataExportersRegistry;
import org.jkiss.dbeaver.registry.DataFormatterRegistry;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.EntityEditorsRegistry;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.qm.QMControllerImpl;
import org.jkiss.dbeaver.runtime.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.runtime.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.ui.SharedTextColors;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorAdapterFactory;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * DBeaverCore
 */
public class DBeaverCore implements DBPApplication, DBRRunnableContext {

    static final Log log = LogFactory.getLog(DBeaverCore.class);

    private static final String DEFAULT_PROJECT_NAME = "default";
    private static final String AUTOSAVE_DIR = "autosave";
    private static final String LOB_DIR = "lob";

    private static DBeaverCore instance;
    private DBeaverActivator plugin;
    private DatabaseEditorAdapterFactory editorsAdapter;
    //private DBeaverProgressProvider progressProvider;
    private IWorkspace workspace;
    private IWorkbench workbench;
    private IProject defaultProject;
    private IPath rootPath;

    private DataSourceRegistry dataSourceRegistry;
    private EntityEditorsRegistry editorsRegistry;
    private DataExportersRegistry dataExportersRegistry;
    private DataFormatterRegistry dataFormatterRegistry;

    private DBNModel metaModel;
    private QMControllerImpl queryManager;
    private SharedTextColors sharedTextColors;

    public static DBeaverCore getInstance()
    {
        return instance;
    }

    static DBeaverCore createInstance(DBeaverActivator plugin)
    {
        instance = new DBeaverCore(plugin);
        instance.initialize();
        return instance;
    }

    DBeaverCore(DBeaverActivator plugin)
    {
        this.plugin = plugin;
    }

    private void initialize()
    {
        //progressProvider = new DBeaverProgressProvider();
        this.sharedTextColors = new SharedTextColors();

        // Register properties adapter
        editorsAdapter = new DatabaseEditorAdapterFactory();
        IAdapterManager mgr = Platform.getAdapterManager();
        mgr.registerAdapters(editorsAdapter, IWorkbenchPart.class);

        DBeaverIcons.initRegistry(plugin.getBundle());

        this.workspace = ResourcesPlugin.getWorkspace();
        this.rootPath = Platform.getLocation();

        IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
        this.dataSourceRegistry = new DataSourceRegistry(this);
        this.dataSourceRegistry.loadExtensions(extensionRegistry);
        
        this.editorsRegistry = new EntityEditorsRegistry(extensionRegistry);
        this.dataExportersRegistry = new DataExportersRegistry(extensionRegistry);
        this.dataFormatterRegistry = new DataFormatterRegistry(extensionRegistry);

        this.metaModel = new DBNModel(dataSourceRegistry);
        this.queryManager = new QMControllerImpl(dataSourceRegistry);
        // Make default project
        this.defaultProject = this.workspace.getRoot().getProject(DEFAULT_PROJECT_NAME);

        try {
            PlatformUI.getWorkbench().getProgressService().run(false, false, new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    try {
                        if (!defaultProject.exists()) {
                            defaultProject.create(monitor);
                        }
                        defaultProject.open(monitor);
                    }
                    catch (CoreException ex) {
                        throw new InvocationTargetException(ex);
                    }
                }
            });
        }
        catch (InvocationTargetException e) {
            log.error(e.getTargetException());
        }
        catch (InterruptedException e) {
            // do nothing
        }

        // Init preferences
        initDefaultPreferences();
    }

    public synchronized void dispose()
    {
        IProgressMonitor monitor = new NullProgressMonitor();
        if (defaultProject != null) {
            try {
                //defaultProject.
                defaultProject.close(monitor);
            }
            catch (CoreException ex) {
                log.error("Can't close default project", ex);
            }
        }
        if (workspace != null) {
            try {
                workspace.save(true, monitor);
            }
            catch (CoreException ex) {
                log.error("Can't save workspace", ex);
            }
        }
        if (queryManager != null) {
            queryManager.dispose();
        }
        if (metaModel != null) {
            metaModel.dispose();
        }
        this.dataExportersRegistry.dispose();
        this.dataFormatterRegistry.dispose();
        this.dataSourceRegistry.dispose();

        // Unregister properties adapter
        Platform.getAdapterManager().unregisterAdapters(editorsAdapter);

        this.sharedTextColors.dispose();
        //progressProvider.shutdown();
        //progressProvider = null;

        instance = null;
    }

    public String getPluginID()
    {
        return plugin.getBundle().getSymbolicName();
    }

    public ILog getPluginLog()
    {
        return plugin.getLog();
    }

    public DBeaverActivator getPlugin()
    {
        return plugin;
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

    public DBNModel getNavigatorModel()
    {
        return metaModel;
    }

    public QMController getQueryManager()
    {
        return queryManager;
    }

    public DataSourceRegistry getDataSourceRegistry()
    {
        return this.dataSourceRegistry;
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

    public void runAndWait(final DBRRunnableWithProgress runnable)
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
                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    runnable.run(RuntimeUtils.makeMonitor(monitor));
                }
            });
        }
        catch (InvocationTargetException e) {
            log.error(e.getTargetException());
        }
        catch (InterruptedException e) {
            // do nothing
        }
    }

    public void runAndWait2(final DBRRunnableWithProgress runnable)
        throws InvocationTargetException, InterruptedException
    {
        this.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                runnable.run(RuntimeUtils.makeMonitor(monitor));
            }
        });
    }

    public static void runUIJob(String jobName, final DBRRunnableWithProgress runnableWithProgress)
    {
        new AbstractUIJob(jobName) {
            public IStatus runInUIThread(DBRProgressMonitor monitor)
            {
                try {
                    runnableWithProgress.run(monitor);
                }
                catch (InvocationTargetException e) {
                    return RuntimeUtils.makeExceptionStatus(e);
                }
                catch (InterruptedException e) {
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    public void runInUI(IRunnableContext context, final DBRRunnableWithProgress runnable)
    {
        try {
            this.getWorkbench().getProgressService().runInUI(context, new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    runnable.run(RuntimeUtils.makeMonitor(monitor));
                }
            }, DBeaverActivator.getWorkspace().getRoot());
        }
        catch (InvocationTargetException e) {
            log.error(e.getTargetException());
        }
        catch (InterruptedException e) {
            // do nothing
        }
    }

    public IFolder getAutosaveFolder(DBRProgressMonitor monitor)
        throws IOException
    {
        return getLocalFolder(monitor, AUTOSAVE_DIR);
    }

    public IFolder getLobFolder(DBRProgressMonitor monitor)
        throws IOException
    {
        return getLocalFolder(monitor, LOB_DIR);
    }

    private IFolder getLocalFolder(DBRProgressMonitor monitor, String name)
        throws IOException
    {
        IPath tempPath = defaultProject.getProjectRelativePath().append(name);
        IFolder tempFolder = defaultProject.getFolder(tempPath);
        if (!tempFolder.exists()) {
            try {
                tempFolder.create(true, true, monitor.getNestedMonitor());
            }
            catch (CoreException ex) {
                throw new IOException("Could not create temp directory '" + tempFolder.toString() + "'", ex);
            }
        }
        return tempFolder;
    }

    public IFile makeTempFile(DBRProgressMonitor monitor, IFolder folder, String name, String extension)
        throws IOException
    {
        IFile tempFile = folder.getFile(name + "-" + System.currentTimeMillis() + "." + extension);
        try {
            InputStream contents = new ByteArrayInputStream(new byte[0]);
            tempFile.create(contents, true, monitor.getNestedMonitor());
        }
        catch (CoreException ex) {
            throw new IOException("Coud not create temp file '" + tempFile.toString() + "' in '" + folder.toString() + "'", ex);
        }
        return tempFile;
    }

    private void initDefaultPreferences()
    {
        IPreferenceStore store = getGlobalPreferenceStore();

        // Common
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.DEFAULT_AUTO_COMMIT, true);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.QUERY_ROLLBACK_ON_ERROR, true);

        // SQL execution
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.SCRIPT_COMMIT_TYPE, SQLScriptCommitType.NO_COMMIT.name());
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.SCRIPT_COMMIT_LINES, 1000);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.SCRIPT_ERROR_HANDLING, SQLScriptErrorHandling.STOP_ROLLBACK.name());
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.SCRIPT_FETCH_RESULT_SETS, false);

        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.RESULT_SET_MAX_ROWS, 200);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.STATEMENT_TIMEOUT, 10 * 1000);

        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.RS_EDIT_MAX_TEXT_SIZE, 10 * 1000000);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.RS_EDIT_LONG_AS_LOB, true);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.RS_COMMIT_ON_EDIT_APPLY, false);
        RuntimeUtils.setDefaultPreferenceValue(store, PrefConstants.RS_COMMIT_ON_CONTENT_APPLY, false);
        
        RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION, true);
        RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTO_ACTIVATION_DELAY, 500);
        RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO, true);

        RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES, true);
        RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES, true);
        RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS, true);
        RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_COMMENTS, true);
        RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_BEGIN_END, true);
        
        // Text editor default preferences
        RuntimeUtils.setDefaultPreferenceValue(store, AbstractTextEditor.PREFERENCE_TEXT_DRAG_AND_DROP_ENABLED, true);

        // QM
        queryManager.initDefaultPreferences(store);
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

}
