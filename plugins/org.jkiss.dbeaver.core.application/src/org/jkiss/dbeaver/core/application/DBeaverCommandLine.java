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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.core.application.rpc.IInstanceController;
import org.jkiss.dbeaver.core.application.rpc.InstanceClient;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Command line processing.
 * Note:
 * there are two modes of command line processing:
 * 1. On DBeaver start. It tries to find already running DBeaver instance (thru RMI) and make it execute passed commands
 *    If DBeaver will execute at least one command using remote invocation then application won't start.
 *    Otherwise it will start normally (and then will try to process commands in UI)
 * 2. After DBeaver UI start. It will execute commands directly
 */
public class DBeaverCommandLine
{
    private static final Log log = Log.getLog(DBeaverCommandLine.class);

    public static final String PARAM_HELP = "help";
    public static final String PARAM_FILE = "f";
    public static final String PARAM_STOP = "stop";
    public static final String PARAM_THREAD_DUMP = "dump";
    public static final String PARAM_CONNECT = "con";

    public static final String PARAM_CLOSE_TABS = "closeTabs";
    public static final String PARAM_DISCONNECT_ALL = "disconnectAll";
    public static final String PARAM_REUSE_WORKSPACE = "reuseWorkspace";
    public static final String PARAM_NEW_INSTANCE = "newInstance";

    public final static Options ALL_OPTIONS = new Options()
        .addOption(PARAM_HELP, false, "Help")

        .addOption(PARAM_FILE, "file", true, "File top open")
        .addOption(PARAM_STOP, "quit", false, "Stop DBeaver running instance")
        .addOption(PARAM_THREAD_DUMP, "thread-dump", false, "Print instance thread dump")
        .addOption(PARAM_CONNECT, "connect", true, "Connects to a specified database")
        .addOption(PARAM_DISCONNECT_ALL, "disconnectAll", false, "Disconnect from all databases")
        .addOption(PARAM_CLOSE_TABS, "closeTabs", false, "Close all open editors")
        .addOption(PARAM_REUSE_WORKSPACE, PARAM_REUSE_WORKSPACE, false, "Force workspace reuse (do not show warnings)")
        .addOption(PARAM_NEW_INSTANCE, PARAM_NEW_INSTANCE, false, "Force creating new application instance (do not try to activate already running)")

        // Eclipse options
        .addOption("product", true, "Product id")
        .addOption("nl", true, "National locale")
        .addOption("data", true, "Data directory")
        .addOption("nosplash", false, "No splash screen")
        .addOption("showlocation", false, "Show location")
        ;

    /**
     * @return true if called should exit after CLI processing
     */
    static boolean executeCommandLineCommands(CommandLine commandLine, IInstanceController controller, boolean uiActivated) throws Exception {
        if (commandLine == null) {
            return false;
        }
        if (controller == null) {
            return false;
        }
        boolean exitAfterExecute = false;
        if (!uiActivated) {
            // These command can't be executed locally
            if (commandLine.hasOption(PARAM_STOP)) {
                controller.quit();
                return true;
            }
            if (commandLine.hasOption(PARAM_THREAD_DUMP)) {
                String threadDump = controller.getThreadDump();
                System.out.println(threadDump);
                return true;
            }
        }

        {
            // Open files
            String[] files = commandLine.getOptionValues(PARAM_FILE);
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
                exitAfterExecute = true;
            }
        }
        {
            // Connect
            String[] connectParams = commandLine.getOptionValues(PARAM_CONNECT);
            if (!ArrayUtils.isEmpty(connectParams)) {
                for (String cp : connectParams) {
                    controller.openDatabaseConnection(cp);
                }
                exitAfterExecute = true;
            }
        }

        if (commandLine.hasOption(PARAM_CLOSE_TABS)) {
            controller.closeAllEditors();
            exitAfterExecute = true;
        }
        if (commandLine.hasOption(PARAM_DISCONNECT_ALL)) {
            controller.executeWorkbenchCommand(CoreCommands.CMD_DISCONNECT_ALL);
            exitAfterExecute = true;
        }

        if (commandLine.hasOption(PARAM_REUSE_WORKSPACE)) {
            if (DBeaverApplication.instance != null) {
                DBeaverApplication.instance.reuseWorkspace = true;
            }
        }

        return exitAfterExecute;
    }

    static CommandLine getCommandLine() {
        try {
            return new DefaultParser().parse(ALL_OPTIONS, Platform.getApplicationArgs(), false);
        } catch (Exception e) {
            log.error("Error parsing command line: " + e.getMessage());
            return null;
        }
    }

    /**
     * @return true if application should terminate after this call
     */
    static boolean handleCommandLine(String instanceLoc) {
        CommandLine commandLine = getCommandLine();
        if (commandLine == null || (ArrayUtils.isEmpty(commandLine.getArgs()) && ArrayUtils.isEmpty(commandLine.getOptions()))) {
            return false;
        }
        if (commandLine.hasOption(PARAM_HELP)) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.setWidth(120);
            helpFormatter.setOptionComparator((o1, o2) -> 0);
            helpFormatter.printHelp("dbeaver", GeneralUtils.getProductTitle(), ALL_OPTIONS, "(C) 2017 JKISS", true);
            return true;
        }
        if (commandLine.hasOption(PARAM_NEW_INSTANCE)) {
            // Do not try to execute commands in running instance
            return false;
        }

        IInstanceController controller = null;
        try {
            controller = InstanceClient.createClient(instanceLoc);
        } catch (Exception e) {
            // its ok
            log.debug("Error detecting DBeaver running instance: " + e.getMessage());
        }
        if (controller == null) {
            // Can't execute commands as there is no remote instance
            log.debug("No running DBeaver instance found");
            return false;
        }
        try {
            return executeCommandLineCommands(commandLine, controller, false);
        } catch (RemoteException e) {
            log.error("Error calling remote server", e);
        } catch (Throwable e) {
            log.error("Error while calling remote server", e);
        }
        return false;
    }
}
