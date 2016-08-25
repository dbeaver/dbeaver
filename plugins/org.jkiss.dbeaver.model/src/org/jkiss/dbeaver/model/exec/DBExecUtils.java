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
package org.jkiss.dbeaver.model.exec;

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
}