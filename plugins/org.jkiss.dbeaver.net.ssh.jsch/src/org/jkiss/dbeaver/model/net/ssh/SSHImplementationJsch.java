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
package org.jkiss.dbeaver.model.net.ssh;

import com.jcraft.jsch.*;
import org.eclipse.osgi.util.NLS;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * SSH tunnel
 */
public class SSHImplementationJsch extends SSHImplementationAbstract {
    private static final String CHANNEL_TYPE_SFTP = "sftp";

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
                    if (auth.getKeyFile() != null) {
                        addIdentityKeyFile(monitor, configuration.getDataSource(), auth.getKeyFile(), auth.getPassword());
                    } else {
                        addIdentityKeyValue(auth.getKeyValue(), auth.getPassword());
                    }
                } catch (JSchException e) {
                    throw new DBException("Cannot add identity key", e);
                }
            } else if (auth.getType() == AuthType.AGENT) {
                log.debug("Creating identity repository");
                jsch.setIdentityRepository(agentIdentityRepository);
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
                    userInfo = userInfoPromptProvider.createUserInfoPrompt(host, session);
                }
                if (userInfo == null) {
                    userInfo = new JschUserInfo(auth);
                }

                session.setUserInfo(userInfo);
                session.setHostKeyAlias(host.getHostname());
                setupHostKeyVerification(session, configuration);
                session.setServerAliveInterval(configuration.getIntProperty(SSHConstants.PROP_ALIVE_INTERVAL));
                session.setTimeout(configuration.getIntProperty(SSHConstants.PROP_CONNECT_TIMEOUT));

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

    private void setupHostKeyVerification(Session session, DBWHandlerConfiguration configuration) throws JSchException {
        if (DBWorkbench.getPlatform().getApplication().isHeadlessMode() ||
            configuration.getBooleanProperty(SSHConstants.PROP_BYPASS_HOST_VERIFICATION)) {
            session.setConfig("StrictHostKeyChecking", "no");
        } else {
            File knownHosts = SSHUtils.getKnownSshHostsFileOrNull();
            if (knownHosts != null) {
                try {
                    jsch.setKnownHosts(knownHosts.getAbsolutePath());
                    session.setConfig("StrictHostKeyChecking", "ask");
                } catch (JSchException e) {
                    if (e.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        if (DBWorkbench.getPlatformUI().confirmAction(JSCHUIMessages.ssh_file_corrupted_dialog_title, 
                            JSCHUIMessages.ssh_file_corrupted_dialog_message, true)) {
                            session.setConfig("StrictHostKeyChecking", "no");
                        } else {
                            throw e;
                        }
                    }
                }
            } else {
                session.setConfig("StrictHostKeyChecking", "ask");
            }
        }
    }

    @Override
    public synchronized void closeTunnel(DBRProgressMonitor monitor) {
        if (ArrayUtils.isEmpty(sessions)) {
            return;
        }
        RuntimeUtils.runTask(monitor1 -> {
            Session[] sessions = this.sessions;
            if (ArrayUtils.isEmpty(sessions)) {
                return;
            }
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
            initTunnel(monitor, savedConfiguration, savedConnectionInfo);
        }
    }

    @Override
    public void getFile(
        @NotNull String src,
        @NotNull OutputStream dst,
        @NotNull DBRProgressMonitor monitor
    ) throws DBException, IOException {
        final ChannelSftp channel = openSftpChannel();

        try {
            channel.get(src, dst, new SftpProgressMonitorAdapter(monitor));
        } catch (SftpException e) {
            throw new IOException("Error downloading file through SFTP channel", e);
        } finally {
            channel.disconnect();
        }
    }

    @Override
    public void putFile(
        @NotNull InputStream src,
        @NotNull String dst,
        @NotNull DBRProgressMonitor monitor
    ) throws DBException, IOException {
        final ChannelSftp channel = openSftpChannel();

        try {
            channel.put(src, dst, new SftpProgressMonitorAdapter(monitor));
        } catch (SftpException e) {
            throw new IOException("Error uploading file through SFTP channel", e);
        } finally {
            channel.disconnect();
        }
    }

    @NotNull
    private ChannelSftp openSftpChannel() throws DBException, IOException {
        final Session[] sessions = this.sessions;
        final ChannelSftp channel;

        if (ArrayUtils.isEmpty(sessions)) {
            throw new DBException("No active session available");
        }

        try {
            channel = (ChannelSftp) sessions[sessions.length - 1].openChannel(CHANNEL_TYPE_SFTP);
            channel.connect();
        } catch (JSchException e) {
            throw new IOException("Error opening SFTP channel", e);
        }

        return channel;
    }

    private void addIdentityKeyValue(String keyValue, String password) throws JSchException {
        byte[] keyBinary = keyValue.getBytes(StandardCharsets.UTF_8);
        if (!CommonUtils.isEmpty(password)) {
            jsch.addIdentity("key", keyBinary, null, password.getBytes());
        } else {
            jsch.addIdentity("key", keyBinary, null, null);
        }
    }

    private void addIdentityKeyFile(DBRProgressMonitor monitor, DBPDataSourceContainer dataSource, Path key, String password) throws IOException, JSchException {
        String header;

        try (BufferedReader reader = Files.newBufferedReader(key)) {
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
            Path dir = DBWorkbench.getPlatform().getTempFolder(monitor, "openssh-pkey");
            Path tmp = dir.resolve(id + ".pem");

            Files.copy(key, tmp, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);

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
                    "-f", tmp.toAbsolutePath().toString(),
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
                try {
                    Files.delete(tmp);
                } catch (IOException e) {
                    log.debug("Failed to delete private key file", e);
                }
            }
        } else {
            addIdentityKey0(key, password);
        }
    }

    private void addIdentityKey0(Path key, String password) throws JSchException {
        if (!CommonUtils.isEmpty(password)) {
            jsch.addIdentity(key.toAbsolutePath().toString(), password);
        } else {
            jsch.addIdentity(key.toAbsolutePath().toString());
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

    private static class SftpProgressMonitorAdapter implements SftpProgressMonitor {
        private final DBRProgressMonitor delegate;

        public SftpProgressMonitorAdapter(@NotNull DBRProgressMonitor delegate) {
            this.delegate = delegate;
        }

        @Override
        public void init(int op, String src, String dst, long max) {
            if (op == PUT) {
                delegate.beginTask(NLS.bind("Upload file ''{0}'' -> ''{1}''", src, dst), (int) max);
            } else {
                delegate.beginTask(NLS.bind("Download file ''{0}'' -> ''{1}''", src, dst), (int) max);
            }
        }

        @Override
        public boolean count(long count) {
            delegate.worked((int) count);

            return !delegate.isCanceled();
        }

        @Override
        public void end() {
            delegate.done();
        }
    }
}
