/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.net.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWTunnel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * SSH tunnel
 */
public class SSHTunnelImpl implements DBWTunnel {

    private DBWHandlerConfiguration configuration;
    private String host;
    private int port;
    private String user;
    private SSHConstants.AuthType authType;
    private String privateKeyPath;

    private static transient JSch jsch;
    private transient Session session;

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public SSHConstants.AuthType getAuthType()
    {
        return authType;
    }

    public void setAuthType(SSHConstants.AuthType authType)
    {
        this.authType = authType;
    }

    public String getPrivateKeyPath()
    {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath)
    {
        this.privateKeyPath = privateKeyPath;
    }

    @Override
    public DBPConnectionInfo initializeTunnel(DBRProgressMonitor monitor, DBWHandlerConfiguration configuration, DBPConnectionInfo connectionInfo)
        throws DBException, IOException
    {
        this.configuration = configuration;
        String dbPortString = connectionInfo.getHostPort();
        if (CommonUtils.isEmpty(dbPortString)) {
            dbPortString = configuration.getDriver().getDefaultPort();
            if (CommonUtils.isEmpty(dbPortString)) {
                throw new DBException("Database port not specified and no default port number for driver '" + configuration.getDriver().getName() + "'");
            }
        }
        UserInfo ui = new UIUserInfo();
        int dbPort;
        try {
            dbPort = Integer.parseInt(dbPortString);
        } catch (NumberFormatException e) {
            throw new DBException("Bad database port number: " + dbPortString);
        }
        int localPort = findFreePort();
        try {
            if (jsch == null) {
                jsch = new JSch();
            }
            session = jsch.getSession(user, host, port);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setUserInfo(ui);
            session.connect();
            session.setPortForwardingL(connectionInfo.getHostName(), localPort, "localhost", dbPort);
        } catch (JSchException e) {
            throw new DBException("Cannot establish tunnel", e);
        }
        connectionInfo = new DBPConnectionInfo(connectionInfo);
        connectionInfo.setHostPort(String.valueOf(localPort));
        return connectionInfo;
    }

    private int findFreePort()
    {
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();
        int minPort = store.getInt(PrefConstants.NET_TUNNEL_PORT_MIN);
        int maxPort = store.getInt(PrefConstants.NET_TUNNEL_PORT_MAX);
        int portRange = Math.abs(maxPort - minPort);
        while (true) {
            int portNum = minPort + SecurityUtils.getRandom().nextInt(portRange);
            try {
                ServerSocket socket = new ServerSocket(portNum);
                try {
                    socket.close();
                } catch (IOException e) {
                    // just skip
                }
                return portNum;
            } catch (IOException e) {
                // Port is busy
            }
        }
    }

    @Override
    public void closeTunnel(DBRProgressMonitor monitor, DBPConnectionInfo connectionInfo) throws DBException, IOException
    {
        if (session != null) {
            session.disconnect();
            session = null;
        }
    }

    private class UIUserInfo implements UserInfo {

        private UIUserInfo()
        {
        }

        @Override
        public String getPassphrase()
        {
            return null;
        }

        @Override
        public String getPassword()
        {
            return configuration.getPassword();
        }

        @Override
        public boolean promptPassword(String message)
        {
            return false;
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
            UIUtils.showMessageBox(null, "SSH Tunnel", message, SWT.ICON_INFORMATION);
        }
    }
}
