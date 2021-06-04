/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.apache.commons.cli.CommandLine;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.ide.ChooseWorkspaceData;
import org.eclipse.ui.internal.ide.ChooseWorkspaceDialog;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.app.DBASecureStorage;
import org.jkiss.dbeaver.model.app.DBPApplicationController;
import org.jkiss.dbeaver.model.impl.app.DefaultSecureStorage;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.registry.BaseApplicationImpl;
import org.jkiss.dbeaver.registry.BaseWorkspaceImpl;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.standalone.rpc.DBeaverInstanceServer;
import org.jkiss.dbeaver.ui.app.standalone.rpc.IInstanceController;
import org.jkiss.dbeaver.ui.app.standalone.rpc.InstanceClient;
import org.jkiss.dbeaver.ui.app.standalone.update.VersionUpdateDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.StandardConstants;
import org.osgi.framework.Version;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class controls all aspects of the application's execution
 */
public class DBeaverApplication extends BaseApplicationImpl implements DBPApplicationController {

    private static final Log log = Log.getLog(DBeaverApplication.class);

    public static final String APPLICATION_PLUGIN_ID = "org.jkiss.dbeaver.ui.app.standalone";

    public static final String WORKSPACE_DIR_LEGACY = "${user.home}/.dbeaver"; //$NON-NLS-1$
    public static final String WORKSPACE_DIR_4 = "${user.home}/.dbeaver4"; //$NON-NLS-1$
    public static final String WORKSPACE_DIR_6; //$NON-NLS-1$

    private static final Path FILE_WITH_WORKSPACES;

    public static final String DBEAVER_DATA_DIR = "DBeaverData";

    public static final String WORKSPACE_DIR_CURRENT;

    public static final String[] WORKSPACE_DIR_PREVIOUS = {
        WORKSPACE_DIR_4,
        WORKSPACE_DIR_LEGACY};

    static final String VERSION_PROP_PRODUCT_NAME = "product-name";
    static final String VERSION_PROP_PRODUCT_VERSION = "product-version";

    private static final String PROP_EXIT_DATA = IApplicationContext.EXIT_DATA_PROPERTY; //$NON-NLS-1$
    private static final String PROP_EXIT_CODE = "eclipse.exitcode"; //$NON-NLS-1$

    static boolean WORKSPACE_MIGRATED = false;

    static DBeaverApplication instance;

    private boolean exclusiveMode = false;
    private boolean reuseWorkspace = false;
    private boolean primaryInstance = true;
    private boolean headlessMode = false;

    private IInstanceController instanceServer;

    private OutputStream debugWriter;
    private PrintStream oldSystemOut;
    private PrintStream oldSystemErr;

    private Display display = null;

    private boolean resetUIOnRestart, resetWorkspaceOnRestart;
    private long lastUserActivityTime = -1;

    static {
        // Explicitly set UTF-8 as default file encoding
        // In some places Eclipse reads this property directly.
        //System.setProperty(StandardConstants.ENV_FILE_ENCODING, GeneralUtils.UTF8_ENCODING);

        // Detect default workspace location
        // Since 6.1.3 it is different for different OSes
        // Windows: %AppData%/DBeaverData
        // MacOS: ~/Library/DBeaverData
        // Linux: $XDG_DATA_HOME/DBeaverData
        String osName = (System.getProperty("os.name")).toUpperCase();
        String workingDirectory;
        if (osName.contains("WIN")) {
            String appData = System.getenv("AppData");
            if (appData == null) {
                appData = System.getProperty("user.home");
            }
            workingDirectory = appData + "\\" + DBEAVER_DATA_DIR;
        } else if (osName.contains("MAC")) {
            workingDirectory = System.getProperty("user.home") + "/Library/" + DBEAVER_DATA_DIR;
        } else {
            // Linux
            String dataHome = System.getProperty("XDG_DATA_HOME");
            if (dataHome == null) {
                dataHome = System.getProperty("user.home") + "/.local/share";
            }
            String badWorkingDir = dataHome + "/." + DBEAVER_DATA_DIR;
            String goodWorkingDir = dataHome + "/" + DBEAVER_DATA_DIR;
            if (!new File(goodWorkingDir).exists() && new File(badWorkingDir).exists()) {
                // Let's use bad working dir if it exists (#6316)
                workingDirectory = badWorkingDir;
            } else {
                workingDirectory = goodWorkingDir;
            }
        }

        // Workspace dir
        WORKSPACE_DIR_6 = new File(workingDirectory, "workspace6").getAbsolutePath();
        WORKSPACE_DIR_CURRENT = WORKSPACE_DIR_6;
        FILE_WITH_WORKSPACES = Paths.get(workingDirectory, ".workspaces"); //$NON-NLS-1$
    }



    /**
     * Gets singleton instance of DBeaver application
     * @return application or null if application wasn't started or was stopped.
     */
    public static DBeaverApplication getInstance() {
        return instance;
    }

    @Override
    public long getLastUserActivityTime() {
        return lastUserActivityTime;
    }

    @Override
    public Object start(IApplicationContext context) {
        instance = this;

        Location instanceLoc = Platform.getInstanceLocation();

        CommandLine commandLine = DBeaverCommandLine.getCommandLine();
        {
            String defaultHomePath = getDefaultInstanceLocation();
            if (DBeaverCommandLine.handleCommandLine(commandLine, defaultHomePath)) {
                System.err.println("Commands processed. Exit " + GeneralUtils.getProductName() + ".");
                return IApplication.EXIT_OK;
            }
        }

        boolean ideWorkspaceSet = setIDEWorkspace(instanceLoc);

        {
            // Lock the workspace
            try {
                if (!instanceLoc.isSet()) {
                    if (!setDefaultWorkspacePath(instanceLoc)) {
                        return IApplication.EXIT_OK;
                    }
                } else if (instanceLoc.isLocked() && !ideWorkspaceSet && !isExclusiveMode()) {
                    // Check for locked workspace
                    if (!setDefaultWorkspacePath(instanceLoc)) {
                        return IApplication.EXIT_OK;
                    }
                }

                if (isExclusiveMode()) {
                    markLocationReadOnly(instanceLoc);
                } else {
                    // Lock the workspace
                    if (!instanceLoc.isLocked()) {
                        instanceLoc.lock();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Custom parameters
        try {
            headlessMode = true;
            if (DBeaverCommandLine.handleCustomParameters(commandLine)) {
                return IApplication.EXIT_OK;
            }
        } finally {
            headlessMode = false;
        }

        if (isExclusiveMode()) {
            // In shared mode we mustn't run UI
            return IApplication.EXIT_OK;
        }

        initDebugWriter();

        updateSplashHandler();

        final Runtime runtime = Runtime.getRuntime();

        // Init Core plugin and mark it as standalone version

        log.debug(GeneralUtils.getProductName() + " " + GeneralUtils.getProductVersion() + " is starting"); //$NON-NLS-1$
        log.debug("OS: " + System.getProperty(StandardConstants.ENV_OS_NAME) + " " + System.getProperty(StandardConstants.ENV_OS_VERSION) + " (" + System.getProperty(StandardConstants.ENV_OS_ARCH) + ")");
        log.debug("Java version: " + System.getProperty(StandardConstants.ENV_JAVA_VERSION) + " by " + System.getProperty(StandardConstants.ENV_JAVA_VENDOR) + " (" + System.getProperty(StandardConstants.ENV_JAVA_ARCH) + "bit)");
        log.debug("Install path: '" + SystemVariablesResolver.getInstallPath() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        log.debug("Instance path: '" + instanceLoc.getURL() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        log.debug("Memory available " + (runtime.totalMemory() / (1024 * 1024)) + "Mb/" + (runtime.maxMemory() / (1024 * 1024)) + "Mb");

        // Write version info
        writeWorkspaceInfo();

        initializeApplication();

        // Run instance server
        instanceServer = DBeaverInstanceServer.startInstanceServer(commandLine, createInstanceController());

        // Prefs default
        PlatformUI.getPreferenceStore().setDefault(
            IWorkbenchPreferenceConstants.KEY_CONFIGURATION_ID,
            ApplicationWorkbenchAdvisor.DBEAVER_SCHEME_NAME);
        try {
            log.debug("Run workbench");
            getDisplay();
            int returnCode = PlatformUI.createAndRunWorkbench(display, createWorkbenchAdvisor());

            if (resetUIOnRestart || resetWorkspaceOnRestart) {
                resetUISettings(instanceLoc);
            }
            if (resetWorkspaceOnRestart) {
                // FIXME: ???
            }

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

    private void markLocationReadOnly(Location instanceLoc) {
        try {
            Field isReadOnlyField = instanceLoc.getClass().getDeclaredField("isReadOnly");
            isReadOnlyField.setAccessible(true);
            isReadOnlyField.set(instanceLoc, true);
        } catch (Throwable e) {
            // ignore
            e.printStackTrace();
        }
    }

    private static boolean setIDEWorkspace(@NotNull Location instanceLoc) {
        if (instanceLoc.isSet()) {
            return false;
        }
        Collection<String> recentWorkspaces = getRecentWorkspaces(instanceLoc);
        if (recentWorkspaces.isEmpty()) {
            return false;
        }
        String lastWorkspace = recentWorkspaces.iterator().next();
        if (!CommonUtils.isEmpty(lastWorkspace) && !WORKSPACE_DIR_CURRENT.equals(lastWorkspace)) {
            try {
                final URL selectedWorkspaceURL = new URL(
                    "file",  //$NON-NLS-1$
                    null,
                    lastWorkspace);
                instanceLoc.set(selectedWorkspaceURL, true);

                return true;
            } catch (Exception e) {
                System.err.println("Can't set IDE workspace to '" + lastWorkspace + "'");
                e.printStackTrace();
            }
        }
        return false;
    }

    @NotNull
    private static Collection<String> getRecentWorkspaces(@NotNull Location instanceLoc) {
        ChooseWorkspaceData launchData = new ChooseWorkspaceData(instanceLoc.getDefault());
        String[] arrayOfRecentWorkspaces = launchData.getRecentWorkspaces();
        Collection<String> recentWorkspaces;
        int maxSize;
        if (arrayOfRecentWorkspaces == null) {
            maxSize = 0;
            recentWorkspaces = new ArrayList<>();
        } else {
            maxSize = arrayOfRecentWorkspaces.length;
            recentWorkspaces = new ArrayList<>(Arrays.asList(arrayOfRecentWorkspaces));
        }
        recentWorkspaces.removeIf(Objects::isNull);
        Collection<String> backedUpWorkspaces = getBackedUpWorkspaces();
        if (recentWorkspaces.equals(backedUpWorkspaces) && backedUpWorkspaces.contains(WORKSPACE_DIR_CURRENT)) {
            return backedUpWorkspaces;
        }

        List<String> workspaces = Stream.concat(recentWorkspaces.stream(), backedUpWorkspaces.stream())
            .distinct()
            .limit(maxSize)
            .collect(Collectors.toList());
        if (!recentWorkspaces.contains(WORKSPACE_DIR_CURRENT)) {
            if (recentWorkspaces.size() < maxSize) {
                recentWorkspaces.add(WORKSPACE_DIR_CURRENT);
            } else if (maxSize > 1) {
                workspaces.set(recentWorkspaces.size() - 1, WORKSPACE_DIR_CURRENT);
            }
        }
        launchData.setRecentWorkspaces(Arrays.copyOf(workspaces.toArray(new String[0]), maxSize));
        launchData.writePersistedData();
        saveWorkspacesToBackup(workspaces);
        return workspaces;
    }

    @NotNull
    private static Collection<String> getBackedUpWorkspaces() {
        if (!Files.exists(FILE_WITH_WORKSPACES)) {
            return Collections.emptyList();
        }
        try {
            return Files.readAllLines(FILE_WITH_WORKSPACES);
        } catch (IOException e) {
            System.err.println("Unable to read backed up workspaces"); //$NON-NLS-1$
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private static void saveWorkspacesToBackup(@NotNull Iterable<? extends CharSequence> workspaces) {
        try {
            Files.write(FILE_WITH_WORKSPACES, workspaces, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Unable to save backed up workspaces"); //$NON-NLS-1$
            e.printStackTrace();
        }
    }

    private String getDefaultInstanceLocation() {
        String defaultHomePath = WORKSPACE_DIR_CURRENT;
        Location instanceLoc = Platform.getInstanceLocation();
        if (instanceLoc.isSet()) {
            defaultHomePath = instanceLoc.getURL().getFile();
        }
        return defaultHomePath;
    }

    private void updateSplashHandler() {
        if (ArrayUtils.contains(Platform.getApplicationArgs(), "-nosplash")) {
            return;
        }
        try {
            getDisplay();

            // look and see if there's a splash shell we can parent off of
            Shell shell = WorkbenchPlugin.getSplashShell(display);
            if (shell != null) {
                // should set the icon and message for this shell to be the
                // same as the chooser dialog - this will be the guy that lives in
                // the task bar and without these calls you'd have the default icon
                // with no message.
                shell.setText(ChooseWorkspaceDialog.getWindowTitle());
                shell.setImages(Window.getDefaultImages());
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            System.err.println("Error updating splash shell");
        }

        Log.addListener((message, t) -> DBeaverSplashHandler.showMessage(CommonUtils.toString(message)));
    }

    protected IInstanceController createInstanceController() {
        return new DBeaverInstanceServer();
    }

    private void resetUISettings(Location instanceLoc) {
        try {
            File instanceDir = new File(instanceLoc.getURL().toURI());
            if (instanceDir.exists()) {
                File settingsFile = new File(instanceDir, ".metadata/.plugins/org.eclipse.e4.workbench/workbench.xmi");
                if (settingsFile.exists()) {
                    settingsFile.deleteOnExit();
                }
                //markFoldertoDelete(new File(instanceDir, ".metadata/.plugins/org.eclipse.core.resources/.root"));
                //markFoldertoDelete(new File(instanceDir, ".metadata/.plugins/org.eclipse.core.resources/.safetable"));
            }
        } catch (Throwable e) {
            log.error("Error resetting UI settings", e);
        }
    }

    private void markFoldertoDelete(File folder) {
        if (!folder.exists()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    markFoldertoDelete(file);
                } else {
                    log.debug("Delete resource file " + file.getAbsolutePath());
                    file.deleteOnExit();
                }
            }
        }
        folder.deleteOnExit();
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

    private boolean setDefaultWorkspacePath(Location instanceLoc) {
        String defaultHomePath = WORKSPACE_DIR_CURRENT;
        final File homeDir = new File(defaultHomePath);
        try {
            if (!homeDir.exists() || ArrayUtils.isEmpty(homeDir.listFiles())) {
                File previousVersionWorkspaceDir = null;
                for (String oldDir : WORKSPACE_DIR_PREVIOUS) {
                    oldDir = GeneralUtils.replaceSystemPropertyVariables(oldDir);
                    final File oldWorkspaceDir = new File(oldDir);
                    if (oldWorkspaceDir.exists() && GeneralUtils.getMetadataFolder(oldWorkspaceDir).exists()) {
                        previousVersionWorkspaceDir = oldWorkspaceDir;
                        break;
                    }
                }
                if (previousVersionWorkspaceDir != null) {
                    DBeaverSettingsImporter importer = new DBeaverSettingsImporter(this, getDisplay());
                    if (!importer.migrateFromPreviousVersion(previousVersionWorkspaceDir, homeDir)) {
                        return false;
                    }
                }
            }
        } catch (Throwable e) {
            log.error("Error migrating old workspace version", e);
        }
        try {
            // Make URL manually because file.toURI().toURL() produces bad path (with %20).
            final URL defaultHomeURL = new URL(
                "file",  //$NON-NLS-1$
                null,
                defaultHomePath);
            boolean keepTrying = true;
            while (keepTrying) {
                if (instanceLoc.isLocked() || !instanceLoc.set(defaultHomeURL, true)) {
                    if (exclusiveMode || reuseWorkspace) {
                        instanceLoc.set(defaultHomeURL, false);
                        keepTrying = false;
                        primaryInstance = false;
                    } else {
                        // Can't lock specified path
                        int msgResult = showMessageBox(
                            "DBeaver - Can't lock workspace",
                            "Can't lock workspace at " + defaultHomePath + ".\n" +
                                "It seems that you have another DBeaver instance running.\n" +
                                "You may ignore it and work without lock but it is recommended to shutdown previous instance otherwise you may corrupt workspace data.",
                            SWT.ICON_WARNING | SWT.IGNORE | SWT.RETRY | SWT.ABORT);

                        switch (msgResult) {
                            case SWT.ABORT:
                                return false;
                            case SWT.IGNORE:
                                instanceLoc.set(defaultHomeURL, false);
                                keepTrying = false;
                                primaryInstance = false;
                                break;
                            case SWT.RETRY:
                                break;
                        }
                    }
                } else {
                    break;
                }
            }

        } catch (Throwable e) {
            // Just skip it
            // Error may occur if -data parameter was specified at startup
            System.err.println("Can't switch workspace to '" + defaultHomePath + "' - " + e.getMessage());  //$NON-NLS-1$ //$NON-NLS-2$
        }

        return true;
    }

    public static void writeWorkspaceInfo() {
        final File metadataFolder = GeneralUtils.getMetadataFolder();
        Properties props = BaseWorkspaceImpl.readWorkspaceInfo(metadataFolder);
        props.setProperty(VERSION_PROP_PRODUCT_NAME, GeneralUtils.getProductName());
        props.setProperty(VERSION_PROP_PRODUCT_VERSION, GeneralUtils.getProductVersion().toString());
        BaseWorkspaceImpl.writeWorkspaceInfo(metadataFolder, props);
    }

    @NotNull
    protected ApplicationWorkbenchAdvisor createWorkbenchAdvisor() {
        return new ApplicationWorkbenchAdvisor();
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

    private void shutdown() {
        log.debug("DBeaver is stopping"); //$NON-NLS-1$
        try {
            instanceServer = null;
            RuntimeUtils.runTask(monitor -> {
                DBeaverInstanceServer.stopInstanceServer();
            }, "Stop RMI", 1000);
        } catch (Throwable e) {
            log.error(e);
        } finally {
            instance = null;

            log.debug("DBeaver shutdown completed"); //$NON-NLS-1$

            stopDebugWriter();
        }
    }

    private void initDebugWriter() {
        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        if (!preferenceStore.getBoolean(DBeaverPreferences.LOGS_DEBUG_ENABLED)) {
            return;
        }
        String logLocation = preferenceStore.getString(DBeaverPreferences.LOGS_DEBUG_LOCATION);
        if (CommonUtils.isEmpty(logLocation)) {
            logLocation = new File(GeneralUtils.getMetadataFolder(), DBConstants.DEBUG_LOG_FILE_NAME).getAbsolutePath(); //$NON-NLS-1$
        }
        logLocation = GeneralUtils.replaceVariables(logLocation, new SystemVariablesResolver());
        File debugLogFile = new File(logLocation);
        if (debugLogFile.exists()) {
            if (!debugLogFile.delete()) {
                System.err.println("Can't delete debug log file"); //$NON-NLS-1$
                return;
            }
        }
        try {
            debugWriter = new FileOutputStream(debugLogFile);
            oldSystemOut = System.out;
            oldSystemErr = System.err;
            System.setOut(new PrintStream(new ProxyPrintStream(debugWriter, oldSystemOut)));
            System.setErr(new PrintStream(new ProxyPrintStream(debugWriter, oldSystemErr)));
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    private void stopDebugWriter() {
        if (oldSystemOut != null) System.setOut(oldSystemOut);
        if (oldSystemErr != null) System.setErr(oldSystemErr);

        if (debugWriter != null) {
            IOUtils.close(debugWriter);
            debugWriter = null;
        }
    }

    public IInstanceController getInstanceServer() {
        return instanceServer;
    }

    public IInstanceController createInstanceClient() {
        return InstanceClient.createClient(getDefaultInstanceLocation());
    }

    private static File getDefaultWorkspaceLocation(String path) {
        return new File(
            System.getProperty(StandardConstants.ENV_USER_HOME),
            path);
    }

    @Override
    public boolean isStandalone() {
        return true;
    }

    @Override
    public boolean isPrimaryInstance() {
        return primaryInstance;
    }

    @Override
    public boolean isHeadlessMode() {
        return headlessMode;
    }

    @Override
    public boolean isExclusiveMode() {
        return exclusiveMode;
    }

    public void setExclusiveMode(boolean exclusiveMode) {
        this.exclusiveMode = exclusiveMode;
    }

    public boolean isReuseWorkspace() {
        return reuseWorkspace;
    }

    public void setReuseWorkspace(boolean reuseWorkspace) {
        this.reuseWorkspace = reuseWorkspace;
    }

    @Override
    public void setHeadlessMode(boolean headlessMode) {
        this.headlessMode = headlessMode;
    }

    @NotNull
    @Override
    public DBASecureStorage getSecureStorage() {
        return DefaultSecureStorage.INSTANCE;
    }

    @Override
    public String getInfoDetails() {
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
            UIUtils.getActiveWorkbenchShell(),
            currentVersion,
            newVersion,
            showSkip);
        dialog.open();
    }

    public void setResetUIOnRestart(boolean resetUIOnRestart) {
        this.resetUIOnRestart = resetUIOnRestart;
    }

    public void setResetWorkspaceOnRestart(boolean resetWorkspaceOnRestart) {
        this.resetWorkspaceOnRestart = resetWorkspaceOnRestart;
    }

    private class ProxyPrintStream extends OutputStream {
        private final OutputStream debugWriter;
        private final OutputStream stdOut;

        ProxyPrintStream(OutputStream debugWriter, OutputStream stdOut) {
            this.debugWriter = debugWriter;
            this.stdOut = stdOut;
        }

        @Override
        public void write(@NotNull byte[] b) throws IOException {
            debugWriter.write(b);
            stdOut.write(b);
        }

        @Override
        public void write(@NotNull byte[] b, int off, int len) throws IOException {
            debugWriter.write(b, off, len);
            stdOut.write(b, off, len);
        }

        @Override
        public void write(int b) throws IOException {
            debugWriter.write(b);
            stdOut.write(b);
        }

        @Override
        public void flush() throws IOException {
            debugWriter.flush();
            stdOut.flush();
        }

    }
}
