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
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.cli.command.AbstractTopLevelCommand;
import org.jkiss.dbeaver.model.cli.registry.CommandLineParameterDescriptor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ApplicationCommandLine<T extends ApplicationInstanceController> {
    private static final Log log = Log.getLog(ApplicationCommandLine.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.commandLine";

    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .create();


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
    }

    protected ApplicationCommandLine() {

    }

    protected abstract AbstractTopLevelCommand createTopLevelCommand(
        @Nullable T applicationInstanceController,
        @NotNull CommandLineContext context,
        @NotNull CLIRunMeta runMeta
    );

    public CLIProcessResult executeCommandLineCommands(
        @Nullable T controller,
        boolean uiActivated,
        boolean supportNewInstance
    ) throws Exception {
        String[] args = Platform.getApplicationArgs();
        CLIProcessResult result;
        try (var context = new CommandLineContext(controller)) {
            CommandLine commandLine = initCommandLine(
                controller,
                context,
                new CLIRunMeta(uiActivated, supportNewInstance)
            );
            CommandLine.ParseResult parseResult = commandLine.parseArgs(args);

            if (commandLineIsEmpty(parseResult)) {
                return new CLIProcessResult(CLIProcessResult.PostAction.START_INSTANCE);
            }

            if (supportNewInstance) {
                for (CommandLineParameterDescriptor param : customParameters.values()) {
                    if (param.isExclusiveMode() && find(parseResult, param.getName()) != null) {
                        if (param.isForceNewInstance()) {
                            return new CLIProcessResult(CLIProcessResult.PostAction.START_INSTANCE);
                        }
                        break;
                    }
                }
            }
            if (parseResult.isUsageHelpRequested()) {
                CommandLine.Model.CommandSpec spec = parseResult.commandSpec();
                CommandLine.Model.UsageMessageSpec helpSpec = spec.usageMessage();
                helpSpec.header("dbeaver", GeneralUtils.getProductTitle(), "(C) 2010-2025 DBeaver Corp");
                try (
                    var out = new StringWriter();
                    var print = new PrintWriter(out)
                ) {
                    var updatedCmd = new CommandLine(spec);
                    updatedCmd.usage(print);
                    return new CLIProcessResult(CLIProcessResult.PostAction.SHUTDOWN, out.toString());
                } catch (Exception e) {
                    log.error("Error handling command line: " + e.getMessage());
                    return new CLIProcessResult(CLIProcessResult.PostAction.ERROR, e.getMessage());
                }
            }

            if (parseResult.isVersionHelpRequested()) {
                String version = GeneralUtils.getLongProductTitle();
                System.out.println(version);
                return new CLIProcessResult(CLIProcessResult.PostAction.SHUTDOWN, version);
            }

            commandLine.execute(args);
            CLIProcessResult.PostAction action = context.getPostAction() != null
                ? context.getPostAction()
                : CLIProcessResult.PostAction.UNKNOWN_COMMAND;
            if (!CommonUtils.isEmpty(context.getResults())) {
                var finalAction = action == CLIProcessResult.PostAction.UNKNOWN_COMMAND
                    ? CLIProcessResult.PostAction.SHUTDOWN
                    : action;
                return new CLIProcessResult(finalAction, gson.toJson(context.getResults()));
            }
            return new CLIProcessResult(action);

        } catch (Exception e) {
            log.error("Error evaluating cli:" + e.getMessage(), e);
            String output = "Error evaluating cli: " + CommonUtils.getAllExceptionMessages(e);
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
        }


        return result;
    }

    @NotNull
    protected CommandLine initCommandLine(
        @Nullable T applicationInstanceController,
        @NotNull CommandLineContext context,
        @NotNull CLIRunMeta runMeta
    ) {
        var cmd = new CommandLine(createTopLevelCommand(applicationInstanceController, context, runMeta));
        for (CommandLineParameterDescriptor param : customParameters.values()) {
            if (param.getImplClass().getAnnotation(CommandLine.Command.class) == null) {
                log.warn("Class is not annotated '" + param.getImplClass().getName() + "'");
                continue;
            }
            cmd.addSubcommand(param.getName(), param.getImplClass());
        }
        cmd.setUnmatchedArgumentsAllowed(true);
        return cmd;
    }


    protected boolean commandLineIsEmpty(@Nullable CommandLine.ParseResult commandLine) {
        return commandLine == null || (
            CommonUtils.isEmpty(commandLine.matchedArgs())
                && CommonUtils.isEmpty(commandLine.matchedOptions())
                && CommonUtils.isEmpty(commandLine.subcommands())
        );
    }


    @Nullable
    protected CommandLine.ParseResult find(@NotNull CommandLine.ParseResult pr, @NotNull String name) {
        if (pr.commandSpec().name().equals(name) || pr.hasMatchedOption(name)) {
            return pr;
        }
        for (var sub : pr.subcommands()) {
            var found = find(sub, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
