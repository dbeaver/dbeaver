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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.net.SocksConstants;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Global proxy selector
 */
public class GlobalProxySelector extends ProxySelector {

    private static final Log log = Log.getLog(GlobalProxySelector.class);

    private final ProxySelector parent;

    public GlobalProxySelector(ProxySelector parent) {
        this.parent = parent;
    }

    public ProxySelector getParent() {
        return parent;
    }

    @Override
    public List<Proxy> select(URI uri) {
        DBPDataSourceContainer dataSourceContainer = getActiveDataSourceContainer(uri);

        if (dataSourceContainer != null) {
            List<Proxy> proxies = getProxiesForDataSource(uri, dataSourceContainer);
            if (proxies != null) {
                return proxies;
            }
        }
        return parent.select(uri);
    }

    @Nullable
    protected List<Proxy> getProxiesForDataSource(@NotNull URI uri, @NotNull DBPDataSourceContainer dataSourceContainer) {
        if (SocksConstants.SOCKET_SCHEME.equals(uri.getScheme())) {
            // 2. Check for connections' proxy config
            List<Proxy> proxies = null;
            for (DBWHandlerConfiguration networkHandler : dataSourceContainer.getActualConnectionConfiguration().getHandlers()) {
                if (networkHandler.isEnabled() && networkHandler.getType() == DBWHandlerType.PROXY) {
                    String proxyHost = networkHandler.getStringProperty(SocksConstants.PROP_HOST);
                    int proxyPort = networkHandler.getIntProperty(SocksConstants.PROP_PORT);
                    if (!CommonUtils.isEmpty(proxyHost) && proxyPort != 0) {
                        InetSocketAddress proxyAddr = new InetSocketAddress(proxyHost, proxyPort);
                        Proxy proxy = new Proxy(Proxy.Type.SOCKS, proxyAddr);
                        if (proxies == null) {
                            proxies = new ArrayList<>();
                        }
                        proxies.add(proxy);
                        log.debug("Use SOCKS proxy [" + proxyAddr + "]");
                    }
                }
            }
            return proxies;
        }
        return null;
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        parent.connectFailed(uri, sa, ioe);
    }

    @Nullable
    protected DBPDataSourceContainer getActiveDataSourceContainer(@NotNull URI uri) {
        String scheme = uri.getScheme();
        if (CommonUtils.isEmpty(scheme)) {
            return null;
        }

        String host = uri.getHost();
        if (CommonUtils.isEmpty(host)) {
            return null;
        }

        // Skip localhosts. In fact, it is a bad idea (see #3592)
//        if (ArrayUtils.contains(LOCAL_HOSTS, host)) {
//            return parent.select(uri);
//        }
        int port = uri.getPort();
        String path = uri.getPath();

        // 2. Check for connections' proxy config
        return DBExecUtils.findConnectionContext(host, port, path);
    }

}
