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
package org.jkiss.dbeaver.model.rcp;

import org.apache.commons.cli.CommandLine;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.datalocation.Location;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.LogOutputStream;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.model.cli.CmdProcessResult;
import org.jkiss.dbeaver.model.impl.app.BaseApplicationImpl;
import org.jkiss.dbeaver.model.impl.app.BaseWorkspaceImpl;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.StandardConstants;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

/**
 * DesktopApplication.
 *
 * Supposed to be run on a desktop machine as UI or CLI applications.
 * Not supposed to be used in server-side apps.
 */
public abstract class RCPApplicationImpl extends BaseApplicationImpl implements DBPApplicationDesktop, DBPApplicationController {

    private static final Log log = Log.getLog(RCPApplicationImpl.class);

    public static final String WORKSPACE_DIR_LEGACY = "${user.home}/.dbeaver"; //$NON-NLS-1$
    public static final String WORKSPACE_DIR_4 = "${user.home}/.dbeaver4"; //$NON-NLS-1$

    public static final String[] WORKSPACE_DIR_PREVIOUS = {
        WORKSPACE_DIR_4,
        WORKSPACE_DIR_LEGACY};

    static final String VERSION_PROP_PRODUCT_NAME = "product-name";
    public static final String VERSION_PROP_PRODUCT_VERSION = "product-version";

    public static final String DEFAULT_WORKSPACE_FOLDER = "workspace6";
    public static final String DEFAULT_WORKSPACES_FILE = ".workspaces";

    private static final String PLUGINS_FOLDER = ".plugins";
    private static final String CORE_RESOURCES_PLUGIN_FOLDER = "org.eclipse.core.resources";

    private static final String STARTUP_ACTIONS_FILE = "dbeaver-startup-actions.properties";
    private static final String RESET_USER_PREFERENCES = "reset_user_preferences";
    private static final String RESET_WORKSPACE_CONFIGURATION = "reset_workspace_configuration";

    protected final Path FILE_WITH_WORKSPACES;
    protected final String WORKSPACE_DIR_CURRENT;

    static RCPApplicationImpl instance;

    private OutputStream debugWriter;
    private PrintStream oldSystemOut;
    private PrintStream oldSystemErr;

    private boolean headlessMode = false;
    private boolean exclusiveMode = false;
    private boolean reuseWorkspace = false;
    private boolean primaryInstance = true;

    private boolean resetUserPreferencesOnRestart, resetWorkspaceConfigurationOnRestart;
    protected Location instanceLoc;

    public RCPApplicationImpl() {
        this(RCPConstants.DBEAVER_DATA_DIR, DEFAULT_WORKSPACE_FOLDER, DEFAULT_WORKSPACES_FILE);
    }

    protected RCPApplicationImpl(String defaultWorkspaceLocation, String defaultAppWorkspaceName, String defaultWorkspacesFile) {

        // Explicitly set UTF-8 as default file encoding
        // In some places Eclipse reads this property directly.
        //System.setProperty(StandardConstants.ENV_FILE_ENCODING, GeneralUtils.UTF8_ENCODING);

        // Detect default workspace location
        // Since 6.1.3 it is different for different OSes
        // Windows: %AppData%/DBeaverData
        // MacOS: ~/Library/DBeaverData
        // Linux: $XDG_DATA_HOME/DBeaverData
        String workingDirectory = RuntimeUtils.getWorkingDirectory(defaultWorkspaceLocation);

        // Workspace dir
        WORKSPACE_DIR_CURRENT = new File(workingDirectory, defaultAppWorkspaceName).getAbsolutePath();
        FILE_WITH_WORKSPACES = Paths.get(workingDirectory, defaultWorkspacesFile); //$NON-NLS-1$
    }

    /**
     * Gets singleton instance of DBeaver application
     * @return application or null if application wasn't started or was stopped.
     */
    public static RCPApplicationImpl getInstance() {
        return instance;
    }

    @Override
    public Object start(IApplicationContext context) {
        instance = this;

        instanceLoc = Platform.getInstanceLocation();

        CommandLine commandLine = DBeaverCommandLine.getInstance().getCommandLine();
        String defaultHomePath = getDefaultInstanceLocation();
        if (DBeaverCommandLine.getInstance()
            .handleCommandLineAsClient(commandLine, defaultHomePath)
            .getPostAction() == CmdProcessResult.PostAction.SHUTDOWN
        ) {
            if (!Log.isQuietMode()) {
                System.err.println("Commands processed. Exit " + GeneralUtils.getProductName() + ".");
            }
            return IApplication.EXIT_OK;
        }


        if (!isWorkspaceSwitchingAllowed() && !WORKSPACE_DIR_CURRENT.equals(defaultHomePath)) {
            log.error("Workspace switching is not allowed when participating in the early access program. Exiting "
                + GeneralUtils.getProductName() + ".");
            return IApplication.EXIT_OK;
        }

        boolean ideWorkspaceSet = setCustomWorkspaceLocation(instanceLoc);

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

        loadStartupActions(instanceLoc);

        // Register core components
        initializeApplicationServices();

        // Custom parameters
        try {
            headlessMode = true;
            CmdProcessResult cmdProcessResult = DBeaverCommandLine.getInstance().handleCustomParameters(commandLine);
            if (cmdProcessResult.getPostAction() == CmdProcessResult.PostAction.SHUTDOWN) {
                return IApplication.EXIT_OK;
            }
        } finally {
            headlessMode = false;
        }

        if (isExclusiveMode()) {
            // In shared mode we mustn't run UI
            return IApplication.EXIT_OK;
        }

        final Runtime runtime = Runtime.getRuntime();

        initializeConfiguration();

        // Debug logger
        initDebugWriter();

        log.debug(GeneralUtils.getProductName() + " " + GeneralUtils.getProductVersion() + " is starting"); //$NON-NLS-1$
        log.debug("OS: " + System.getProperty(StandardConstants.ENV_OS_NAME) + " " + System.getProperty(StandardConstants.ENV_OS_VERSION) + " (" + System.getProperty(StandardConstants.ENV_OS_ARCH) + ")");
        log.debug("Java version: " + System.getProperty(StandardConstants.ENV_JAVA_VERSION) + " by " + System.getProperty(StandardConstants.ENV_JAVA_VENDOR) + " (" + System.getProperty(StandardConstants.ENV_JAVA_ARCH) + "bit)");
        log.debug("Install path: '" + SystemVariablesResolver.getInstallPath() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        log.debug("Instance path: '" + instanceLoc.getURL() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        log.debug("Memory available " + (runtime.totalMemory() / (1024 * 1024)) + "Mb/" + (runtime.maxMemory() / (1024 * 1024)) + "Mb");

        // Write version info
        writeWorkspaceInfo();

        return EXIT_OK;
    }

    @Override
    public void stop() {
        instance = null;
    }

    @Nullable
    public IInstanceController createInstanceClient(String instanceLoc) {
        return null;
    }

    protected void shutdown() {
        log.debug("DBeaver is stopping"); //$NON-NLS-1$

        saveStartupActions();

        log.debug("DBeaver shutdown completed"); //$NON-NLS-1$

        stopDebugWriter();
    }

    protected void initializeConfiguration() {
        ModelPreferences.IPType stack = ModelPreferences.IPType.getPreferredStack();
        if (stack != ModelPreferences.IPType.AUTO) {
            System.setProperty("java.net.preferIPv4Stack", String.valueOf(stack == ModelPreferences.IPType.IPV4));
        }
        ModelPreferences.IPType address = ModelPreferences.IPType.getPreferredAddresses();
        if (address != ModelPreferences.IPType.AUTO) {
            System.setProperty("java.net.preferIPv6Addresses", String.valueOf(address == ModelPreferences.IPType.IPV6));
        }
        boolean debugNetworkConnections = ModelPreferences.getPreferences().getBoolean(ModelPreferences.PROP_DEBUG_NETWORK_CONNECTIONS);
        if (debugNetworkConnections) {
            System.setProperty("javax.net.debug", "all");
        }
    }

    @NotNull
    @Override
    public DBPWorkspaceDesktop createWorkspace(@NotNull DBPPlatform platform) {
        return new DesktopWorkspaceImpl(platform, loadEclipseWorkspace());
    }

    @NotNull
    protected IWorkspace loadEclipseWorkspace() {
        return ResourcesPlugin.getWorkspace();
    }

    @Override
    public boolean isEnvironmentVariablesAccessible() {
        return true;
    }

    @Override
    public boolean isHeadlessMode() {
        return headlessMode;
    }

    public void setHeadlessMode(boolean headlessMode) {
        this.headlessMode = headlessMode;
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
    public boolean isPrimaryInstance() {
        return primaryInstance && !isHeadlessMode();
    }

    private static boolean isEmptyFolder(Path path) throws IOException {
        try (Stream<Path> list = Files.list(path)) {
            return list.findAny().isEmpty();
        }
    }

    public String getDefaultInstanceLocation() {
        String defaultHomePath = WORKSPACE_DIR_CURRENT;
        Location instanceLoc = Platform.getInstanceLocation();
        if (instanceLoc.isSet()) {
            try {
                defaultHomePath = RuntimeUtils.getLocalFileFromURL(instanceLoc.getURL()).getAbsolutePath();
            } catch (IOException e) {
                System.err.println("Unable to resolve workspace location " + instanceLoc);
                e.printStackTrace();
            }
        }
        return defaultHomePath;
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

    protected boolean setCustomWorkspaceLocation(@NotNull Location instanceLoc) {
        return false;
    }

    private boolean setDefaultWorkspacePath(Location instanceLoc) {
        String defaultHomePath = WORKSPACE_DIR_CURRENT;
        final Path homeDir = Path.of(defaultHomePath);
        try {
            if (!Files.exists(homeDir) || isEmptyFolder(homeDir)) {
                if (!tryMigrateFromPreviousVersion(homeDir)) {
                    return false;
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
                        RCPConfirmation confirmation = showActionConfirmation(
                            "DBeaver - Can't lock workspace",
                            "Can't lock workspace at " + defaultHomePath + ".\n" +
                                "It seems that you have another DBeaver instance running.\n" +
                                "You may ignore it and work without lock but it is recommended to\n"
                                + "shutdown previous instance otherwise you may corrupt workspace data.");
                        // Can't lock specified path
                        switch (confirmation) {
                            case ABORT:
                                return false;
                            case IGNORE:
                                instanceLoc.set(defaultHomeURL, false);
                                keepTrying = false;
                                primaryInstance = false;
                                break;
                            case RETRY:
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

    protected RCPConfirmation showActionConfirmation(@NotNull String title, @NotNull String message) {
        return RCPConfirmation.IGNORE;
    }

    protected boolean tryMigrateFromPreviousVersion(Path homeDir) {
        return true;
    }

    protected void writeWorkspaceInfo() {
        Path defaultDir = getDefaultWorkingFolder();
        Path metadataFolder;
        if (defaultDir != null) {
            metadataFolder = defaultDir.resolve(DBPWorkspace.METADATA_FOLDER);
            if (!Files.exists(metadataFolder)) {
                try {
                    Files.createDirectories(metadataFolder);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Error creating metadata folder: " + metadataFolder);
                }
            }
        } else {
            metadataFolder = GeneralUtils.getMetadataFolder();
        }
        Properties props = BaseWorkspaceImpl.readWorkspaceInfo(metadataFolder);
        props.setProperty(VERSION_PROP_PRODUCT_NAME, GeneralUtils.getProductName());
        props.setProperty(VERSION_PROP_PRODUCT_VERSION, GeneralUtils.getProductVersion().toString());
        BaseWorkspaceImpl.writeWorkspaceInfo(metadataFolder, props);
    }

    @NotNull
    protected String getLogsLocation() {
        return GeneralUtils.getMetadataFolder().resolve(DBConstants.DEBUG_LOG_FILE_NAME).toAbsolutePath().toString();
    }

    protected void initDebugWriter() {
        String logLocation = getLogsLocation();

        logLocation = GeneralUtils.replaceVariables(logLocation, new SystemVariablesResolver());
        File debugLogFile = new File(logLocation);
        try {
            debugWriter = new LogOutputStream(debugLogFile);
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

    public void setResetUserPreferencesOnRestart(boolean resetUserPreferencesOnRestart) {
        this.resetUserPreferencesOnRestart = resetUserPreferencesOnRestart;
    }

    public void setResetWorkspaceConfigurationOnRestart(boolean resetWorkspaceConfigurationOnRestart) {
        this.resetWorkspaceConfigurationOnRestart = resetWorkspaceConfigurationOnRestart;
    }

    private void saveStartupActions() {
        final Properties props = new Properties();

        if (resetWorkspaceConfigurationOnRestart) {
            props.setProperty(RESET_WORKSPACE_CONFIGURATION, Boolean.TRUE.toString());
        }

        if (resetUserPreferencesOnRestart) {
            props.setProperty(RESET_USER_PREFERENCES, Boolean.TRUE.toString());
        }
        if (!props.isEmpty()) {
            Path path = GeneralUtils.getMetadataFolder().resolve(STARTUP_ACTIONS_FILE);
            try (Writer writer = Files.newBufferedWriter(path)) {
                props.store(writer, "DBeaver startup actions");
            } catch (Exception e) {
                log.error("Unable to save startup actions", e);
            }
        }
    }

    protected void loadStartupActions(@NotNull Location instanceLoc) {
        Path instancePath;
        Path actionsPath;

        try {
            instancePath = RuntimeUtils.getLocalPathFromURL(instanceLoc.getURL()).resolve(DBPWorkspace.METADATA_FOLDER);
            actionsPath = instancePath.resolve(STARTUP_ACTIONS_FILE);
        } catch (Exception e) {
            return;
        }

        if (Files.notExists(actionsPath)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(actionsPath)) {
            final Properties properties = new Properties();
            properties.load(reader);

            if (!properties.isEmpty()) {
                processStartupActions(instancePath, properties.stringPropertyNames());
            }
        } catch (Exception e) {
            log.error("Unable to read startup actions", e);
        } finally {
            try {
                Files.delete(actionsPath);
            } catch (IOException e) {
                log.error("Unable to delete startup actions file: " + e.getMessage());
            }
        }
    }

    private void processStartupActions(
        @NotNull Path instancePath,
        @NotNull Set<String> actions
    ) throws Exception {
        final boolean resetUserPreferences = actions.contains(RESET_USER_PREFERENCES);
        final boolean resetWorkspaceConfiguration = actions.contains(RESET_WORKSPACE_CONFIGURATION);

        if (!resetUserPreferences && !resetWorkspaceConfiguration) {
            return;
        }
        Path path = instancePath.resolve(PLUGINS_FOLDER);
        if (Files.notExists(path) || !Files.isDirectory(path)) {
            return;
        }

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                log.trace("Deleting " + file);

                try {
                    Files.delete(file);
                } catch (IOException e) {
                    log.trace("Unable to delete " + file + ":" + e.getMessage());
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (dir.endsWith(PLUGINS_FOLDER)) {
                    return FileVisitResult.CONTINUE;
                }

                final Path relative = path.relativize(dir);

                if (resetUserPreferences && !relative.startsWith(CORE_RESOURCES_PLUGIN_FOLDER)) {
                    return FileVisitResult.CONTINUE;
                }

                if (resetWorkspaceConfiguration && relative.startsWith(CORE_RESOURCES_PLUGIN_FOLDER)) {
                    return FileVisitResult.CONTINUE;
                }

                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                log.trace("Deleting " + dir);

                try {
                    Files.delete(dir);
                } catch (IOException e) {
                    log.trace("Unable to delete " + dir + ":" + e.getMessage());
                }

                return FileVisitResult.CONTINUE;
            }
        });
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
