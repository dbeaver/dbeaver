/*
 * Copyright (C) 2010-2013 Serge Rieder
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

import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DBCStatement
 */
public interface DBCStatement extends DBPObject, DBRBlockingObject
{

    /**
     * Statement's context
     * @return context
     */
    DBCExecutionContext getContext();

    /**
     * Statement's query string.
     * @return query string
     */
    String getQueryString();

    /**
     * Statement's description
     * @return description string or null
     */
    String getDescription();

    /**
     * Executes statement
     * @return true if statement returned result set, false otherwise
     * @throws DBCException on error
     */
    boolean executeStatement() throws DBCException;

    /**
     * Returns result set. Valid only on after {@link #executeStatement} invocation.
     * @return result set or null
     * @throws DBCException on error
     */
    DBCResultSet openResultSet() throws DBCException;

    /**
     * Returns result set with generated key values. Valid only on after {@link #executeStatement} invocation.
     * @return result set or null
     * @throws DBCException on error
     */
    DBCResultSet openGeneratedKeysResultSet() throws DBCException;

    /**
     * Returns number of rows updated by this statement executed.
     * @return number of row updated
     * @throws DBCException on error
     */
    int getUpdateRowCount() throws DBCException;

    /**
     * Close statement.
     * No exceptions could be thrown from this method. If any error will occur then it'll be logged.
     */
    void close();

    /**
     * Sets statement result set limitations
     * @param offset first row index
     * @param limit maximum number of rows
     * @throws DBCException on error
     */
    void setLimit(long offset, long limit) throws DBCException;

    /**
     * Statement data container.
     * Usually it is null, but in some cases (like table data editor) it's set to certain DBS object.
     * Can be used by result set metadata objects to perform values manipulation.
     * @return data container or null
     */
    DBSObject getDataContainer();

    /**
     * Sets statement data container
     * @param container data container object
     */
    void setDataContainer(DBSObject container);

    /**
     * Gets any user object associated with this statement
     * @return user data object or null
     */
    Object getUserData();

    /**
     * Sets user data
     */
    void setUserData(Object userData);

    //long getExecutionTimeout() throws DBCException;

    //void setExecutionTimeout(long  seconds) throws DBCException;

}
