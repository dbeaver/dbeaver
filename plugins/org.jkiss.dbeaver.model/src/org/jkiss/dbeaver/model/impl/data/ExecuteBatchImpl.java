/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Execute batch.
 * Can be used in JDBC or any other underlying DB APIs
 *
*/
public abstract class ExecuteBatchImpl implements DBSDataManipulator.ExecuteBatch {

    private static final Log log = Log.getLog(ExecuteBatchImpl.class);

    protected final DBSAttributeBase[] attributes;
    protected final List<Object[]> values = new ArrayList<>();
    protected final DBDDataReceiver keysReceiver;
    protected final boolean reuseStatement;

    /**
     * Constructs new batch
     * @param attributes array of attributes used in batch
     * @param keysReceiver keys receiver (or null)
     * @param reuseStatement true if engine should reuse single prepared statement for each execution.
     */
    protected ExecuteBatchImpl(@NotNull DBSAttributeBase[] attributes, @Nullable DBDDataReceiver keysReceiver, boolean reuseStatement)
    {
        this.attributes = attributes;
        this.keysReceiver = keysReceiver;
        this.reuseStatement = reuseStatement;
    }

    @Override
    public void add(@NotNull Object[] attributeValues) throws DBCException
    {
        if (!ArrayUtils.isEmpty(attributes) && ArrayUtils.isEmpty(attributeValues)) {
            throw new DBCException("Bad attribute values: " + Arrays.toString(attributeValues));
        }
        values.add(attributeValues);
    }

    @NotNull
    @Override
    public DBCStatistics execute(@NotNull DBCSession session) throws DBCException
    {
        return processBatch(session, null);
    }

    @Override
    public void generatePersistActions(@NotNull DBCSession session, @NotNull List<DBEPersistAction> actions) throws DBCException {
        processBatch(session, actions);
    }

    /**
     * Execute batch OR generate batch script.
     * @param session    session
     * @param actions    script actions. If not null then no execution will be done
     * @return execution statistics
     * @throws DBCException
     */
    @NotNull
    private DBCStatistics processBatch(@NotNull DBCSession session, @Nullable List<DBEPersistAction> actions) throws DBCException
    {
        DBDValueHandler[] handlers = new DBDValueHandler[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i] instanceof DBDAttributeBinding) {
                handlers[i] = ((DBDAttributeBinding)attributes[i]).getValueHandler();
            } else {
                handlers[i] = DBUtils.findValueHandler(session, attributes[i]);
            }
        }

        boolean useBatch = session.getDataSource().getInfo().supportsBatchUpdates() && reuseStatement;
        if (values.size() <= 1) {
            useBatch = false;
        }

        DBCStatistics statistics = new DBCStatistics();
        DBCStatement statement = null;

        try {
            // Here we'll try to reuse prepared statement.
            // It makes a great sense in case of data transfer where we need millions of inserts.
            // We must be aware of nulls because actual insert statements may differ depending on null values.
            // So if row nulls aren't the same as in previous row we need to prepare new statement and restart batch.
            // Quite complicated but works.
            boolean[] prevNulls = new boolean[attributes.length];
            boolean[] nulls = new boolean[attributes.length];
            int statementsInBatch = 0;

            for (Object[] rowValues : values) {
                boolean reuse = reuseStatement;
                if (reuse) {
                    for (int i = 0; i < rowValues.length; i++) {
                        nulls[i] = DBUtils.isNullValue(rowValues[i]);
                    }
                    if (!Arrays.equals(prevNulls, nulls) && statementsInBatch > 0) {
                        reuse = false;
                    }
                    System.arraycopy(nulls, 0, prevNulls, 0, nulls.length);
                    if (!reuse && statementsInBatch > 0) {
                        // Flush batch
                        if (actions == null) {
                            flushBatch(statistics, statement);
                        }
                        statement.close();
                        statement = null;
                        statementsInBatch = 0;
                        reuse = true;
                    }
                }
                if (statement == null || !reuse) {
                    statement = prepareStatement(session, rowValues);
                    statistics.setQueryText(statement.getQueryString());
                    statistics.addStatementsCount();
                }
                try {
                    bindStatement(handlers, statement, rowValues);
                    if (actions == null) {
                        if (useBatch) {
                            statement.addToBatch();
                            statementsInBatch++;
                        } else {
                            // Execute each row separately
                            long startTime = System.currentTimeMillis();
                            executeStatement(statement);
                            statistics.addExecuteTime(System.currentTimeMillis() - startTime);

                            long rowCount = statement.getUpdateRowCount();
                            if (rowCount > 0) {
                                statistics.addRowsUpdated(rowCount);
                            }

                            // Read keys
                            if (keysReceiver != null) {
                                try {
                                    readKeys(statement.getSession(), statement, keysReceiver);
                                } catch (Exception e) {
                                    log.warn("Error reading auto-generated keys", e);
                                }
                            }
                        }
                    } else {
                        String queryString;
                        if (statement instanceof DBCParameterizedStatement) {
                            queryString = ((DBCParameterizedStatement)statement).getFormattedQuery();
                        } else {
                            queryString = statement.getQueryString();
                        }
                        actions.add(
                            new SQLDatabasePersistAction(
                                "Execute statement",
                                queryString));
                    }
                } finally {
                    if (!reuse) {
                        statement.close();
                    }
                }
            }
            values.clear();

            if (statementsInBatch > 0) {
                if (actions == null) {
                    flushBatch(statistics, statement);
                }
                statement.close();
                statement = null;
            }
        } finally {
            if (reuseStatement && statement != null) {
                statement.close();
            }
        }

        return statistics;
    }

    private void flushBatch(DBCStatistics statistics, DBCStatement statement) throws DBCException {
        long startTime = System.currentTimeMillis();
        int[] updatedRows = statement.executeStatementBatch();
        statistics.addExecuteTime(System.currentTimeMillis() - startTime);
        if (!ArrayUtils.isEmpty(updatedRows)) {
            for (int rows : updatedRows) {
                statistics.addRowsUpdated(rows);
            }
        }
    }

    @Override
    public void close()
    {
    }

    private void readKeys(@NotNull DBCSession session, @NotNull DBCStatement dbStat, @NotNull DBDDataReceiver keysReceiver)
        throws DBCException
    {
        DBCResultSet dbResult;
        try {
            dbResult = dbStat.openGeneratedKeysResultSet();
        }
        catch (Throwable e) {
            log.debug("Error obtaining generated keys", e); //$NON-NLS-1$
            return;
        }
        if (dbResult == null) {
            return;
        }
        try {
            keysReceiver.fetchStart(session, dbResult, -1, -1);
            try {
                while (dbResult.nextRow()) {
                    keysReceiver.fetchRow(session, dbResult);
                }
            }
            finally {
                keysReceiver.fetchEnd(session, dbResult);
            }
        }
        finally {
            dbResult.close();
            keysReceiver.close();
        }
    }

    @NotNull
    protected abstract DBCStatement prepareStatement(@NotNull DBCSession session, Object[] attributeValues) throws DBCException;

    protected abstract void bindStatement(@NotNull DBDValueHandler[] handlers, @NotNull DBCStatement statement, Object[] attributeValues) throws DBCException;

    protected void executeStatement(DBCStatement statement) throws DBCException {
        statement.executeStatement();
    }

}
