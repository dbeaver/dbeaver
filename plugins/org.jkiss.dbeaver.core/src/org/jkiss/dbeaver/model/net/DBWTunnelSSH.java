/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.net;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;

/**
 * SSH tunnel
 */
public class DBWTunnelSSH implements DBWTunnel {

    public static enum AuthType {
        PASSWORD,
        PUBLIC_KEY
    }

    private String host;
    private int port;
    private String user;
    private AuthType authType;
    private String privateKeyPath;
    private String password;
    private boolean savePassword;

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

    public AuthType getAuthType()
    {
        return authType;
    }

    public void setAuthType(AuthType authType)
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

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public boolean isSavePassword()
    {
        return savePassword;
    }

    public void setSavePassword(boolean savePassword)
    {
        this.savePassword = savePassword;
    }

    @Override
    public void initializeTunnel(DBRProgressMonitor monitor, DBPDriver driver, DBPConnectionInfo connectionInfo, Shell windowShell) throws DBException, IOException
    {
        String dbPortString = connectionInfo.getHostPort();
        if (CommonUtils.isEmpty(dbPortString)) {
            dbPortString = driver.getDefaultPort();
            if (CommonUtils.isEmpty(dbPortString)) {
                throw new DBException("Database port not specified and no default port number for driver '" + driver.getName() + "'");
            }
        }
        UserInfo ui = new UIUserInfo(windowShell);
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
    }

    private int findFreePort()
    {
        return 0;
    }

    @Override
    public void closeTunnel(DBPConnectionInfo connectionInfo, DBRProgressMonitor monitor) throws DBException, IOException
    {
        if (session != null) {
            session.disconnect();
            session = null;
        }
    }

    private class UIUserInfo implements UserInfo {
        private Shell windowShell;

        private UIUserInfo(Shell windowShell)
        {
            this.windowShell = windowShell;
        }

        @Override
        public String getPassphrase()
        {
            return null;
        }

        @Override
        public String getPassword()
        {
            return password;
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
            UIUtils.showMessageBox(windowShell, "SSH Tunnel", message, SWT.ICON_INFORMATION);
        }
    }
}
