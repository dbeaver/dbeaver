/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.method.AuthMethod;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.PasswordUtils;
import net.schmizz.sshj.xfer.InMemoryDestFile;
import net.schmizz.sshj.xfer.InMemorySourceFile;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHAuthConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHPortForwardConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SSHJ tunnel
 */
public class SSHImplementationSshj extends SSHImplementationAbstract {

    private static final Log log = Log.getLog(SSHImplementationSshj.class);

    private final ExecutorService forwarders = Executors.newCachedThreadPool(target -> new Thread(null, target, "Port forwarder listener"));
    private SSHClient[] clients;

    @Override
    protected synchronized void setupTunnel(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration[] hosts,
        @NotNull SSHPortForwardConfiguration portForward
    ) throws DBException {
        this.clients = new SSHClient[hosts.length];

        final int connectTimeout = configuration.getIntProperty(SSHConstants.PROP_CONNECT_TIMEOUT);
        final int keepAliveInterval = configuration.getIntProperty(SSHConstants.PROP_ALIVE_INTERVAL);

        for (int index = 0; index < hosts.length; index++) {
            final SSHHostConfiguration host = hosts[index];
            final SSHAuthConfiguration auth = host.getAuthConfiguration();
            final SSHClient client = new SSHClient();

            client.setConnectTimeout(connectTimeout);
            client.getConnection().getKeepAlive().setKeepAliveInterval(keepAliveInterval);

            try {
                setupHostKeyVerification(client, configuration);
            } catch (IOException e) {
                log.debug("Error loading known hosts: " + e.getMessage());
            }

            try {
                if (index > 0) {
                    final int port = setPortForwarding(clients[index - 1], host.getHostname(), host.getPort());

                    monitor.subTask(String.format(
                        "Instantiate tunnel %s:%d -> %s:%d",
                        hosts[index - 1].getHostname(), port,
                        host.getHostname(), host.getPort()));

                    client.connect("localhost", port);
                } else {
                    monitor.subTask(String.format(
                        "Instantiate tunnel to %s:%d",
                        host.getHostname(), host.getPort()));

                    client.connect(host.getHostname(), host.getPort());
                }

                switch (auth.getType()) {
                    case PASSWORD:
                        client.authPassword(host.getUsername(), auth.getPassword());
                        break;
                    case PUBLIC_KEY:
                        if (auth.getKeyFile() != null) {
                            final String location = auth.getKeyFile().toAbsolutePath().toString();
                            if (CommonUtils.isEmpty(auth.getPassword())) {
                                client.authPublickey(host.getUsername(), location);
                            } else {
                                client.authPublickey(host.getUsername(), client.loadKeys(location, auth.getPassword().toCharArray()));
                            }
                        } else {
                            final PasswordFinder finder = CommonUtils.isEmpty(auth.getPassword())
                                ? null
                                : PasswordUtils.createOneOff(auth.getPassword().toCharArray());
                            client.authPublickey(host.getUsername(), client.loadKeys(auth.getKeyValue(), null, finder));
                        }
                        break;
                    case AGENT: {
                        final List<AuthMethod> methods = new ArrayList<>();
                        for (SSHAgentIdentity identity : getAgentData()) {
                            methods.add(new DBeaverAuthAgent(this, identity));
                        }
                        client.auth(host.getUsername(), methods);
                        break;
                    }
                    default:
                        break;
                }

                if (index == hosts.length - 1) {
                    log.debug(String.format(
                        "Set port forwarding %s:%d -> %s:%d",
                        portForward.getLocalHost(), portForward.getLocalPort(),
                        portForward.getRemoteHost(), portForward.getRemotePort()));
                    setPortForwarding(
                        client,
                        portForward.getLocalHost(), portForward.getLocalPort(),
                        portForward.getRemoteHost(), portForward.getRemotePort());
                }
            } catch (IOException e) {
                closeTunnel(monitor);
                throw new DBException("Cannot establish tunnel to " + host.getHostname() + ":" + host.getPort(), e);
            }

            clients[index] = client;
        }
    }

    private static void setupHostKeyVerification(
        @NotNull SSHClient client,
        @NotNull DBWHandlerConfiguration configuration
    ) throws IOException {
        if (DBWorkbench.getPlatform().getApplication().isHeadlessMode() ||
            configuration.getBooleanProperty(SSHConstants.PROP_BYPASS_HOST_VERIFICATION)
        ) {
            client.addHostKeyVerifier(new PromiscuousVerifier());
        } else {
            client.addHostKeyVerifier(new KnownHostsVerifier(SSHUtils.getKnownSshHostsFileOrDefault(), DBWorkbench.getPlatformUI()));
        }

        client.loadKnownHosts();
    }

    @Override
    public void closeTunnel(DBRProgressMonitor monitor) {
        try {
            forwarders.shutdown();
        } catch (Exception e) {
            log.debug("Error terminating port forwarders", e);
        }

        RuntimeUtils.runTask(monitor1 -> {
            final SSHClient[] clients = this.clients;

            if (ArrayUtils.isEmpty(clients)) {
                return;
            }

            for (SSHClient client : clients) {
                if (client != null && client.isConnected()) {
                    try {
                        client.disconnect();
                    } catch (IOException e) {
                        log.debug("Error closing session", e);
                    }
                }
            }
        }, "Close SSH session", 1000);

        clients = null;
    }

    @Override
    public String getClientVersion() {
        return ArrayUtils.isEmpty(clients) ? null : clients[clients.length - 1].getTransport().getClientVersion();
    }

    @Override
    public String getServerVersion() {
        return ArrayUtils.isEmpty(clients) ? null : clients[clients.length - 1].getTransport().getServerVersion();
    }

    @Override
    public void invalidateTunnel(DBRProgressMonitor monitor) throws DBException, IOException {
        closeTunnel(monitor);
        initTunnel(monitor, savedConfiguration, savedConnectionInfo);
    }

    @Override
    public void getFile(
        @NotNull String src,
        @NotNull OutputStream dst,
        @NotNull DBRProgressMonitor monitor
    ) throws DBException, IOException {
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
    ) throws DBException, IOException {
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
    private SFTPClient openSftpClient() throws DBException, IOException {
        if (ArrayUtils.isEmpty(clients)) {
            throw new DBException("No active session available");
        }

        return clients[clients.length - 1].newSFTPClient();
    }

    private int setPortForwarding(@NotNull SSHClient client, String host, int port) throws IOException {
        return setPortForwarding(client, "127.0.0.1", 0, host, port);
    }

    private int setPortForwarding(
        @NotNull SSHClient client,
        @NotNull String localHost, int localPort,
        @NotNull String remoteHost, int remotePort
    ) throws IOException {
        final ServerSocket ss = new ServerSocket(localPort, 0, InetAddress.getByName(localHost));
        final Parameters parameters = new Parameters(localHost, ss.getLocalPort(), remoteHost, remotePort);
        final LocalPortForwarder forwarder = client.newLocalPortForwarder(parameters, ss);

        forwarders.submit(() -> {
            try {
                forwarder.listen();
            } finally {
                forwarder.close();
            }

            return null;
        });

        return ss.getLocalPort();
    }
}
