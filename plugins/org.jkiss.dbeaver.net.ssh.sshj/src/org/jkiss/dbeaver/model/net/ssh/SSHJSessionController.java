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

import com.jcraft.jsch.Identity;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.LoggerFactory;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.method.AuthMethod;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.PasswordUtils;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHAuthConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SSHJSessionController extends AbstractSessionController<SSHJSession> {
    private static final Log log = Log.getLog(SSHJSessionController.class);

    @NotNull
    @Override
    protected SSHJSession createSession() {
        return new SSHJSession(this);
    }

    @NotNull
    protected SSHClient createNewSession(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration host
    ) throws DBException {
        final int connectTimeout = configuration.getIntProperty(
            SSHConstants.PROP_CONNECT_TIMEOUT,
            SSHConstants.DEFAULT_CONNECT_TIMEOUT);
        final int keepAliveInterval = configuration.getIntProperty(SSHConstants.PROP_ALIVE_INTERVAL) / 1000; // sshj uses seconds for keep-alive interval

        final SSHAuthConfiguration auth = host.auth();
        final SSHClient client = new SSHClient();

        client.setConnectTimeout(connectTimeout);
        client.getConnection().getKeepAlive().setKeepAliveInterval(keepAliveInterval);
        client.getTransport().getConfig().setLoggerFactory(new FilterLoggerFactory());

        try {
            setupHostKeyVerification(client, configuration, host);
        } catch (IOException e) {
            log.debug("Error loading known hosts: " + e.getMessage());
        }

        monitor.subTask(String.format("Instantiate tunnel to %s:%d", host.hostname(), host.port()));

        try {
            client.connect(host.hostname(), host.port());

            if (auth instanceof SSHAuthConfiguration.Password password && password.password() != null) {
                client.authPassword(host.username(), password.password());
            } else if (auth instanceof SSHAuthConfiguration.KeyFile key) {
                if (CommonUtils.isEmpty(key.password())) {
                    client.authPublickey(host.username(), key.path());
                } else {
                    client.authPublickey(host.username(), client.loadKeys(key.path(), key.password().toCharArray()));
                }
            } else if (auth instanceof SSHAuthConfiguration.KeyData key) {
                final PasswordFinder finder = CommonUtils.isEmpty(key.password())
                    ? null
                    : PasswordUtils.createOneOff(key.password().toCharArray());
                client.authPublickey(host.username(), client.loadKeys(key.data(), null, finder));
            } else if (auth instanceof SSHAuthConfiguration.Agent) {
                final List<AuthMethod> methods = new ArrayList<>();
                for (Object identity : createAgentIdentityRepository().getIdentities()) {
                    methods.add(new DBeaverAuthAgent((Identity) identity));
                }
                client.auth(host.username(), methods);
            }
        } catch (Exception e) {
            throw new DBException("Error establishing SSHJ tunnel", e);
        }

        return client;
    }

    private static void setupHostKeyVerification(
        @NotNull SSHClient client,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration actualHostConfiguration
    ) throws IOException {
        if (DBWorkbench.getPlatform().getApplication().isHeadlessMode() ||
            configuration.getBooleanProperty(SSHConstants.PROP_BYPASS_HOST_VERIFICATION)
        ) {
            client.addHostKeyVerifier(new PromiscuousVerifier());
            client.getTransport().getConfig().setVerifyHostKeyCertificates(false);
        } else {
            client.addHostKeyVerifier(new KnownHostsVerifier(SSHUtils.getKnownSshHostsFileOrDefault(), actualHostConfiguration));
        }

        client.loadKnownHosts();
    }

    private static class FilterLoggerFactory implements LoggerFactory {
        private static final Set<String> FILTERED_OUT_CLASSES = Set.of("net.schmizz.sshj.common.StreamCopier");

        @Override
        public Logger getLogger(String s) {
            if (FILTERED_OUT_CLASSES.contains(s)) {
                return NOPLogger.NOP_LOGGER;
            } else {
                return org.slf4j.LoggerFactory.getLogger(s);
            }
        }

        @Override
        public Logger getLogger(Class<?> cls) {
            return getLogger(cls.getName());
        }
    }
}
