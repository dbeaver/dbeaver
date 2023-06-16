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

import com.sun.net.httpserver.HttpServer;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DPIRestServer {

    private static final Log log = Log.getLog(DPIRestServer.class);

    private static final String ENDPOINT = "/";

    private final DBPApplication application;
    private HttpServer server;

    public DPIRestServer(DBPApplication application, int portNumber) throws IOException {
        this.application = application;
        InetSocketAddress localPort = new InetSocketAddress(portNumber);
        server = HttpServer.create(localPort, 0);
        server.createContext(ENDPOINT, new DPIRestHandler(this));

        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1,
            10,
            60,
            TimeUnit.SECONDS,
            queue
        );
        server.setExecutor(executor); // creates a default executor
        executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                log.error("Request rejected: " + r);
            }
        });
        server.start();
    }

    DBPApplication getApplication() {
        return application;
    }

    void stopServer() {
        server.stop(2);
        server = null;
    }

    public void join() {
        while (server != null) {
            RuntimeUtils.pause(100);
        }
    }
}
