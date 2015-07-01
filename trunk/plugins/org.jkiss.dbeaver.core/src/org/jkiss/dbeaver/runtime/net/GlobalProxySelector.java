/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.net.SocksConstants;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
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

    static final Log log = Log.getLog(GlobalProxySelector.class);

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

        if (SocksConstants.SOCKET_SCHEME.equals(scheme)) {
            // 2. Check for connections' proxy config
            DBCExecutionContext activeContext = DBCExecutionContext.ACTIVE_CONTEXT.get();
            if (activeContext != null) {
                List<Proxy> proxies = null;
                DBSDataSourceContainer container = activeContext.getDataSource().getContainer();
                for (DBWHandlerConfiguration networkHandler : container.getConnectionConfiguration().getDeclaredHandlers()) {
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
                            Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, portNumber));
                            if (proxies == null) {
                                proxies = new ArrayList<Proxy>();
                            }
                            proxies.add(proxy);
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
