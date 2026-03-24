/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.cli.command.AbstractTopLevelCommand;
import org.jkiss.dbeaver.model.cli.model.NonExecutableOption;
import org.jkiss.dbeaver.model.cli.registry.CLICommandDescriptor;
import org.jkiss.dbeaver.model.cli.registry.CLITransformerDescriptor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public abstract class ApplicationCommandLine<T extends ApplicationInstanceController> {
    private static final Log log = Log.getLog(ApplicationCommandLine.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.commandLine";

    protected static final Map<Class<?>, CLICommandDescriptor> commands = new LinkedHashMap<>();
    //transformers for top level command
    protected static final List<CLITransformerDescriptor> globalTransformers = new ArrayList<>();
    //transformers for specific command
    protected static final Map<Class<?>, List<CLITransformerDescriptor>> commandTransformer = new LinkedHashMap<>();
    static {
        IExtensionRegistry er = Platform.getExtensionRegistry();
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = er.getConfigurationElementsFor(EXTENSION_ID);
        Map<Class<?>, CLICommandDescriptor> replacedBy = new LinkedHashMap<>();
        for (IConfigurationElement ext : extElements) {
            if ("parameter".equals(ext.getName())) {
                try {
                    CLICommandDescriptor parameter = new CLICommandDescriptor(ext);
                    if (parameter.getReplacedHandler() != null) {
                        replacedBy.put(parameter.getReplacedHandler(), parameter);
                    }
                    commands.put(parameter.getImplClass(), parameter);
                } catch (Exception e) {
                    log.error("Can't load contributed parameter", e);
                }
            } else if ("transformer".equals(ext.getName())) {
                try {
                    CLITransformerDescriptor transformer = new CLITransformerDescriptor(ext);
                    if (transformer.getCommandClass() == null) {
                        globalTransformers.add(transformer);
                    } else {
                        commandTransformer.computeIfAbsent(transformer.getCommandClass(), k -> new ArrayList<>())
                            .add(transformer);
                    }
                } catch (Exception e) {
                    log.error("Can't load contributed transformer", e);
                }
            }
        }
        replacedBy.keySet().forEach(commands::remove);
    }

    protected ApplicationCommandLine() {

    }

    protected abstract AbstractTopLevelCommand createTopLevelCommand(
        @Nullable T applicationInstanceController,
        @NotNull CLIContextImpl context,
        @NotNull CLIRunMeta runMeta
    );

    /**
     * @param supportNewInstance whether to support starting new instance, true if called from ApplicationInstanceController
     */

    public CLIProcessResult executeCommandLineCommands(
        @Nullable T controller,
        boolean uiActivated,
        boolean supportNewInstance,
        @NotNull String[] args
    ) throws Exception {
        log.trace("Executing command line: " + String.join(" ", args));
        CLIProcessResult result;
        try (var context = new CLIContextImpl(controller)) {
            CommandLine commandLine = initCommandLine(
                controller,
                context,
                new CLIRunMeta(uiActivated, supportNewInstance)
            );
            CommandLine.ParseResult parseResult;
            try {
                parseResult = commandLine.parseArgs(args);
            } catch (CommandLine.UnmatchedArgumentException e) {
                String message;
                if (!CommonUtils.isEmpty(e.getUnmatched())) {
                    String command = e.getCommandLine().getCommandName();
                    message = "Parameter(s) " + String.join(" ", e.getUnmatched()) + " cannot be specified after '" + command + "'";
                } else {
                    message = e.getMessage();
                }
                log.error(message);
                return new CLIProcessResult(
                    CLIProcessResult.PostAction.ERROR,
                    List.of(message),
                    CLIConstants.EXIT_CODE_ERROR
                );
            } catch (CommandLine.MissingParameterException e) {
                return new CLIProcessResult(
                    CLIProcessResult.PostAction.ERROR,
                    List.of(e.getMessage()),
                    CLIConstants.EXIT_CODE_ERROR
                );
            }

            if (commandLineIsEmpty(parseResult)) {
                return new CLIProcessResult(CLIProcessResult.PostAction.START_INSTANCE);
            }
            validateCommandLineParameters(parseResult);

            // Handle help/version before executing commands,
            // because we don't need to execute/start new instance for this cases
            CommandLine.Model.CommandSpec commandForHelp = findCommandForHelp(parseResult);

            if (commandForHelp != null) {
                String help = CLIUtils.getHelpFromCommand(commandForHelp);
                return new CLIProcessResult(CLIProcessResult.PostAction.SHUTDOWN, help);
            }

            if (parseResult.isVersionHelpRequested()) {
                String version = GeneralUtils.getLongProductTitle();
                return new CLIProcessResult(CLIProcessResult.PostAction.SHUTDOWN, version);
            }

            for (CLICommandDescriptor descriptor : commands.values()) {
                CommandLine.ParseResult cliCommand = findCommand(parseResult, descriptor.getImplClass());
                if (cliCommand == null) {
                    continue;
                }
                if (supportNewInstance && descriptor.isExclusiveMode() && descriptor.isForceNewInstance()) {
                    return new CLIProcessResult(CLIProcessResult.PostAction.START_INSTANCE);
                }
            }

            commandLine.execute(args);
            if (commandLine.getExecutionExceptionHandler() instanceof ExceptionHandler exceptionHandler) {
                Exception executionException = exceptionHandler.getException();
                if (executionException != null) {
                    throw executionException;
                }
            }
            CLIProcessResult.PostAction action = context.getPostAction() != null
                ? context.getPostAction()
                : CLIProcessResult.PostAction.UNKNOWN_COMMAND;
            if (!CommonUtils.isEmpty(context.getResults())) {
                var finalAction = action == CLIProcessResult.PostAction.UNKNOWN_COMMAND
                    ? CLIProcessResult.PostAction.SHUTDOWN
                    : action;
                return new CLIProcessResult(finalAction, context.getResults());
            }
            return new CLIProcessResult(action);

        } catch (Exception e) {
            log.error("Error evaluating cli:" + e.getMessage(), e);
            String output = "Error evaluating cli: " + CommonUtils.getAllExceptionMessages(e);
            if (e instanceof CLIException cliException) {
                result = new CLIProcessResult(
                    CLIProcessResult.PostAction.ERROR,
                    List.of(output),
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

    private static CommandLine.Model.CommandSpec findCommandForHelp(
        @NotNull CommandLine.ParseResult parseResult
    ) {
        if (parseResult.isUsageHelpRequested()) {
            return parseResult.commandSpec();
        }
        for (var sub : parseResult.subcommands()) {
            var command = findCommandForHelp(sub);
            if (command != null) {
                return command;
            }
        }
        return null;
    }

    protected void validateCommandLineParameters(@NotNull CommandLine.ParseResult parseResult) throws CLIException {

    }

    @NotNull
    public String[] preprocessCommandLine(@NotNull String[] args) throws DBException {
        try (var context = new CLIContextImpl(null)) {
            CommandLine commandLine = initCommandLine(
                null,
                context,
                new CLIRunMeta(false, false)
            );
            commandLine.setUnmatchedArgumentsAllowed(true);
            CommandLine.ParseResult parseResult;
            parseResult = commandLine.parseArgs(args);
            if (commandLineIsEmpty(parseResult)) {
                return new String[0];
            }
            for (CLICommandDescriptor descriptor : commands.values()) {
                CommandLine.ParseResult cliCommand = findCommand(parseResult, descriptor.getImplClass());
                if (cliCommand == null) {
                    continue;
                }
                preprocessCommandLineParameter(
                    descriptor,
                    cliCommand,
                    context,
                    false
                );
            }
        } catch (Exception e) {
            log.error("Error preprocessing command line: " + e.getMessage(), e);
        }
        return args;
    }

    protected void preprocessCommandLineParameter(
        @NotNull CLICommandDescriptor descriptor,
        @NotNull CommandLine.ParseResult cliCommand,
        @NotNull CLIContextImpl context,
        boolean uiActivated
    ) {

    }

    @NotNull
    protected CommandLine initCommandLine(
        @Nullable T applicationInstanceController,
        @NotNull CLIContextImpl context,
        @NotNull CLIRunMeta runMeta
    ) {
        AbstractTopLevelCommand topLevelImp = createTopLevelCommand(applicationInstanceController, context, runMeta);
        var topLevel = new CommandLine(topLevelImp);
        topLevel.setExecutionStrategy(new CommandLine.RunAll());
        ExceptionHandler exceptionHandler = new ExceptionHandler();
        topLevel.setExecutionExceptionHandler(exceptionHandler);
        transformCommand(topLevel.getCommandSpec(), topLevelImp.getClass());
        for (CLICommandDescriptor param : commands.values()) {
            if (param.getImplClass().getAnnotation(CommandLine.Command.class) == null) {
                log.warn("Class is not annotated '" + param.getImplClass().getName() + "'");
                continue;
            }
            CommandLine command = new CommandLine(param.getImplClass());
            transformCommand(command.getCommandSpec(), param.getImplClass());
            topLevel.addSubcommand(command);
        }
        // call after adding subcommands, because global transformers can affect all command tree
        for (CLITransformerDescriptor transformer : globalTransformers) {
            transformer.getTransformer().transform(topLevel.getCommandSpec());
        }
        return topLevel;
    }

    private void transformCommand(
        @NotNull CommandLine.Model.CommandSpec commandSpec,
        @NotNull Class<?> implClass
    ) {
        List<CLITransformerDescriptor> transformers = commandTransformer.get(implClass);
        if (!CommonUtils.isEmpty(transformers)) {
            for (CLITransformerDescriptor transformer : transformers) {
                transformer.getTransformer().transform(commandSpec);
            }
        }
        if (!CommonUtils.isEmpty(commandSpec.subcommands())) {
            for (Map.Entry<String, CommandLine> stringCommandLineEntry : commandSpec.subcommands().entrySet()) {
                CommandLine.Model.CommandSpec subCommandSpec = stringCommandLineEntry.getValue().getCommandSpec();
                transformCommand(subCommandSpec, subCommandSpec.userObject().getClass());
            }
        }
    }

    protected boolean commandLineIsEmpty(@Nullable CommandLine.ParseResult commandLine) {
        if (commandLine == null) {
            return true;
        }

        var noOptions = Stream.concat(commandLine.matchedArgs().stream(), commandLine.matchedOptions().stream())
            .allMatch(argSpec -> {
                if (argSpec.userObject() instanceof Field field) {
                    return field.isAnnotationPresent(NonExecutableOption.class);
                }
                return false;
            });

        return noOptions && CommonUtils.isEmpty(commandLine.subcommands());
    }


    @Nullable
    protected CommandLine.ParseResult findCommand(@NotNull CommandLine.ParseResult pr, @NotNull Class<?> clazz) {
        Object commandObject = pr.commandSpec().userObject();
        if (clazz.equals(commandObject.getClass())) {
            return pr;
        }
        for (var sub : pr.subcommands()) {
            var found = findCommand(sub, clazz);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
