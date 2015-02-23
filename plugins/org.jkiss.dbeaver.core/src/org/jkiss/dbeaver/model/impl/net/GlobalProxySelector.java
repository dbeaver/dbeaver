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

import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
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
                for (DBWHandlerConfiguration networkHandler : container.getConnectionInfo().getDeclaredHandlers()) {
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
