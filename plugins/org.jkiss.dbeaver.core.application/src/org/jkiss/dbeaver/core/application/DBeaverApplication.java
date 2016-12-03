/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.core.application;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.eclipse.core.resources.ResourcesPlugin;
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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.core.application.rpc.DBeaverInstanceServer;
import org.jkiss.dbeaver.core.application.rpc.IInstanceController;
import org.jkiss.dbeaver.core.application.rpc.InstanceClient;
import org.jkiss.dbeaver.model.app.DBASecureStorage;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.impl.app.DefaultSecureStorage;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.StandardConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

import java.io.*;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This class controls all aspects of the application's execution
 */
public class DBeaverApplication implements IApplication, DBPApplication {

    public static final String APPLICATION_PLUGIN_ID = "org.jkiss.dbeaver.core.application";
    public static final String DBEAVER_DEFAULT_DIR = ".dbeaver"; //$NON-NLS-1$

    private static final Log log = Log.getLog(DBeaverApplication.class);

    private static DBeaverApplication instance;
    private IInstanceController instanceServer;

    private OutputStream debugWriter;
    private PrintStream oldSystemOut;
    private PrintStream oldSystemErr;

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
        Display display = null;

        Location instanceLoc = Platform.getInstanceLocation();
        //log.debug("Default instance location: " + instanceLoc.getDefault());
        String defaultHomePath = getDefaultWorkspaceLocation().getAbsolutePath();
        try {
            URL defaultHomeURL = new URL(
                "file",  //$NON-NLS-1$
                null,
                defaultHomePath);
            boolean keepTrying = true;
            while (keepTrying) {
                if (!instanceLoc.set(defaultHomeURL, true)) {
                    if (handleCommandLine(defaultHomePath)) {
                        return IApplication.EXIT_OK;
                    }
                    // Can't lock specified path
                    if (display == null) {
                        display = PlatformUI.createDisplay();
                    }

                    Shell shell = new Shell(display, SWT.ON_TOP);
                    MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.IGNORE | SWT.RETRY | SWT.ABORT);
                    messageBox.setText("DBeaver - Can't lock workspace");
                    messageBox.setMessage("Can't lock workspace at " + defaultHomePath + ".\n" +
                        "It seems that you have another DBeaver instance running.\n" +
                        "You may ignore it and work without lock but it is recommended to shutdown previous instance otherwise you may corrupt workspace data.");
                    int msgResult = messageBox.open();
                    shell.dispose();

                    switch (msgResult) {
                        case SWT.ABORT:
                            return IApplication.EXIT_OK;
                        case SWT.IGNORE:
                            instanceLoc.set(defaultHomeURL, false);
                            keepTrying = false;
                            break;
                        case SWT.RETRY:
                            break;
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

        Bundle brandingBundle = context.getBrandingBundle();
        if (brandingBundle != null) {
            BundleContext bundleContext = brandingBundle.getBundleContext();
            if (bundleContext != null) {
                bundleContext.addBundleListener(new BundleListener() {
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
                });
            }
        }
        Log.addListener(new Log.Listener() {
            @Override
            public void loggedMessage(Object message, Throwable t) {
                DBeaverSplashHandler.showMessage(CommonUtils.toString(message));
            }
        });

        final Runtime runtime = Runtime.getRuntime();

        // Init Core plugin and mark it as standalone version
        DBeaverCore.setApplication(this);

        initDebugWriter();

        log.debug(DBeaverCore.getProductTitle() + " is starting"); //$NON-NLS-1$
        log.debug("Install path: '" + SystemVariablesResolver.getInstallPath() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        log.debug("Instance path: '" + instanceLoc.getURL() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        log.debug("Memory available " + (runtime.totalMemory() / (1024 * 1024)) + "Mb/" + (runtime.maxMemory() / (1024 * 1024)) + "Mb");

        // Run instance server
        instanceServer = DBeaverInstanceServer.startInstanceServer();

        // Set default resource encoding to UTF-8
        String defEncoding = DBeaverCore.getGlobalPreferenceStore().getString(DBeaverPreferences.DEFAULT_RESOURCE_ENCODING);
        if (CommonUtils.isEmpty(defEncoding)) {
            defEncoding = GeneralUtils.UTF8_ENCODING;
        }
        ResourcesPlugin.getPlugin().getPluginPreferences().setValue(ResourcesPlugin.PREF_ENCODING, defEncoding);

        // Create display
        if (display == null) {
            log.debug("Initialize display");
            display = PlatformUI.createDisplay();
        }

        // Prefs default
        PlatformUI.getPreferenceStore().setDefault(
            IWorkbenchPreferenceConstants.KEY_CONFIGURATION_ID,
            ApplicationWorkbenchAdvisor.DBEAVER_SCHEME_NAME);
        try {
            int returnCode = PlatformUI.createAndRunWorkbench(display, createWorkbenchAdvisor());
            if (returnCode == PlatformUI.RETURN_RESTART) {
                return IApplication.EXIT_RESTART;
            }
            return IApplication.EXIT_OK;
        } finally {
/*
            try {
                Job.getJobManager().join(null, new NullProgressMonitor());
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
*/
            display.dispose();
        }
    }

    @NotNull
    protected ApplicationWorkbenchAdvisor createWorkbenchAdvisor() {
        return new ApplicationWorkbenchAdvisor();
    }

    private boolean handleCommandLine(String instanceLoc) {
        CommandLine commandLine = getCommandLine();
        if (commandLine == null) {
            return false;
        }
        if (commandLine.hasOption(DBeaverCommandLine.PARAM_HELP)) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.setWidth(120);
            helpFormatter.setOptionComparator(new Comparator<Option>() {
                @Override
                public int compare(Option o1, Option o2) {
                    return 0;
                }
            });
            helpFormatter.printHelp("dbeaver", DBeaverCore.getProductTitle(), DBeaverCommandLine.ALL_OPTIONS, "(C) 2016 JKISS", true);
            return true;
        }

        try {
            IInstanceController controller = InstanceClient.createClient(instanceLoc);
            if (controller == null) {
                return false;
            }

            return executeCommandLineCommands(commandLine, controller);
        } catch (RemoteException e) {
            log.error("Error calling remote server", e);
            return true;
        } catch (Throwable e) {
            log.error("Internal error while calling remote server", e);
            return false;
        }
    }

    @Override
    public void stop() {
        log.debug("DBeaver is stopping"); //$NON-NLS-1$
        try {
            final IWorkbench workbench = PlatformUI.getWorkbench();
            if (workbench == null)
                return;

            instanceServer = null;
            DBeaverInstanceServer.stopInstanceServer();

            final Display display = workbench.getDisplay();
            DBeaverUI.syncExec(new Runnable() {
                @Override
                public void run() {
                    if (!display.isDisposed())
                        workbench.close();
                }
            });

        } catch (Throwable e) {
            log.error(e);
        } finally {
            instance = null;
            stopDebugWriter();
        }
    }

    private void initDebugWriter() {
        File logPath = GeneralUtils.getMetadataFolder();
        File debugLogFile = new File(logPath, "dbeaver-debug.log"); //$NON-NLS-1$
        if (debugLogFile.exists()) {
            if (!debugLogFile.delete()) {
                System.err.println("Can't delete debug log file"); //$NON-NLS-1$
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

    public static boolean executeCommandLineCommands(CommandLine commandLine, IInstanceController controller) throws Exception {
        if (commandLine == null) {
            return false;
        }
        String[] files = commandLine.getOptionValues(DBeaverCommandLine.PARAM_FILE);
        String[] fileArgs = commandLine.getArgs();
        if (!ArrayUtils.isEmpty(files) || !ArrayUtils.isEmpty(fileArgs)) {
            List<String> fileNames = new ArrayList<>();
            if (!ArrayUtils.isEmpty(files)) {
                Collections.addAll(fileNames, files);
            }
            if (!ArrayUtils.isEmpty(fileArgs)) {
                Collections.addAll(fileNames, fileArgs);
            }
            controller.openExternalFiles(fileNames.toArray(new String[fileNames.size()]));
            return true;
        }
        if (commandLine.hasOption(DBeaverCommandLine.PARAM_STOP)) {
            controller.quit();
            return true;
        }
        if (commandLine.hasOption(DBeaverCommandLine.PARAM_THREAD_DUMP)) {
            String threadDump = controller.getThreadDump();
            System.out.println(threadDump);
            return true;
        }
        return false;
    }

    public IInstanceController getInstanceServer() {
        return instanceServer;
    }

    private static File getDefaultWorkspaceLocation() {
        return new File(
            System.getProperty(StandardConstants.ENV_USER_HOME),
            DBEAVER_DEFAULT_DIR);
    }

    public static CommandLine getCommandLine() {
        try {
            return new DefaultParser().parse(DBeaverCommandLine.ALL_OPTIONS, Platform.getApplicationArgs(), false);
        } catch (Exception e) {
            log.error("Error parsing command line: " + e.getMessage());
            return null;
        }
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

    private class ProxyPrintStream extends OutputStream {
        private final OutputStream debugWriter;
        private final OutputStream stdOut;

        public ProxyPrintStream(OutputStream debugWriter, OutputStream stdOut) {
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
