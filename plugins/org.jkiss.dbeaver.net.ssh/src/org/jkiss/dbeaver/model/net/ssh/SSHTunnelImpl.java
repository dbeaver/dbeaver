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
import org.jkiss.dbeaver.registry.DataSourceUtils;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.Base64;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Override
    public SSHSession getImplementation() {
        return session;
    }

    @Override
    public void addCloseListener(Runnable listener) {
        this.listeners.add(listener);
    }

    @Override
    public DBPConnectionConfiguration initializeHandler(
        DBRProgressMonitor monitor,
        DBWHandlerConfiguration configuration,
        DBPConnectionConfiguration connectionInfo
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
    public boolean matchesParameters(String host, int port) {
        if (host.equals(configuration.getStringProperty(DBWHandlerConfiguration.PROP_HOST))) {
            int sshPort = configuration.getIntProperty(DBWHandlerConfiguration.PROP_PORT);
            return sshPort == port;
        }
        return false;
    }

    @Override
    public AuthCredentials getRequiredCredentials(
        @NotNull DBWHandlerConfiguration configuration,
        @Nullable String prefix
    ) {
        String start = prefix;
        if (start == null) {
            start = "";
        }
        if (!configuration.isEnabled() || !configuration.isSecured()) {
            return AuthCredentials.NONE;
        }
        if (configuration.getBooleanProperty(start + RegistryConstants.ATTR_SAVE_PASSWORD)) {
            return AuthCredentials.NONE;
        }

        String sshAuthType = configuration.getStringProperty(start + SSHConstants.PROP_AUTH_TYPE);
        SSHConstants.AuthType authType = SSHConstants.AuthType.PASSWORD;
        if (sshAuthType != null) {
            authType = SSHConstants.AuthType.valueOf(sshAuthType);
        }
        if (authType == SSHConstants.AuthType.PUBLIC_KEY) {
            String privKeyValue = configuration.getSecureProperty(start + SSHConstants.PROP_KEY_VALUE);
            if (privKeyValue != null) {
                byte[] pkBinary = Base64.decode(privKeyValue);
                if (SSHUtils.isKeyEncrypted(pkBinary)) {
                    return AuthCredentials.PASSWORD;
                }
            }
            // Check whether this key is encrypted
            String privKeyPath = configuration.getStringProperty(start + SSHConstants.PROP_KEY_PATH);
            if (!CommonUtils.isEmpty(privKeyPath) && SSHUtils.isKeyFileEncrypted(privKeyPath)) {
                return AuthCredentials.PASSWORD;
            }
            return AuthCredentials.NONE;
        }
        if (authType == SSHConstants.AuthType.AGENT) {
            return AuthCredentials.NONE;
        }
        return AuthCredentials.CREDENTIALS;
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
    public void closeTunnel(DBRProgressMonitor monitor) throws DBException {
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
    ) throws DBException, IOException {
        final SSHHostConfiguration[] hosts = loadHostConfigurations(configuration);
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
            sshRemoteHost = connectionInfo.getHostName();
        }

        int sshRemotePort = configuration.getIntProperty(SSHConstants.PROP_REMOTE_PORT);
        if (sshRemotePort == 0 && configuration.getDriver() != null) {
            sshRemotePort = CommonUtils.toInt(connectionInfo.getHostPort());
        }

        return new SSHPortForwardConfiguration(sshLocalHost, sshLocalPort, sshRemoteHost, sshRemotePort);
    }

    @NotNull
    private static SSHHostConfiguration[] loadHostConfigurations(
        @NotNull DBWHandlerConfiguration configuration
    ) throws DBException {
        final List<SSHHostConfiguration> hostConfigurations = new ArrayList<>();

        // primary host
        hostConfigurations.add(loadHostConfiguration(configuration, ""));

        // jump host, if present
        final String jumpServerPrefix = DataSourceUtils.getJumpServerSettingsPrefix(0);
        if (configuration.getBooleanProperty(jumpServerPrefix + RegistryConstants.ATTR_ENABLED)) {
            hostConfigurations.add(0, loadHostConfiguration(configuration, jumpServerPrefix));
        }

        return hostConfigurations.toArray(SSHHostConfiguration[]::new);
    }

    @NotNull
    private static SSHHostConfiguration loadHostConfiguration(
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull String prefix
    ) throws DBException {
        String username;
        final String password;
        final boolean savePassword = configuration.isSavePassword();

        if (prefix.isEmpty()) {
            username = CommonUtils.notEmpty(configuration.getUserName());
            password = CommonUtils.notEmpty(configuration.getPassword());
        } else {
            username = CommonUtils.notEmpty(configuration.getStringProperty(prefix + RegistryConstants.ATTR_NAME));
            password = CommonUtils.notEmpty(configuration.getSecureProperty(prefix + RegistryConstants.ATTR_PASSWORD));
        }

        if (CommonUtils.isEmpty(username)) {
            username = System.getProperty("user.name");
        }

        final String hostname = configuration.getStringProperty(prefix + DBWHandlerConfiguration.PROP_HOST);
        if (CommonUtils.isEmpty(hostname)) {
            throw new DBException("SSH host not specified");
        }

        final int port = configuration.getIntProperty(prefix + DBWHandlerConfiguration.PROP_PORT);
        if (port == 0) {
            throw new DBException("SSH port not specified");
        }

        final SSHConstants.AuthType authType = CommonUtils.valueOf(
            SSHConstants.AuthType.class,
            configuration.getStringProperty(prefix + SSHConstants.PROP_AUTH_TYPE),
            SSHConstants.AuthType.PASSWORD
        );
        final SSHAuthConfiguration authentication = switch (authType) {
            case PUBLIC_KEY -> {
                final String path = configuration.getStringProperty(prefix + SSHConstants.PROP_KEY_PATH);
                if (CommonUtils.isEmpty(path)) {
                    String privKeyValue = configuration.getSecureProperty(prefix + SSHConstants.PROP_KEY_VALUE);
                    if (privKeyValue == null) {
                        throw new DBException("Private key not specified");
                    }
                    yield new SSHAuthConfiguration.KeyData(privKeyValue, password, savePassword);
                } else {
                    final Path file = Path.of(path);
                    if (!Files.exists(file)) {
                        throw new DBException("Private key file '" + path + "' does not exist");
                    }
                    yield new SSHAuthConfiguration.KeyFile(file, password, savePassword);
                }
            }
            case PASSWORD -> new SSHAuthConfiguration.Password(password, savePassword);
            case AGENT -> new SSHAuthConfiguration.Agent();
        };

        return new SSHHostConfiguration(username, hostname, port, authentication);
    }
}
