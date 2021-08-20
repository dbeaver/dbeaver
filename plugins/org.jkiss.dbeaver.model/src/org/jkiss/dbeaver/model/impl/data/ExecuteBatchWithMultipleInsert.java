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
package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public class ExecuteBatchWithMultipleInsert extends ExecuteInsertBatchImpl {

    private final DBCExecutionSource source;
    private DBSTable table;

    /**
     * Constructs new batch
     *
     * @param attributes     array of attributes used in batch
     * @param keysReceiver   keys receiver (or null)
     * @param reuseStatement true if engine should reuse single prepared statement for each execution.
     */
    public ExecuteBatchWithMultipleInsert(@NotNull DBSAttributeBase[] attributes, @Nullable DBDDataReceiver keysReceiver, boolean reuseStatement, @NotNull DBCSession session, @NotNull final DBCExecutionSource source, @NotNull DBSTable table) {
        super(attributes, keysReceiver, reuseStatement, session, source, table, false);
        this.source = source;
        this.table = table;
    }

    @Override
    protected void bindStatement(@NotNull DBDValueHandler[] handlers, @NotNull DBCStatement statement, Object[] attributeValues) throws DBCException {
        int paramIndex = 0;
        int handlersLength = handlers.length;
        int attributeCount = 0;
        for (Object attribute : attributeValues) {
            if (DBUtils.isPseudoAttribute(attributes[attributeCount])) {
                continue;
            }
            handlers[attributeCount].bindValueObject(statement.getSession(), statement, attributes[attributeCount], paramIndex++, attribute);
            attributeCount++;
            if (attributeCount == handlersLength) {
                attributeCount = 0;
            }
        }
    }

    @NotNull
    @Override
    protected DBCStatement prepareStatement(@NotNull DBCSession session, DBDValueHandler[] handlers, Object[] attributeValues, Map<String, Object> options) throws DBCException {
        StringBuilder queryForStatement = prepareQueryForStatement(session, handlers, attributeValues, attributes, table, true, options);
        // Execute
        DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, queryForStatement.toString(), false, false, keysReceiver != null);
        dbStat.setStatementSource(source);
        return dbStat;
    }

    @NotNull
    @Override
    public DBCStatistics processBatch(@NotNull DBCSession session, @Nullable List<DBEPersistAction> actions, Map<String, Object> options) throws DBCException {
        int attributesLength = attributes.length;
        DBDValueHandler[] handlers = new DBDValueHandler[attributesLength];
        for (int i = 0; i < attributesLength; i++) {
            if (attributes[i] instanceof DBDAttributeBinding) {
                handlers[i] = ((DBDAttributeBinding)attributes[i]).getValueHandler();
            } else {
                handlers[i] = DBUtils.findValueHandler(session, attributes[i]);
            }
        }

        DBCStatistics statistics = new DBCStatistics();
        DBCStatement batchStatement = null;

        try {
            int multiRowInsertBatchSize = CommonUtils.toInt(options.get(DBSDataManipulator.OPTION_MULTI_INSERT_BATCH_SIZE), 100);
            boolean skipBindValues = CommonUtils.toBoolean(options.get(DBSDataManipulator.OPTION_SKIP_BIND_VALUES));


            int rowsCount = values.size();
            List<Object> multiRowInsertBatchValuesList = new ArrayList<>();
            for (int i = 0; i < rowsCount; i++) {
                if (session.getProgressMonitor().isCanceled()) {
                    break;
                }
                Object[] objects = values.get(i);
                // Execute batch if it has a suitable size, or this are the last values
                if (i == rowsCount - 1 || (i != 0 && i % multiRowInsertBatchSize == 0)) {
                    // We can reuse statement, but not for the last values (their amount can be different from previous batches)
                    if (i == rowsCount - 1) {
                        Collections.addAll(multiRowInsertBatchValuesList, objects);
                        Object[] allMultiInsertValues = multiRowInsertBatchValuesList.toArray(new Object[0]);
                        try (DBCStatement statement = prepareStatement(session, handlers, allMultiInsertValues, options)) {
                            bindAndFlushStatement(handlers, statistics, statement, allMultiInsertValues, skipBindValues);
                            multiRowInsertBatchValuesList.clear();
                            break;
                        }
                    }
                    Object[] allMultiInsertValuesBatch = multiRowInsertBatchValuesList.toArray(new Object[0]);
                    batchStatement = prepareStatement(session, handlers, allMultiInsertValuesBatch, options);
                    bindAndFlushStatement(handlers, statistics, batchStatement, allMultiInsertValuesBatch, skipBindValues);
                    multiRowInsertBatchValuesList.clear();
                }
                Collections.addAll(multiRowInsertBatchValuesList, objects);
            }
            values.clear();
        } finally {
            if (batchStatement != null) {
                 batchStatement.close();
            }
        }

        return statistics;
    }

    private void bindAndFlushStatement(DBDValueHandler[] handlers, DBCStatistics statistics, DBCStatement batchStatement, Object[] allMultiInsertValues, boolean skipBindValues) throws DBCException {
        statistics.setQueryText(batchStatement.getQueryString());
        statistics.addStatementsCount();
        if (!skipBindValues) {
            bindStatement(handlers, batchStatement, allMultiInsertValues);
        }
        batchStatement.addToBatch();
        flushBatch(statistics, batchStatement);
    }
}
