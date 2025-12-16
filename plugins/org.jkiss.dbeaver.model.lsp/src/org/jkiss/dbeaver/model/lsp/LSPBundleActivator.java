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
package org.jkiss.dbeaver.model.lsp;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class LSPBundleActivator implements BundleActivator {
    private static final int PORT = 8989;

    private volatile boolean stopping;
    private volatile ServerSocket serverSocket;
    private volatile Socket clientSocket;
    private Thread serverThread;

    @Override
    public void start(BundleContext context) {
        stopping = false;

        serverThread = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(PORT)) {
                serverSocket = ss;
                System.out.println("DBLServer LSP listening on tcp://127.0.0.1:" + PORT);

                while (!stopping && !ss.isClosed()) {
                    try (Socket socket = ss.accept()) {
                        clientSocket = socket;
                        System.out.println("Client connected from " + socket.getRemoteSocketAddress());

                        InputStream in = socket.getInputStream();
                        OutputStream out = socket.getOutputStream();
                        DBLFacade.runLanguageServer(in, out);
                    } catch (SocketException e) {
                        if (!stopping) {
                            throw e;
                        }
                        break;
                    } finally {
                        clientSocket = null;
                    }
                }
            } catch (Exception e) {
                if (!stopping) {
                    e.printStackTrace();
                }
            } finally {
                serverSocket = null;
            }
        }, "DBLServer-LSP");

        serverThread.setDaemon(true);
        serverThread.start();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        stopping = true;

        Socket socket = clientSocket;
        if (socket != null) {
            socket.close();
        }

        ServerSocket ss = serverSocket;
        if (ss != null) {
            ss.close();
        }

        Thread t = serverThread;
        if (t != null) {
            t.interrupt();
            try {
                t.join(5_000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            serverThread = null;
        }
    }}
