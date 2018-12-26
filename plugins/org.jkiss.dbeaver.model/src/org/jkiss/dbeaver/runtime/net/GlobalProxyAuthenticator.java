/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.runtime.net;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.net.SocksConstants;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.encode.EncryptionException;
import org.jkiss.dbeaver.runtime.encode.SecuredPasswordEncrypter;
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
            DBPPreferenceStore store = ModelPreferences.getPreferences();

            // 1. Check for drivers download proxy
            final String proxyHost = store.getString(ModelPreferences.UI_PROXY_HOST);
            if (!CommonUtils.isEmpty(proxyHost) && proxyHost.equalsIgnoreCase(getRequestingHost()) &&
                store.getInt(ModelPreferences.UI_PROXY_PORT) == getRequestingPort()) {
                String userName = store.getString(ModelPreferences.UI_PROXY_USER);
                String userPassword = decryptPassword(store.getString(ModelPreferences.UI_PROXY_PASSWORD));
                if (CommonUtils.isEmpty(userName) || CommonUtils.isEmpty(userPassword)) {
                    DBAAuthInfo authInfo = readCredentialsInUI("Auth proxy '" + proxyHost + "'", userName, userPassword);
                    if (authInfo != null) {
                        userName = authInfo.getUserName();
                        userPassword = authInfo.getUserPassword();
                        if (authInfo.isSavePassword()) {
                            // Save in preferences
                            store.setValue(ModelPreferences.UI_PROXY_USER, userName);
                            store.setValue(ModelPreferences.UI_PROXY_PASSWORD, encryptPassword(userPassword));
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
                DBPDataSourceContainer activeContext = DBExecUtils.findConnectionContext(getRequestingHost(), getRequestingPort(), getRequestingScheme());
                if (activeContext != null) {
                    for (DBWHandlerConfiguration networkHandler : activeContext.getConnectionConfiguration().getDeclaredHandlers()) {
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
                                        activeContext.getRegistry().flushConfig();
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

    private DBAAuthInfo readCredentialsInUI(String prompt, String userName, String userPassword) {
        return DBWorkbench.getPlatformUI().promptUserCredentials(prompt, userName, userPassword, false, true);
    }

}
