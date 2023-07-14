/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.dpi.server;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.dpi.api.DPIContext;
import org.jkiss.dbeaver.model.dpi.api.DPIController;
import org.jkiss.dbeaver.model.dpi.api.DPISerializer;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.rest.RestServer;

import java.io.IOException;

public class DPIRestServer {

    private static final Log log = Log.getLog(DPIRestServer.class);

    private static final String ENDPOINT = "/";

    private final DBPApplication application;
    private final RestServer<?> restServer;
    private final DPIContext dpiContext;

    public DPIRestServer(DBPApplication application, int portNumber) throws IOException {
        this.application = application;
        this.dpiContext = new DPIContext(application);

        DPIControllerImpl dpiController = new DPIControllerImpl(dpiContext);
        restServer = RestServer
            .builder(DPIController.class, dpiController)
            .setFilter(address -> address.getAddress().isLoopbackAddress())
            .setPort(portNumber)
            .setGson(DPISerializer.createSerializer(dpiContext))
            .create();
        dpiController.setServer(restServer);
    }

    public void join() {
        while (restServer.isRunning()) {
            RuntimeUtils.pause(100);
        }
    }

}
