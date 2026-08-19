/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHAuthConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class JSCHSessionController extends AbstractSessionController<JSCHSession> {
    private static final Log log = Log.getLog(JSCHSessionController.class);

    public JSCHSessionController() {
        JSch.setLogger(new JschLogger());
    }

    @NotNull
    @Override
    protected JSCHSession createSession() {
        return new JSCHSession(this);
    }

    @NotNull
    protected Session createNewSession(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration destination
    ) throws DBException {
        final JSch jsch = new JSch();
        final SSHAuthConfiguration auth = destination.auth();

        if (auth instanceof SSHAuthConfiguration.Password) {
            log.debug("SSHSessionController: Using password authentication");
        } else if (auth instanceof SSHAuthConfiguration.KeyFile key) {
            log.debug("SSHSessionController: Using public key authentication");
            try {
                addIdentityKeyFile(jsch, monitor, configuration.getDataSource(), Path.of(key.path()), key.password());
            } catch (Exception e) {
                throw new DBException("Error adding identity key", e);
            }
        } else if (auth instanceof SSHAuthConfiguration.KeyData key) {
            log.debug("SSHSessionController: Using public key authentication");
            try {
                addIdentityKeyValue(jsch, key.data(), key.password());
            } catch (Exception e) {
                throw new DBException("Error adding identity key", e);
            }
        } else if (auth instanceof SSHAuthConfiguration.Agent) {
            log.debug("SSHSessionController: Using agent authentication");
            jsch.setIdentityRepository(createAgentIdentityRepository());
        }

        try {
            final Session session = jsch.getSession(
                destination.username(),
                destination.hostname(),
                destination.port()
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
            session.setHostKeyAlias(destination.hostname());
            session.setServerAliveInterval(configuration.getIntProperty(SSHConstants.PROP_ALIVE_INTERVAL));
            session.setTimeout(configuration.getIntProperty(
                SSHConstants.PROP_CONNECT_TIMEOUT,
                SSHConstants.DEFAULT_CONNECT_TIMEOUT));
            setupHostKeyVerification(jsch, session, configuration);

            if (auth instanceof SSHAuthConfiguration.Password) {
                session.setConfig("PreferredAuthentications", "password,keyboard-interactive");
            } else {
                session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            }

            session.connect();

            return session;
        } catch (JSchException e) {
            throw new DBException("Failed to open JSch tunnel", e);
        }
    }

    private void setupHostKeyVerification(
        @NotNull JSch jsch,
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

    private void addIdentityKeyValue(@NotNull JSch jsch, String keyValue, String password) throws JSchException {
        byte[] keyBinary = keyValue.getBytes(StandardCharsets.UTF_8);
        if (!CommonUtils.isEmpty(password)) {
            jsch.addIdentity("key", keyBinary, null, password.getBytes());
        } else {
            jsch.addIdentity("key", keyBinary, null, null);
        }
    }

    private void addIdentityKeyFile(
        @NotNull JSch jsch,
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBPDataSourceContainer dataSource,
        @NotNull Path key,
        @Nullable String password
    ) throws JSchException {
        if (CommonUtils.isEmpty(password)) {
            jsch.addIdentity(key.toAbsolutePath().toString());
        } else {
            jsch.addIdentity(key.toAbsolutePath().toString(), password.getBytes(StandardCharsets.UTF_8));
        }
    }

    private record JschUserInfo(@NotNull SSHAuthConfiguration configuration) implements UserInfo, UIKeyboardInteractive {
        @Override
        public String getPassphrase() {
            return ((SSHAuthConfiguration.WithPassword) configuration).password();
        }

        @Override
        public String getPassword() {
            return getPassphrase();
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
            return new String[]{getPassphrase()};
        }
    }

    private static class JschLogger implements Logger {
        private static final Pattern[] SENSITIVE_DATA_PATTERNS = {
            Pattern.compile("^Connecting to (.*?) port"),
            Pattern.compile("^Disconnecting from (.*?) port"),
            Pattern.compile("^Host '(.*?)'"),
            Pattern.compile("^Permanently added '(.*?)'")
        };

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

            for (Pattern pattern : SENSITIVE_DATA_PATTERNS) {
                message = CommonUtils.replaceFirstGroup(message, pattern, 1, SecurityUtils::mask);
            }

            log.debug("SSH: " + levelStr + ": " + message);
        }
    }
}
