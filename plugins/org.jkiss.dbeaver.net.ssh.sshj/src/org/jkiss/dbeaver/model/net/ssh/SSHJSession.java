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
import java.util.concurrent.ConcurrentHashMap;

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
        // FIXME: timeout is not used

        for (LocalPortListener listener : listeners.values()) {
            listener.disconnect();
        }

        listeners.clear();

        try {
            client.disconnect();
        } catch (IOException e) {
            throw new DBException("Error disconnecting SSH session", e);
        }
    }

    @NotNull
    @Override
    public SSHPortForwardConfiguration setupPortForward(@NotNull SSHPortForwardConfiguration cfg) throws DBException {
        try {
            final ServerSocket ss = new ServerSocket(cfg.localPort(), 0, InetAddress.getByName(cfg.localHost()));
            final Parameters parameters = new Parameters(cfg.localHost(), ss.getLocalPort(), cfg.remoteHost(), cfg.remotePort());
            final LocalPortForwarder forwarder = client.newLocalPortForwarder(parameters, ss);
            final LocalPortListener listener = new LocalPortListener(forwarder, parameters);

            final SSHPortForwardConfiguration resolved = new SSHPortForwardConfiguration(
                cfg.localHost(),
                ss.getLocalPort(),
                cfg.remoteHost(),
                cfg.remotePort()
            );

            listener.start();
            listeners.put(resolved, listener);

            return resolved;
        } catch (IOException e) {
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
        private final LocalPortForwarder forwarder;

        public LocalPortListener(@NotNull LocalPortForwarder forwarder, @NotNull Parameters parameters) {
            this.forwarder = forwarder;

            setName(String.format(
                "Port forwarder listener (%s:%d -> %s:%d)",
                parameters.getLocalHost(), parameters.getLocalPort(),
                parameters.getRemoteHost(), parameters.getRemotePort()
            ));
        }

        @Override
        public void run() {
            try {
                forwarder.listen();
            } catch (IOException e) {
                log.error("Error while listening on the port forwarder", e);
            }
        }

        public void disconnect() {
            try {
                if (forwarder.isRunning()) {
                    forwarder.close();
                }
            } catch (Exception e) {
                log.error("Error while stopping port forwarding", e);
            }
        }
    }
}
