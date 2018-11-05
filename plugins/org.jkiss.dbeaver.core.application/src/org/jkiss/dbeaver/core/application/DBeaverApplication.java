/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.core.application;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.application.DelayedEventsProcessor;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.application.rpc.DBeaverInstanceServer;
import org.jkiss.dbeaver.core.application.rpc.IInstanceController;
import org.jkiss.dbeaver.model.app.DBASecureStorage;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.impl.app.DefaultSecureStorage;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.StandardConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

import java.io.*;
import java.net.URL;
import java.util.Properties;

/**
 * This class controls all aspects of the application's execution
 */
public class DBeaverApplication implements IApplication, DBPApplication {

    private static final Log log = Log.getLog(DBeaverApplication.class);

    public static final String APPLICATION_PLUGIN_ID = "org.jkiss.dbeaver.core.application";

    public static final String WORKSPACE_DIR_LEGACY = ".dbeaver"; //$NON-NLS-1$
    public static final String WORKSPACE_DIR_4 = ".dbeaver4"; //$NON-NLS-1$

    public static final String WORKSPACE_DIR_CURRENT = WORKSPACE_DIR_4;
    public static final String WORKSPACE_DIR_PREVIOUS[] = { WORKSPACE_DIR_LEGACY };

    static final String VERSION_PROP_PRODUCT_NAME = "product-name";
    static final String VERSION_PROP_PRODUCT_VERSION = "product-version";
    static boolean WORKSPACE_MIGRATED = false;

    static DBeaverApplication instance;
    boolean reuseWorkspace = false;

    private IInstanceController instanceServer;

    private OutputStream debugWriter;
    private PrintStream oldSystemOut;
    private PrintStream oldSystemErr;

    private Display display = null;

    static {
        // Explicitly set UTF-8 as default file encoding
        // In some places Eclipse reads this property directly.
        //System.setProperty(StandardConstants.ENV_FILE_ENCODING, GeneralUtils.UTF8_ENCODING);
    }

    /**
     * Gets singleton instance of DBeaver application
     * @return application or null if application wasn't started or was stopped.
     */
    public static DBeaverApplication getInstance() {
        return instance;
    }

    @Override
    public Object start(IApplicationContext context) {
        instance = this;

        // Set display name at the very beginning (#609)
        // This doesn't initialize display - just sets default title
        Display.setAppName(GeneralUtils.getProductName());

        Location instanceLoc = Platform.getInstanceLocation();
        if (!instanceLoc.isSet()) {
            if (!setDefaultWorkspacePath(instanceLoc)) {
                return IApplication.EXIT_OK;
            }
        }

        // Create display
        getDisplay();

        DelayedEventsProcessor processor = new DelayedEventsProcessor(display);

        // Add bundle load logger
        Bundle brandingBundle = context.getBrandingBundle();
        if (brandingBundle != null) {
            BundleContext bundleContext = brandingBundle.getBundleContext();
            if (bundleContext != null) {
                bundleContext.addBundleListener(new BundleLoadListener());
            }
        }
        Log.addListener((message, t) -> DBeaverSplashHandler.showMessage(CommonUtils.toString(message)));

        final Runtime runtime = Runtime.getRuntime();

        // Init Core plugin and mark it as standalone version

        DBeaverCore.setApplication(this);

        initDebugWriter();

        log.debug(GeneralUtils.getProductName() + " " + GeneralUtils.getProductVersion() + " is starting"); //$NON-NLS-1$
        log.debug("OS: " + System.getProperty(StandardConstants.ENV_OS_NAME) + " " + System.getProperty(StandardConstants.ENV_OS_VERSION) + " (" + System.getProperty(StandardConstants.ENV_OS_ARCH) + ")");
        log.debug("Java version: " + System.getProperty(StandardConstants.ENV_JAVA_VERSION) + " by " + System.getProperty(StandardConstants.ENV_JAVA_VENDOR) + " (" + System.getProperty(StandardConstants.ENV_JAVA_ARCH) + "bit)");
        log.debug("Install path: '" + SystemVariablesResolver.getInstallPath() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        log.debug("Instance path: '" + instanceLoc.getURL() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        log.debug("Memory available " + (runtime.totalMemory() / (1024 * 1024)) + "Mb/" + (runtime.maxMemory() / (1024 * 1024)) + "Mb");

        // Write version info
        writeWorkspaceInfo();

        // Run instance server
        instanceServer = DBeaverInstanceServer.startInstanceServer();

        // Prefs default
        PlatformUI.getPreferenceStore().setDefault(
            IWorkbenchPreferenceConstants.KEY_CONFIGURATION_ID,
            ApplicationWorkbenchAdvisor.DBEAVER_SCHEME_NAME);
        try {
            log.debug("Run workbench");
            int returnCode = PlatformUI.createAndRunWorkbench(display, createWorkbenchAdvisor());
            if (returnCode == PlatformUI.RETURN_RESTART) {
                return IApplication.EXIT_RESTART;
            }
            return IApplication.EXIT_OK;
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

    private Display getDisplay() {
        if (display == null) {
            log.debug("Create display");
            display = PlatformUI.createDisplay();
        }
        return display;
    }

    private boolean setDefaultWorkspacePath(Location instanceLoc) {
        String defaultHomePath = getDefaultWorkspaceLocation(WORKSPACE_DIR_CURRENT).getAbsolutePath();
        final File homeDir = new File(defaultHomePath);
        try {
            if (!homeDir.exists()) {
                File previousVersionWorkspaceDir = null;
                for (String oldDir : WORKSPACE_DIR_PREVIOUS) {
                    final File oldWorkspaceDir = new File(getDefaultWorkspaceLocation(oldDir).getAbsolutePath());
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
        if (DBeaverCommandLine.handleCommandLine(defaultHomePath)) {
            log.debug("Commands processed. Exit " + GeneralUtils.getProductName() + ".");
            return false;
        }
        try {
            // Make URL manually because file.toURI().toURL() produces bad path (with %20).
            final URL defaultHomeURL = new URL(
                "file",  //$NON-NLS-1$
                null,
                defaultHomePath);
            boolean keepTrying = true;
            while (keepTrying) {
                if (!instanceLoc.set(defaultHomeURL, true)) {
                    if (reuseWorkspace) {
                        instanceLoc.set(defaultHomeURL, false);
                        keepTrying = false;
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
        // Custom parameters
        DBeaverCommandLine.handleCustomParameters();

        return true;
    }

    public static void writeWorkspaceInfo() {
        final File metadataFolder = GeneralUtils.getMetadataFolder();
        Properties props = DBeaverCore.readWorkspaceInfo(metadataFolder);
        props.setProperty(VERSION_PROP_PRODUCT_NAME, GeneralUtils.getProductName());
        props.setProperty(VERSION_PROP_PRODUCT_VERSION, GeneralUtils.getProductVersion().toString());
        DBeaverCore.writeWorkspaceInfo(metadataFolder, props);
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
/*
            final IWorkbench workbench = PlatformUI.getWorkbench();
            if (workbench == null)
                return;
*/

            instanceServer = null;
            DBeaverInstanceServer.stopInstanceServer();
        } catch (Throwable e) {
            log.error(e);
        } finally {
            instance = null;
            stopDebugWriter();
        }
    }

    private void initDebugWriter() {
        DBPPreferenceStore preferenceStore = DBeaverCore.getGlobalPreferenceStore();
        if (!preferenceStore.getBoolean(DBeaverPreferences.LOGS_DEBUG_ENABLED)) {
            return;
        }
        String logLocation = preferenceStore.getString(DBeaverPreferences.LOGS_DEBUG_LOCATION);
        if (CommonUtils.isEmpty(logLocation)) {
            logLocation = new File(GeneralUtils.getMetadataFolder(), "dbeaver-debug.log").getAbsolutePath(); //$NON-NLS-1$
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

    IInstanceController getInstanceServer() {
        return instanceServer;
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

    @NotNull
    @Override
    public DBASecureStorage getSecureStorage() {
        return DefaultSecureStorage.INSTANCE;
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

    private static class BundleLoadListener implements BundleListener {
        @Override
        public void bundleChanged(BundleEvent event) {
            String message = null;

            if (event.getType() == BundleEvent.STARTED) {
                message = "> Start " + event.getBundle().getSymbolicName() + " [" + event.getBundle().getVersion() + "]";
            } else if (event.getType() == BundleEvent.STOPPED) {
                message = "< Stop " + event.getBundle().getSymbolicName() + " [" + event.getBundle().getVersion() + "]";
            }
            if (message != null) {
                log.debug(message);
            }
        }
    }

    private class ProxyPrintStream extends OutputStream {
        private final OutputStream debugWriter;
        private final OutputStream stdOut;

        ProxyPrintStream(OutputStream debugWriter, OutputStream stdOut) {
            this.debugWriter = debugWriter;
            this.stdOut = stdOut;
        }

        @Override
        public void write(int b) throws IOException {
            debugWriter.write(b);
            stdOut.write(b);
        }
    }

}
