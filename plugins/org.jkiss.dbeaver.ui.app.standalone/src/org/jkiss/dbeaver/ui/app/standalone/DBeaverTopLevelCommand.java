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
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.cli.CLIRunMeta;
import org.jkiss.dbeaver.model.cli.CommandLineContext;
import org.jkiss.dbeaver.model.cli.command.AbstractTopLevelCommand;
import org.jkiss.dbeaver.ui.actions.ConnectionCommands;
import org.jkiss.dbeaver.ui.app.standalone.rpc.IInstanceController;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@CommandLine.Command(name = "dbeaver", description = "DBeaver commands")
public class DBeaverTopLevelCommand extends AbstractTopLevelCommand {
    private static final Log log = Log.getLog(DBeaverTopLevelCommand.class);

    // Eclipse cmd for desktop
    @CommandLine.Option(
        names = {NOSPASH_OPTION},
        description = "Hide splash screen on start",
        scope = CommandLine.ScopeType.INHERIT
    )
    private boolean noSplash;

    @CommandLine.Option(names = {"-vars", "-variablesFile"}, description = "Uses a specified configuration file for variable resolving")
    private String variablesFile;

    @CommandLine.Option(names = {"-newInstance"},
        description = "Force creating new application instance (do not try to activate already running)"
    )
    private boolean newInstance;
    @CommandLine.Option(names = {CLIConstants.COMMAND_REUSE_WORKSPACE}, description = "Force workspace reuse (do not show warnings)")
    private boolean reuseWorkspace;
    @CommandLine.Option(names = {"-stop", "-quit"}, description = "Stop DBeaver running instance")
    private boolean stop;

    @CommandLine.Option(names = {"-f", "-file"}, arity = "1", split = ",", description = "Open a file")
    private List<String> filesToOpen;

    // open files via double-click or "Open with DBeaver"
    @CommandLine.Parameters(index = "0", arity = "0..*", description = "Open files", hidden = true)
    private List<String> filesToOpenParams;


    @CommandLine.Option(names = {"-con", "-connect"}, arity = "1", split = ",", description = "Connects to a specified database")
    private List<String> connectionSpecs;

    @CommandLine.Option(names = {"-disconnectAll"}, description = "Disconnect from all databases")
    private boolean disconnectAll;
    @CommandLine.Option(names = {"-closeTabs"}, description = "Close all open editors")
    private boolean closeTabs;
    

    @CommandLine.Option(names = {"-bringToFront"}, description = "Bring DBeaver window on top of other applications")
    private boolean bringToFront;
    @CommandLine.Option(names = {"-q"}, description = "Run quietly (do not print logs)")
    private boolean quiet;

    @Nullable
    private final IInstanceController instanceController;

    protected DBeaverTopLevelCommand(
        @Nullable IInstanceController controller,
        @NotNull CommandLineContext context,
        @NotNull CLIRunMeta meta
    ) {
        super(controller, context, meta);
        this.instanceController = controller;
    }

    @Override
    public void run() {
        super.run();
        if (context.getPostAction() != null) {
            return;
        }

        if (newInstance) {
            context.setPostAction(CLIProcessResult.PostAction.START_INSTANCE);
            return;
        }

        if (!CommonUtils.isEmpty(variablesFile)) {
            try (InputStream stream = new FileInputStream(variablesFile)) {
                Properties properties = new Properties();
                properties.load(stream);
                SystemVariablesResolver.setConfiguration(properties);
            } catch (Exception e) {
                log.error("Error parsing command line ", e);
                context.setPostAction(CLIProcessResult.PostAction.START_INSTANCE);
            }
        }

        if (instanceController == null) {
            log.debug("Can't process commands because no running instance is present");
            context.setPostAction(CLIProcessResult.PostAction.START_INSTANCE);
            return;
        }

        boolean exitAfterExecute = false;

        if (stop) {
            instanceController.quit();
            exitAfterExecute = true;
        }


        List<String> allFilesToOpen = new ArrayList<>();
        if (!CommonUtils.isEmpty(filesToOpenParams)) {
            allFilesToOpen.addAll(filesToOpenParams);
        }
        if (!CommonUtils.isEmpty(filesToOpen)) {
            allFilesToOpen.addAll(filesToOpen);
        }
        if (!CommonUtils.isEmpty(allFilesToOpen)) {
            instanceController.openExternalFiles(allFilesToOpen.toArray(new String[0]));
            exitAfterExecute = true;
        }

        // Connect
        if (!CommonUtils.isEmpty(connectionSpecs)) {
            for (String con : connectionSpecs) {
                instanceController.openDatabaseConnection(con);
            }
            exitAfterExecute = true;
        }

        if (closeTabs) {
            instanceController.closeAllEditors();
            exitAfterExecute = true;
        }
        if (disconnectAll) {
            instanceController.executeWorkbenchCommand(ConnectionCommands.CMD_DISCONNECT_ALL);
            exitAfterExecute = true;
        }
        if (bringToFront) {
            instanceController.bringToFront();
            exitAfterExecute = true;
        }

        context.setPostAction(exitAfterExecute ? CLIProcessResult.PostAction.SHUTDOWN : CLIProcessResult.PostAction.START_INSTANCE);
    }
}
