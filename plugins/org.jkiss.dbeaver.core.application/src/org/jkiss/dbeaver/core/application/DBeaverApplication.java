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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.application.rpc.DBeaverInstanceServer;
import org.jkiss.dbeaver.core.application.rpc.IInstanceController;
import org.jkiss.dbeaver.core.application.rpc.InstanceClient;
import org.jkiss.utils.ArrayUtils;

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This class controls all aspects of the application's execution
 */
public class DBeaverApplication implements IApplication
{
    private static final Log log = Log.getLog(DBeaverApplication.class);
    public static final String DBEAVER_DEFAULT_DIR = ".dbeaver"; //$NON-NLS-1$

    private static IInstanceController instanceServer;

    @Override
    public Object start(IApplicationContext context)
    {
        Display display = null;

        Location instanceLoc = Platform.getInstanceLocation();
        String defaultHomePath = getDefaultWorkspaceLocation().getAbsolutePath();
        try {
            URL defaultHomeURL = new File(defaultHomePath).toURI().toURL();
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

        if (display == null) {
            display = PlatformUI.createDisplay();
        }
        final Runtime runtime = Runtime.getRuntime();

        DBeaverCore.setStandalone(true);
        log.debug(DBeaverCore.getProductTitle() + " is starting"); //$NON-NLS-1$
        log.debug("Install path: '" + Platform.getInstallLocation().getURL() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        log.debug("Instance path: '" + instanceLoc.getURL() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        log.debug("Memory available " + (runtime.totalMemory() / (1024*1024)) + "Mb/" + (runtime.maxMemory() / (1024*1024)) + "Mb");

        // Run instance server
        instanceServer = DBeaverInstanceServer.startInstanceServer();

        // Prefs default
        PlatformUI.getPreferenceStore().setDefault(
            IWorkbenchPreferenceConstants.KEY_CONFIGURATION_ID,
            ApplicationWorkbenchAdvisor.DBEAVER_SCHEME_NAME);
        try {
            int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
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

    public static boolean executeCommandLineCommands(CommandLine commandLine, IInstanceController controller) throws Exception {
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

    private static File getDefaultWorkspaceLocation() {
        return new File(
            System.getProperty("user.home"),
            DBEAVER_DEFAULT_DIR);
    }

    @Override
    public void stop()
    {
        log.debug("DBeaver is stopping"); //$NON-NLS-1$
        final IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench == null)
            return;

        instanceServer = null;
        DBeaverInstanceServer.stopInstanceServer();

        final Display display = workbench.getDisplay();
        display.syncExec(new Runnable()
        {
            @Override
            public void run()
            {
                if (!display.isDisposed())
                    workbench.close();
            }
        });
    }

    public static IInstanceController getInstanceServer() {
        return instanceServer;
    }

    public static CommandLine getCommandLine() {
        try {
            return new DefaultParser().parse(DBeaverCommandLine.ALL_OPTIONS, Platform.getApplicationArgs(), false);
        } catch (Exception e) {
            log.error("Error parsing command line", e);
            return null;
        }
    }

}
