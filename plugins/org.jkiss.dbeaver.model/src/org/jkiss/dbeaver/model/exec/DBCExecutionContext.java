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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Execution context.
 * Provides access to execution sessions.
 * Usually contains some kind of physical database connection inside
 */
public interface DBCExecutionContext extends DBPObject,DBPCloseableObject
{
    /**
     * Current execution context. Used by global authenticators and network handlers
     */
    ThreadLocal<DBCExecutionContext> ACTIVE_CONTEXT = new ThreadLocal<DBCExecutionContext>();

    enum InvalidateResult {
        DISCONNECTED,
        CONNECTED,
        RECONNECTED,
        ALIVE,
        ERROR
    }

    String getContextName();

    /**
     * Owner datasource
     */
    @NotNull
    DBPDataSource getDataSource();

    /**
     * Checks this context is really connected to remote database.
     * Usually DBSDataSourceContainer.getDataSource() returns datasource only if datasource is connected.
     * But in some cases (e.g. connection invalidation) datasource remains disconnected for some period of time.
     * @return true if underlying connection is alive.
     */
    boolean isConnected();

    /**
     * Opens new session
     * @param monitor progress monitor
     * @param purpose context purpose
     * @param task task description
     * @return execution context
     */
    @NotNull
    DBCSession openSession(@NotNull DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String task);

    /**
     * Checks context is alive and reconnects if needed.
     *
     * @throws org.jkiss.dbeaver.DBException on any error
     * @param monitor progress monitor
     * @return true if reconnect was applied false if connection is alive and nothing was done.
     */
    InvalidateResult invalidateContext(@NotNull DBRProgressMonitor monitor) throws DBException;

}
