/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.core;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.IInstanceController;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
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
        return DBeaverCore.getVersion().toString();
    }

    @Override
    public void openExternalFiles(final String[] fileNames) {
        final IWorkbenchWindow window = DBeaverUI.getActiveWorkbenchWindow();
        final Shell shell = window.getShell();
        UIUtils.runInUI(shell, new Runnable() {
            @Override
            public void run() {
                for (String filePath : fileNames) {
                    File file = new File(filePath);
                    if (file.exists()) {
                        try {
                            IEditorDescriptor desc = window.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());
                            IFileStore fileStore = EFS.getStore(file.toURI());
                            IEditorInput input = new FileStoreEditorInput(fileStore);
                            IDE.openEditor(window.getActivePage(), input, desc.getId());
                        } catch (CoreException e) {
                            log.error("Can't open editor from file '" + file.getAbsolutePath(), e);
                        }
                    }
                }
//                if (!shell.getMinimized())
//                {
//                    shell.setMinimized(true);
//                }
                shell.setMinimized(false);
                shell.forceActive();
            }
        });
    }

    @Override
    public String getThreadDump() {
        return null;
    }

    @Override
    public void quit() {
        log.info("Program termination requested");
        System.exit(-1);
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