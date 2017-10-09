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
            if (!rmiFile.exists()) {
                return null;
            }
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