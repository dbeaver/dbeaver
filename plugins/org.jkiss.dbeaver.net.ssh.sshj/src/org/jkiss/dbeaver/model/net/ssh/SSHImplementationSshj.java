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
package org.jkiss.dbeaver.model.net.ssh;

import net.schmizz.sshj.Config;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.LoggerFactory;
import net.schmizz.sshj.connection.channel.direct.LocalPortForwarder;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * SSHJ tunnel
 */
public class SSHImplementationSshj extends SSHImplementationAbstract {

    private static final Log log = Log.getLog(SSHImplementationSshj.class);

    private transient SSHClient sshClient;
    private transient LocalPortListener portListener;

    @Override
    protected void setupTunnel(DBRProgressMonitor monitor, DBWHandlerConfiguration configuration, String dbHost, String sshHost, String aliveInterval, int sshPortNum, File privKeyFile, int connectTimeout, int dbPort, int localPort) throws DBException, IOException {
        try {
            Config clientConfig = new DefaultConfig();
            clientConfig.setLoggerFactory(LoggerFactory.DEFAULT);
            sshClient = new SSHClient(clientConfig);
            // TODO: make real host verifier
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());

            String sshUser = configuration.getUserName();
            String sshPassword = configuration.getPassword();

            try {
                sshClient.loadKnownHosts();
            } catch (IOException e) {
                log.debug("Error loading known hosts: " + e.getMessage());
            }

            sshClient.connect(sshHost, sshPortNum);

            if (privKeyFile != null) {
                if (!CommonUtils.isEmpty(sshPassword)) {
                    KeyProvider keyProvider = sshClient.loadKeys(privKeyFile.getAbsolutePath(), sshPassword.toCharArray());
                    sshClient.authPublickey(sshUser, keyProvider);
                } else {
                    sshClient.authPublickey(sshUser, privKeyFile.getAbsolutePath());
                }
            } else {
                sshClient.authPassword(sshUser, sshPassword);
            }

            log.debug("Instantiate SSH tunnel");

            final LocalPortForwarder.Parameters params
                = new LocalPortForwarder.Parameters(SSHConstants.LOCALHOST_NAME, localPort, dbHost, dbPort);
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
        boolean isAlive = sshClient != null && sshClient.isConnected();
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
