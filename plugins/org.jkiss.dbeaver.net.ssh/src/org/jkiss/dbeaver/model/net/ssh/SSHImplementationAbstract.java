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

import com.jcraft.jsch.agentproxy.AgentProxy;
import com.jcraft.jsch.agentproxy.connector.PageantConnector;
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector;
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHAuthConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHPortForwardConfiguration;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHImplementationDescriptor;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHImplementationRegistry;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SSH tunnel
 */
public abstract class SSHImplementationAbstract implements SSHImplementation {

    private static final Log log = Log.getLog(SSHImplementationAbstract.class);

    // Saved config - used for tunnel invalidate
    private transient int savedLocalPort = 0;

    protected transient DBWHandlerConfiguration savedConfiguration;
    protected transient DBPConnectionConfiguration savedConnectionInfo;
    protected AgentProxy agentProxy = null;

    @Override
    public DBPConnectionConfiguration initTunnel(DBRProgressMonitor monitor, DBPPlatform platform, DBWHandlerConfiguration configuration, DBPConnectionConfiguration connectionInfo)
        throws DBException, IOException
    {
    	String dbPortString = connectionInfo.getHostPort();
        if (CommonUtils.isEmpty(dbPortString)) {
            dbPortString = configuration.getDriver().getDefaultPort();
            if (CommonUtils.isEmpty(dbPortString)) {
                throw new DBException("Database port not specified and no default port number for driver '" + configuration.getDriver().getName() + "'");
            }
        }

        int connectTimeout = configuration.getIntProperty(SSHConstants.PROP_CONNECT_TIMEOUT);
        String sshLocalHost = CommonUtils.toString(configuration.getProperty(SSHConstants.PROP_LOCAL_HOST));
        int sshLocalPort = configuration.getIntProperty(SSHConstants.PROP_LOCAL_PORT);
        String sshRemoteHost = CommonUtils.toString(configuration.getProperty(SSHConstants.PROP_REMOTE_HOST));
        int sshRemotePort = configuration.getIntProperty(SSHConstants.PROP_REMOTE_PORT);

        if (sshLocalPort == 0) {
            if (savedLocalPort != 0) {
                sshLocalPort = savedLocalPort;
            } else if (platform != null) {
                sshLocalPort = SSHUtils.findFreePort(platform);
            }
        }
        if (CommonUtils.isEmpty(sshRemoteHost)) {
            sshRemoteHost = connectionInfo.getHostName();
        }
        if (sshRemotePort == 0 && configuration.getDriver() != null) {
            sshRemotePort = CommonUtils.toInt(connectionInfo.getHostPort());
        }
        if (connectTimeout == 0) {
            configuration.setProperty(SSHConstants.PROP_CONNECT_TIMEOUT, SSHConstants.DEFAULT_CONNECT_TIMEOUT);
        }

        final SSHPortForwardConfiguration portForwardConfiguration = new SSHPortForwardConfiguration(sshLocalHost, sshLocalPort, sshRemoteHost, sshRemotePort);
        final List<SSHHostConfiguration> hostConfigurations = new ArrayList<>();

        // primary host
        hostConfigurations.add(loadConfiguration(configuration, ""));

        // jump hosts, if supported and present
        if (isSupportsJumpServer()) {
            final String prefix = getJumpServerSettingsPrefix(0);
            if (configuration.getBooleanProperty(prefix + RegistryConstants.ATTR_ENABLED)) {
                hostConfigurations.add(0, loadConfiguration(configuration, prefix));
            }
        }

        for (SSHHostConfiguration host : hostConfigurations) {
            if (host.getAuthConfiguration().getType() == SSHConstants.AuthType.AGENT) {
                try {
                    agentProxy = new AgentProxy(new PageantConnector());
                    log.debug("SSH: Connected with pageant");
                } catch (Exception e) {
                    log.debug("pageant connect exception", e);
                }

                if (agentProxy == null) {
                    try {
                        agentProxy = new AgentProxy(new SSHAgentConnector(new JNAUSocketFactory()));
                        log.debug("SSH: Connected with ssh-agent");
                    } catch (Exception e) {
                        log.debug("ssh-agent connection exception", e);
                    }
                }

                if (agentProxy == null) {
                    throw new DBException("Unable to initialize SSH agent");
                }

                break;
            }
        }

        setupTunnel(monitor, configuration, hostConfigurations.toArray(new SSHHostConfiguration[0]), portForwardConfiguration);
        savedLocalPort = sshLocalPort;
        savedConfiguration = configuration;
        savedConnectionInfo = connectionInfo;

        connectionInfo = new DBPConnectionConfiguration(connectionInfo);
        // Replace database host/port and URL
        if (CommonUtils.isEmpty(sshLocalHost)) {
            connectionInfo.setHostName(SSHConstants.LOCALHOST_NAME);
        } else {
            connectionInfo.setHostName(sshLocalHost);
        }
        connectionInfo.setHostPort(Integer.toString(sshLocalPort));
        if (configuration.getDriver() != null) {
            // Driver can be null in case of orphan tunnel config (e.g. in network profile)
            String newURL = configuration.getDriver().getDataSourceProvider().getConnectionURL(
                configuration.getDriver(),
                connectionInfo);
            connectionInfo.setUrl(newURL);
        }
        return connectionInfo;
    }

    @NotNull
    public byte[] agentSign(@NotNull byte [] blob, @NotNull byte[] data) {
        return agentProxy.sign(blob, data);
    }

    @NotNull
    protected List<SSHAgentIdentity> getAgentData() {
        return Arrays.stream(agentProxy.getIdentities())
            .map(i -> {
                SSHAgentIdentity id = new SSHAgentIdentity();
                id.setBlob(i.getBlob());
                id.setComment(i.getComment());
                return id;
            })
            .collect(Collectors.toList());
    }

    protected abstract void setupTunnel(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration[] hostConfiguration,
        @NotNull SSHPortForwardConfiguration portForwardingConfiguration
    ) throws DBException, IOException;

    @NotNull
    private static SSHHostConfiguration loadConfiguration(@NotNull DBWHandlerConfiguration configuration, @NotNull String prefix) throws DBException {
        final SSHConstants.AuthType authType = CommonUtils.valueOf(SSHConstants.AuthType.class, configuration.getStringProperty(prefix + SSHConstants.PROP_AUTH_TYPE), SSHConstants.AuthType.PASSWORD);
        final String hostname = configuration.getStringProperty(prefix + DBWHandlerConfiguration.PROP_HOST);
        final int port = configuration.getIntProperty(prefix + DBWHandlerConfiguration.PROP_PORT);
        final String username;
        final String password;
        final boolean savePassword = configuration.isSavePassword();

        if (prefix.isEmpty()) {
            username = CommonUtils.notEmpty(configuration.getUserName());
            password = CommonUtils.notEmpty(configuration.getPassword());
        } else {
            username = CommonUtils.notEmpty(configuration.getStringProperty(prefix + RegistryConstants.ATTR_NAME));
            password = CommonUtils.notEmpty(configuration.getSecureProperty(prefix + RegistryConstants.ATTR_PASSWORD));
        }

        if (CommonUtils.isEmpty(hostname)) {
            throw new DBException("SSH host not specified");
        }
        if (port == 0) {
            throw new DBException("SSH port not specified");
        }
        if (CommonUtils.isEmpty(username)) {
            throw new DBException("SSH user not specified");
        }

        final SSHAuthConfiguration authentication;
        switch (authType) {
            case PUBLIC_KEY: {
                final String path = configuration.getStringProperty(prefix + SSHConstants.PROP_KEY_PATH);
                if (CommonUtils.isEmpty(path)) {
                    throw new DBException("Private key path is empty");
                }
                final File file = new File(path);
                if (!file.exists()) {
                    throw new DBException("Private key file '" + path + "' does not exist");
                }
                authentication = SSHAuthConfiguration.usingKey(file, password, savePassword);
                break;
            }
            case PASSWORD: {
                authentication = SSHAuthConfiguration.usingPassword(password, savePassword);
                break;
            }
            default:
                authentication = SSHAuthConfiguration.usingAgent();
                break;
        }

        return new SSHHostConfiguration(username, hostname, port, authentication);
    }

    @NotNull
    public static String getJumpServerSettingsPrefix(int index) {
        return SSHConstants.PROP_JUMP_SERVER + index + ".";
    }

    protected boolean isSupportsJumpServer() {
        for (SSHImplementationDescriptor descriptor : SSHImplementationRegistry.getInstance().getDescriptors()) {
            if (descriptor.getImplClass().getObjectClass() == getClass()) {
                return descriptor.isSupportsJumpServer();
            }
        }
        return false;
    }
}
