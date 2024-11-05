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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCInvalidatePhase;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWTunnel;
import org.jkiss.dbeaver.model.net.DBWUtils;
import org.jkiss.dbeaver.model.net.ssh.config.SSHAuthConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHPortForwardConfiguration;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHSessionControllerDescriptor;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHSessionControllerRegistry;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.Base64;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * SSH tunnel
 */
public class SSHTunnelImpl implements DBWTunnel {

    private static final Log log = Log.getLog(SSHTunnelImpl.class);
    private static final String DEF_IMPLEMENTATION = "sshj";

    private DBWHandlerConfiguration configuration;
    private SSHSessionController controller;
    private SSHSession session;
    private final List<Runnable> listeners = new ArrayList<>();

    @Nullable
    @Override
    public SSHSession getImplementation() {
        return session;
    }

    @Override
    public void addCloseListener(@NotNull Runnable listener) {
        this.listeners.add(listener);
    }

    @NotNull
    @Override
    public DBPConnectionConfiguration initializeHandler(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull DBPConnectionConfiguration connectionInfo
    ) throws DBException, IOException {
        this.configuration = configuration;

        String implId = configuration.getStringProperty(SSHConstants.PROP_IMPLEMENTATION);
        if (CommonUtils.isEmpty(implId)) {
            // Backward compatibility
            implId = DEF_IMPLEMENTATION;
        }

        SSHSessionControllerDescriptor descriptor = SSHSessionControllerRegistry.getInstance().getDescriptor(implId);
        if (descriptor == null) {
            descriptor = SSHSessionControllerRegistry.getInstance().getDescriptor(DEF_IMPLEMENTATION);
        }
        if (descriptor == null) {
            throw new DBException("Can't find SSH tunnel implementation '" + implId + "'");
        }

        try {
            controller = descriptor.getInstance();
        } catch (DBException e) {
            throw new DBException("Can't create SSH tunnel implementation '" + implId + "'", e);
        }

        return initTunnel(monitor, configuration, connectionInfo, controller);
    }

    @Override
    public boolean matchesParameters(@NotNull String host, int port) {
        if (host.equals(configuration.getStringProperty(DBWHandlerConfiguration.PROP_HOST))) {
            int sshPort = configuration.getIntProperty(DBWHandlerConfiguration.PROP_PORT);
            return sshPort == port;
        }
        return false;
    }

    @NotNull
    @Override
    public AuthCredentials getRequiredCredentials(@NotNull DBWHandlerConfiguration configuration) throws DBException {
        if (!configuration.isEnabled() || !configuration.isSecured()) {
            return AuthCredentials.NONE;
        }

        // TODO: Check jump hosts as well
        final SSHHostConfiguration[] hosts = SSHUtils.loadHostConfigurations(configuration, false);
        final SSHHostConfiguration host = hosts[hosts.length - 1];

        if (host.auth() instanceof SSHAuthConfiguration.WithPassword password && password.savePassword()) {
            return AuthCredentials.NONE;
        } else if (host.auth() instanceof SSHAuthConfiguration.KeyFile key) {
            return SSHUtils.isKeyFileEncrypted(key.path()) ? AuthCredentials.PASSWORD : AuthCredentials.NONE;
        } else if (host.auth() instanceof SSHAuthConfiguration.KeyData key) {
            return SSHUtils.isKeyEncrypted(Base64.decode(key.data())) ? AuthCredentials.PASSWORD : AuthCredentials.NONE;
        } else if (host.auth() instanceof SSHAuthConfiguration.Agent) {
            return AuthCredentials.NONE;
        } else {
            return AuthCredentials.CREDENTIALS;
        }
    }

    @Override
    public void invalidateHandler(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull DBCInvalidatePhase phase
    ) throws DBException {
        if (session == null) {
            return;
        }
        try {
            monitor.subTask("Invalidate SSH tunnel");
            controller.invalidate(
                monitor,
                session,
                phase,
                configuration,
                dataSource.getContainer().getPreferenceStore().getInt(ModelPreferences.CONNECTION_VALIDATION_TIMEOUT)
            );
        } catch (DBException e) {
            log.debug("Error invalidating SSH tunnel. Closing.", e);
            try {
                closeTunnel(monitor);
            } catch (Exception e1) {
                log.error("Error closing broken tunnel", e1);
            }
            throw new DBException("Error invalidating SSH tunnel", e);
        } finally {
            monitor.done();
        }
    }

    @Override
    public void closeTunnel(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (session != null) {
            final DBPDataSourceContainer container = configuration.getDataSource();
            final int timeout = container != null
                ? container.getPreferenceStore().getInt(ModelPreferences.CONNECTION_VALIDATION_TIMEOUT)
                : 0;
            controller.release(monitor, session, configuration, timeout);
        }
        for (Runnable listener : this.listeners) {
            listener.run();
        }
        this.listeners.clear();
    }

    @NotNull
    @Override
    public DBPDataSourceContainer[] getDependentDataSources() {
        return controller.getDependentDataSources(session);
    }

    @NotNull
    private DBPConnectionConfiguration initTunnel(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull DBPConnectionConfiguration connectionInfo,
        @NotNull SSHSessionController controller
    ) throws DBException {
        final SSHHostConfiguration[] hosts = SSHUtils.loadHostConfigurations(configuration, true);
        final SSHPortForwardConfiguration portForward = loadPortForwardConfiguration(configuration, connectionInfo);
        final SSHSession[] sessions = new SSHSession[hosts.length];

        for (int index = 0; index < hosts.length; index++) {
            // NOTE: If acquireSession fails, all previously acquired sessions will not be released. Not sure if it's a problem.
            sessions[index] = controller.acquireSession(
                monitor,
                configuration,
                hosts[index],
                index != 0 ? sessions[index - 1] : null,
                index == hosts.length - 1 ? portForward : null
            );
        }

        session = sessions[sessions.length - 1];

        connectionInfo = new DBPConnectionConfiguration(connectionInfo);
        DBWUtils.updateConfigWithTunnelInfo(configuration, connectionInfo, portForward.localHost(), portForward.localPort());
        return connectionInfo;
    }

    @Nullable
    public SSHSessionController getController() {
        return controller;
    }

    @NotNull
    private static SSHPortForwardConfiguration loadPortForwardConfiguration(
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull DBPConnectionConfiguration connectionInfo
    ) {
        String sshLocalHost = CommonUtils.toString(configuration.getProperty(SSHConstants.PROP_LOCAL_HOST));

        int sshLocalPort = configuration.getIntProperty(SSHConstants.PROP_LOCAL_PORT);
        if (sshLocalPort == 0) {
            sshLocalPort = SSHUtils.findFreePort();
        }

        String sshRemoteHost = CommonUtils.toString(configuration.getProperty(SSHConstants.PROP_REMOTE_HOST));
        if (CommonUtils.isEmpty(sshRemoteHost)) {
            sshRemoteHost = CommonUtils.notEmpty(connectionInfo.getHostName());
        }

        int sshRemotePort = configuration.getIntProperty(SSHConstants.PROP_REMOTE_PORT);
        if (sshRemotePort == 0 && configuration.getDriver() != null) {
            sshRemotePort = CommonUtils.toInt(connectionInfo.getHostPort());
        }

        return new SSHPortForwardConfiguration(sshLocalHost, sshLocalPort, sshRemoteHost, sshRemotePort);
    }
}
