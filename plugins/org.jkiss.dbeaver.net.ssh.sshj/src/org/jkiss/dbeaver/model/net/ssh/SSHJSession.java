/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.net.ssh;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.LocalPortForwarder;
import net.schmizz.sshj.connection.channel.direct.Parameters;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.InMemoryDestFile;
import net.schmizz.sshj.xfer.InMemorySourceFile;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHPortForwardConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class SSHJSession extends AbstractSession {
    private static final Log log = Log.getLog(SSHJSession.class);

    private final Map<SSHPortForwardConfiguration, LocalPortListener> listeners = new ConcurrentHashMap<>();
    private final SSHJSessionController controller;

    private SSHClient client;

    public SSHJSession(@NotNull SSHJSessionController controller) {
        this.controller = controller;
    }

    @Override
    public void connect(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SSHHostConfiguration destination,
        @NotNull DBWHandlerConfiguration configuration
    ) throws DBException {
        client = controller.createNewSession(monitor, configuration, destination);
    }

    @Override
    public void disconnect(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        long timeout
    ) throws DBException {
        // Listeners will be closed by the client itself
        listeners.clear();

        try {
            // FIXME: timeout is not used
            client.disconnect();
        } catch (IOException e) {
            throw new DBException("Error disconnecting SSH session", e);
        }
    }

    @NotNull
    @Override
    public SSHPortForwardConfiguration setupPortForward(@NotNull SSHPortForwardConfiguration config) throws DBException {
        try {
            final LocalPortListener listener = LocalPortListener.setup(client, config);
            final SSHPortForwardConfiguration resolved = Objects.requireNonNull(listener.resolved);

            listeners.put(resolved, listener);

            return resolved;
        } catch (Exception e) {
            throw new DBException("Error setting up port forwarding", e);
        }
    }

    @Override
    public void removePortForward(@NotNull SSHPortForwardConfiguration configuration) throws DBException {
        final LocalPortListener listener = listeners.remove(configuration);
        if (listener == null) {
            throw new DBException("No such port forward configuration: " + configuration);
        }
        listener.disconnect();
    }

    @Override
    public void getFile(
        @NotNull String src,
        @NotNull OutputStream dst,
        @NotNull DBRProgressMonitor monitor
    ) throws IOException {
        try (SFTPClient client = openSftpClient()) {
            client.get(src, new InMemoryDestFile() {
                @Override
                public long getLength() {
                    return -1;
                }

                @Override
                public OutputStream getOutputStream() {
                    return dst;
                }

                @Override
                public OutputStream getOutputStream(boolean b) {
                    return dst;
                }
            });
        }
    }

    @Override
    public void putFile(
        @NotNull InputStream src,
        @NotNull String dst,
        @NotNull DBRProgressMonitor monitor
    ) throws IOException {
        try (SFTPClient client = openSftpClient()) {
            client.put(new InMemorySourceFile() {
                @Override
                public String getName() {
                    return "memory";  //$NON-NLS-1$
                }

                @Override
                public long getLength() {
                    return -1;
                }

                @Override
                public InputStream getInputStream() {
                    return src;
                }
            }, dst);
        }
    }

    @NotNull
    @Override
    public String getClientVersion() {
        return client.getTransport().getClientVersion();
    }

    @NotNull
    @Override
    public String getServerVersion() {
        return client.getTransport().getServerVersion();
    }

    @NotNull
    private SFTPClient openSftpClient() throws IOException {
        SFTPClient sftpClient = client.newSFTPClient();
        sftpClient.getFileTransfer().setPreserveAttributes(false);
        return sftpClient;
    }

    private static class LocalPortListener extends Thread {
        private final SSHClient client;
        private final SSHPortForwardConfiguration config;
        private final CountDownLatch started = new CountDownLatch(1);

        private volatile LocalPortForwarder forwarder;
        private volatile SSHPortForwardConfiguration resolved;

        public LocalPortListener(@NotNull SSHClient client, @NotNull SSHPortForwardConfiguration config) {
            this.client = client;
            this.config = config;
        }

        @NotNull
        public static LocalPortListener setup(
            @NotNull SSHClient client,
            @NotNull SSHPortForwardConfiguration config
        ) throws InterruptedException {
            final LocalPortListener listener = new LocalPortListener(client, config);

            listener.start();
            listener.await();

            return listener;
        }

        @Override
        public void run() {
            try {
                final ServerSocket socket = new ServerSocket(config.localPort(), 0, InetAddress.getByName(config.localHost()));
                final Parameters parameters = new Parameters(config.localHost(), socket.getLocalPort(), config.remoteHost(), config.remotePort());

                forwarder = client.newLocalPortForwarder(parameters, socket);
                resolved = new SSHPortForwardConfiguration(config.localHost(), socket.getLocalPort(), config.remoteHost(), config.remotePort());

                setName("Port forwarder listener (" + resolved + ")");

                started.countDown();
                forwarder.listen();
            } catch (IOException e) {
                log.error("Error while listening on the port forwarder", e);
            }
        }

        public void disconnect() {
            try {
                forwarder.close();
                forwarder = null;
            } catch (Exception e) {
                log.error("Error while stopping port forwarding", e);
            }
        }

        private void await() throws InterruptedException {
            started.await();

            while (!forwarder.isRunning()) {
                // Wait for the forwarder to actually start
                Thread.yield();
            }
        }
    }
}
