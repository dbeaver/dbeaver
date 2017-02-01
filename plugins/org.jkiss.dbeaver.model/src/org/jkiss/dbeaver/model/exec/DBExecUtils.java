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
package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Execution utils
 */
public class DBExecUtils {

    /**
     * Current execution context. Used by global authenticators and network handlers
     */
    private static final ThreadLocal<DBCExecutionContext> ACTIVE_CONTEXT = new ThreadLocal<>();
    private static final List<DBCExecutionContext> ACTIVE_CONTEXTS = new ArrayList<>();

    public static DBCExecutionContext getCurrentThreadContext() {
        return ACTIVE_CONTEXT.get();
    }

    public static List<DBCExecutionContext> getActiveContexts() {
        synchronized (ACTIVE_CONTEXTS) {
            return new ArrayList<>(ACTIVE_CONTEXTS);
        }
    }

    public static void startContextInitiation(DBCExecutionContext context) {
        ACTIVE_CONTEXT.set(context);
        synchronized (ACTIVE_CONTEXTS) {
            ACTIVE_CONTEXTS.add(context);
        }
    }

    public static void finishContextInitiation(DBCExecutionContext context) {
        ACTIVE_CONTEXT.remove();
        synchronized (ACTIVE_CONTEXTS) {
            ACTIVE_CONTEXTS.remove(context);
        }
    }

    public static DBCExecutionContext findConnectionContext(String host, int port, String path) {
        DBCExecutionContext curContext = getCurrentThreadContext();
        if (curContext != null) {
            return curContext;
        }
        synchronized (ACTIVE_CONTEXTS) {
            for (DBCExecutionContext ctx : ACTIVE_CONTEXTS) {
                DBPConnectionConfiguration cfg = ctx.getDataSource().getContainer().getConnectionConfiguration();
                if (CommonUtils.equalObjects(cfg.getHostName(), host) && String.valueOf(port).equals(cfg.getHostPort())) {
                    return ctx;
                }
            }
        }
        return null;
    }
}