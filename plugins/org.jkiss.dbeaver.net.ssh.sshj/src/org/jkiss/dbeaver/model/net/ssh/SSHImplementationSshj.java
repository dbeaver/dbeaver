/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import net.schmizz.sshj.Config;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.LoggerFactory;
import net.schmizz.sshj.connection.channel.direct.LocalPortForwarder;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.method.AuthMethod;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHAuthConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHPortForwardConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * SSHJ tunnel
 */
public class SSHImplementationSshj extends SSHImplementationAbstract {

    private static final Log log = Log.getLog(SSHImplementationSshj.class);

    private transient SSHClient sshClient;
    private transient LocalPortListener portListener;

    @Override
    protected synchronized void setupTunnel(@NotNull DBRProgressMonitor monitor, @NotNull DBWHandlerConfiguration configuration, @NotNull SSHHostConfiguration[] hosts, @NotNull SSHPortForwardConfiguration portForward) throws DBException, IOException  {
        try {
            final SSHHostConfiguration host = hosts[0];
            final SSHAuthConfiguration auth = host.getAuthConfiguration();

            Config clientConfig = new DefaultConfig();
            clientConfig.setLoggerFactory(LoggerFactory.DEFAULT);
            sshClient = new SSHClient(clientConfig);
            // TODO: make real host verifier
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());

            try {
                sshClient.loadKnownHosts();
            } catch (IOException e) {
                log.debug("Error loading known hosts: " + e.getMessage());
            }

            sshClient.connect(host.getHostname(), host.getPort());

            switch (auth.getType()) {
                case PASSWORD:
                    sshClient.authPassword(host.getUsername(), auth.getPassword());
                    break;
                case PUBLIC_KEY:
                    if (!CommonUtils.isEmpty(auth.getPassword())) {
                        KeyProvider keyProvider = sshClient.loadKeys(auth.getKey().getAbsolutePath(), auth.getPassword().toCharArray());
                        sshClient.authPublickey(host.getUsername(), keyProvider);
                    } else {
                        sshClient.authPublickey(host.getUsername(), auth.getKey().getAbsolutePath());
                    }
                    break;
                case AGENT: {
                    List<SSHAgentIdentity> identities = getAgentData();
                    List<AuthMethod> authMethods = new ArrayList<>();
                    for (SSHAgentIdentity identity : identities) {
                        authMethods.add(new DBeaverAuthAgent(this, identity));
                    }
                    sshClient.auth(host.getUsername(), authMethods);
                    break;
                }
            }

            log.debug("Instantiate SSH tunnel");

            final LocalPortForwarder.Parameters params
                = new LocalPortForwarder.Parameters(portForward.getLocalHost(), portForward.getLocalPort(), portForward.getRemoteHost(), portForward.getRemotePort());
            portListener = new LocalPortListener(params);
            portListener.start();
            RuntimeUtils.pause(100);
        } catch (Exception e) {
            throw new DBException("Cannot establish tunnel", e);
        }
    }

    @Override
    public void closeTunnel(DBRProgressMonitor monitor) throws DBException, IOException {
        if (portListener != null) {
            portListener.stopServer();
        }
        if (sshClient != null) {
            RuntimeUtils.runTask(monitor1 -> {
                try {
                    sshClient.disconnect();
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            }, "Close SSH client", 1000);
            sshClient = null;
        }
    }

    @Override
    public String getClientVersion() {
        return sshClient == null ? null : sshClient.getTransport().getClientVersion();
    }

    @Override
    public String getServerVersion() {
        return sshClient == null ? null : sshClient.getTransport().getServerVersion();
    }

    @Override
    public void invalidateTunnel(DBRProgressMonitor monitor) throws DBException, IOException {
        // Do not test - just reopen the tunnel. Otherwise it may take too much time.
        boolean isAlive = false;//sshClient != null && sshClient.isConnected();
        if (isAlive) {
            try {
                sshClient.isConnected();
            } catch (Exception e) {
                isAlive = false;
            }
        }
        if (!isAlive) {
            closeTunnel(monitor);
            initTunnel(monitor, null, savedConfiguration, savedConnectionInfo);
        }
    }

    private class LocalPortListener extends Thread {
        private LocalPortForwarder.Parameters params;
        private LocalPortForwarder portForwarder;

        LocalPortListener(LocalPortForwarder.Parameters params) {
            this.params = params;
        }

        public void run() {
            setName("Local port forwarder " + params.getRemoteHost() + ":" + params.getRemotePort() + " socket listener");
            final LocalPortForwarder.Parameters params
                = new LocalPortForwarder.Parameters(
                    this.params.getLocalHost(), this.params.getLocalPort(), this.params.getRemoteHost(), this.params.getRemotePort());

            try {
                ServerSocket serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(params.getLocalHost(), params.getLocalPort()));
                portForwarder = sshClient.newLocalPortForwarder(params, serverSocket);
                portForwarder.listen();
            } catch (IOException e) {
                log.error(e);
            }
            log.debug("Server socket closed. Tunnel is terminated");
        }

        void stopServer() {
            if (portForwarder != null) {
                try {
                    portForwarder.close();
                } catch (IOException e) {
                    log.error("Error closing port forwarder", e);
                }
                portForwarder = null;
            }
        }
    }

}
