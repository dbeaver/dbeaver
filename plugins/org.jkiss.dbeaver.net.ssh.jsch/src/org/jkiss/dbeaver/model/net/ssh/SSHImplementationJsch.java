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

import com.jcraft.jsch.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.SSHConstants.AuthType;
import org.jkiss.dbeaver.model.net.ssh.config.SSHAuthConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHPortForwardConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * SSH tunnel
 */
public class SSHImplementationJsch extends SSHImplementationAbstract {
    private static final Log log = Log.getLog(SSHImplementationJsch.class);

    private transient JSch jsch;
    private transient volatile Session[] sessions;

    @Override
    protected synchronized void setupTunnel(@NotNull DBRProgressMonitor monitor, @NotNull DBWHandlerConfiguration configuration, @NotNull SSHHostConfiguration[] hosts, @NotNull SSHPortForwardConfiguration portForward) throws DBException, IOException {
        if (jsch == null) {
            jsch = new JSch();
            JSch.setLogger(new JschLoggerProxy());
        }

        sessions = new Session[hosts.length];

        for (int index = 0; index < hosts.length; index++) {
            final SSHHostConfiguration host = hosts[index];
            final SSHAuthConfiguration auth = host.getAuthConfiguration();
            final Session session;

            if (auth.getType() == AuthType.PUBLIC_KEY) {
                log.debug("Adding identity key");
                try {
                    addIdentityKey(monitor, configuration.getDataSource(), auth.getKey(), auth.getPassword());
                } catch (JSchException e) {
                    throw new DBException("Cannot add identity key", e);
                }
            } else if (auth.getType() == AuthType.AGENT) {
                log.debug("Creating identity repository");
                jsch.setIdentityRepository(new DBeaverIdentityRepository(this, getAgentData()));
            }

            try {
                if (index > 0) {
                    final int port = sessions[index - 1].setPortForwardingL(0, host.getHostname(), host.getPort());
                    monitor.subTask("Instantiate tunnel " + hosts[index - 1].getHostname() + ":" + port + " -> " + host.getHostname() + ":" + host.getPort());
                    session = jsch.getSession(host.getUsername(), "localhost", port);
                } else {
                    monitor.subTask("Instantiate tunnel to " + host.getHostname() + ":" + host.getPort());
                    session = jsch.getSession(host.getUsername(), host.getHostname(), host.getPort());
                }

                log.debug("Configure tunnel");

                UserInfo userInfo = null;
                JSCHUserInfoPromptProvider userInfoPromptProvider = GeneralUtils.adapt(this, JSCHUserInfoPromptProvider.class);
                if (userInfoPromptProvider != null) {
                    userInfo = userInfoPromptProvider.createUserInfoPrompt(auth, session);
                }
                if (userInfo == null) {
                    userInfo = new JschUserInfo(auth);
                }

                session.setUserInfo(userInfo);
                session.setConfig("StrictHostKeyChecking", "no");
                session.setConfig("ConnectTimeout", String.valueOf(configuration.getIntProperty(SSHConstants.PROP_CONNECT_TIMEOUT)));
                session.setConfig("ServerAliveInterval", String.valueOf(configuration.getIntProperty(SSHConstants.PROP_ALIVE_INTERVAL)));

                if (auth.getType() == AuthType.PASSWORD) {
                    session.setConfig("PreferredAuthentications", "password,keyboard-interactive");
                } else {
                    session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
                }

                log.debug("Connect to tunnel host");

                session.connect();

                if (index == hosts.length - 1) {
                    log.debug("Set port forwarding " + portForward.getLocalHost() + ":" + portForward.getLocalPort() + " -> " + portForward.getRemoteHost() + ":" + portForward.getRemotePort());
                    session.setPortForwardingL(portForward.getLocalHost(), portForward.getLocalPort(), portForward.getRemoteHost(), portForward.getRemotePort());
                }
            } catch (JSchException e) {
                closeTunnel(monitor);
                throw new DBException("Cannot establish tunnel to " + host.getHostname() + ":" + host.getPort(), e);
            }

            sessions[index] = session;
        }
    }

    @Override
    public synchronized void closeTunnel(DBRProgressMonitor monitor) {
        if (ArrayUtils.isEmpty(sessions)) {
            return;
        }
        RuntimeUtils.runTask(monitor1 -> {
            for (Session session : sessions) {
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            }
        }, "Close SSH session", 1000);
        sessions = null;
    }

    @Override
    public synchronized String getClientVersion() {
        return ArrayUtils.isEmpty(sessions) ? null : sessions[sessions.length - 1].getClientVersion();
    }

    @Override
    public synchronized String getServerVersion() {
        return ArrayUtils.isEmpty(sessions) ? null : sessions[sessions.length - 1].getServerVersion();
    }

    @Override
    public void invalidateTunnel(DBRProgressMonitor monitor) throws DBException, IOException {
        // Do not test - just reopen the tunnel. Otherwise it may take too much time.
        boolean isAlive = false;//session != null && session.isConnected();
        if (isAlive) {
            try {
                for (Session session : sessions) {
                    session.sendKeepAliveMsg();
                }
            } catch (Exception e) {
                isAlive = false;
            }
        }
        if (!isAlive) {
            closeTunnel(monitor);
            initTunnel(monitor, DBWorkbench.getPlatform(), savedConfiguration, savedConnectionInfo);
        }
    }

    private void addIdentityKey(DBRProgressMonitor monitor, DBPDataSourceContainer dataSource, File key, String password) throws IOException, JSchException {
        String header;

        try (BufferedReader reader = new BufferedReader(new FileReader(key))) {
            header = reader.readLine();
        }

        /*
         * This code is a workaround for JSCH because it cannot load
         * newer private keys produced by ssh-keygen, so we need
         * to convert it to the older format manually. This
         * algorithm will fail if the 'ssh-keygen' cannot be found (#5845)
         */
        if (header.equals("-----BEGIN OPENSSH PRIVATE KEY-----")) {
            log.debug("Attempting to convert an unsupported key into suitable format");

            String id = dataSource != null ? dataSource.getId() : "profile";
            File dir = DBWorkbench.getPlatform().getTempFolder(monitor, "openssh-pkey");
            File tmp = new File(dir, id + ".pem");

            Files.copy(key.toPath(), tmp.toPath(), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);

            password = CommonUtils.notEmpty(password);

            if (RuntimeUtils.isWindows()) {
                password = '"' + password + '"';
            }

            Process process = new ProcessBuilder()
                .command(
                    "ssh-keygen",
                    "-p",
                    "-P", password,
                    "-N", password,
                    "-m", "PEM",
                    "-f", tmp.getAbsolutePath(),
                    "-q")
                .start();

            try {
                if (!process.waitFor(5000, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                }

                int status = process.exitValue();

                if (status != 0) {
                    String message;

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        message = reader.lines().collect(Collectors.joining("\n"));
                    }

                    throw new IOException("Specified private key cannot be converted:\n" + message);
                }

                addIdentityKey0(tmp, password);
            } catch (InterruptedException e) {
                throw new IOException(e);
            } finally {
                if (!tmp.delete()) {
                    log.debug("Failed to delete private key file");
                }
            }
        } else {
            addIdentityKey0(key, password);
        }
    }

    private void addIdentityKey0(File key, String password) throws JSchException {
        if (!CommonUtils.isEmpty(password)) {
            jsch.addIdentity(key.getAbsolutePath(), password);
        } else {
            jsch.addIdentity(key.getAbsolutePath());
        }
    }

    private static class JschUserInfo implements UserInfo, UIKeyboardInteractive {
        private final SSHAuthConfiguration configuration;

        private JschUserInfo(@NotNull SSHAuthConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public String getPassphrase() {
            return configuration.getPassword();
        }

        @Override
        public String getPassword() {
            return configuration.getPassword();
        }

        @Override
        public boolean promptPassword(String message) {
            return true;
        }

        @Override
        public boolean promptPassphrase(String message) {
            return true;
        }

        @Override
        public boolean promptYesNo(String message) {
            return false;
        }

        @Override
        public void showMessage(String message) {
            log.info(message);
        }

        @Override
        public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt, boolean[] echo) {
            log.debug("JSCH keyboard interactive auth");
            return new String[]{ configuration.getPassword() };
        }
    }

    private static class JschLoggerProxy implements Logger {
        @Override
        public boolean isEnabled(int level) {
            return true;
        }

        @Override
        public void log(int level, String message) {
            String levelStr;
            switch (level) {
                case INFO:
                    levelStr = "INFO";
                    break;
                case WARN:
                    levelStr = "WARN";
                    break;
                case ERROR:
                    levelStr = "ERROR";
                    break;
                case FATAL:
                    levelStr = "FATAL";
                    break;
                case DEBUG:
                default:
                    levelStr = "DEBUG";
                    break;
            }
            log.debug("SSH " + levelStr + ": " + message);

        }
    }
}
