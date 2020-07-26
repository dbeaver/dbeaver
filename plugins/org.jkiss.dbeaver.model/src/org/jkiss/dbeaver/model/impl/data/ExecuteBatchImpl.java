/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

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
        return processBatch(session, null, Collections.emptyMap());
    }

    @NotNull
    @Override
    public void generatePersistActions(@NotNull DBCSession session, @NotNull List<DBEPersistAction> actions, Map<String, Object> options) throws DBCException {
        processBatch(session, actions, options);
    }

    /**
     * Execute batch OR generate batch script.
     * @param session    session
     * @param actions    script actions. If not null then no execution will be done
     * @param options
     * @return execution statistics
     * @throws DBCException
     */
    @NotNull
    private DBCStatistics processBatch(@NotNull DBCSession session, @Nullable List<DBEPersistAction> actions, Map<String, Object> options) throws DBCException
    {
        //session.getProgressMonitor().subTask("Save batch (" + values.size() + ")");
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

            for (int rowIndex = 0; rowIndex < values.size(); rowIndex++) {
                Object[] rowValues = values.get(rowIndex);
                if (session.getProgressMonitor().isCanceled()) {
                    break;
                }
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
                    statement = prepareStatement(session, handlers, rowValues, options);
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
                        String queryString = formatQueryParameters(session, statement.getQueryString(), handlers, rowValues);
                        actions.add(
                            new SQLDatabasePersistAction(
                                "Execute statement",
                                queryString));
                    }
                } finally {
                    if (!reuse) {
                        statement.close();
                    }
                    if (rowIndex > 0 && rowIndex % 100 == 0) {
                        session.getProgressMonitor().subTask("Save batch (" + rowIndex + " of " + values.size() + ")");
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

    protected int getNextUsedParamIndex(Object[] attributeValues, int paramIndex) {
        return paramIndex + 1;
    }

    private String formatQueryParameters(DBCSession session, String queryString, DBDValueHandler[] handlers, Object[] rowValues) {
        if (handlers.length == 0) {
            return queryString;
        }
        if (CommonUtils.isEmpty(queryString)) {
            return queryString;
        }
        int length = queryString.length();
        StringBuilder formatted = new StringBuilder(length * 2);
        int paramIndex = -1;

        for (int i = 0; i < length; i++) {
            char c = queryString.charAt(i);
            switch (c) {
                case '?': {
                    paramIndex = getNextUsedParamIndex(rowValues, paramIndex);
                    if (paramIndex >= handlers.length) {
                        log.error("Parameter index out of range (" + paramIndex + " > " + handlers.length + ")");
                        continue;
                    }
                    Object paramValue = SQLUtils.convertValueToSQL(
                        session.getDataSource(),
                        attributes[paramIndex],
                        handlers[paramIndex],
                        rowValues[paramIndex]);
                    formatted.append(paramValue);
                    continue;
                }
                case ':': {
                    // FIXME: process named parameters
                    break;
                }
                case '\'':
                case '"': {
                    formatted.append(c);
                    for (int k = i + 1; k < length; k++) {
                        char c2 = queryString.charAt(k);
                        if (c2 == c && queryString.charAt(k - 1) != '\\') {
                            i = k;
                            c = c2;
                            break;
                        } else {
                            formatted.append(c2);
                        }
                    }
                    break;
                }
            }
            formatted.append(c);
        }

        return formatted.toString();
    }

    private void flushBatch(DBCStatistics statistics, DBCStatement statement) throws DBCException {
        long startTime = System.currentTimeMillis();
        int[] updatedRows = statement.executeStatementBatch();
        statistics.addExecuteTime(System.currentTimeMillis() - startTime);
        if (!ArrayUtils.isEmpty(updatedRows)) {
            for (int rows : updatedRows) {
                if (rows < 0) {
                    // In some cases (e.g. JDBC API) negative means "unknown".
                    // "Statement.SUCCESS_NO_INFO — the command was processed successfully, but the number of rows affected is unknown"
                    // But we are quite sure that it has to be 1 (because each statement inserts/deletes/updates a single row)
                    // The only exception is bulk delete  (without WHERE condition)
                    if (!ArrayUtils.isEmpty(attributes)) {
                        rows = 1;
                    }
                }
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
    protected abstract DBCStatement prepareStatement(@NotNull DBCSession session, DBDValueHandler[] handlers, Object[] attributeValues, Map<String, Object> options) throws DBCException;

    protected abstract void bindStatement(@NotNull DBDValueHandler[] handlers, @NotNull DBCStatement statement, Object[] attributeValues) throws DBCException;

    protected void executeStatement(DBCStatement statement) throws DBCException {
        statement.executeStatement();
    }

}
