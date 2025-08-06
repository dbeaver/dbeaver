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

import org.apache.commons.cli.*;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.cli.ApplicationCommandLine;
import org.jkiss.dbeaver.model.cli.CmdProcessResult;
import org.jkiss.dbeaver.ui.actions.ConnectionCommands;
import org.jkiss.dbeaver.ui.app.standalone.rpc.DBeaverInstanceServer;
import org.jkiss.dbeaver.ui.app.standalone.rpc.IInstanceController;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Command line processing.
 * Note:
 * there are two modes of command line processing:
 * 1. On DBeaver start. It tries to find already running DBeaver instance (thru REST API) and make it execute passed commands
 *    If DBeaver will execute at least one command using remote invocation then application won't start.
 *    Otherwise it will start normally (and then will try to process commands in UI)
 * 2. After DBeaver UI start. It will execute commands directly
 */
public class DBeaverCommandLine extends ApplicationCommandLine<IInstanceController> {
    private static final Log log = Log.getLog(DBeaverCommandLine.class);


    public static final String PARAM_FILE = "f";
    public static final String PARAM_CONFIG = "vars";
    public static final String PARAM_STOP = "stop";
    public static final String PARAM_CONNECT = "con";
    public static final String PARAM_CLOSE_TABS = "closeTabs";
    public static final String PARAM_DISCONNECT_ALL = "disconnectAll";
    public static final String PARAM_REUSE_WORKSPACE = "reuseWorkspace";
    public static final String PARAM_NEW_INSTANCE = "newInstance";
    public static final String PARAM_BRING_TO_FRONT = "bringToFront";
    public static final String PARAM_QUIET = "q";

    private static DBeaverCommandLine INSTANCE = null;

    static {
         ALL_OPTIONS.addOption(PARAM_CONFIG, "variablesFile", true, "Uses a specified configuration file for variable resolving")
            .addOption(PARAM_FILE, "file", true, "Open a file")
            .addOption(PARAM_STOP, "quit", false, "Stop DBeaver running instance")
            .addOption(PARAM_CONNECT, "connect", true, "Connects to a specified database")
            .addOption(PARAM_DISCONNECT_ALL, "disconnectAll", false, "Disconnect from all databases")
            .addOption(PARAM_CLOSE_TABS, "closeTabs", false, "Close all open editors")
            .addOption(PARAM_REUSE_WORKSPACE, PARAM_REUSE_WORKSPACE, false, "Force workspace reuse (do not show warnings)")
            .addOption(PARAM_NEW_INSTANCE, PARAM_NEW_INSTANCE, false, "Force creating new application instance (do not try to activate already running)")
            .addOption(PARAM_BRING_TO_FRONT, PARAM_BRING_TO_FRONT, false, "Bring DBeaver window on top of other applications")
            .addOption(PARAM_QUIET, PARAM_QUIET, false, "Run quietly (do not print logs)")
            // Eclipse options
            .addOption("product", true, "Product id")
            .addOption("nl", true, "National locale")
            .addOption("data", true, "Data directory")
            .addOption("nosplash", false, "No splash screen")
            .addOption("showlocation", false, "Show location")
            .addOption("registryMultiLanguage", false, "Multi-language mode")
        ;
    }
    protected static final Map<String, CommandLineParameterDescriptor> customParameters = new LinkedHashMap<>();

    static {
        IExtensionRegistry er = Platform.getExtensionRegistry();
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = er.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if ("parameter".equals(ext.getName())) {
                try {
                    CommandLineParameterDescriptor parameter = new CommandLineParameterDescriptor(ext);
                    customParameters.put(parameter.getName(), parameter);
                } catch (Exception e) {
                    log.error("Can't load contributed parameter", e);
                }
            }
        }

        for (CommandLineParameterDescriptor param : customParameters.values()) {
            ALL_OPTIONS.addOption(param.getName(), param.getLongName(), param.hasArg(), param.getDescription());
        }
    }

    private DBeaverCommandLine() {
        super();
    }

    public synchronized static DBeaverCommandLine getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DBeaverCommandLine();
        }
        return INSTANCE;
    }

    /**
     * @return {@link CmdProcessResult.PostAction#SHUTDOWN} if called should exit after CLI processing
     */
    public CmdProcessResult executeCommandLineCommands(
        @Nullable CommandLine commandLine,
        @Nullable IInstanceController controller,
        boolean uiActivated
    ) throws Exception {
        CmdProcessResult result = super.executeCommandLineCommands(commandLine, controller, uiActivated);
        if (result.getPostAction() != CmdProcessResult.PostAction.UNKNOWN_COMMAND) {
            return result;
        }
        //must be checked in super method
        Objects.requireNonNull(commandLine);
        for (CommandLineParameterDescriptor param : customParameters.values()) {
            if (param.isExclusiveMode() && (commandLine.hasOption(param.getName()) || commandLine.hasOption(param.getLongName()))) {
                if (param.isForceNewInstance()) {
                    return new CmdProcessResult(CmdProcessResult.PostAction.START_INSTANCE);
                }
                break;
            }
        }


        if (commandLine.hasOption(PARAM_NEW_INSTANCE)) {
            // Do not try to execute commands in running instance
            return new CmdProcessResult(CmdProcessResult.PostAction.START_INSTANCE);
        }

        if (commandLine.hasOption(PARAM_REUSE_WORKSPACE)) {
            if (DBeaverApplication.instance != null) {
                DBeaverApplication.instance.setReuseWorkspace(true);
            }
        }

        {
            //Set configuration file for SystemVariableResolver
            String file = commandLine.getOptionValue(PARAM_CONFIG);
            if (!CommonUtils.isEmpty(file)) {
                try (InputStream stream = new FileInputStream(file)) {
                    Properties properties = new Properties();
                    properties.load(stream);
                    SystemVariablesResolver.setConfiguration(properties);
                } catch (Exception e) {
                    log.error("Error parsing command line ", e);
                    return new CmdProcessResult(CmdProcessResult.PostAction.START_INSTANCE);
                }
            }
        }

        if (controller == null) {
            log.debug("Can't process commands because no running instance is present");
            return new CmdProcessResult(CmdProcessResult.PostAction.START_INSTANCE);
        }

        boolean exitAfterExecute = false;
        if (!uiActivated) {
            // These command can't be executed locally
            if (commandLine.hasOption(PARAM_STOP)) {
                controller.quit();
                return new CmdProcessResult(CmdProcessResult.PostAction.SHUTDOWN);
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
        if (commandLine.hasOption(PARAM_BRING_TO_FRONT)) {
            controller.bringToFront();
            exitAfterExecute = true;
        }

        var postAction = exitAfterExecute ? CmdProcessResult.PostAction.SHUTDOWN : CmdProcessResult.PostAction.START_INSTANCE;
        return new CmdProcessResult(postAction);
    }

    /**
     * @return {@link CmdProcessResult.PostAction#SHUTDOWN} if called should exit after CLI processing
     */
    //TODO: we should never call this method?
    public CmdProcessResult handleCommandLineAsClient(CommandLine commandLine, String instanceLoc) {
        if (commandLine == null || (ArrayUtils.isEmpty(commandLine.getArgs()) && ArrayUtils.isEmpty(commandLine.getOptions()))) {
            return new CmdProcessResult(CmdProcessResult.PostAction.START_INSTANCE);
        }

        // Reuse workspace if custom parameters are specified
        for (CommandLineParameterDescriptor param : customParameters.values()) {
            if (param.isExclusiveMode() && (commandLine.hasOption(param.getName()) || commandLine.hasOption(param.getLongName()))) {
                if (DBeaverApplication.instance != null) {
                    DBeaverApplication.instance.setExclusiveMode(true);
                }
                break;
            }
        }

        try {
            IInstanceController client = DBeaverInstanceServer.createClient(instanceLoc);
            return executeCommandLineCommands(commandLine, client, false);
        } catch (Throwable e) {
            log.error("Error while calling remote server", e);
        }
        return new CmdProcessResult(CmdProcessResult.PostAction.START_INSTANCE);
    }

    public boolean handleCustomParameters(CommandLine commandLine) {
        if (commandLine == null) {
            return false;
        }
        boolean exit = false;
        for (Option cliOption : commandLine.getOptions()) {
            CommandLineParameterDescriptor param = customParameters.get(cliOption.getOpt());
            if (param == null) {
                param = customParameters.get(cliOption.getLongOpt());
            }
            if (param == null) {
                //log.error("Wrong command line parameter " + cliOption);
                continue;
            }
            try {
                if (param.hasArg()) {
                    for (String optValue : commandLine.getOptionValues(param.getName())) {
                        param.getHandler().handleParameter(
                            commandLine,
                            param.getName(),
                            optValue);
                    }
                } else {
                    param.getHandler().handleParameter(
                        commandLine,
                        param.getName(),
                        null);
                }
            } catch (Exception e) {
                log.error("Error evaluating parameter '" + param.getName() + "'", e);
            }
            if (param.isExitAfterExecute()) {
                exit = true;
            }
        }

        return exit;
    }

}
