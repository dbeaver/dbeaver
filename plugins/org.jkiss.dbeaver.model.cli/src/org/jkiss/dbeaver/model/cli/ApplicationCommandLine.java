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
package org.jkiss.dbeaver.model.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.cli.*;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.cli.registry.CommandLineParameterDescriptor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

public abstract class ApplicationCommandLine<T extends ApplicationInstanceController> {
    private static final Log log = Log.getLog(ApplicationCommandLine.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.commandLine";

    public static final String PARAM_HELP = "help";
    public static final String PARAM_THREAD_DUMP = "dump";
    public static final String PARAM_DB_LIST = "databaseList";
    private static final String PARAM_VERSION = "version";
    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    public final static Options ALL_OPTIONS = new Options()
        .addOption(PARAM_HELP, PARAM_HELP, false, "Help")
        .addOption(PARAM_DB_LIST, "database-driver-list", true, "Show list of supported database drivers in json format")
        .addOption(PARAM_THREAD_DUMP, "thread-dump", false, "Print instance thread dump")
        .addOption(
            PARAM_VERSION,
            PARAM_VERSION,
            false,
            "Displays the app name, edition, and version in Major.Minor.Micro.Timestamp format"
        );

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
            Option newOption = new Option(param.getName(), param.getLongName(), param.hasArg(), param.getDescription());
            if (param.hasOptionalArg()) {
                newOption.setOptionalArg(param.hasOptionalArg());
                newOption.setArgs(1);
            }
            ALL_OPTIONS.addOption(newOption);
        }
    }

    protected ApplicationCommandLine() {
    }

    public CLIProcessResult executeCommandLineCommands(
        @Nullable CommandLine commandLine,
        @Nullable T controller,
        boolean uiActivated,
        boolean supportNewInstance
    ) throws Exception {
        if (commandLine == null || (ArrayUtils.isEmpty(commandLine.getArgs()) && ArrayUtils.isEmpty(commandLine.getOptions()))) {
            return new CLIProcessResult(CLIProcessResult.PostAction.START_INSTANCE);
        }

        if (supportNewInstance) {
            for (CommandLineParameterDescriptor param : customParameters.values()) {
                if (param.isExclusiveMode() && (commandLine.hasOption(param.getName()) || commandLine.hasOption(param.getLongName()))) {
                    if (param.isForceNewInstance()) {
                        return new CLIProcessResult(CLIProcessResult.PostAction.START_INSTANCE);
                    }
                    break;
                }
            }
        }
        if (commandLine.hasOption(PARAM_HELP)) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.setWidth(120);
            helpFormatter.setOptionComparator((o1, o2) -> 0);
            helpFormatter.printHelp("dbeaver", GeneralUtils.getProductTitle(), ALL_OPTIONS, "(C) 2010-2025 DBeaver Corp", true);
            try (
                var out = new StringWriter();
                var print = new PrintWriter(out)
            ) {
                helpFormatter.printHelp(
                    print, 100, "dbeaver", GeneralUtils.getProductTitle(), ALL_OPTIONS, 4, 4, "(C) 2010-2025 DBeaver Corp", true
                );
                return new CLIProcessResult(CLIProcessResult.PostAction.SHUTDOWN, out.toString());
            } catch (Exception e) {
                log.error("Error handling command line: " + e.getMessage());
                return new CLIProcessResult(CLIProcessResult.PostAction.ERROR, e.getMessage());
            }
        }

        if (commandLine.hasOption(PARAM_VERSION)) {
            String version = GeneralUtils.getLongProductTitle();
            System.out.println(version);
            return new CLIProcessResult(CLIProcessResult.PostAction.SHUTDOWN, version);
        }

        if (!uiActivated) {
            if (commandLine.hasOption(PARAM_THREAD_DUMP)) {
                if (controller == null) {
                    log.debug("Can't process commands because no running instance is present");
                    return new CLIProcessResult(CLIProcessResult.PostAction.START_INSTANCE);
                }
                String threadDump = controller.getThreadDump();
                System.out.println(threadDump);
                return new CLIProcessResult(CLIProcessResult.PostAction.SHUTDOWN, threadDump);
            }
        }

        return handleCustomParameters(commandLine, controller);
    }

    public CLIProcessResult handleCustomParameters(@Nullable CommandLine commandLine, @Nullable T controller) {
        CLIProcessResult result = new CLIProcessResult(CLIProcessResult.PostAction.UNKNOWN_COMMAND);

        if (commandLine == null) {
            return result;
        }

        List<CommandLineParameterDescriptor> initialParameters = new ArrayList<>();
        List<CommandLineParameterDescriptor> parameters = new ArrayList<>();
        for (Option cliOption : commandLine.getOptions()) {
            CommandLineParameterDescriptor param = customParameters.get(cliOption.getOpt());
            if (param == null) {
                param = customParameters.get(cliOption.getLongOpt());
            }
            if (param == null) {
                //log.error("Wrong command line parameter " + cliOption);
                continue;
            }
            if (param.isContextInitializer()) {
                initialParameters.add(param);
            } else {
                parameters.add(param);
            }
        }
        List<CommandLineParameterDescriptor> allParameters = new ArrayList<>(initialParameters);
        allParameters.addAll(parameters);

        try (CommandLineContext context = new CommandLineContext(controller)) {
            for (CommandLineParameterDescriptor param : allParameters) {
                try {
                    if (param.canBeWithArg() && commandLine.getOptionValues(param.getName()) != null) {
                        for (String optValue : commandLine.getOptionValues(param.getName())) {
                            param.getHandler().handleParameter(
                                commandLine,
                                param.getName(),
                                optValue,
                                context
                            );
                        }
                    } else {
                        param.getHandler().handleParameter(
                            commandLine,
                            param.getName(),
                            null,
                            context
                        );
                    }
                } catch (Exception e) {
                    log.error("Error evaluating parameter '" + param.getName() + "'", e);
                    String output = "Error evaluating parameter '" + param.getName() + "': " + CommonUtils.getAllExceptionMessages(e);
                    if (e instanceof CLIException cliException) {
                        result = new CLIProcessResult(
                            CLIProcessResult.PostAction.ERROR,
                            output,
                            cliException.getExitCode()
                        );
                    } else {
                        result = new CLIProcessResult(
                            CLIProcessResult.PostAction.ERROR,
                            output
                        );
                    }
                    break;
                }
                if (param.isExitAfterExecute()) {
                    result = new CLIProcessResult(CLIProcessResult.PostAction.SHUTDOWN);
                    break;
                }
            }
            if (!CommonUtils.isEmpty(context.getResults())) {
                result = new CLIProcessResult(CLIProcessResult.PostAction.SHUTDOWN, gson.toJson(context.getResults()));
            }
        }
        
        return result;
    }


    @Nullable
    public CommandLine getCommandLine() {
        return getCommandLine(Platform.getApplicationArgs());
    }

    @Nullable
    public CommandLine getCommandLine(@NotNull String[] args) {
        try {
            // Remove keyring parameter because its name contains special characters
            // Actual valuation of keyring happens in app launcher

            List<String> applicationArgs = Arrays.stream(args).collect(Collectors.toList());
            Iterator<String> iterator = applicationArgs.iterator();
            boolean removeArgs = false;
            while (iterator.hasNext()) {
                String arg = iterator.next();
                if (CommonUtils.isEmpty(arg)) {
                    continue;
                }
                // argument name start with '-', example '-help'
                if (arg.startsWith("-")) {
                    boolean argSupported = ALL_OPTIONS.hasOption(arg);
                    if (argSupported) {
                        removeArgs = false;
                    } else {
                        //remove not supported argument to avoid parser exception
                        //also remove all arguments for this arg
                        iterator.remove();
                        removeArgs = true;
                    }
                } else if (removeArgs) {
                    iterator.remove();
                }
            }

            return new DefaultParser().parse(ALL_OPTIONS, applicationArgs.toArray(new String[0]), false);
        } catch (Exception e) {
            log.warn("Error parsing command line: " + e.getMessage());
            return null;
        }
    }
}
