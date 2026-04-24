/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBRuntimeException;
import org.jkiss.dbeaver.model.DBFetchProgress;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Data receiver.
 * Used to receive some result set data.
 * Result set can be a result of some query execution, cursor returned from stored procedure, generated keys result set, etc.
 */
public interface DBDDataReceiver extends AutoCloseable {

    void fetchStart(@NotNull DBCSession session, @NotNull DBCResultSet resultSet, long offset, long maxRows)
        throws DBException;

    void fetchRow(@NotNull DBCSession session, @NotNull DBCResultSet resultSet)
        throws DBException;

    /**
     * Called after entire result set if fetched.
     * WARN: It SHOULD be called after owner statement close. Because in fetchEnd additional queries/server reads may be performed.
     * This may cause statement lock issues.
     * @throws DBCException on error
     * @param session execution context
     * @param resultSet    result set
     */
    void fetchEnd(@NotNull DBCSession session, @NotNull DBCResultSet resultSet)
        throws DBException;

    /**
     * Called after entire result set is fetched and closed.
     * This method is called even if fetchStart wasn't called in this data receiver (may occur if statement throws an error)
     */
    void close();

    // FIXME: we should keep in variable or do not keep it at all (use separate interface)
    @NotNull
    default DBCStatistics getStatistics() {
        return new DBCStatistics();
    }

    static void startFetchWorkflow(
        @NotNull DBDDataReceiver dataReceiver,
        @NotNull DBCSession session,
        @NotNull DBCResultSet resultSet,
        long offset,
        long maxRows
    ) throws DBException {
        dataReceiver.fetchStart(session, resultSet, offset, maxRows);
        resultSet.getSourceStatement().autoCloseDependant(() -> {
            try (dataReceiver) {
                dataReceiver.fetchEnd(session, resultSet);
            } catch (DBCException e) {
                throw new DBRuntimeException("Error while finishing result set fetching into " + dataReceiver, e);
            }
        });
    }

    static void fetchRowsWithStatistics(
        @NotNull DBDDataReceiver dataReceiver,
        @NotNull DBCSession session,
        @NotNull DBCResultSet resultSet,
        @NotNull DBCStatistics statistics
    ) throws DBException {
        DBRProgressMonitor progressMonitor = session.getProgressMonitor();
        DBFetchProgress fetchProgress = new DBFetchProgress(progressMonitor);

        while (!progressMonitor.isCanceled() && resultSet.nextRow()) {
            dataReceiver.fetchRow(session, resultSet);
            fetchProgress.monitorRowFetch();
        }
        fetchProgress.dumpStatistics(statistics);
    }


}
