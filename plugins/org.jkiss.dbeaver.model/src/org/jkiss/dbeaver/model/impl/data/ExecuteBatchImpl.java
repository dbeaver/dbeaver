/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;
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
                        flushBatch(statistics, statement);
                        statement.close();
                        statement = null;
                        statementsInBatch = 0;
                        reuse = true;
                    }
                }
                if (statement == null || !reuse) {
                    statement = prepareStatement(session, rowValues);
                    statistics.setQueryText(statement.getQueryString());
                }
                try {
                    bindStatement(handlers, statement, rowValues);
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
                            readKeys(statement.getSession(), statement, keysReceiver);
                        }
                    }
                } finally {
                    if (!reuse) {
                        statement.close();
                    }
                }
            }
            values.clear();

            if (statementsInBatch > 0) {
                flushBatch(statistics, statement);
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
