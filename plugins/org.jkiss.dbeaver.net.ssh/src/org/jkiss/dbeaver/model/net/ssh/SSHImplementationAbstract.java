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

import com.jcraft.jsch.agentproxy.AgentProxy;
import com.jcraft.jsch.agentproxy.Identity;
import com.jcraft.jsch.agentproxy.USocketFactory;
import com.jcraft.jsch.agentproxy.connector.PageantConnector;
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector;
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
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

        String sshAuthType = configuration.getStringProperty(SSHConstants.PROP_AUTH_TYPE);
        String sshHost = configuration.getStringProperty(SSHConstants.PROP_HOST);
        int sshPortNum = configuration.getIntProperty(SSHConstants.PROP_PORT);
        int aliveInterval = configuration.getIntProperty(SSHConstants.PROP_ALIVE_INTERVAL);
        int connectTimeout = configuration.getIntProperty(SSHConstants.PROP_CONNECT_TIMEOUT);
        String sshLocalHost = CommonUtils.toString(configuration.getProperty(SSHConstants.PROP_LOCAL_HOST));
        int sshLocalPort = configuration.getIntProperty(SSHConstants.PROP_LOCAL_PORT);
        String sshRemoteHost = CommonUtils.toString(configuration.getProperty(SSHConstants.PROP_REMOTE_HOST));
        int sshRemotePort = configuration.getIntProperty(SSHConstants.PROP_REMOTE_PORT);
        //String aliveCount = properties.get(SSHConstants.PROP_ALIVE_COUNT);
        if (CommonUtils.isEmpty(sshHost)) {
            throw new DBException("SSH host not specified");
        }
        if (sshPortNum == 0) {
            throw new DBException("SSH port not specified");
        }
        if (CommonUtils.isEmpty(configuration.getUserName())) {
            throw new DBException("SSH user not specified");
        }
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

        SSHConstants.AuthType authType = SSHConstants.AuthType.PASSWORD;
        if (sshAuthType != null) {
            authType = SSHConstants.AuthType.valueOf(sshAuthType); 
        }
        File privKeyFile = null;
        String privKeyPath = configuration.getStringProperty(SSHConstants.PROP_KEY_PATH);
        if (authType == SSHConstants.AuthType.PUBLIC_KEY) {
            if (CommonUtils.isEmpty(privKeyPath)) {
                throw new DBException("Private key path is empty");
            }
            privKeyFile = new File(privKeyPath);
            if (!privKeyFile.exists()) {
                throw new DBException("Private key file '" + privKeyFile.getAbsolutePath() + "' doesn't exist");
            }
        }
        if (authType == SSHConstants.AuthType.AGENT) {
            try {
                agentProxy = new AgentProxy(new PageantConnector());
                log.debug("SSH: Connected with pageant");
            } catch (Exception e) {
                log.debug("pageant connect exception", e);
            }
            if (agentProxy==null) {
                try {
                    USocketFactory udsf = new JNAUSocketFactory();
                    agentProxy = new AgentProxy(new SSHAgentConnector(udsf));
                    log.debug("SSH: Connected with ssh-agent");
                } catch (Exception e) {
                    log.debug("ssh-agent connection exception", e);
                }
            }
            if (agentProxy==null) {
                throw new DBException("Unable to initialize SSH agent");
            }
        }

        if (connectTimeout == 0){
            connectTimeout = SSHConstants.DEFAULT_CONNECT_TIMEOUT;
        }

        monitor.subTask("Initiating tunnel at '" + sshHost + "'");

        setupTunnel(monitor, configuration, sshHost, aliveInterval, sshPortNum, privKeyFile, connectTimeout, sshLocalHost, sshLocalPort, sshRemoteHost, sshRemotePort);
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

    public byte [] agentSign(byte [] blob, byte [] data) {
        return agentProxy.sign(blob, data);
    }

    protected List<SSHAgentIdentity> getAgentData() {
        Identity [] identities = agentProxy.getIdentities();
        List<SSHAgentIdentity> result = Arrays.asList(identities).stream().map(i -> {
            SSHAgentIdentity id = new SSHAgentIdentity();
            id.setBlob(i.getBlob());
            id.setComment(i.getComment());
            return id;
        }).collect(Collectors.toList());
        return result;
    }

    protected abstract void setupTunnel(
        DBRProgressMonitor monitor,
        DBWHandlerConfiguration configuration,
        String sshHost,
        int aliveInterval,
        int sshPortNum,
        File privKeyFile,
        int connectTimeout,
        String sshLocalHost,
        int sshLocalPort,
        String sshRemoteHost,
        int sshRemotePort
    ) throws DBException, IOException;
}
