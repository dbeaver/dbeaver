/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.app.standalone;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.ide.ChooseWorkspaceDialog;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DesktopPlatform;
import org.jkiss.dbeaver.core.DesktopUI;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.rcp.IInstanceController;
import org.jkiss.dbeaver.model.rcp.RCPApplicationImpl;
import org.jkiss.dbeaver.model.rcp.RCPConfirmation;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.SWTBrowserRegistry;
import org.jkiss.dbeaver.registry.timezone.TimezoneRegistry;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.runtime.ui.console.ConsoleUserInterface;
import org.jkiss.dbeaver.ui.app.standalone.internal.WorkbenchPatcher;
import org.jkiss.dbeaver.ui.app.standalone.rpc.DBeaverInstanceServer;
import org.jkiss.dbeaver.ui.app.standalone.update.VersionUpdateDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This class controls all aspects of the application's execution
 */
public class DBeaverApplication extends RCPApplicationImpl {

    private static final Log log = Log.getLog(DBeaverApplication.class);

    public static final String APPLICATION_PLUGIN_ID = "org.jkiss.dbeaver.ui.app.standalone";

    //private static final String PROP_EXIT_DATA = IApplicationContext.EXIT_DATA_PROPERTY; //$NON-NLS-1$
    private static final String PROP_EXIT_CODE = "eclipse.exitcode"; //$NON-NLS-1$

    static boolean WORKSPACE_MIGRATED = false;

    private DBeaverInstanceServer instanceServer;

    private Display display = null;

    private long lastUserActivityTime = -1;

    public DBeaverApplication() {
    }

    protected DBeaverApplication(String defaultWorkspaceLocation, String defaultAppWorkspaceName, String defaultWorkspacesFile) {
        super(defaultWorkspaceLocation, defaultAppWorkspaceName, defaultWorkspacesFile);
    }

    public static DBeaverApplication getInstance() {
        return (DBeaverApplication) RCPApplicationImpl.getInstance();
    }

    @Override
    public long getLastUserActivityTime() {
        return lastUserActivityTime;
    }

    @NotNull
    @Override
    public DBPPreferenceStore getPreferenceStore() {
        return DBeaverActivator.getInstance().getPreferences();
    }

    @Override
    public Object start(IApplicationContext context) {
        super.start(context);

        // Initialize display early
        // It sets main windows name and images
        getDisplay();

        // https://github.com/eclipse-platform/eclipse.platform.swt/issues/772
        if (!RuntimeUtils.isMacOS() || !RuntimeUtils.isOSVersionAtLeast(14, 0, 0)) {
            // Update splash. Do it AFTER platform startup because platform may initiate some splash shell interactions
            updateSplashHandler();
        }

        if (RuntimeUtils.isWindows() && isStandalone()) {
            SWTBrowserRegistry.overrideBrowser();
        }

        DBWorkbench.getPlatform();

        WorkbenchPatcher.patchWorkbenchXmi(instanceLoc);
        initializeApplication();

        // Run instance server
        try {
            instanceServer = DBeaverInstanceServer.createServer();
        } catch (Exception e) {
            log.error("Can't start instance server: " + e.getMessage());
        }

        TimezoneRegistry.overrideTimezone();

        if (CommonUtils.isEmpty(System.getProperty(GeneralUtils.PROP_TRUST_STORE))
            && CommonUtils.isEmpty(System.getProperty(GeneralUtils.PROP_TRUST_STORE_TYPE))
        ) {
            DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
            if (RuntimeUtils.isWindows() && preferenceStore.getBoolean(ModelPreferences.PROP_USE_WIN_TRUST_STORE_TYPE)) {
                System.setProperty(GeneralUtils.PROP_TRUST_STORE_TYPE, GeneralUtils.VALUE_TRUST_STORE_TYPE_WINDOWS);
            }
        }

        // Prefs default
        PlatformUI.getPreferenceStore().setDefault(
            IWorkbenchPreferenceConstants.KEY_CONFIGURATION_ID,
            ApplicationWorkbenchAdvisor.DBEAVER_SCHEME_NAME);
        try {
            log.debug("Run workbench");
            getDisplay();
            int returnCode = PlatformUI.createAndRunWorkbench(display, createWorkbenchAdvisor());

            // Copy-pasted from IDEApplication
            // Magic with exit codes to let Eclipse starter switcg workspace

            // the workbench doesn't support relaunch yet (bug 61809) so
            // for now restart is used, and exit data properties are checked
            // here to substitute in the relaunch return code if needed
            if (returnCode != PlatformUI.RETURN_RESTART) {
                return EXIT_OK;
            }

            // if the exit code property has been set to the relaunch code, then
            // return that code now, otherwise this is a normal restart
            return EXIT_RELAUNCH.equals(Integer.getInteger(PROP_EXIT_CODE)) ? EXIT_RELAUNCH
                : EXIT_RESTART;

        } catch (Throwable e) {
            log.debug("Internal error in workbench lifecycle", e);
            return IApplication.EXIT_OK;
        } finally {
            shutdown();
/*
            try {
                Job.getJobManager().join(null, new NullProgressMonitor());
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
*/
            display.dispose();
            display = null;
        }
    }

    @Override
    protected boolean setCustomWorkspaceLocation(@NotNull Location instanceLoc) {
        if (instanceLoc.isSet()) {
            return false;
        }
        if (!isWorkspaceSwitchingAllowed()) {
            return false;
        }
        String lastWorkspace = DBeaverWorkspaces.fetchRecentWorkspaces(this, instanceLoc).getFirst();
        if (!WORKSPACE_DIR_CURRENT.equals(lastWorkspace)) {
            try {
                final URL selectedWorkspaceURL = new URL(
                    "file",  //$NON-NLS-1$
                    null,
                    lastWorkspace);
                instanceLoc.set(selectedWorkspaceURL, true);

                return true;
            } catch (Exception e) {
                log.debug("Can't set IDE workspace to '" + lastWorkspace + "'", e);
            }
        }
        return false;
    }

    @Override
    protected RCPConfirmation showActionConfirmation(@NotNull String title, @NotNull String message) {
        int msgResult = showMessageBox(
            title,
            message,
            SWT.ICON_WARNING | SWT.IGNORE | SWT.RETRY | SWT.ABORT);

        return switch (msgResult) {
            case SWT.ABORT -> RCPConfirmation.ABORT;
            case SWT.IGNORE -> RCPConfirmation.IGNORE;
            default -> RCPConfirmation.IGNORE;
        };
    }

    /**
     * Returns path to the {@code .workspaces} file.
     */
    @NotNull
    public Path getWorkspacesFile() {
        return FILE_WITH_WORKSPACES;
    }

    @NotNull
    public Path getDefaultWorkingFolder() {
        return Path.of(WORKSPACE_DIR_CURRENT);
    }

    @NotNull
    @Override
    public Class<? extends DBPPlatform> getPlatformClass() {
        return DesktopPlatform.class;
    }

    @Override
    public Class<? extends DBPPlatformUI> getPlatformUIClass() {
        return isHeadlessMode() ? ConsoleUserInterface.class : DesktopUI.class;
    }

    private void updateSplashHandler() {
        if (ArrayUtils.contains(Platform.getApplicationArgs(), "-nosplash")) {
            return;
        }
        try {
            // look and see if there's a splash shell we can parent off of
            Shell shell = WorkbenchPlugin.getSplashShell(display);
            if (shell != null) {
                // should set the icon and message for this shell to be the
                // same as the chooser dialog - this will be the guy that lives in
                // the task bar and without these calls you'd have the default icon
                // with no message.
                shell.setText(ChooseWorkspaceDialog.getWindowTitle());
                shell.setImages(Window.getDefaultImages());

                Log.Listener splashListener = (message, t) ->
                    DBeaverSplashHandler.showMessage(CommonUtils.toString(message));
                Log.addListener(splashListener);
                shell.addDisposeListener(e -> Log.removeListener(splashListener));
                DBeaverSplashHandler.showMessage("Starting " + Platform.getProduct().getName());
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            System.err.println("Error updating splash shell");
        }

    }

    /**
     * May be overrided in implementors
     */
    protected void initializeApplication() {

    }

    private Display getDisplay() {
        if (display == null) {
            log.debug("Create display");
            // Set display name at the very beginning (#609)
            // This doesn't initialize display - just sets default title
            Display.setAppName(GeneralUtils.getProductName());

            display = Display.getCurrent();
            if (display == null) {
                display = PlatformUI.createDisplay();
            }

            // Check for resource leaks
            Resource.setNonDisposeHandler(originStack -> log.warn("SWT resource leak detected", originStack));
            
            addIdleListeners();
        }
        return display;
    }

    private void addIdleListeners() {
        int [] events = {SWT.KeyDown, SWT.KeyUp, SWT.MouseDown, SWT.MouseMove, SWT.MouseUp, SWT.MouseWheel};
        Listener idleListener = event -> lastUserActivityTime = System.currentTimeMillis();
        for (int event : events) {
            display.addFilter(event, idleListener);
        }
    }

    @Override
    protected boolean tryMigrateFromPreviousVersion(Path homeDir) {
        Path previousVersionWorkspaceDir = null;
        for (String oldDir : WORKSPACE_DIR_PREVIOUS) {
            oldDir = GeneralUtils.replaceSystemPropertyVariables(oldDir);
            final Path oldWorkspaceDir = Path.of(oldDir);
            if (Files.exists(oldWorkspaceDir) &&
                Files.exists(GeneralUtils.getMetadataFolder(oldWorkspaceDir))) {
                previousVersionWorkspaceDir = oldWorkspaceDir;
                break;
            }
        }
        if (previousVersionWorkspaceDir != null) {
            DBeaverSettingsImporter importer = new DBeaverSettingsImporter(this, getDisplay());
            if (!importer.migrateFromPreviousVersion(previousVersionWorkspaceDir.toFile(), homeDir.toFile())) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    protected ApplicationWorkbenchAdvisor createWorkbenchAdvisor() {
        return new ApplicationWorkbenchAdvisor(this);
    }

    @Override
    public void stop() {
        final IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench == null)
            return;
        final Display display = workbench.getDisplay();
        display.syncExec(() -> {
            if (!display.isDisposed())
                workbench.close();
        });
    }

    @Nullable
    @Override
    public IInstanceController createInstanceClient(String instanceLoc) {
        return DBeaverInstanceServer.createClient(instanceLoc);
    }

    @NotNull
    @Override
    protected String getLogsLocation() {
        DBPPreferenceStore preferenceStore = DBeaverActivator.getInstance().getPreferences();
        if (!preferenceStore.getBoolean(DBeaverPreferences.LOGS_DEBUG_ENABLED)) {
            return super.getLogsLocation();
        }
        String logLocation = preferenceStore.getString(DBeaverPreferences.LOGS_DEBUG_LOCATION);
        if (CommonUtils.isEmpty(logLocation)) {
            logLocation = super.getLogsLocation();
        }
        return logLocation;
    }

    protected void shutdown() {
        Location location = Platform.getInstanceLocation();
        if (location.isSet()) {
            DBeaverWorkspaces.flushRecentWorkspaces(this, location);
        }

        try {
            DBeaverInstanceServer server = instanceServer;
            if (server != null) {
                instanceServer = null;
                RuntimeUtils.runTask(monitor -> server.stopInstanceServer(), "Stop instance server", 1000);
            }
        } catch (Throwable e) {
            log.error(e);
        }

        super.shutdown();
    }


    @Nullable
    @Override
    public IInstanceController getInstanceServer() {
        return instanceServer;
    }

    @Nullable
    public IInstanceController createInstanceClient() {
        return DBeaverInstanceServer.createClient(getDefaultInstanceLocation());
    }

    @Override
    public boolean isStandalone() {
        return true;
    }

    @Override
    public boolean isCommunity() {
        return true;
    }

    @Override
    public String getInfoDetails(DBRProgressMonitor monitor) {
        return null;
    }

    @Override
    public String getDefaultProjectName() {
        return "General";
    }

    private int showMessageBox(String title, String message, int style) {
        // Can't lock specified path
        Shell shell = new Shell(getDisplay(), SWT.ON_TOP);
        shell.setText(GeneralUtils.getProductTitle());
        MessageBox messageBox = new MessageBox(shell, style);
        messageBox.setText(title);
        messageBox.setMessage(message);
        int msgResult = messageBox.open();
        shell.dispose();
        return msgResult;
    }

    public void notifyVersionUpgrade(@NotNull Version currentVersion, @NotNull VersionDescriptor newVersion, boolean showSkip) {
        VersionUpdateDialog dialog = new VersionUpdateDialog(
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
            currentVersion,
            newVersion,
            showSkip);
        dialog.open();
    }

}
