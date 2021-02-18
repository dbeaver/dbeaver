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

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;

/**
 * Data receiver.
 * Used to receive some result set data.
 * Result set can be a result of some query execution, cursor returned from stored procedure, generated keys result set, etc.
 */
public interface DBDDataReceiver extends AutoCloseable {

    void fetchStart(DBCSession session, DBCResultSet resultSet, long offset, long maxRows)
        throws DBCException;

    void fetchRow(DBCSession session, DBCResultSet resultSet)
        throws DBCException;

    /**
     * Called after entire result set if fetched.
     * @throws DBCException on error
     * @param session execution context
     * @param resultSet    result set
     */
    void fetchEnd(DBCSession session, DBCResultSet resultSet)
        throws DBCException;

    /**
     * Called after entire result set is fetched and closed.
     * This method is called even if fetchStart wasn't called in this data receiver (may occur if statement throws an error)
     */
    void close();

}
