/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSInstance;

/**
 * Execution context.
 * Provides access to execution sessions.
 * Usually contains some kind of physical database connection inside
 */
public interface DBCExecutionContext extends DBPObject,DBPCloseableObject
{
    enum InvalidateResult {
        DISCONNECTED,
        CONNECTED,
        RECONNECTED,
        ALIVE,
        ERROR
    }

    /**
     * Unique context ID. Generated in the moment of context creation and never changes during context lifetime.
     */
    long getContextId();

    /**
     * Context name. Like MAin, Metadata, Script X, etc.
     */
    @NotNull
    String getContextName();

    /**
     * Owner datasource
     */
    @NotNull
    DBPDataSource getDataSource();

    DBSInstance getOwnerInstance();

    /**
     * Checks this context is really connected to remote database.
     * Usually DBPDataSourceContainer.getDataSource() returns datasource only if datasource is connected.
     * But in some cases (e.g. connection invalidation) datasource remains disconnected for some period of time.
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
    DBCSession openSession(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionPurpose purpose, @NotNull String task);

    /**
     * Checks whether this context is alive and underlying network connection isn't broken.
     * Implementation should perform server round-trip.
     * This function is also used for keep-alive function.
     * @param monitor    monitor
     * @throws DBException on any network errors
     */
    void checkContextAlive(DBRProgressMonitor monitor)
        throws DBException;

    /**
     * Checks context is alive and reconnects if needed.
     *
     * @throws org.jkiss.dbeaver.DBException on any error
     * @param monitor progress monitor
     * @param closeOnFailure
     * @return true if reconnect was applied false if connection is alive and nothing was done.
     */
    @NotNull
    InvalidateResult invalidateContext(@NotNull DBRProgressMonitor monitor, boolean closeOnFailure) throws DBException;

    /**
     * Defaults reader/writer.
     * @return null if defaults are not supported
     */
    @Nullable
    DBCExecutionContextDefaults getContextDefaults();
}
