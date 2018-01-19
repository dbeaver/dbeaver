/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.core.application.rpc;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.handlers.OpenHandler;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * DBeaver instance controller.
 */
public class DBeaverInstanceServer implements IInstanceController {

    private static final Log log = Log.getLog(DBeaverInstanceServer.class);

    private static int portNumber;
    private static Registry registry;

    @Override
    public String getVersion() {
        return GeneralUtils.getProductVersion().toString();
    }

    @Override
    public void openExternalFiles(final String[] fileNames) {
        log.debug("Open external file(s) [" + Arrays.toString(fileNames) + "]");

        final IWorkbenchWindow window = DBeaverUI.getActiveWorkbenchWindow();
        final Shell shell = window.getShell();
        DBeaverUI.syncExec(() -> {
            for (String filePath : fileNames) {
                File file = new File(filePath);
                if (file.exists()) {
                    EditorUtils.openExternalFileEditor(file, window);
                } else {
                    DBUserInterface.getInstance().showError("Open file", "Can't open '" + file.getAbsolutePath() + "': file doesn't exist");
                }
            }
            shell.setMinimized(false);
            shell.forceActive();
        });
    }

    @Override
    public void openDatabaseConnection(String connectionSpec) throws RemoteException {
        log.debug("Open external database connection [" + connectionSpec + "]");

        final IWorkbenchWindow workbenchWindow = DBeaverUI.getActiveWorkbenchWindow();
        DataSourceRegistry dsRegistry = DBeaverCore.getInstance().getProjectRegistry().getActiveDataSourceRegistry();

        String driverName = null, url = null, host = null, port = null, server = null, database = null, user = null, password = null;
        boolean makeConnect = true, openConsole = false, savePassword = true;
        Boolean autoCommit = null;
        Map<String, String> conProperties = new HashMap<>();
        DBPDataSourceFolder folder = null;
        String dsName = null;

        String[] conParams = connectionSpec.split("\\|");
        for (String cp : conParams) {
            int divPos = cp.indexOf('=');
            if (divPos == -1) {
                continue;
            }
            String paramName = cp.substring(0, divPos);
            String paramValue = cp.substring(divPos + 1);
            switch (paramName) {
                case "driver": driverName = paramValue; break;
                case "name": dsName = paramValue; break;
                case "url": url = paramValue; break;
                case "host": host = paramValue; break;
                case "port": port = paramValue; break;
                case "server": server = paramValue; break;
                case "database": database = paramValue; break;
                case "user": user = paramValue; break;
                case "password": password = paramValue; break;
                case "savePassword": savePassword = CommonUtils.toBoolean(paramValue); break;
                case "connect": makeConnect = CommonUtils.toBoolean(paramValue); break;
                case "openConsole": openConsole = CommonUtils.toBoolean(paramValue); break;
                case "folder": folder = dsRegistry.getFolder(paramValue); break;
                case "autoCommit": autoCommit = CommonUtils.toBoolean(paramValue); break;
                default:
                    if (paramName.length() > 5 && paramName.startsWith("prop.")) {
                        paramName = paramName.substring(5);
                        conProperties.put(paramName, paramValue);
                    }
            }
        }
        if (driverName == null) {
            log.error("Driver name not specified");
            return;
        }
        DriverDescriptor driver = DataSourceProviderRegistry.getInstance().findDriver(driverName);
        if (driver == null) {
            log.error("Driver '" + driverName + "' not found");
            return;
        }
        if (dsName == null) {
            dsName = "Ext: " + driver.getName();
            if (database != null) {
                dsName += " - " + database;
            } else if (server != null) {
                dsName += " - " + server;
            }
        }

        DBPConnectionConfiguration connConfig = new DBPConnectionConfiguration();
        connConfig.setUrl(url);
        connConfig.setHostName(host);
        connConfig.setHostPort(port);
        connConfig.setServerName(server);
        connConfig.setDatabaseName(database);
        connConfig.setUserName(user);
        connConfig.setUserPassword(password);
        connConfig.setProperties(conProperties);

        if (autoCommit != null) {
            connConfig.getBootstrap().setDefaultAutoCommit(autoCommit);
        }

        final DataSourceDescriptor ds = new DataSourceDescriptor(dsRegistry, DataSourceDescriptor.generateNewId(driver), driver, connConfig);
        ds.setName(dsName);
        ds.setTemporary(true);
        if (savePassword) {
            ds.setSavePassword(true);
        }
        if (folder != null) {
            ds.setFolder(folder);
        }
        //ds.set
        dsRegistry.addDataSource(ds);

        if (openConsole) {
            DBeaverUI.syncExec(() -> {
                OpenHandler.openSQLConsole(workbenchWindow, ds, ds.getName(), "");
                workbenchWindow.getShell().forceActive();
            });
        } else if (makeConnect) {
            DataSourceHandler.connectToDataSource(null, ds, null);
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

        System.exit(-1);
    }

    @Override
    public void closeAllEditors() {
        log.debug("Close all open editor tabs");

        DBeaverUI.syncExec(() -> {
            IWorkbenchWindow window = DBeaverUI.getActiveWorkbenchWindow();
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
            ActionUtils.runCommand(commandId, DBeaverUI.getActiveWorkbenchWindow());
        } catch (Exception e) {
            throw new RemoteException("Can't execute command '" + commandId + "'", e);
        }
    }


    public static IInstanceController startInstanceServer() {
        DBeaverInstanceServer server = new DBeaverInstanceServer();

        try {
            portNumber = IOUtils.findFreePort(20000, 65000);

            log.debug("Starting RMI server at " + portNumber);
            IInstanceController stub = (IInstanceController) UnicastRemoteObject.exportObject(server, 0);

            registry = LocateRegistry.createRegistry(portNumber);
            registry.bind(CONTROLLER_ID, stub);

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

}