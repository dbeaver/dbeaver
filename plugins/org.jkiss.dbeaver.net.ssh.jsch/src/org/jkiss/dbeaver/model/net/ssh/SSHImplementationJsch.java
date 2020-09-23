/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.SSHConstants.AuthType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
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
    private transient volatile Session session;

    @Override
    protected synchronized void setupTunnel(DBRProgressMonitor monitor, DBWHandlerConfiguration configuration, String sshHost, int aliveInterval, int sshPortNum, File privKeyFile, int connectTimeout, String sshLocalHost, int sshLocalPort, String sshRemoteHost, int sshRemotePort) throws DBException, IOException {
        try {
            if (jsch == null) {
                jsch = new JSch();
                JSch.setLogger(new LoggerProxy());
            }

            String autoTypeString = CommonUtils.toString(configuration.getProperty(SSHConstants.PROP_AUTH_TYPE));
            AuthType authType = CommonUtils.isEmpty(autoTypeString) ?
                    (privKeyFile == null ? AuthType.PASSWORD : AuthType.PUBLIC_KEY) :
                    CommonUtils.valueOf(AuthType.class, autoTypeString, AuthType.PASSWORD);
            if (authType == AuthType.PUBLIC_KEY) {
                addIdentityKey(monitor, configuration.getDataSource(), privKeyFile, configuration.getPassword());
            } else if (authType == AuthType.AGENT) {
                log.debug("Creating identityRepository");
                IdentityRepository identityRepository = new DBeaverIdentityRepository(this, getAgentData());
                jsch.setIdentityRepository(identityRepository);
            }

            log.debug("Instantiate SSH tunnel");
            session = jsch.getSession(configuration.getUserName(), sshHost, sshPortNum);
            session.setConfig("StrictHostKeyChecking", "no");

            if (authType == AuthType.PASSWORD) {
                session.setConfig("PreferredAuthentications", "password,keyboard-interactive");
            } else {
                session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            }
            session.setConfig("ConnectTimeout", String.valueOf(connectTimeout));

            // Use Eclipse standard prompter
            UserInfo userInfo = null;
            JSCHUserInfoPromptProvider promptProvider = GeneralUtils.adapt(this, JSCHUserInfoPromptProvider.class);
            if (promptProvider != null) {
                userInfo = promptProvider.createUserInfoPrompt(configuration, session);
            }
            if (userInfo == null) {
                userInfo = new UIUserInfo(configuration);
            }
            session.setUserInfo(userInfo);

            if (aliveInterval != 0) {
                session.setServerAliveInterval(aliveInterval);
            }
            log.debug("Connect to tunnel host");
            session.connect(connectTimeout);
            try {
                if (CommonUtils.isEmpty(sshLocalHost)) {
                    session.setPortForwardingL(sshLocalPort, sshRemoteHost, sshRemotePort);
                } else {
                    session.setPortForwardingL(sshLocalHost, sshLocalPort, sshRemoteHost, sshRemotePort);
                }
            } catch (JSchException e) {
                closeTunnel(monitor);
                throw e;
            }
        } catch (JSchException e) {
            throw new DBException("Cannot establish tunnel", e);
        }
    }

    @Override
    public synchronized void closeTunnel(DBRProgressMonitor monitor) throws DBException, IOException {
        if (session != null) {
            RuntimeUtils.runTask(monitor1 -> {
                if (session != null) {
                    try {
                        session.disconnect();
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            }, "Close SSH session", 1000);
            session = null;
        }
    }

    @Override
    public synchronized String getClientVersion() {
        return session == null ? null : session.getClientVersion();
    }

    @Override
    public synchronized String getServerVersion() {
        return session == null ? null : session.getServerVersion();
    }

    @Override
    public void invalidateTunnel(DBRProgressMonitor monitor) throws DBException, IOException {
        // Do not test - just reopen the tunnel. Otherwise it may take too much time.
        boolean isAlive = false;//session != null && session.isConnected();
        if (isAlive) {
            try {
                session.sendKeepAliveMsg();
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
            log.debug("Attempting to convert unsupported key");

            File dir = DBWorkbench.getPlatform().getTempFolder(monitor, "openssh-pkey");
            File tmp = new File(dir, dataSource.getId() + ".pem");

            Files.copy(key.toPath(), tmp.toPath(), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);

            Process process = new ProcessBuilder()
                .command(
                    "ssh-keygen",
                    "-p",
                    "-m", "PEM",
                    "-f", tmp.getAbsolutePath(),
                    "-q",
                    "-N", '"' + (CommonUtils.isEmpty(password) ? "" : password) + '"')
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

    private class UIUserInfo implements UserInfo, UIKeyboardInteractive {
        DBWHandlerConfiguration configuration;

        private UIUserInfo(DBWHandlerConfiguration configuration) {
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

    private class LoggerProxy implements Logger {
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
