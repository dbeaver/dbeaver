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
package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

/**
 * Data Source.
 * Root object of all database structure and data objects.
 * Note: do not store direct references on datasource objects in any GUI components -
 * datasource instance could be refreshed at any time. Obtain references on datasource only
 * from DBSObject or IDataSourceProvider interfaces.
 */
public interface DBPDataSource extends DBCExecutionContext
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

    //DBCExecutionContext getPrimaryContext();

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
