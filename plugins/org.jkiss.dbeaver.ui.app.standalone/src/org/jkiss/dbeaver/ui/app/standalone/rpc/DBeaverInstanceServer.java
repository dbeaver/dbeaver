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

package org.jkiss.dbeaver.ui.app.standalone.rpc;

import org.apache.commons.cli.CommandLine;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.app.standalone.CommandLineParameterHandler;
import org.jkiss.dbeaver.ui.app.standalone.DBeaverCommandLine;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerOpenEditor;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

/**
 * DBeaver instance controller.
 */
public class DBeaverInstanceServer implements IInstanceController {

    private static final Log log = Log.getLog(DBeaverInstanceServer.class);

    private static final String VAR_RMI_SERVER_HOSTNAME = "java.rmi.server.hostname";

    private static int portNumber;
    private static Registry registry;

    private static final RMIClientSocketFactory CSF_DEFAULT = RMISocketFactory.getDefaultSocketFactory();
    private static final RMIServerSocketFactory SSF_LOCAL = port -> new ServerSocket(port, 0, InetAddress.getLoopbackAddress());
    private static boolean localRMI = false;

    @Override
    public String getVersion() {
        return GeneralUtils.getProductVersion().toString();
    }

    @Override
    public void openExternalFiles(final String[] fileNames) {
        log.debug("Open external file(s) [" + Arrays.toString(fileNames) + "]");

        final IWorkbenchWindow window = UIUtils.getActiveWorkbenchWindow();
        final Shell shell = window.getShell();
        UIUtils.syncExec(() -> {
            for (String filePath : fileNames) {
                File file = new File(filePath);
                if (file.exists()) {
                    EditorUtils.openExternalFileEditor(file, window);
                } else {
                    DBWorkbench.getPlatformUI().showError("Open file", "Can't open '" + file.getAbsolutePath() + "': file doesn't exist");
                }
            }
            shell.setMinimized(false);
            shell.forceActive();
        });
    }

    @Override
    public void openDatabaseConnection(String connectionSpec) throws RemoteException {
        // Do not log it (#3788)
        //log.debug("Open external database connection [" + connectionSpec + "]");

        InstanceConnectionParameters instanceConParameters = new InstanceConnectionParameters();
        final DBPDataSourceContainer dataSource = DataSourceUtils.getDataSourceBySpec(
            DBWorkbench.getPlatform().getWorkspace().getActiveProject(),
            connectionSpec,
            instanceConParameters,
            false,
            instanceConParameters.createNewConnection);

        if (dataSource == null) {
            return;
        }

        if (instanceConParameters.openConsole) {
            final IWorkbenchWindow workbenchWindow = UIUtils.getActiveWorkbenchWindow();
            UIUtils.syncExec(() -> {
                SQLEditorHandlerOpenEditor.openSQLConsole(workbenchWindow, new SQLNavigatorContext(dataSource), dataSource.getName(), "");
                workbenchWindow.getShell().forceActive();
            });
        } else if (instanceConParameters.makeConnect) {
            DataSourceHandler.connectToDataSource(null, dataSource, null);
        }
    }

    @Override
    public String getThreadDump() {
        log.info("Making thread dump");

        StringBuilder td = new StringBuilder();
        for (Map.Entry<Thread, StackTraceElement[]> tde : Thread.getAllStackTraces().entrySet()) {
            td.append(tde.getKey().getId()).append(" ").append(tde.getKey().getName()).append(":\n");
            for (StackTraceElement ste : tde.getValue()) {
                td.append("\t").append(ste.toString()).append("\n");
            }
        }
        return td.toString();
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
            if (window != null) {
                IWorkbenchPage page = window.getActivePage();
                if (page != null) {
                    page.closeAllEditors(false);
                }
            }
        });
    }

    @Override
    public void executeWorkbenchCommand(String commandId) throws RemoteException {
        log.debug("Execute workbench command " + commandId);

        try {
            ActionUtils.runCommand(commandId, UIUtils.getActiveWorkbenchWindow());
        } catch (Exception e) {
            throw new RemoteException("Can't execute command '" + commandId + "'", e);
        }
    }

    @Override
    public void fireGlobalEvent(String eventId, Map<String, Object> properties) throws RemoteException {
        DBWorkbench.getPlatform().getGlobalEventManager().fireGlobalEvent(eventId, properties);
    }

    public static IInstanceController startInstanceServer(CommandLine commandLine, IInstanceController server) {
        try {
            openRmiRegistry();

            {
                IInstanceController stub;
                if (localRMI) {
                    stub = (IInstanceController) UnicastRemoteObject.exportObject(server, 0, null, SSF_LOCAL);
                } else {
                    stub = (IInstanceController) UnicastRemoteObject.exportObject(server, 0);
                }

                //IInstanceController stub = (IInstanceController) UnicastRemoteObject.exportObject(server, 0);
                registry.bind(CONTROLLER_ID, stub);
            }
            for (CommandLineParameterHandler remoteHandler : DBeaverCommandLine.getRemoteParameterHandlers(commandLine)) {

            }

            File rmiFile = new File(GeneralUtils.getMetadataFolder(), RMI_PROP_FILE);
            Properties props = new Properties();
            props.setProperty("port", String.valueOf(portNumber));
            try (OutputStream os = new FileOutputStream(rmiFile)) {
                props.store(os, "DBeaver instance server properties");
            }
            return server;
        } catch (Exception e) {
            log.error("Can't start RMI server", e);
            return null;
        }
    }

    private static void openRmiRegistry() throws RemoteException {
        portNumber = IOUtils.findFreePort(20000, 65000);

        log.debug("Starting RMI server at " + portNumber);
        // We must bing to localhost only
        // It is tricky (https://groups.google.com/g/comp.lang.java.programmer/c/QQT2EOTFoKk?pli=1)
        if (System.getProperty(VAR_RMI_SERVER_HOSTNAME) == null) {
            System.setProperty(VAR_RMI_SERVER_HOSTNAME, "127.0.0.1");
            localRMI = true;

            registry = LocateRegistry.createRegistry(portNumber, CSF_DEFAULT, SSF_LOCAL);
        } else {
            registry = LocateRegistry.createRegistry(portNumber);
        }
    }

    public static void stopInstanceServer() {
        try {
            log.debug("Stop RMI server");
            registry.unbind(CONTROLLER_ID);

            File rmiFile = new File(GeneralUtils.getMetadataFolder(), RMI_PROP_FILE);
            if (rmiFile.exists()) {
                if (!rmiFile.delete()) {
                    log.debug("Can't delete props file");
                }
            }

        } catch (Exception e) {
            log.error("Can't stop RMI server", e);
        }

    }

    private static class InstanceConnectionParameters implements GeneralUtils.IParameterHandler {
        boolean makeConnect = true, openConsole = false, createNewConnection = true;

        @Override
        public boolean setParameter(String name, String value) {
            switch (name) {
                case "connect":
                    makeConnect = CommonUtils.toBoolean(value);
                    return true;
                case "openConsole":
                    openConsole = CommonUtils.toBoolean(value);
                    return true;
                case "create":
                    createNewConnection = CommonUtils.toBoolean(value);
                    return true;
                default:
                    return false;
            }
        }
    }
}