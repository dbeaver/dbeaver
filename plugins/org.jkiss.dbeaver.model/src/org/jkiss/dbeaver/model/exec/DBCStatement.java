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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;

/**
 * DBCStatement
 */
public interface DBCStatement extends DBPObject, DBRBlockingObject, DBPCloseableObject
{

    /**
     * Statement's context
     * @return context
     */
    @NotNull
    DBCSession getSession();

    /**
     * Statement's query string.
     * @return query string
     */
    @Nullable
    String getQueryString();

    /**
     * Statement source.
     * In most cases it is some DBSDataContainer (e.g. table). Also it could be SQL editor.
     * @return data container or null
     */
    @Nullable
    DBCExecutionSource getStatementSource();

    /**
     * Sets statement data source
     * @param source data source object
     */
    void setStatementSource(@Nullable DBCExecutionSource source);

    /**
     * Executes statement
     * @return true if statement returned result set, false otherwise
     * @throws DBCException on error
     */
    boolean executeStatement() throws DBCException;

    /**
     * Adds statement to execution batch (if supported)
     * @throws DBCException on error
     */
    void addToBatch() throws DBCException;

    /**
     * Executes batch of statements
     * @throws DBCException on error
     */
    int[] executeStatementBatch() throws DBCException;

    /**
     * Returns result set. Valid only on after {@link #executeStatement} invocation.
     * @return result set or null
     * @throws DBCException on error
     */
    @Nullable
    DBCResultSet openResultSet() throws DBCException;

    /**
     * Returns result set with generated key values. Valid only on after {@link #executeStatement} invocation.
     * @return result set or null
     * @throws DBCException on error
     */
    @Nullable
    DBCResultSet openGeneratedKeysResultSet() throws DBCException;

    /**
     * Returns number of rows updated by this statement executed.
     * @return number of row updated
     * @throws DBCException on error
     */
    int getUpdateRowCount() throws DBCException;

    /**
     * Checks whether there are additional results (result set or update count).
     * Moves statement to the next result set if it presents.
     */
    boolean nextResults() throws DBCException;

    /**
     * Sets statement result set limitations
     * @param offset first row index
     * @param limit maximum number of rows
     * @throws DBCException on error
     */
    void setLimit(long offset, long limit) throws DBCException;

    @Nullable
    Throwable[] getStatementWarnings() throws DBCException;

}
