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

import com.jcraft.jsch.*;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHAuthConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHPortForwardConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Rc;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SSHSessionControllerJsch implements SSHSessionController {
    private static final Log log = Log.getLog(SSHSessionControllerJsch.class);

    private final Map<SSHHostConfiguration, JschSession> sessions = new ConcurrentHashMap<>();
    private final JSch jsch;
    private AgentIdentityRepository agentIdentityRepository;

    public SSHSessionControllerJsch() {
        jsch = new JSch();
        JSch.setLogger(new JschLogger());
    }

    @NotNull
    @Override
    public synchronized SSHSession acquireSession(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration destination,
        @Nullable SSHSession origin,
        @Nullable SSHPortForwardConfiguration portForward
    ) throws DBException, IOException {
        if (origin != null && !(origin instanceof JschSession)) {
            throw new DBException("Invalid origin session type: " + origin.getClass().getName());
        }

        if (origin != null) {
            return acquireNestedSession(monitor, configuration, destination, (JschSession) origin, portForward);
        } else {
            return acquireSession(monitor, configuration, destination, portForward);
        }
    }

    @NotNull
    @Override
    public SSHSession[] getSessions() {
        return sessions.values().toArray(SSHSession[]::new);
    }

    @NotNull
    private JschJumpSession acquireNestedSession(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration destination,
        @NotNull JschSession origin,
        @Nullable SSHPortForwardConfiguration portForward
    ) throws DBException, IOException {
        final SSHPortForwardConfiguration jumpPortForward = origin.setupPortForward(
            destination.getHostname(),
            destination.getPort()
        );

        final SSHHostConfiguration jumpHost = new SSHHostConfiguration(
            destination.getUsername(),
            jumpPortForward.getLocalHost(),
            jumpPortForward.getLocalPort(),
            destination.getAuthConfiguration()
        );

        final JschSession jumpSession = acquireSession(
            monitor,
            configuration,
            jumpHost,
            portForward
        );

        return new JschJumpSession(jumpSession, origin, jumpPortForward);
    }

    @NotNull
    private JschSession acquireSession(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration destination,
        @Nullable SSHPortForwardConfiguration portForward
    ) throws DBException, IOException {
        JschSession session = sessions.get(destination);

        if (session == null) {
            final Consumer<Session> onDisconnect = target -> {
                log.debug("SSHSessionController: Closing session to %s:%d".formatted(target.getHost(), target.getPort()));
                target.disconnect();
                sessions.remove(destination);
            };

            log.debug("SSHSessionController: Creating new session to " + destination);
            session = new JschSession(createNewSession(monitor, configuration, destination), portForward, onDisconnect);
            sessions.put(destination, session);
            return session;
        }

        log.debug("SSHSessionController: Reusing existing session to " + destination);
        return new JschSharedSession(session, portForward);
    }

    @NotNull
    private Session createNewSession(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration destination
    ) throws DBException, IOException {
        final SSHAuthConfiguration auth = destination.getAuthConfiguration();

        switch (auth.getType()) {
            case PASSWORD -> {
                log.debug("SSHSessionController: Using password authentication");
            }
            case PUBLIC_KEY -> {
                log.debug("SSHSessionController: Using public key authentication");

                try {
                    if (auth.getKeyFile() != null) {
                        addIdentityKeyFile(monitor, configuration.getDataSource(), auth.getKeyFile(), auth.getPassword());
                    } else {
                        addIdentityKeyValue(auth.getKeyValue(), auth.getPassword());
                    }
                } catch (JSchException e) {
                    throw new DBException("Cannot add identity key", e);
                }
            }
            case AGENT -> {
                log.debug("SSHSessionController: Using agent authentication");
                jsch.setIdentityRepository(createAgentIdentityRepository());
            }
        }

        try {
            final Session session = jsch.getSession(
                destination.getUsername(),
                destination.getHostname(),
                destination.getPort()
            );

            UserInfo userInfo = null;
            JSCHUserInfoPromptProvider userInfoPromptProvider = GeneralUtils.adapt(this, JSCHUserInfoPromptProvider.class);
            if (userInfoPromptProvider != null) {
                userInfo = userInfoPromptProvider.createUserInfoPrompt(destination, session);
            }
            if (userInfo == null) {
                userInfo = new JschUserInfo(auth);
            }

            session.setUserInfo(userInfo);
            session.setHostKeyAlias(destination.getHostname());
            session.setServerAliveInterval(configuration.getIntProperty(SSHConstants.PROP_ALIVE_INTERVAL));
            session.setTimeout(configuration.getIntProperty(SSHConstants.PROP_CONNECT_TIMEOUT));
            setupHostKeyVerification(session, configuration);

            if (auth.getType() == SSHConstants.AuthType.PASSWORD) {
                session.setConfig("PreferredAuthentications", "password,keyboard-interactive");
            } else {
                session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            }

            session.connect();

            return session;
        } catch (JSchException e) {
            throw new DBException("Failed to create session", e);
        }
    }

    @NotNull
    private IdentityRepository createAgentIdentityRepository() throws DBException {
        if (agentIdentityRepository == null) {
            AgentConnector connector = null;

            try {
                connector = new PageantConnector();
                log.debug("SSHSessionController: connected with pageant");
            } catch (Exception e) {
                log.debug("SSHSessionController: pageant connect exception", e);
            }

            if (connector == null) {
                try {
                    connector = new SSHAgentConnector(new JUnixSocketFactory());
                    log.debug("SSHSessionController: Connected with ssh-agent");
                } catch (Exception e) {
                    log.debug("SSHSessionController: ssh-agent connection exception", e);
                }
            }

            if (connector == null) {
                throw new DBException("Unable to initialize SSH agent");
            }

            agentIdentityRepository = new AgentIdentityRepository(connector);
        }

        return agentIdentityRepository;
    }

    private void setupHostKeyVerification(
        @NotNull Session session,
        @NotNull DBWHandlerConfiguration configuration
    ) throws JSchException {
        if (DBWorkbench.getPlatform().getApplication().isHeadlessMode() ||
            configuration.getBooleanProperty(SSHConstants.PROP_BYPASS_HOST_VERIFICATION)
        ) {
            session.setConfig("StrictHostKeyChecking", "no");
        } else {
            final File knownHosts = SSHUtils.getKnownSshHostsFileOrNull();
            if (knownHosts != null) {
                try {
                    jsch.setKnownHosts(knownHosts.getAbsolutePath());
                    session.setConfig("StrictHostKeyChecking", "ask");
                } catch (JSchException e) {
                    if (e.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        if (DBWorkbench.getPlatformUI().confirmAction(
                            JSCHUIMessages.ssh_file_corrupted_dialog_title,
                            JSCHUIMessages.ssh_file_corrupted_dialog_message,
                            true
                        )) {
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

    private void addIdentityKeyValue(String keyValue, String password) throws JSchException {
        byte[] keyBinary = keyValue.getBytes(StandardCharsets.UTF_8);
        if (!CommonUtils.isEmpty(password)) {
            jsch.addIdentity("key", keyBinary, null, password.getBytes());
        } else {
            jsch.addIdentity("key", keyBinary, null, null);
        }
    }

    private void addIdentityKeyFile(
        DBRProgressMonitor monitor,
        DBPDataSourceContainer dataSource,
        Path key,
        String password
    ) throws IOException, JSchException {
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
                    "-q"
                )
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

    private record JschUserInfo(@NotNull SSHAuthConfiguration configuration) implements UserInfo, UIKeyboardInteractive {
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
        public String[] promptKeyboardInteractive(
            String destination,
            String name,
            String instruction,
            String[] prompt,
            boolean[] echo
        ) {
            log.debug("JSCH keyboard interactive auth");
            return new String[]{configuration.getPassword()};
        }
    }

    private static class JschLogger implements Logger {
        @Override
        public boolean isEnabled(int level) {
            return true;
        }

        @Override
        public void log(int level, String message) {
            String levelStr = switch (level) {
                case INFO -> "INFO";
                case WARN -> "WARN";
                case ERROR -> "ERROR";
                case FATAL -> "FATAL";
                default -> "DEBUG";
            };

            log.debug("SSH: " + levelStr + ": " + message);
        }
    }

    private record SftpProgressMonitorAdapter(@NotNull DBRProgressMonitor delegate) implements SftpProgressMonitor {
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

    public static class JschSession implements SSHSession {
        private final Rc<Session> session;
        private final SSHPortForwardConfiguration portForward;

        public JschSession(
            @NotNull Session session,
            @Nullable SSHPortForwardConfiguration portForward,
            @NotNull Consumer<Session> onDisconnect
        ) throws DBException {
            this.session = new Rc<>(session, onDisconnect);
            this.portForward = portForward;

            if (portForward != null) {
                setupPortForward(portForward);
            }

            log.debug("JschSession.new");
        }

        public JschSession(
            @NotNull Rc<Session> session,
            @Nullable SSHPortForwardConfiguration portForward
        ) throws DBException {
            this.session = session;
            this.portForward = portForward;

            if (portForward != null) {
                setupPortForward(portForward);
            }

            log.debug("JschSession.new");
        }

        @Override
        public void getFile(
            @NotNull String src,
            @NotNull OutputStream dst,
            @NotNull DBRProgressMonitor monitor
        ) throws IOException {
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
        ) throws IOException {
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
        private ChannelSftp openSftpChannel() throws IOException {
            final ChannelSftp channel;

            try {
                channel = (ChannelSftp) session.get().openChannel("sftp");
                channel.connect();
            } catch (JSchException e) {
                throw new IOException("Error opening SFTP channel", e);
            }

            return channel;
        }

        @NotNull
        @Override
        public String getClientVersion() {
            return session.get().getClientVersion();
        }

        @NotNull
        @Override
        public String getServerVersion() {
            return session.get().getServerVersion();
        }

        @Override
        public void invalidate(@NotNull DBRProgressMonitor monitor) throws DBException {
            throw new DBException("Not implemented");
        }

        @Override
        public void release(@NotNull DBRProgressMonitor monitor) throws DBException {
            if (portForward != null) {
                removePortForward(portForward);
            }

            final Session inner = session.get();
            log.debug("SSHSessionController: Releasing session to %s:%d".formatted(inner.getHost(), inner.getPort()));
            session.release();
        }

        @Property(name = "Destination", viewable = true, order = 1)
        public String getDestinationInfo() {
            return session.get().getUserName() + "@" + session.get().getHost() + ":" + session.get().getPort();
        }

        @Property(name = "Port Forwarding", viewable = true, order = 2)
        public String getPortForwardInfo() {
            return portForward != null ? portForward.getLocalPort() + " <- " + portForward.getRemotePort() : null;
        }

        @Property(name = "Connections", viewable = true, order = 3)
        public int getConnections() {
            return session.getCount();
        }

        @NotNull
        protected SSHPortForwardConfiguration setupPortForward(
            @NotNull String remoteHost,
            int remotePort
        ) throws DBException {
            return setupPortForward(new SSHPortForwardConfiguration("127.0.0.1", 0, remoteHost, remotePort));
        }

        @NotNull
        protected SSHPortForwardConfiguration setupPortForward(
            @NotNull SSHPortForwardConfiguration portForward
        ) throws DBException {
            log.debug("SSHSessionController: Set up port forwarding " + portForward);

            try {
                final int port = session.get().setPortForwardingL(
                    portForward.getLocalHost(),
                    portForward.getLocalPort(),
                    portForward.getRemoteHost(),
                    portForward.getRemotePort()
                );

                return new SSHPortForwardConfiguration(
                    portForward.getLocalHost(),
                    port,
                    portForward.getRemoteHost(),
                    portForward.getRemotePort()
                );
            } catch (JSchException e) {
                throw new DBException("Failed to set up port forwarding", e);
            }
        }

        protected void removePortForward(@NotNull SSHPortForwardConfiguration portForward) throws DBException {
            log.debug("SSHSessionController: Remove port forwarding " + portForward);

            try {
                session.get().delPortForwardingL(portForward.getLocalHost(), portForward.getLocalPort());
            } catch (JSchException e) {
                throw new DBException("Failed to remove port forwarding", e);
            }
        }
    }

    public static class JschSharedSession extends JschSession {
        public JschSharedSession(@NotNull JschSession parent, @Nullable SSHPortForwardConfiguration portForward) throws DBException {
            super(parent.session.retain(), portForward);
            log.debug("JschSharedSession.new");
        }
    }

    public static class JschJumpSession extends JschSession {
        private final JschSession origin;
        private final SSHPortForwardConfiguration portForward;

        public JschJumpSession(
            @NotNull JschSession destination,
            @NotNull JschSession origin,
            @NotNull SSHPortForwardConfiguration portForward
        ) throws DBException {
            super(destination.session, null);
            this.origin = origin;
            this.portForward = portForward;
            log.debug("JschJumpSession.new");
        }

        @Override
        public void invalidate(@NotNull DBRProgressMonitor monitor) throws DBException {
            origin.invalidate(monitor);
            super.invalidate(monitor);
        }

        @Override
        public void release(@NotNull DBRProgressMonitor monitor) throws DBException {
            origin.removePortForward(portForward);
            origin.release(monitor);
            super.release(monitor);
        }
    }
}
