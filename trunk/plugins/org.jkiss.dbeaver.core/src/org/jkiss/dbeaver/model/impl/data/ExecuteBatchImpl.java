/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
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

    static final Log log = LogFactory.getLog(ExecuteBatchImpl.class);

    protected final DBSAttributeBase[] attributes;
    protected final List<Object[]> values = new ArrayList<Object[]>();
    protected final DBDDataReceiver keysReceiver;
    protected final boolean reuseStatement;

    public ExecuteBatchImpl(@NotNull DBSAttributeBase[] attributes, @Nullable DBDDataReceiver keysReceiver, boolean reuseStatement)
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
            handlers[i] = DBUtils.findValueHandler(session, attributes[i]);
        }

        boolean useBatch = session.getDataSource().getInfo().supportsBatchUpdates();
        if (values.size() <= 1) {
            useBatch = false;
        }

        DBCStatistics statistics = new DBCStatistics();
        DBCStatement statement = null;

        try {
            for (Object[] rowValues : values) {
                if (statement == null) {
                    statement = prepareStatement(session, rowValues);
                    statistics.setQueryText(statement.getQueryString());
                }
                try {
                    bindStatement(handlers, statement, rowValues);
                    if (useBatch) {
                        statement.addToBatch();
                    } else {
                        // Execute each row separately
                        long startTime = System.currentTimeMillis();
                        statement.executeStatement();
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
                    if (!reuseStatement) {
                        statement.close();
                        statement = null;
                    }
                }
            }
            values.clear();

            if (useBatch && statement != null) {
                // Process batch
                long startTime = System.currentTimeMillis();
                int[] updatedRows = statement.executeStatementBatch();
                statistics.addExecuteTime(System.currentTimeMillis() - startTime);
                if (!ArrayUtils.isEmpty(updatedRows)) {
                    for (int rows : updatedRows) {
                        statistics.addRowsUpdated(rows);
                    }
                }
            }
        } finally {
            if (statement != null) {
                statement.close();
                statement = null;
            }
        }

        return statistics;
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
                keysReceiver.fetchEnd(session);
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

}
