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

import org.apache.commons.cli.*;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.actions.datasource.ConnectionCommands;
import org.jkiss.dbeaver.ui.app.standalone.rpc.IInstanceController;
import org.jkiss.dbeaver.ui.app.standalone.rpc.InstanceClient;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;

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

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.commandLine";

    public static final String PARAM_HELP = "help";
    public static final String PARAM_FILE = "f";
    public static final String PARAM_STOP = "stop";
    public static final String PARAM_THREAD_DUMP = "dump";
    public static final String PARAM_CONNECT = "con";

    public static final String PARAM_CLOSE_TABS = "closeTabs";
    public static final String PARAM_DISCONNECT_ALL = "disconnectAll";
    public static final String PARAM_REUSE_WORKSPACE = "reuseWorkspace";
    public static final String PARAM_NEW_INSTANCE = "newInstance";
    public static final String PARAM_QUIET = "q";

    public final static Options ALL_OPTIONS = new Options()
        .addOption(PARAM_HELP, false, "Help")

        .addOption(PARAM_FILE, "file", true, "Open a file")
        .addOption(PARAM_STOP, "quit", false, "Stop DBeaver running instance")
        .addOption(PARAM_THREAD_DUMP, "thread-dump", false, "Print instance thread dump")
        .addOption(PARAM_CONNECT, "connect", true, "Connects to a specified database")
        .addOption(PARAM_DISCONNECT_ALL, "disconnectAll", false, "Disconnect from all databases")
        .addOption(PARAM_CLOSE_TABS, "closeTabs", false, "Close all open editors")
        .addOption(PARAM_REUSE_WORKSPACE, PARAM_REUSE_WORKSPACE, false, "Force workspace reuse (do not show warnings)")
        .addOption(PARAM_NEW_INSTANCE, PARAM_NEW_INSTANCE, false, "Force creating new application instance (do not try to activate already running)")
        .addOption(PARAM_QUIET, PARAM_QUIET, false, "Run quietly (do not print logs)")

        // Eclipse options
        .addOption("product", true, "Product id")
        .addOption("nl", true, "National locale")
        .addOption("data", true, "Data directory")
        .addOption("nosplash", false, "No splash screen")
        .addOption("showlocation", false, "Show location")
        .addOption("registryMultiLanguage", false, "Multi-language mode")
        ;

    private static class ParameterDescriptor {
        String name;
        String longName;
        String description;
        boolean hasArg;
        boolean exitAfterExecute;
        boolean exclusiveMode;
        CommandLineParameterHandler handler;

        public ParameterDescriptor(IConfigurationElement config) throws Exception {
            this.name = config.getAttribute("name");
            this.longName = config.getAttribute("longName");
            this.description = config.getAttribute("description");
            this.hasArg = CommonUtils.toBoolean(config.getAttribute("hasArg"));
            this.exitAfterExecute = CommonUtils.toBoolean(config.getAttribute("exitAfterExecute"));
            this.exclusiveMode = CommonUtils.toBoolean(config.getAttribute("exclusiveMode"));
            Bundle cBundle = Platform.getBundle(config.getContributor().getName());
            Class<?> implClass = cBundle.loadClass(config.getAttribute("handler"));
            handler = (CommandLineParameterHandler) implClass.newInstance();
        }
    }

    private static final Map<String, ParameterDescriptor> customParameters = new LinkedHashMap<>();

    static {
        IExtensionRegistry er = Platform.getExtensionRegistry();
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = er.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if ("parameter".equals(ext.getName())) {
                try {
                    ParameterDescriptor parameter = new ParameterDescriptor(ext);
                    customParameters.put(parameter.name, parameter);
                } catch (Exception e) {
                    log.error("Can't load contributed parameter", e);
                }
            }
        }

        for (ParameterDescriptor param : customParameters.values()) {
            ALL_OPTIONS.addOption(param.name, param.longName, param.hasArg, param.description);
        }
    }

    public static List<CommandLineParameterHandler> getRemoteParameterHandlers(CommandLine commandLine) {
        List<CommandLineParameterHandler> handlers = new ArrayList<>();
        for (ParameterDescriptor param : customParameters.values()) {
            if (commandLine.hasOption(param.name) && !(param.handler instanceof Remote)) {
                handlers.add(param.handler);
            }
        }
        return handlers;
    }
    /**
     * @return true if called should exit after CLI processing
     */
    static boolean executeCommandLineCommands(@NotNull CommandLine commandLine, @Nullable IInstanceController controller, boolean uiActivated) throws Exception {
        if (commandLine == null) {
            return false;
        }

        if (commandLine.hasOption(PARAM_REUSE_WORKSPACE)) {
            if (DBeaverApplication.instance != null) {
                DBeaverApplication.instance.setReuseWorkspace(true);
            }
        }

        if (controller == null) {
            return false;
        }
        boolean exitAfterExecute = false;
        if (!uiActivated) {
            // These command can't be executed locally
            if (commandLine.hasOption(PARAM_STOP)) {
                try {
                    controller.quit();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
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
                controller.openExternalFiles(fileNames.toArray(new String[0]));
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
            controller.executeWorkbenchCommand(ConnectionCommands.CMD_DISCONNECT_ALL);
            exitAfterExecute = true;
        }

        return exitAfterExecute;
    }

    static CommandLine getCommandLine() {
        try {
            return new DefaultParser().parse(ALL_OPTIONS, Platform.getApplicationArgs(), false);
        } catch (Exception e) {
            log.warn("Error parsing command line: " + e.getMessage());
            return null;
        }
    }

    /**
     * @return true if application should terminate after this call
     */
    static boolean handleCommandLine(CommandLine commandLine, String instanceLoc) {
        if (commandLine == null || (ArrayUtils.isEmpty(commandLine.getArgs()) && ArrayUtils.isEmpty(commandLine.getOptions()))) {
            return false;
        }
        if (commandLine.hasOption(PARAM_HELP)) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.setWidth(120);
            helpFormatter.setOptionComparator((o1, o2) -> 0);
            helpFormatter.printHelp("dbeaver", GeneralUtils.getProductTitle(), ALL_OPTIONS, "(C) 2020-2021 DBeaver Corp", true);
            return true;
        }

        // Reuse workspace if custom parameters are specified
        for (ParameterDescriptor param : customParameters.values()) {
            if (param.exclusiveMode && (commandLine.hasOption(param.name) || commandLine.hasOption(param.longName))) {
                if (DBeaverApplication.instance != null) {
                    DBeaverApplication.instance.setExclusiveMode(true);
                }
                break;
            }
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
        try {
            return executeCommandLineCommands(commandLine, controller, false);
        } catch (RemoteException e) {
            log.error("Error calling remote server", e);
        } catch (Throwable e) {
            log.error("Error while calling remote server", e);
        }
        return false;
    }

    public static boolean handleCustomParameters(CommandLine commandLine) {
        if (commandLine == null) {
            return false;
        }
        boolean exit = false;
        for (Option cliOption : commandLine.getOptions()) {
            ParameterDescriptor param = customParameters.get(cliOption.getOpt());
            if (param == null) {
                param = customParameters.get(cliOption.getLongOpt());
            }
            if (param == null) {
                //log.error("Wrong command line parameter " + cliOption);
                continue;
            }
            try {
                if (param.hasArg) {
                    for (String optValue : commandLine.getOptionValues(param.name)) {
                        param.handler.handleParameter(
                            param.name,
                            optValue);
                    }
                } else {
                    param.handler.handleParameter(
                        param.name,
                        null);
                }
            } catch (Exception e) {
                log.error("Error evaluating parameter '" + param.name + "'", e);
            }
            if (param.exitAfterExecute) {
                exit = true;
            }
        }

        return exit;
    }

}
