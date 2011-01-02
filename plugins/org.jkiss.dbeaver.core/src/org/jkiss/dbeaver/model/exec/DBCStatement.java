/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
}
