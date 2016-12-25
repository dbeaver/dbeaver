/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.net;

import com.jcraft.jsch.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWTunnel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * SSH tunnel
 */
public class SSHTunnelImpl implements DBWTunnel {

    private static final Log log = Log.getLog(SSHTunnelImpl.class);

    //private static final int CONNECT_TIMEOUT = 10000;
    public static final String LOCALHOST_NAME = "127.0.0.1";
    private static transient JSch jsch;
    private transient Session session;

    // Saved config - used for tunnel invalidate
    private transient int savedLocalPort;
    private transient DBWHandlerConfiguration savedConfiguration;
    private transient DBPConnectionConfiguration savedConnectionInfo;

    @Override
    public DBPConnectionConfiguration initializeTunnel(DBRProgressMonitor monitor, DBPPlatform platform, DBWHandlerConfiguration configuration, DBPConnectionConfiguration connectionInfo)
        throws DBException, IOException
    {
        String dbPortString = connectionInfo.getHostPort();
        if (CommonUtils.isEmpty(dbPortString)) {
            dbPortString = configuration.getDriver().getDefaultPort();
            if (CommonUtils.isEmpty(dbPortString)) {
                throw new DBException("Database port not specified and no default port number for driver '" + configuration.getDriver().getName() + "'");
            }
        }
        String dbHost = connectionInfo.getHostName();

        Map<String,String> properties = configuration.getProperties();
        String sshAuthType = properties.get(SSHConstants.PROP_AUTH_TYPE);
        String sshHost = properties.get(SSHConstants.PROP_HOST);
        String sshPort = properties.get(SSHConstants.PROP_PORT);
        String sshUser = configuration.getUserName();
        String aliveInterval = properties.get(SSHConstants.PROP_ALIVE_INTERVAL);
        String connectTimeoutString = properties.get(SSHConstants.PROP_CONNECT_TIMEOUT);
        //String aliveCount = properties.get(SSHConstants.PROP_ALIVE_COUNT);
        if (CommonUtils.isEmpty(sshHost)) {
            throw new DBException("SSH host not specified");
        }
        if (CommonUtils.isEmpty(sshPort)) {
            throw new DBException("SSH port not specified");
        }
        if (CommonUtils.isEmpty(sshUser)) {
            throw new DBException("SSH user not specified");
        }
        int sshPortNum;
        try {
            sshPortNum = Integer.parseInt(sshPort);
        }
        catch (NumberFormatException e) {
            throw new DBException("Invalid SSH port: " + sshPort);
        }
        SSHConstants.AuthType authType = SSHConstants.AuthType.PASSWORD;
        if (sshAuthType != null) {
            authType = SSHConstants.AuthType.valueOf(sshAuthType); 
        }
        File privKeyFile = null;
        String privKeyPath = properties.get(SSHConstants.PROP_KEY_PATH);
        if (authType == SSHConstants.AuthType.PUBLIC_KEY) {
            if (CommonUtils.isEmpty(privKeyPath)) {
                throw new DBException("Private key path is empty");
            }
            privKeyFile = new File(privKeyPath);
            if (!privKeyFile.exists()) {
                throw new DBException("Private key file '" + privKeyFile.getAbsolutePath() + "' doesn't exist");
            }
        }
        int connectTimeout;
        try {
            connectTimeout = Integer.parseInt(connectTimeoutString);
        }
        catch (NumberFormatException e) {
            connectTimeout = SSHConstants.DEFAULT_CONNECT_TIMEOUT;
        }

        monitor.subTask("Initiating tunnel at '" + sshHost + "'");
        UserInfo ui = new UIUserInfo(configuration);
        int dbPort;
        try {
            dbPort = Integer.parseInt(dbPortString);
        } catch (NumberFormatException e) {
            throw new DBException("Bad database port number: " + dbPortString);
        }
        int localPort = savedLocalPort;
        if (platform != null) {
            localPort = findFreePort(platform);
        }
        try {
            if (jsch == null) {
                jsch = new JSch();
                JSch.setLogger(new LoggerProxy());
            }
            if (privKeyFile != null) {
                if (!CommonUtils.isEmpty(ui.getPassphrase())) {
                    jsch.addIdentity(privKeyFile.getAbsolutePath(), ui.getPassphrase());
                } else {
                    jsch.addIdentity(privKeyFile.getAbsolutePath());
                }
            }

            log.debug("Instantiate SSH tunnel");
            session = jsch.getSession(sshUser, sshHost, sshPortNum);
            session.setConfig("StrictHostKeyChecking", "no");
            //session.setConfig("PreferredAuthentications", "password,publickey,keyboard-interactive");
            session.setConfig("PreferredAuthentications",
                privKeyFile != null ? "publickey" : "password");
            session.setConfig("ConnectTimeout", String.valueOf(connectTimeout));
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
        savedLocalPort = localPort;
        savedConfiguration = configuration;
        savedConnectionInfo = connectionInfo;

        connectionInfo = new DBPConnectionConfiguration(connectionInfo);
        String newPortValue = String.valueOf(localPort);
        // Replace database host/port and URL - let's use localhost
        connectionInfo.setHostName(LOCALHOST_NAME);
        connectionInfo.setHostPort(newPortValue);
        String newURL = configuration.getDriver().getDataSourceProvider().getConnectionURL(
            configuration.getDriver(),
            connectionInfo);
        connectionInfo.setUrl(newURL);
        return connectionInfo;
    }

    private int findFreePort(DBPPlatform platform)
    {
        DBPPreferenceStore store = platform.getPreferenceStore();
        int minPort = store.getInt(ModelPreferences.NET_TUNNEL_PORT_MIN);
        int maxPort = store.getInt(ModelPreferences.NET_TUNNEL_PORT_MAX);
        return IOUtils.findFreePort(minPort, maxPort);
    }

    @Override
    public void closeTunnel(DBRProgressMonitor monitor) throws DBException, IOException
    {
        if (session != null) {
            session.disconnect();
            session = null;
        }
    }

    @Override
    public void invalidateHandler(DBRProgressMonitor monitor) throws DBException, IOException {
        boolean isAlive = session != null && session.isConnected();
        if (isAlive) {
            try {
                session.sendKeepAliveMsg();
            } catch (Exception e) {
                isAlive = false;
            }
        }
        if (!isAlive) {
            closeTunnel(monitor);
            initializeTunnel(monitor, null, savedConfiguration, savedConnectionInfo);
        }
    }

    private class UIUserInfo implements UserInfo {
        DBWHandlerConfiguration configuration;
        private UIUserInfo(DBWHandlerConfiguration configuration)
        {
            this.configuration = configuration;
        }

        @Override
        public String getPassphrase()
        {
            return configuration.getPassword();
        }

        @Override
        public String getPassword()
        {
            return configuration.getPassword();
        }

        @Override
        public boolean promptPassword(String message)
        {
            return true;
        }

        @Override
        public boolean promptPassphrase(String message)
        {
            return true;
        }

        @Override
        public boolean promptYesNo(String message)
        {
            return false;
        }

        @Override
        public void showMessage(String message)
        {
            log.info(message);
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
                case INFO: levelStr = "INFO"; break;
                case WARN: levelStr = "WARN"; break;
                case ERROR: levelStr = "ERROR"; break;
                case FATAL: levelStr = "FATAL"; break;
                case DEBUG:
                default:
                    levelStr = "DEBUG";
                    break;
            }
            log.debug("SSH " + levelStr + ": " + message);

        }
    }
}
