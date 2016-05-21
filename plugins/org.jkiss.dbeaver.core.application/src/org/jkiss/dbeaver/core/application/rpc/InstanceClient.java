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

package org.jkiss.dbeaver.core.application.rpc;

import org.jkiss.dbeaver.Log;

import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;

/**
 * InstanceClient
 */
public class InstanceClient {

    private static final Log log = Log.getLog(InstanceClient.class);

    public static IInstanceController createClient(String location) {
        try {
            File rmiFile = new File(location, ".metadata/" + IInstanceController.RMI_PROP_FILE);
            Properties props = new Properties();
            try (InputStream is = new FileInputStream(rmiFile)) {
                props.load(is);
            }
            String rmiPort = props.getProperty("port");
            return (IInstanceController) LocateRegistry.getRegistry(Integer.parseInt(rmiPort)).lookup(IInstanceController.CONTROLLER_ID);
        } catch (Exception e) {
            log.error("Error reading RMI config", e);
        }
        return null;
    }

}