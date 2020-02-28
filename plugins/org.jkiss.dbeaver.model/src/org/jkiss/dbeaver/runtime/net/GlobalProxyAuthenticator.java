/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.bundle.ModelActivator;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPAuthInfo;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.net.SocksConstants;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.encode.EncryptionException;
import org.jkiss.dbeaver.runtime.encode.SecuredPasswordEncrypter;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Global authenticator
 */
public class GlobalProxyAuthenticator extends Authenticator {

    private SecuredPasswordEncrypter encrypter;

    private final IProxyService proxyService;

    public GlobalProxyAuthenticator() {
        BundleContext bundleContext = ModelActivator.getInstance().getBundle().getBundleContext();
        ServiceReference<IProxyService> proxyServiceRef = bundleContext.getServiceReference(IProxyService.class);
        if (proxyServiceRef != null) {
            proxyService = bundleContext.getService(proxyServiceRef);
        } else {
            proxyService = null;
        }
    }

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
                    DBPAuthInfo authInfo = readCredentialsInUI("Auth proxy '" + proxyHost + "'", userName, userPassword);
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
                    for (DBWHandlerConfiguration networkHandler : activeContext.getConnectionConfiguration().getHandlers()) {
                        if (networkHandler.isEnabled() && networkHandler.getType() == DBWHandlerType.PROXY) {
                            String userName = networkHandler.getUserName();
                            String userPassword = networkHandler.getPassword();
                            if (CommonUtils.isEmpty(userName) || CommonUtils.isEmpty(userPassword)) {
                                DBPAuthInfo authInfo = readCredentialsInUI(getRequestingPrompt(), userName, userPassword);
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

        if (proxyService != null) {
            // Try to use Eclispe proxy config for global proxies
            IProxyData[] proxyData = proxyService.getProxyData();
            if (proxyData != null) {
                for (IProxyData pd : proxyData) {
                    if (getRequestingProtocol().startsWith(pd.getType()) && pd.getUserId() != null && pd.getHost() != null && pd.getPort() == this.getRequestingPort() && pd.getHost().equalsIgnoreCase(getRequestingHost())) {
                        return new PasswordAuthentication(pd.getUserId(), pd.getPassword().toCharArray());
                    }
                }

                return null;
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

    private DBPAuthInfo readCredentialsInUI(String prompt, String userName, String userPassword) {
        return DBWorkbench.getPlatformUI().promptUserCredentials(prompt, userName, userPassword, false, true);
    }

}
