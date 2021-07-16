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
import org.jkiss.utils.CommonUtils;

import java.util.*;

public abstract class ExecuteBatchWithMultipleInsert extends ExecuteBatchImpl {
    /**
     * Constructs new batch
     *
     * @param attributes     array of attributes used in batch
     * @param keysReceiver   keys receiver (or null)
     * @param reuseStatement true if engine should reuse single prepared statement for each execution.
     */
    protected ExecuteBatchWithMultipleInsert(@NotNull DBSAttributeBase[] attributes, @Nullable DBDDataReceiver keysReceiver, boolean reuseStatement) {
        super(attributes, keysReceiver, reuseStatement);
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
        DBCStatement statement = null;

        try {
            int multiInsertBatchSize = CommonUtils.toInt(options.get(DBSDataManipulator.OPTION_MULTI_INSERT_BATCH_SIZE), 1000);

            int valuesListSize = values.size();
            List<Object> allMultiInsertValuesList = new ArrayList<>();
            for (int i = 0; i < valuesListSize; i++) {
                if (session.getProgressMonitor().isCanceled()) {
                    break;
                }
                Object[] objects = values.get(i);
                if (i == valuesListSize - 1 || allMultiInsertValuesList.size() + objects.length > multiInsertBatchSize) {
                    if (i == valuesListSize - 1) {
                        Collections.addAll(allMultiInsertValuesList, objects);
                    }
                    Object[] allMultiInsertValues = allMultiInsertValuesList.toArray(new Object[0]);
                    statement = prepareStatement(session, handlers, allMultiInsertValues, options);
                    statistics.setQueryText(statement.getQueryString());
                    statistics.addStatementsCount();
                    bindStatement(handlers, statement, allMultiInsertValues);
                    statement.addToBatch();
                    flushBatch(statistics, statement);
                    allMultiInsertValuesList.clear();
                    if (i == valuesListSize - 1) {
                        break;
                    }
                }
                Collections.addAll(allMultiInsertValuesList, objects);
            }
            values.clear();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }

        return statistics;
    }
}
