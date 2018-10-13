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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.net.SocksConstants;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Global proxy selector
 */
public class GlobalProxySelector extends ProxySelector {

    private static final Log log = Log.getLog(GlobalProxySelector.class);
    private static final String[] LOCAL_HOSTS = { "localhost", "127.0.0.1" };

    private final ProxySelector parent;

    public GlobalProxySelector(ProxySelector parent) {
        this.parent = parent;
    }

    @Override
    public List<Proxy> select(URI uri) {
        String scheme = uri.getScheme();
        if (CommonUtils.isEmpty(scheme)) {
            return parent.select(uri);
        }

        if (scheme.startsWith("http")) {
            // 1. Check for drivers download proxy
        }

        String host = uri.getHost();
        if (CommonUtils.isEmpty(host)) {
            return parent.select(uri);
        }

        // Skip localhosts. In fact it is a bad idea (see #3592)
//        if (ArrayUtils.contains(LOCAL_HOSTS, host)) {
//            return parent.select(uri);
//        }
        int port = uri.getPort();
        String path = uri.getPath();

        if (SocksConstants.SOCKET_SCHEME.equals(scheme)) {
            // 2. Check for connections' proxy config
            DBPDataSourceContainer activeContext = DBExecUtils.findConnectionContext(host, port, path);
            if (activeContext != null) {
                List<Proxy> proxies = null;
                for (DBWHandlerConfiguration networkHandler : activeContext.getConnectionConfiguration().getDeclaredHandlers()) {
                    if (networkHandler.isEnabled() && networkHandler.getType() == DBWHandlerType.PROXY) {
                        Map<String,String> proxyProps = networkHandler.getProperties();
                        String proxyHost = proxyProps.get(SocksConstants.PROP_HOST);
                        String proxyPort = proxyProps.get(SocksConstants.PROP_PORT);
                        if (!CommonUtils.isEmpty(proxyHost)) {
                            int portNumber = SocksConstants.DEFAULT_SOCKS_PORT;
                            if (!CommonUtils.isEmpty(proxyPort)) {
                                try {
                                    portNumber = Integer.parseInt(proxyPort);
                                } catch (NumberFormatException e) {
                                    log.warn("Bad proxy port number", e);
                                }
                            }
                            InetSocketAddress proxyAddr = new InetSocketAddress(proxyHost, portNumber);
                            Proxy proxy = new Proxy(Proxy.Type.SOCKS, proxyAddr);
                            if (proxies == null) {
                                proxies = new ArrayList<>();
                            }
                            proxies.add(proxy);
                            log.debug("Use SOCKS proxy [" + proxyAddr + "]");
                        }
                    }
                }
                if (proxies != null) {
                    return proxies;
                }
            }
        }
        return parent.select(uri);
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        parent.connectFailed(uri, sa, ioe);
    }
}
