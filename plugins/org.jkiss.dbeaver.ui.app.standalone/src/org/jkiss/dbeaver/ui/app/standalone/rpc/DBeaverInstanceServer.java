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

package org.jkiss.dbeaver.ui.app.standalone.rpc;

import org.apache.commons.cli.CommandLine;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.cli.ApplicationInstanceServer;
import org.jkiss.dbeaver.model.cli.CmdProcessResult;
import org.jkiss.dbeaver.registry.DataSourceUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.app.standalone.DBeaverApplication;
import org.jkiss.dbeaver.ui.app.standalone.DBeaverCommandLine;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerOpenEditor;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.rest.RestClient;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * DBeaver instance controller.
 */
public class DBeaverInstanceServer extends ApplicationInstanceServer<IInstanceController> implements IInstanceController {

    private static final Log log = Log.getLog(DBeaverInstanceServer.class);
    private DBPDataSourceContainer dataSourceContainer = null;

    private final List<File> filesToConnect = new ArrayList<>();

    private DBeaverInstanceServer() throws IOException {
        super(IInstanceController.class);
    }

    @Nullable
    public static DBeaverInstanceServer createServer() throws IOException {
        if (createClient() != null) {
            log.debug("Can't start instance server because other instance is already running");
            return null;
        }

        return new DBeaverInstanceServer();
    }

    @Nullable
    public static IInstanceController createClient() {
        return createClient(null);
    }

    @Nullable
    public static IInstanceController createClient(@Nullable String workspacePath) {
        final Path path = getConfigPath(workspacePath);

        if (Files.notExists(path)) {
            log.trace("No instance controller is available");
            return null;
        }

        final Properties properties = new Properties();

        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        } catch (IOException e) {
            log.error("Error reading instance controller configuration: " + e.getMessage());
            return null;
        }

        final String port = properties.getProperty("port");

        if (CommonUtils.isEmptyTrimmed(port)) {
            log.error("No port specified for the instance controller to connect to");
            return null;
        }

        final IInstanceController instance = RestClient
            .builder(URI.create("http://localhost:" + port), IInstanceController.class)
            .create();

        try {
            final long payload = System.currentTimeMillis();
            final long response = instance.ping(payload);

            if (response != payload) {
                throw new IllegalStateException("Invalid ping response: " + response + ", was expecting " + payload);
            }
        } catch (Throwable e) {
            log.error("Error accessing instance server: " + e.getMessage());
            return null;
        }

        return instance;
    }

    @NotNull
    @Override
    public CmdProcessResult handleCommandLine(@NotNull String[] args) {
        CommandLine cmd = DBeaverCommandLine.getInstance().getCommandLine(args);

        try {
            return DBeaverCommandLine.getInstance().executeCommandLineCommands(
                cmd,
                this,
                !DBeaverApplication.getInstance().isHeadlessMode()
            );
        } catch (Exception e) {
            return new CmdProcessResult(CmdProcessResult.PostAction.ERROR, "Error executing command: " + e.getMessage());
        }
    }

    @Override
    public long ping(long payload) {
        return payload;
    }

    @Override
    public void openExternalFiles(@NotNull String[] fileNames) {
        UIUtils.asyncExec(() -> {
            List<Path> paths = EditorUtils.openExternalFiles(fileNames, dataSourceContainer);
            filesToConnect.addAll(paths.stream().map(Path::toFile).toList());
        });
    }

    @Override
    public void openDatabaseConnection(@NotNull String connectionSpec) {
        // Do not log it (#3788)
        //log.debug("Open external database connection [" + connectionSpec + "]");
        InstanceConnectionParameters instanceConParameters = new InstanceConnectionParameters();
        DBPProject activeProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (activeProject == null) {
            log.error("No active project in workspace");
            return;
        }
        dataSourceContainer = DataSourceUtils.getDataSourceBySpec(
            activeProject,
            GeneralUtils.replaceVariables(connectionSpec, SystemVariablesResolver.INSTANCE),
            instanceConParameters,
            false,
            instanceConParameters.createNewConnection);
        if (dataSourceContainer == null) {
            filesToConnect.clear();
            return;
        }
        if (!CommonUtils.isEmpty(filesToConnect)) {
            for (File file : filesToConnect) {
                EditorUtils.setFileDataSource(file, new SQLNavigatorContext(dataSourceContainer));
            }
        }
        if (instanceConParameters.openConsole) {
            final IWorkbenchWindow workbenchWindow = UIUtils.getActiveWorkbenchWindow();
            UIUtils.syncExec(() -> {
                SQLEditorHandlerOpenEditor.openSQLConsole(workbenchWindow, new SQLNavigatorContext(dataSourceContainer), dataSourceContainer.getName(), "");
                workbenchWindow.getShell().forceActive();

            });
        } else if (instanceConParameters.makeConnect) {
            DataSourceHandler.connectToDataSource(null, dataSourceContainer, null);
        }
        filesToConnect.clear();
    }

    @Override
    public void quit() {
        log.info("Program termination requested");

        new Job("Terminate application") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                System.exit(-1);
                return Status.OK_STATUS;
            }
        }.schedule(1000);
    }

    @Override
    public void closeAllEditors() {
        log.debug("Close all open editor tabs");

        UIUtils.syncExec(() -> {
            IWorkbenchWindow window = UIUtils.getActiveWorkbenchWindow();
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                page.closeAllEditors(false);
            }
        });
    }

    @Override
    public void executeWorkbenchCommand(@NotNull String commandId) {
        log.debug("Execute workbench command " + commandId);
        ActionUtils.runCommand(commandId, UIUtils.getActiveWorkbenchWindow());
    }

    @Override
    public void fireGlobalEvent(@NotNull String eventId, @NotNull Map<String, Object> properties) {
        DBPPlatformDesktop.getInstance().getGlobalEventManager().fireGlobalEvent(eventId, properties);
    }

    @Override
    public void bringToFront() {
        UIUtils.syncExec(() -> {
            final Shell shell = UIUtils.getActiveShell();
            if (shell != null) {
                if (!shell.getMinimized()) {
                    shell.setMinimized(true);
                }
                shell.setMinimized(false);
                shell.setActive();
            }
        });
    }

    private static class InstanceConnectionParameters implements GeneralUtils.IParameterHandler {
        boolean makeConnect = true, openConsole = false, createNewConnection = true;

        @Override
        public boolean setParameter(String name, String value) {
            return switch (name) {
                case "connect" -> {
                    makeConnect = CommonUtils.toBoolean(value);
                    yield true;
                }
                case "openConsole" -> {
                    openConsole = CommonUtils.toBoolean(value);
                    yield true;
                }
                case "create" -> {
                    createNewConnection = CommonUtils.toBoolean(value);
                    yield true;
                }
                default -> false;
            };
        }
    }
}