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
package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

import java.util.Collection;

/**
 * Data Source.
 * Root object of all database structure and data objects.
 * Note: do not store direct references on datasource objects in any GUI components -
 * datasource instance could be refreshed at any time. Obtain references on datasource only
 * from DBSObject or DBPContextProvider interfaces.
 */
public interface DBPDataSource extends DBPObject, DBPCloseableObject
{
    /**
     * Datasource container
     * @return container implementation
     */
    @NotNull
    DBSDataSourceContainer getContainer();

    /**
     * Datasource information/options
     * Info SHOULD be read at datasource initialization stage and should be cached and available
     * at the moment of invocation of this function.
     * @return datasource info.
     */
    @NotNull
    DBPDataSourceInfo getInfo();

    /**
     * Default execution context
     * @param meta request for metadata operations context
     * @return default data source execution context.
     */
    @NotNull
    DBCExecutionContext getDefaultContext(boolean meta);

    /**
     * All opened execution contexts
     * @return collection of contexts
     */
    @NotNull
    Collection<? extends DBCExecutionContext> getAllContexts();

    /**
     * Opens new isolated execution context.
     *
     * @param monitor progress monitor
     * @param purpose context purpose (just a descriptive string)
     * @return execution context
     */
    @NotNull
    DBCExecutionContext openIsolatedContext(@NotNull DBRProgressMonitor monitor, @NotNull String purpose) throws DBException;

    /**
     * Reads base metadata from remote database or do any necessarily initialization routines.
     * @throws DBException on any DB error
     * @param monitor progress monitor
     */
    void initialize(@NotNull DBRProgressMonitor monitor) throws DBException;

}
