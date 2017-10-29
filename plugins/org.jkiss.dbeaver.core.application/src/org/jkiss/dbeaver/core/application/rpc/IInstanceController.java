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

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * DBeaver instance controller.
 */
public interface IInstanceController extends Remote {

    String CONTROLLER_ID = "DBeaver.InstanceController";
    String RMI_PROP_FILE = "dbeaver-instance.properties";

    String getVersion() throws RemoteException;

    void openExternalFiles(String[] fileNames) throws RemoteException;

    void openDatabaseConnection(String connectionSpec) throws RemoteException;

    String getThreadDump() throws RemoteException;

    void quit() throws RemoteException;

    void closeAllEditors() throws RemoteException;

    void executeWorkbenchCommand(String commandID) throws RemoteException;
}