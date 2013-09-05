/*
 * Copyright (C) 2010-2013 Serge Rieder
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
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.struct.AbstractTable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JDBC abstract table implementation
 */
public abstract class JDBCTable<DATASOURCE extends DBPDataSource, CONTAINER extends DBSObjectContainer>
    extends AbstractTable<DATASOURCE, CONTAINER>
    implements DBSDataManipulator, DBPSaveableObject
{
    static final Log log = LogFactory.getLog(JDBCTable.class);

    private boolean persisted;

    protected JDBCTable(CONTAINER container, boolean persisted)
    {
        super(container);
        this.persisted = persisted;
    }

    protected JDBCTable(CONTAINER container, String tableName, boolean persisted)
    {
        super(container, tableName);
        this.persisted = persisted;
    }

    public abstract JDBCStructCache<CONTAINER, ? extends JDBCTable, ? extends JDBCTableColumn> getCache();

    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    @Override
    public String getName()
    {
        return super.getName();
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }

    @Override
    public int getSupportedFeatures()
    {
        return DATA_COUNT | DATA_INSERT | DATA_UPDATE | DATA_DELETE | DATA_FILTER;
    }

    @Override
    public DBCStatistics readData(DBCExecutionContext context, DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows)
        throws DBCException
    {
        DBCStatistics statistics = new DBCStatistics();
        boolean hasLimits = firstRow >= 0 && maxRows > 0;

        DBRProgressMonitor monitor = context.getProgressMonitor();
        try {
            readRequiredMeta(monitor);
        } catch (DBException e) {
            log.warn(e);
        }

        StringBuilder query = new StringBuilder(100);
        query.append("SELECT * FROM ").append(getFullQualifiedName()); //$NON-NLS-1$
        appendQueryConditions(query, dataFilter);
        appendQueryOrder(query, dataFilter);

        monitor.subTask(CoreMessages.model_jdbc_fetch_table_data);
        DBCStatement dbStat = DBUtils.prepareStatement(
            context,
            DBCStatementType.SCRIPT,
            query.toString(),
            firstRow,
            maxRows);
        try {
            dbStat.setDataContainer(this);
            long startTime = System.currentTimeMillis();
            boolean executeResult = dbStat.executeStatement();
            statistics.setExecuteTime(System.currentTimeMillis() - startTime);
            if (executeResult) {
                DBCResultSet dbResult = dbStat.openResultSet();
                if (dbResult != null) {
                    try {
                        dataReceiver.fetchStart(context, dbResult);

                        try {
                            startTime = System.currentTimeMillis();
                            long rowCount = 0;
                            while (dbResult.nextRow()) {
                                if (monitor.isCanceled() || (hasLimits && rowCount >= maxRows)) {
                                    // Fetch not more than max rows
                                    break;
                                }
                                dataReceiver.fetchRow(context, dbResult);
                                rowCount++;
                                if (rowCount % 100 == 0) {
                                    monitor.subTask(rowCount + CoreMessages.model_jdbc__rows_fetched);
                                    monitor.worked(100);
                                }

                            }
                            statistics.setFetchTime(System.currentTimeMillis() - startTime);
                            statistics.setRowsFetched(rowCount);
                        } finally {
                            try {
                                dataReceiver.fetchEnd(context);
                            } catch (DBCException e) {
                                log.error("Error while finishing result set fetch", e); //$NON-NLS-1$
                            }
                        }
                    }
                    finally {
                        dbResult.close();
                    }
                }
            }
            return statistics;
        }
        finally {
            dbStat.close();
            dataReceiver.close();
        }
    }

    @Override
    public long countData(DBCExecutionContext context, DBDDataFilter dataFilter) throws DBCException
    {
        DBRProgressMonitor monitor = context.getProgressMonitor();

        StringBuilder query = new StringBuilder("SELECT COUNT(*) FROM "); //$NON-NLS-1$
        query.append(getFullQualifiedName());
        appendQueryConditions(query, dataFilter);
        monitor.subTask(CoreMessages.model_jdbc_fetch_table_row_count);
        DBCStatement dbStat = context.prepareStatement(
            DBCStatementType.QUERY,
            query.toString(),
            false, false, false);
        try {
            dbStat.setDataContainer(this);
            if (!dbStat.executeStatement()) {
                return 0;
            }
            DBCResultSet dbResult = dbStat.openResultSet();
            if (dbResult == null) {
                return 0;
            }
            try {
                if (dbResult.nextRow()) {
                    Object result = dbResult.getColumnValue(1);
                    if (result == null) {
                        return 0;
                    } else if (result instanceof Number) {
                        return ((Number) result).longValue();
                    } else {
                        return Long.parseLong(result.toString());
                    }
                } else {
                    return 0;
                }
            }
            finally {
                dbResult.close();
            }
        }
        finally {
            dbStat.close();
        }
    }

    @Override
    public ExecuteBatch insertData(DBCExecutionContext context, DBSEntityAttribute[] attributes, DBDDataReceiver keysReceiver)
        throws DBCException
    {
        readRequiredMeta(context.getProgressMonitor());

        // Make query
        StringBuilder query = new StringBuilder(200);
        query.append("INSERT INTO ").append(getFullQualifiedName()).append(" ("); //$NON-NLS-1$ //$NON-NLS-2$

        boolean hasKey = false;
        for (int i = 0; i < attributes.length; i++) {
            DBSEntityAttribute attribute = attributes[i];
            if (attribute.isSequence()) {
                continue;
            }
            if (hasKey) query.append(","); //$NON-NLS-1$
            hasKey = true;
            query.append(DBUtils.getQuotedIdentifier(getDataSource(), attribute.getName()));
        }
        query.append(") VALUES ("); //$NON-NLS-1$
        hasKey = false;
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i].isSequence()) {
                continue;
            }
            if (hasKey) query.append(","); //$NON-NLS-1$
            hasKey = true;
            query.append("?"); //$NON-NLS-1$
        }
        query.append(")"); //$NON-NLS-1$

        // Execute
        DBCStatement dbStat = context.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, keysReceiver != null);
        dbStat.setDataContainer(this);

        return new BatchImpl(dbStat, attributes, keysReceiver, true);
    }

    @Override
    public ExecuteBatch updateData(
        DBCExecutionContext context,
        DBSEntityAttribute[] updateAttributes,
        DBSEntityAttribute[] keyAttributes,
        DBDDataReceiver keysReceiver)
        throws DBCException
    {
        readRequiredMeta(context.getProgressMonitor());

        // Make query
        StringBuilder query = new StringBuilder();
        query.append("UPDATE ").append(getFullQualifiedName()).append(" SET "); //$NON-NLS-1$ //$NON-NLS-2$

        boolean hasKey = false;
        for (DBSEntityAttribute attribute : updateAttributes) {
            if (hasKey) query.append(","); //$NON-NLS-1$
            hasKey = true;
            query.append(DBUtils.getQuotedIdentifier(getDataSource(), attribute.getName())).append("=?"); //$NON-NLS-1$
        }
        query.append(" WHERE "); //$NON-NLS-1$
        hasKey = false;
        for (DBSEntityAttribute attribute : keyAttributes) {
            if (hasKey) query.append(" AND "); //$NON-NLS-1$
            hasKey = true;
            query.append(DBUtils.getQuotedIdentifier(getDataSource(), attribute.getName())).append("=?"); //$NON-NLS-1$
        }

        // Execute
        DBCStatement dbStat = context.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, keysReceiver != null);

        dbStat.setDataContainer(this);

        DBSEntityAttribute[] attributes = CommonUtils.concatArrays(updateAttributes, keyAttributes);

        return new BatchImpl(dbStat, attributes, keysReceiver, false);
    }

    @Override
    public ExecuteBatch deleteData(DBCExecutionContext context, DBSEntityAttribute[] keyAttributes)
        throws DBCException
    {
        readRequiredMeta(context.getProgressMonitor());

        // Make query
        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ").append(getFullQualifiedName()).append(" WHERE "); //$NON-NLS-1$ //$NON-NLS-2$

        boolean hasKey = false;
        for (DBSEntityAttribute attribute : keyAttributes) {
            if (hasKey) query.append(" AND "); //$NON-NLS-1$
            hasKey = true;
            query.append(DBUtils.getQuotedIdentifier(getDataSource(), attribute.getName())).append("=?"); //$NON-NLS-1$
        }

        // Execute
        DBCStatement dbStat = context.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, false);
        dbStat.setDataContainer(this);
        return new BatchImpl(dbStat, keyAttributes, null, false);
    }

    private void appendQueryConditions(StringBuilder query, DBDDataFilter dataFilter)
    {
        if (dataFilter != null && dataFilter.hasConditions()) {
            query.append(" WHERE "); //$NON-NLS-1$
            dataFilter.appendConditionString(getDataSource(), query);
        }
    }

    private void appendQueryOrder(StringBuilder query, DBDDataFilter dataFilter)
    {
        if (dataFilter != null) {
            // Construct ORDER BY
            if (dataFilter.hasOrdering()) {
                query.append(" ORDER BY "); //$NON-NLS-1$
                dataFilter.appendOrderString(getDataSource(), null, query);
            }
        }
    }

    private void readKeys(DBCExecutionContext context, DBCStatement dbStat, DBDDataReceiver keysReceiver)
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
            keysReceiver.fetchStart(context, dbResult);
            try {
                while (dbResult.nextRow()) {
                    keysReceiver.fetchRow(context, dbResult);
                }
            }
            finally {
                keysReceiver.fetchEnd(context);
            }
        }
        finally {
            dbResult.close();
            keysReceiver.close();
        }
    }

    /**
     * Reads and caches metadata which is required for data requests
     * @param monitor progress monitor
     * @throws DBException on error
     */
    private void readRequiredMeta(DBRProgressMonitor monitor)
        throws DBCException
    {
        try {
            getAttributes(monitor);
        }
        catch (DBException e) {
            throw new DBCException("Could not cache table columns", e);
        }
    }

    private class BatchImpl implements ExecuteBatch {

        private DBCStatement statement;
        private final DBSEntityAttribute[] attributes;
        private final List<Object[]> values = new ArrayList<Object[]>();
        private final DBDDataReceiver keysReceiver;
        private final boolean skipSequences;

        private BatchImpl(DBCStatement statement, DBSEntityAttribute[] attributes, DBDDataReceiver keysReceiver, boolean skipSequences)
        {
            this.statement = statement;
            this.attributes = attributes;
            this.keysReceiver = keysReceiver;
            this.skipSequences = skipSequences;
        }

        @Override
        public void add(Object[] attributeValues) throws DBCException
        {
            if (!CommonUtils.isEmpty(attributes) && CommonUtils.isEmpty(attributeValues)) {
                throw new DBCException("Bad attribute values: " + Arrays.toString(attributeValues));
            }
            values.add(attributeValues);
        }

        @Override
        public DBCStatistics execute() throws DBCException
        {
            if (statement == null) {
                throw new DBCException("Execute batch closed");
            }
            DBDValueHandler[] handlers = new DBDValueHandler[attributes.length];
            for (int i = 0; i < attributes.length; i++) {
                handlers[i] = DBUtils.findValueHandler(statement.getContext(), attributes[i]);
            }

            boolean useBatch = statement.getContext().getDataSource().getInfo().supportsBatchUpdates();
            if (values.size() <= 1) {
                useBatch = false;
            }

            DBCStatistics statistics = new DBCStatistics();
            for (Object[] rowValues : values) {
                int paramIndex = 0;
                for (int k = 0; k < handlers.length; k++) {
                    DBDValueHandler handler = handlers[k];
                    if (skipSequences && attributes[k].isSequence()) {
                        continue;
                    }
                    handler.bindValueObject(statement.getContext(), statement, attributes[k], paramIndex++, rowValues[k]);
                }
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
                        readKeys(statement.getContext(), statement, keysReceiver);
                    }
                }
            }
            values.clear();

            if (useBatch) {
                // Process batch
                long startTime = System.currentTimeMillis();
                int[] updatedRows = statement.executeStatementBatch();
                statistics.addExecuteTime(System.currentTimeMillis() - startTime);
                if (!CommonUtils.isEmpty(updatedRows)) {
                    for (int rows : updatedRows) {
                        statistics.addRowsUpdated(rows);
                    }
                }
            }

            return statistics;
        }

        @Override
        public void close()
        {
            statement.close();
            statement = null;
        }

    }

}
