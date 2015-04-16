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
package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Execution session
 */
public interface DBCSession extends DBPCloseableObject, DBDPreferences {

    /**
     * Session title
     * @return title
     */
    String getTaskTitle();

    /**
     * Data source of this session
     * @return data source
     */
    DBCExecutionContext getExecutionContext();

    /**
     * Data source of this session
     * @return data source
     */
    DBPDataSource getDataSource();

    /**
     * Performs check that this context is really connected to remote database
     * @return connected state
     */
    boolean isConnected();

    /**
     * Context's progress monitor.
     * Each context has it's progress monitor which is passed at context creation time and never changes.
     * @return progress monitor
     */
    DBRProgressMonitor getProgressMonitor();

    /**
     * Context's purpose
     * @return purpose
     */
    DBCExecutionPurpose getPurpose();

    /**
     * Prepares statements
     */
    DBCStatement prepareStatement(
        DBCStatementType type,
        String query,
        boolean scrollable,
        boolean updatable,
        boolean returnGeneratedKeys) throws DBCException;

    boolean isLoggingEnabled();
    /**
     * Enables/disables operations logging within this session
     * @param enable enable
     */
    void enableLogging(boolean enable);
}
