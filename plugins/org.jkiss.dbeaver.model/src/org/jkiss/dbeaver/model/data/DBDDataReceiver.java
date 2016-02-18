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

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;

/**
 * Data receiver.
 * Used to receive some result set data.
 * Result set can be a result of some query execution, cursor returned from stored procedure, generated keys result set, etc.
 */
public interface DBDDataReceiver {

    void fetchStart(DBCSession session, DBCResultSet resultSet, long offset, long maxRows)
        throws DBCException;

    void fetchRow(DBCSession session, DBCResultSet resultSet)
        throws DBCException;

    /**
     * Called after entire result set if fetched.
     * @throws DBCException on error
     * @param session execution context
     * @param resultSet
     */
    void fetchEnd(DBCSession session, DBCResultSet resultSet)
        throws DBCException;

    /**
     * Called after entire result set is fetched and closed.
     * This method is called even if fetchStart wasn't called in this data receiver (may occur if statement throws an error)
     */
    void close();

}
