/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.net.*;
import org.jkiss.dbeaver.runtime.net.GlobalProxyAuthenticator;
import org.jkiss.utils.CommonUtils;

import java.net.Authenticator;
import java.util.ArrayList;
import java.util.List;

/**
 * Execution utils
 */
public class DBExecUtils {

    /**
     * Current execution context. Used by global authenticators and network handlers
     */
    private static final ThreadLocal<DBPDataSourceContainer> ACTIVE_CONTEXT = new ThreadLocal<>();
    private static final List<DBPDataSourceContainer> ACTIVE_CONTEXTS = new ArrayList<>();

    public static DBPDataSourceContainer getCurrentThreadContext() {
        return ACTIVE_CONTEXT.get();
    }

    public static List<DBPDataSourceContainer> getActiveContexts() {
        synchronized (ACTIVE_CONTEXTS) {
            return new ArrayList<>(ACTIVE_CONTEXTS);
        }
    }

    public static void startContextInitiation(DBPDataSourceContainer context) {
        ACTIVE_CONTEXT.set(context);
        synchronized (ACTIVE_CONTEXTS) {
            ACTIVE_CONTEXTS.add(context);
        }
        // Set proxy auth (if required)
        // Note: authenticator may be changed by Eclipse frameword on startup or later.
        // That's why we set new default authenticator on connection initiation
        boolean hasProxy = false;
        for (DBWHandlerConfiguration handler : context.getConnectionConfiguration().getDeclaredHandlers()) {
            if (handler.isEnabled() && handler.getType() == DBWHandlerType.PROXY) {
                hasProxy = true;
                break;
            }
        }
        if (hasProxy) {
            Authenticator.setDefault(new GlobalProxyAuthenticator());
        }
    }

    public static void finishContextInitiation(DBPDataSourceContainer context) {
        ACTIVE_CONTEXT.remove();
        synchronized (ACTIVE_CONTEXTS) {
            ACTIVE_CONTEXTS.remove(context);
        }
    }

    public static DBPDataSourceContainer findConnectionContext(String host, int port, String path) {
        DBPDataSourceContainer curContext = getCurrentThreadContext();
        if (curContext != null) {
            return contextMatches(host, port, curContext) ? curContext : null;
        }
        synchronized (ACTIVE_CONTEXTS) {
            for (DBPDataSourceContainer ctx : ACTIVE_CONTEXTS) {
                if (contextMatches(host, port, ctx)) {
                    return ctx;
                }
            }
        }
        return null;
    }

    private static boolean contextMatches(String host, int port, DBPDataSourceContainer ctx) {
        DBPConnectionConfiguration cfg = ctx.getConnectionConfiguration();
        if (CommonUtils.equalObjects(cfg.getHostName(), host) && String.valueOf(port).equals(cfg.getHostPort())) {
            return true;
        }
        for (DBWNetworkHandler networkHandler : ctx.getActiveNetworkHandlers()) {
            if (networkHandler instanceof DBWForwarder && ((DBWForwarder) networkHandler).matchesParameters(host, port)) {
                return true;
            }
        }
        return false;
    }
}