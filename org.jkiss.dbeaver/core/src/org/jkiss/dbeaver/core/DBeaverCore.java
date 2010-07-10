/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.model.DBPApplication;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.meta.DBMModel;
import org.jkiss.dbeaver.model.qm.QMController;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.EntityEditorsRegistry;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.runtime.qm.QMControllerImpl;
import org.jkiss.dbeaver.runtime.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.runtime.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * DBeaverCore
 */
public class DBeaverCore implements DBPApplication, DBRRunnableContext {

    static Log log = LogFactory.getLog(DBeaverCore.class);

    private static final String DEFAULT_PROJECT_NAME = "default";
    private static final String AUTOSAVE_DIR = "autosave";
    private static final String LOB_DIR = "lob";

    private static DBeaverCore instance;
    private DBeaverActivator plugin;
    private DBeaverAdapterFactory propertiesAdapter;
    //private DBeaverProgressProvider progressProvider;
    private IWorkspace workspace;
    private IWorkbench workbench;
    private IProject defaultProject;
    private IPath rootPath;

    private DataSourceRegistry dataSourceRegistry;
    private EntityEditorsRegistry editorsRegistry;
    private DBMModel metaModel;
    private QMControllerImpl queryManager;
    private JexlEngine jexlEngine;

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
        jexlEngine = new JexlEngine();

        // Register properties adapter
        propertiesAdapter = new DBeaverAdapterFactory();
        IAdapterManager mgr = Platform.getAdapterManager();
        mgr.registerAdapters(propertiesAdapter, DBPObject.class);

        DBeaverIcons.initRegistry(plugin.getBundle());

        this.workspace = ResourcesPlugin.getWorkspace();
        this.rootPath = Platform.getLocation();
        this.dataSourceRegistry = new DataSourceRegistry(this, Platform.getExtensionRegistry());
        this.editorsRegistry = new EntityEditorsRegistry(this, Platform.getExtensionRegistry());
        this.metaModel = new DBMModel(dataSourceRegistry);
        this.queryManager = new QMControllerImpl(dataSourceRegistry);
        // Make default project
        defaultProject = this.workspace.getRoot().getProject(DEFAULT_PROJECT_NAME);

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
        this.dataSourceRegistry.dispose();

        // Unregister properties adapter
        Platform.getAdapterManager().unregisterAdapters(propertiesAdapter);

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

    public JexlEngine getJexlEngine() {
        return jexlEngine;
    }

    public DBMModel getMetaModel()
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

    public void saveGlobalPreferences()
    {
        //InstanceScope.getNode(plugin).flush();
        plugin.savePluginPreferences();
    }

    public DBeaverAdapterFactory getPropertiesAdapter()
    {
        return propertiesAdapter;
    }

    public void runAndWait(boolean fork, boolean cancelable, final DBRRunnableWithProgress runnable)
    {
        try {
            this.getWorkbench().getProgressService().run(fork, cancelable, new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    runnable.run(DBeaverUtils.makeMonitor(monitor));
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

    public void runAndWait2(boolean fork, boolean cancelable, final DBRRunnableWithProgress runnable)
        throws InvocationTargetException, InterruptedException
    {
        this.getWorkbench().getProgressService().run(fork, cancelable, new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                runnable.run(DBeaverUtils.makeMonitor(monitor));
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
                    return DBeaverUtils.makeExceptionStatus(e);
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
                    runnable.run(DBeaverUtils.makeMonitor(monitor));
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

    public void run(boolean fork, boolean cancelable, final IRunnableWithProgress runnable)
        throws InvocationTargetException, InterruptedException
    {
        this.getWorkbench().getProgressService().run(fork, cancelable, runnable);
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
        setDefaultPreferenceValue(store, PrefConstants.DEFAULT_AUTO_COMMIT, true);
        setDefaultPreferenceValue(store, PrefConstants.QUERY_ROLLBACK_ON_ERROR, true);

        // SQL execution
        setDefaultPreferenceValue(store, PrefConstants.SCRIPT_COMMIT_TYPE, SQLScriptCommitType.NO_COMMIT.name());
        setDefaultPreferenceValue(store, PrefConstants.SCRIPT_COMMIT_LINES, 1000);
        setDefaultPreferenceValue(store, PrefConstants.SCRIPT_ERROR_HANDLING, SQLScriptErrorHandling.STOP_ROLLBACK.name());
        setDefaultPreferenceValue(store, PrefConstants.SCRIPT_FETCH_RESULT_SETS, false);

        setDefaultPreferenceValue(store, PrefConstants.RESULT_SET_MAX_ROWS, 1000);
        setDefaultPreferenceValue(store, PrefConstants.STATEMENT_TIMEOUT, 10 * 1000);

        setDefaultPreferenceValue(store, PrefConstants.RS_EDIT_MAX_TEXT_SIZE, 10 * 1000000);
        setDefaultPreferenceValue(store, PrefConstants.RS_EDIT_LONG_AS_LOB, true);
        setDefaultPreferenceValue(store, PrefConstants.RS_COMMIT_ON_EDIT_APPLY, false);
        setDefaultPreferenceValue(store, PrefConstants.RS_COMMIT_ON_CONTENT_APPLY, false);
        
        setDefaultPreferenceValue(store, SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION, true);
        setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTO_ACTIVATION_DELAY, 500);
        setDefaultPreferenceValue(store, SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO, true);

        setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES, true);
        setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES, true);
        setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS, true);
        setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_COMMENTS, true);
        setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_BEGIN_END, true);
    }

    private static void setDefaultPreferenceValue(IPreferenceStore store, String name, Object value)
    {
        if (!store.contains(name)) {
            store.setValue(name, value.toString());
        }
        store.setDefault(name, value.toString());
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
