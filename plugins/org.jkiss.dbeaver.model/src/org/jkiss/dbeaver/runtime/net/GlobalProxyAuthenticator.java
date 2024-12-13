/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.bundle.ModelActivator;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPAuthInfo;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.net.SocksConstants;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
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
    private static final Log log = Log.getLog(GlobalProxyAuthenticator.class);

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
            DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

            // 1. Check for drivers download proxy
            final String proxyHost = store.getString(ModelPreferences.UI_PROXY_HOST);
            if (!CommonUtils.isEmpty(proxyHost) && proxyHost.equalsIgnoreCase(getRequestingHost()) &&
                store.getInt(ModelPreferences.UI_PROXY_PORT) == getRequestingPort()) {
                DBPAuthInfo credentials = null;
                try {
                    credentials = readCredentials();
                } catch (DBException e) {
                    log.error("Error reading proxy credentials", e);
                }
                if (credentials == null) {
                    credentials = readCredentialsInUI("Auth proxy '" + proxyHost + "'", null, null);
                }
                if (credentials != null) {
                    if (credentials.isSavePassword()) {
                        try {
                            saveCredentials(credentials.getUserName(), credentials.getUserPassword());
                        } catch (DBException e) {
                            log.error("Error saving proxy credentials", e);
                        }
                    }
                    return new PasswordAuthentication(credentials.getUserName(), credentials.getUserPassword().toCharArray());
                }
            }
        }

        {
            // 2. Check for connections' proxies
            String requestingProtocol = getRequestingProtocol();
            if (SocksConstants.PROTOCOL_SOCKS5.equals(requestingProtocol) || SocksConstants.PROTOCOL_SOCKS4.equals(requestingProtocol)) {
                DBPDataSourceContainer activeContext = DBExecUtils.findConnectionContext(getRequestingHost(), getRequestingPort(), getRequestingScheme());
                if (activeContext != null) {
                    for (DBWHandlerConfiguration networkHandler : activeContext.getActualConnectionConfiguration().getHandlers()) {
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
            // Try to use Eclipse proxy config for global proxies
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

    private DBPAuthInfo readCredentialsInUI(String prompt, String userName, String userPassword) {
        return DBWorkbench.getPlatformUI().promptUserCredentials(prompt, userName, userPassword, false, true);
    }

    @Nullable
    public static DBPAuthInfo readCredentials() throws DBException {
        // Modern storage
        DBSSecretController secrets = DBSSecretController.getGlobalSecretController();
        String userName = secrets.getPrivateSecretValue(ModelPreferences.UI_PROXY_USER);
        String password = secrets.getPrivateSecretValue(ModelPreferences.UI_PROXY_PASSWORD);

        if (CommonUtils.isNotEmpty(userName)) {
            return new DBPAuthInfo(userName, CommonUtils.notEmpty(password), true);
        }

        // Backward compatibility
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        userName = store.getString(ModelPreferences.UI_PROXY_USER);
        password = store.getString(ModelPreferences.UI_PROXY_PASSWORD);

        if (CommonUtils.isNotEmpty(userName)) {
            if (CommonUtils.isNotEmpty(password)) {
                try {
                    password = new SecuredPasswordEncrypter().decrypt(password);
                } catch (EncryptionException e) {
                    throw new DBException("Can't decrypt legacy password", e);
                }
            }

            return new DBPAuthInfo(userName, CommonUtils.notEmpty(password), true);
        }

        return null;
    }

    public static void saveCredentials(@NotNull String username, @NotNull String password) throws DBException {
        if (CommonUtils.isNotEmpty(username)) {
            DBSSecretController secrets = DBSSecretController.getGlobalSecretController();
            secrets.setPrivateSecretValue(ModelPreferences.UI_PROXY_USER, username);
            if (CommonUtils.isNotEmpty(password)) {
                secrets.setPrivateSecretValue(ModelPreferences.UI_PROXY_PASSWORD, password);
            }
        }
    }

}
