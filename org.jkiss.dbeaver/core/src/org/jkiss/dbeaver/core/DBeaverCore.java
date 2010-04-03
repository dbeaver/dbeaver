package org.jkiss.dbeaver.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.model.DBPApplication;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.meta.DBMModel;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.EntityEditorsRegistry;
import org.jkiss.dbeaver.runtime.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.runtime.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * DBeaverCore
 */
public class DBeaverCore implements DBPApplication, DBRRunnableContext {

    static Log log = LogFactory.getLog(DBeaverCore.class);

    private static final String DEFAULT_PROJECT_NAME = "default";
    private static final String AUTOSAVE_DIR = "autosave";

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

    public synchronized void dispose(IProgressMonitor monitor)
    {
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

    public DBMModel getMetaModel()
    {
        return metaModel;
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

    public DBeaverAdapterFactory getPropertiesAdapter()
    {
        return propertiesAdapter;
    }

    public void run(boolean fork, boolean cancelable, final DBRRunnableWithProgress runnable)
        throws InvocationTargetException, InterruptedException
    {
        DBeaverUtils.run(this.getWorkbench().getProgressService(), fork, cancelable, runnable);
    }

    public IFolder getTempFolder(IProgressMonitor monitor)
    {
        IPath tempPath = defaultProject.getProjectRelativePath().append(AUTOSAVE_DIR);
        IFolder tempFolder = defaultProject.getFolder(tempPath);
        if (!tempFolder.exists()) {
            try {
                tempFolder.create(true, true, monitor);
            }
            catch (CoreException ex) {
                log.warn("Can't create temp directory '" + tempFolder.toString() + "'", ex);
                return null;
            }
        }
        return tempFolder;
    }

    public IFile makeTempFile(String name, String extension, IProgressMonitor monitor)
    {
        IFolder tempFolder = getTempFolder(monitor);
        if (tempFolder == null) {
            return null;
        }

        IFile tempFile = tempFolder.getFile(name + "-" + System.currentTimeMillis() + "." + extension);
        try {
            InputStream contents = new ByteArrayInputStream(new byte[0]);
            tempFile.create(contents, true, monitor);
        }
        catch (CoreException ex) {
            log.warn("Can't create temp file '" + tempFile.toString() + "'", ex);
            return null;
        }
        return tempFile;
    }

    private void initDefaultPreferences()
    {
        IPreferenceStore store = getGlobalPreferenceStore();

        // SQL execution
        setDefaultPreferenceValue(store, PrefConstants.SCRIPT_COMMIT_TYPE, SQLScriptCommitType.NO_COMMIT.name());
        setDefaultPreferenceValue(store, PrefConstants.SCRIPT_COMMIT_LINES, 1000);
        setDefaultPreferenceValue(store, PrefConstants.SCRIPT_ERROR_HANDLING, SQLScriptErrorHandling.STOP_ROLLBACK.name());
        setDefaultPreferenceValue(store, PrefConstants.SCRIPT_FETCH_RESULT_SETS, false);

        setDefaultPreferenceValue(store, PrefConstants.RESULT_SET_MAX_ROWS, 1000);
        setDefaultPreferenceValue(store, PrefConstants.DEFAULT_AUTO_COMMIT, true);
        setDefaultPreferenceValue(store, PrefConstants.STATEMENT_TIMEOUT, 10 * 1000);

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
