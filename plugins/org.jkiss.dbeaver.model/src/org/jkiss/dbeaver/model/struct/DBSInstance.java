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
package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * Data Source instance.
 * Instance wraps physical connection to database server.
 * Instance manages execution contexts.
 *
 * Single datasource may implement DBSInstance or DBSInstanceContainer
 */
public interface DBSInstance extends DBSObject, DBPCloseableObject
{
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

}
