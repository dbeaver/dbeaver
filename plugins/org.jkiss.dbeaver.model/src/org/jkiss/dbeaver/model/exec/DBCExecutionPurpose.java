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

package org.jkiss.dbeaver.model.exec;

/**
 * Execution purpose.
 *
 * Each query which executed withing application have some purpose.
 * Some of queries are instantiated by user, some are executed internally to obtain metadata, etc.
 * This enum defines different query purposes.
 *
 * Note: for sure, we can't detect ALL executed queries. Some of them are executed by drivers internally,
 * some are executed by plugins and not reported to query manager.
 */
public enum DBCExecutionPurpose {

    USER(true),               // User query
    USER_FILTERED(true),      // User query with additional filters
    USER_SCRIPT(true),        // User script query
    UTIL(false),              // Utility query (utility method initialized by user)
    META(false),              // Metadata query, processed by data source providers internally
    META_DDL(false),;

    private final boolean user;

    DBCExecutionPurpose(boolean user) {
        this.user = user;
    }

    public boolean isUser() {
        return user;
    }           // Metadata modifications (DDL)

}
