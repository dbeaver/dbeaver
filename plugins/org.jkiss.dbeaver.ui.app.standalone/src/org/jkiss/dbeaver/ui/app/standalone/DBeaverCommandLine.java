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


import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.cli.ApplicationCommandLine;
import org.jkiss.dbeaver.model.cli.CLIRunMeta;
import org.jkiss.dbeaver.model.cli.CommandLineContext;
import org.jkiss.dbeaver.model.cli.registry.CommandLineParameterDescriptor;
import org.jkiss.dbeaver.ui.app.standalone.cli.DBeaverMixin;
import org.jkiss.dbeaver.ui.app.standalone.rpc.IInstanceController;
import picocli.CommandLine;

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

    private static DBeaverCommandLine INSTANCE = null;

    private DBeaverCommandLine() {
        super();
    }

    @Override
    protected DBeaverTopLevelCommand createTopLevelCommand(
        @Nullable IInstanceController applicationInstanceController,
        @NotNull CommandLineContext context,
        @NotNull CLIRunMeta runMeta
    ) {
        return new DBeaverTopLevelCommand(applicationInstanceController, context, runMeta);
    }

    public synchronized static DBeaverCommandLine getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DBeaverCommandLine();
        }
        return INSTANCE;
    }

    @Override
    protected void preprocessCommandLineParameter(
        @NotNull CommandLineParameterDescriptor descriptor,
        @NotNull CommandLine.ParseResult cliCommand,
        @NotNull CommandLineContext context,
        boolean uiActivated
    ) {
        super.preprocessCommandLineParameter(descriptor, cliCommand, context, uiActivated);
        if (!uiActivated && descriptor.isExclusiveMode()) {
            if (DBeaverApplication.instance != null) {
                DBeaverApplication.instance.setExclusiveMode(true);
            }
        }
    }

    @NotNull
    @Override
    protected CommandLine initCommandLine(
        @Nullable IInstanceController applicationInstanceController,
        @NotNull CommandLineContext context,
        @NotNull CLIRunMeta runMeta
    ) {
        CommandLine cmd = super.initCommandLine(applicationInstanceController, context, runMeta);
        cmd.addMixin("dbeaver", new DBeaverMixin());
        return cmd;
    }
}
