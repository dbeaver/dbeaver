/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.net;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.encode.EncryptionException;
import org.jkiss.dbeaver.registry.encode.SecuredPasswordEncrypter;
import org.jkiss.dbeaver.runtime.RunnableWithResult;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.BaseAuthDialog;
import org.jkiss.utils.CommonUtils;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Global authenticator
 */
public class GlobalProxyAuthenticator extends Authenticator {

    private SecuredPasswordEncrypter encrypter;

    @Nullable
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        {
            IPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

            // 1. Check for drivers download proxy
            final String proxyHost = store.getString(DBeaverPreferences.UI_PROXY_HOST);
            if (!CommonUtils.isEmpty(proxyHost) && proxyHost.equalsIgnoreCase(getRequestingHost()) &&
                store.getInt(DBeaverPreferences.UI_PROXY_PORT) == getRequestingPort())
            {
                String userName = store.getString(DBeaverPreferences.UI_PROXY_USER);
                String userPassword = decryptPassword(store.getString(DBeaverPreferences.UI_PROXY_PASSWORD));
                if (CommonUtils.isEmpty(userName) || CommonUtils.isEmpty(userPassword)) {
                    BaseAuthDialog.AuthInfo authInfo = readCredentialsInUI("Auth proxy '" + proxyHost + "'", userName, userPassword);
                    if (authInfo != null) {
                        userName = authInfo.userName;
                        userPassword = authInfo.userPassword;
                        if (authInfo.savePassword) {
                            // Save in preferences
                            store.setValue(DBeaverPreferences.UI_PROXY_USER, userName);
                            store.setValue(DBeaverPreferences.UI_PROXY_PASSWORD, encryptPassword(userPassword));
                        }
                    }
                }
                if (!CommonUtils.isEmpty(userName) && !CommonUtils.isEmpty(userPassword)) {
                    return new PasswordAuthentication(userName, userPassword.toCharArray());
                }
            }
        }

        {
            // 2. Check for connections' proxies
            String requestingProtocol = getRequestingProtocol();
            if (SocksConstants.PROTOCOL_SOCKS5.equals(requestingProtocol) || SocksConstants.PROTOCOL_SOCKS4.equals(requestingProtocol)) {
                DBCExecutionContext activeContext = DBCExecutionContext.ACTIVE_CONTEXT.get();
                if (activeContext != null) {
                    DBSDataSourceContainer container = activeContext.getDataSource().getContainer();
                    for (DBWHandlerConfiguration networkHandler : container.getConnectionInfo().getDeclaredHandlers()) {
                        if (networkHandler.isEnabled() && networkHandler.getType() == DBWHandlerType.PROXY) {
                            String userName = networkHandler.getUserName();
                            String userPassword = networkHandler.getPassword();
                            if (CommonUtils.isEmpty(userName) || CommonUtils.isEmpty(userPassword)) {
                                BaseAuthDialog.AuthInfo authInfo = readCredentialsInUI(getRequestingPrompt(), userName, userPassword);
                                if (authInfo != null) {
                                    userName = authInfo.userName;
                                    userPassword = authInfo.userPassword;
                                    if (authInfo.savePassword) {
                                        // Save DS config
                                        networkHandler.setUserName(userName);
                                        networkHandler.setPassword(userPassword);
                                        networkHandler.setSavePassword(true);
                                        container.getRegistry().flushConfig();
                                    }
                                }
                            }
                            if (!CommonUtils.isEmpty(userName) && !CommonUtils.isEmpty(userPassword)) {
                                return new PasswordAuthentication(userName, userPassword.toCharArray());
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private String encryptPassword(String password) {
        try {
            if (encrypter == null) {
                encrypter = new SecuredPasswordEncrypter();
            }
            return encrypter.encrypt(password);
        } catch (EncryptionException e) {
            return password;
        }
    }

    private String decryptPassword(String password) {
        if (CommonUtils.isEmpty(password)) {
            return password;
        }
        try {
            if (encrypter == null) {
                encrypter = new SecuredPasswordEncrypter();
            }
            return encrypter.decrypt(password);
        } catch (EncryptionException e) {
            return password;
        }
    }

    private BaseAuthDialog.AuthInfo readCredentialsInUI(String prompt, String userName, String userPassword)
    {
        // Ask user
        final Shell shell = DBeaverUI.getActiveWorkbenchShell();
        final BaseAuthDialog authDialog = new BaseAuthDialog(shell, prompt, DBIcon.CONNECTIONS.getImage());
        authDialog.setUserName(userName);
        authDialog.setUserPassword(userPassword);
        final RunnableWithResult<Boolean> binder = new RunnableWithResult<Boolean>() {
            @Override
            public void run()
            {
                result = (authDialog.open() == IDialogConstants.OK_ID);
            }
        };
        UIUtils.runInUI(shell, binder);
        if (binder.getResult() != null && binder.getResult()) {
            return authDialog.getAuthInfo();
        } else {
            return null;
        }
    }

}
