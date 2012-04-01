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
import java.util.Map;

/**
 * SSH tunnel
 */
public class SSHTunnelImpl implements DBWTunnel {

    private static transient JSch jsch;
    private transient Session session;

    @Override
    public DBPConnectionInfo initializeTunnel(DBRProgressMonitor monitor, DBWHandlerConfiguration configuration, DBPConnectionInfo connectionInfo)
        throws DBException, IOException
    {
        String dbPortString = connectionInfo.getHostPort();
        if (CommonUtils.isEmpty(dbPortString)) {
            dbPortString = configuration.getDriver().getDefaultPort();
            if (CommonUtils.isEmpty(dbPortString)) {
                throw new DBException("Database port not specified and no default port number for driver '" + configuration.getDriver().getName() + "'");
            }
        }

        Map<String,String> properties = configuration.getProperties();
        String sshHost = properties.get(SSHConstants.PROP_HOST);
        String sshPort = properties.get(SSHConstants.PROP_PORT);
        String sshUser = configuration.getUserName();
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
        monitor.subTask("Initiating tunnel at '" + sshHost + "'");
        UserInfo ui = new UIUserInfo(configuration);
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
            session = jsch.getSession(sshUser, sshHost, sshPortNum);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setUserInfo(ui);
            session.connect();
            session.setPortForwardingL(connectionInfo.getHostName(), localPort, "localhost", dbPort);
        } catch (JSchException e) {
            throw new DBException("Cannot establish tunnel", e);
        }
        connectionInfo = new DBPConnectionInfo(connectionInfo);
        String newPortValue = String.valueOf(localPort);
        connectionInfo.setHostPort(newPortValue);
        connectionInfo.setUrl(replacePort(connectionInfo.getUrl(), dbPortString, newPortValue));
        return connectionInfo;
    }

    private String replacePort(String url, String oldValue, String newValue)
    {
        int fistIndex = 0;
        while (true) {
            int divPos = url.indexOf(oldValue, fistIndex);
            if (divPos == -1) {
                break;
            }
            if (divPos > 0) {
                char prevChar = url.charAt(divPos - 1);
                if (Character.isDigit(prevChar)) {
                    continue;
                }
            }
            if (url.length() > divPos + oldValue.length()) {
                char nextChar = url.charAt(divPos + oldValue.length());
                if (Character.isDigit(nextChar)) {
                    continue;
                }
            }
            url = url.substring(0, divPos) + newValue + url.substring(divPos + oldValue.length());
            fistIndex = divPos + newValue.length();
        }
        return url;
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
        DBWHandlerConfiguration configuration;
        private UIUserInfo(DBWHandlerConfiguration configuration)
        {
            this.configuration = configuration;
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
            UIUtils.showMessageBox(null, "SSH Tunnel", message, SWT.ICON_INFORMATION);
        }
    }
}
