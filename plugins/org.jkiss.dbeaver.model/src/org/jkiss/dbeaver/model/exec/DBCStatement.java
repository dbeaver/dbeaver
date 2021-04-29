/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
    long getUpdateRowCount() throws DBCException;

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

    /**
     * Returns warnings if any.
     * Also clears these warnings - immediate second invocation won't return any warnings.
     */
    @Nullable
    Throwable[] getStatementWarnings() throws DBCException;

    /**
     * Sets statement execution timeout (in seconds)
     * @throws DBCException
     */
    void setStatementTimeout(int timeout) throws DBCException;

    void setResultsFetchSize(int fetchSize) throws DBCException;
}
