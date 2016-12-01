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
package org.jkiss.dbeaver.runtime.net;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.net.SocksConstants;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.registry.encode.EncryptionException;
import org.jkiss.dbeaver.registry.encode.SecuredPasswordEncrypter;
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
            DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

            // 1. Check for drivers download proxy
            final String proxyHost = store.getString(DBeaverPreferences.UI_PROXY_HOST);
            if (!CommonUtils.isEmpty(proxyHost) && proxyHost.equalsIgnoreCase(getRequestingHost()) &&
                store.getInt(DBeaverPreferences.UI_PROXY_PORT) == getRequestingPort())
            {
                String userName = store.getString(DBeaverPreferences.UI_PROXY_USER);
                String userPassword = decryptPassword(store.getString(DBeaverPreferences.UI_PROXY_PASSWORD));
                if (CommonUtils.isEmpty(userName) || CommonUtils.isEmpty(userPassword)) {
                    DBAAuthInfo authInfo = readCredentialsInUI("Auth proxy '" + proxyHost + "'", userName, userPassword);
                    if (authInfo != null) {
                        userName = authInfo.getUserName();
                        userPassword = authInfo.getUserPassword();
                        if (authInfo.isSavePassword()) {
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
                DBCExecutionContext activeContext = DBExecUtils.findConnectionContext(getRequestingHost(), getRequestingPort(), getRequestingScheme());
                if (activeContext != null) {
                    DBPDataSourceContainer container = activeContext.getDataSource().getContainer();
                    for (DBWHandlerConfiguration networkHandler : container.getConnectionConfiguration().getDeclaredHandlers()) {
                        if (networkHandler.isEnabled() && networkHandler.getType() == DBWHandlerType.PROXY) {
                            String userName = networkHandler.getUserName();
                            String userPassword = networkHandler.getPassword();
                            if (CommonUtils.isEmpty(userName) || CommonUtils.isEmpty(userPassword)) {
                                DBAAuthInfo authInfo = readCredentialsInUI(getRequestingPrompt(), userName, userPassword);
                                if (authInfo != null) {
                                    userName = authInfo.getUserName();
                                    userPassword = authInfo.getUserPassword();
                                    if (authInfo.isSavePassword()) {
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

    private DBAAuthInfo readCredentialsInUI(String prompt, String userName, String userPassword)
    {
        return DBUserInterface.getInstance().promptUserCredentials(prompt, userName, userPassword, false);
    }

}
