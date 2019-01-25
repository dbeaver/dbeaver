/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.jsch.ui.UserInfoPrompter;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * SSH tunnel
 */
public class SSHImplementationJsch extends SSHImplementationAbstract {

    private static final Log log = Log.getLog(SSHImplementationJsch.class);

    private static transient JSch jsch;
    private transient volatile Session session;

    @Override
    protected void setupTunnel(DBRProgressMonitor monitor, DBWHandlerConfiguration configuration, String dbHost, String sshHost, String aliveInterval, int sshPortNum, File privKeyFile, int connectTimeout, int dbPort, int localPort) throws DBException, IOException {
        try {
            if (jsch == null) {
                jsch = new JSch();
                JSch.setLogger(new LoggerProxy());
            }
            if (privKeyFile != null) {
                if (!CommonUtils.isEmpty(configuration.getPassword())) {
                    jsch.addIdentity(privKeyFile.getAbsolutePath(), configuration.getPassword());
                } else {
                    jsch.addIdentity(privKeyFile.getAbsolutePath());
                }
            }

            log.debug("Instantiate SSH tunnel");
            session = jsch.getSession(configuration.getUserName(), sshHost, sshPortNum);
            session.setConfig("StrictHostKeyChecking", "no");
            //session.setConfig("PreferredAuthentications", "password,publickey,keyboard-interactive");
            session.setConfig("PreferredAuthentications",
                    privKeyFile != null ? "publickey" : "password,keyboard-interactive");
            session.setConfig("ConnectTimeout", String.valueOf(connectTimeout));

            // Use Eclipse standard prompter
            UserInfoCustom ui = new UserInfoCustom(configuration);

            session.setUserInfo(ui);
            if (!CommonUtils.isEmpty(aliveInterval)) {
                session.setServerAliveInterval(Integer.parseInt(aliveInterval));
            }
            log.debug("Connect to tunnel host");
            session.connect(connectTimeout);
            try {
                session.setPortForwardingL(localPort, dbHost, dbPort);
            } catch (JSchException e) {
                closeTunnel(monitor);
                throw e;
            }
        } catch (JSchException e) {
            throw new DBException("Cannot establish tunnel", e);
        }
    }

    @Override
    public void closeTunnel(DBRProgressMonitor monitor) throws DBException, IOException {
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
    public String getClientVersion() {
        return session == null ? null : session.getClientVersion();
    }

    @Override
    public String getServerVersion() {
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
            initTunnel(monitor, null, savedConfiguration, savedConnectionInfo);
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
            System.out.printf("Keyboard interactive auth");
            return new String[] { configuration.getPassword() } ;
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

    private class UserInfoCustom extends UserInfoPrompter {
        private final DBWHandlerConfiguration configuration;
        UserInfoCustom(DBWHandlerConfiguration configuration) {
            super(SSHImplementationJsch.this.session);
            this.configuration = configuration;
        }

        @Override
        public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt, boolean[] echo) {
            if (configuration.isSavePassword()) {
                setPassword(configuration.getPassword());
            }
            return super.promptKeyboardInteractive(destination, name, instruction, prompt, echo);
        }

        @Override
        public boolean promptPassword(String message) {
            if (configuration.isSavePassword()) {
                setPassword(configuration.getPassword());
                return true;
            }
            return super.promptPassword(message);
        }

        @Override
        public boolean promptPassphrase(String message) {
            if (configuration.isSavePassword()) {
                setPassphrase(configuration.getPassword());
                return true;
            }
            return super.promptPassphrase(message);
        }

        @Override
        public void showMessage(String message) {
            // do not show ssh banner
        }
    }

}
