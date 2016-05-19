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

package org.jkiss.dbeaver.runtime.rmi;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.utils.IOUtils;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * DBeaver instance controller.
 */
public class InstanceServer implements IInstanceController {

    private static final Log log = Log.getLog(InstanceServer.class);

    @Override
    public String getVersion() {
        return DBeaverCore.getVersion().toString();
    }

    @Override
    public void openExternalFiles(String[] fileNames) {

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

    public static void startInstanceServer() {
        InstanceServer server = new InstanceServer();

        try {
            final int portNumber = IOUtils.findFreePort(20000, 65000);

            log.info("Starting RMI server at " + portNumber);
            IInstanceController stub = (IInstanceController) UnicastRemoteObject.exportObject(server, 0);

            Registry registry = LocateRegistry.createRegistry(portNumber);
            registry.bind(CONTROLLER_ID, stub);
        } catch (Exception e) {
            log.error("Can't start RMI server", e);
        }
    }

}