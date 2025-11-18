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
package org.jkiss.dbeaver.model.cli.command;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.cli.*;
import org.jkiss.dbeaver.model.cli.model.CommandLineAuthenticator;
import org.jkiss.dbeaver.model.cli.model.option.EclipseOptions;
import org.jkiss.dbeaver.model.cli.model.option.HiddenOptions;
import picocli.CommandLine;

public abstract class AbstractTopLevelCommand implements Runnable, CommandLine.IExitCodeGenerator {
    private final Log log = Log.getLog(getClass());

    @CommandLine.Option(names = {"-dump"},
        description = "Print instance thread dump.")
    private boolean dump;

    @CommandLine.Option(names = {"-v", "--debug-logs"},
        description = "Show verbose debug logs.")
    private boolean debugLogs;

    @CommandLine.Mixin
    private EclipseOptions eclipseOptions;
    @CommandLine.Mixin
    private HiddenOptions hiddenOptions;

    @NotNull
    protected final CLIRunMeta meta;
    @Nullable
    protected final ApplicationInstanceController controller;
    @NotNull
    protected final CommandLineContext context;

    private int code = CLIConstants.EXIT_CODE_OK;

    protected AbstractTopLevelCommand(
        @Nullable ApplicationInstanceController controller,
        @NotNull CommandLineContext context,
        @NotNull CLIRunMeta meta
    ) {
        this.controller = controller;
        this.context = context;
        this.meta = meta;
    }

    @Override
    public void run() {
        if (debugLogs) {
            Log.setLogHandler(null);
        }
        try {
            if (dump) {
                if (controller == null) {
                    log.debug("Can't process commands because no running instance is present");
                    context.setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
                    return;
                }
                String threadDump = controller.getThreadDump();
                context.addResult(threadDump);
                context.setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
                return;
            }
        } catch (Exception e) {
            log.error("Error executing command", e);
            code = CLIConstants.EXIT_CODE_ERROR;
        }
    }

    @Override
    public int getExitCode() {
        return code;
    }

    @NotNull
    public CommandLineContext getContext() {
        return context;
    }

    @Nullable
    public ApplicationInstanceController getController() {
        return controller;
    }

    @NotNull
    public CLIRunMeta getMeta() {
        return meta;
    }

    @Nullable
    public CommandLineAuthenticator getAuthenticator() {
        return null;
    }
}
