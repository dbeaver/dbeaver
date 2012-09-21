/*
 * Copyright (C) 2010-2012 Serge Rieder
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
import org.jkiss.dbeaver.model.data.DBDAttributeValue;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.query.DBQOrderColumn;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.struct.AbstractTable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC abstract table implementation
 */
public abstract class JDBCTable<DATASOURCE extends DBPDataSource, CONTAINER extends DBSObjectContainer>
    extends AbstractTable<DATASOURCE, CONTAINER>
    implements DBSDataContainer, DBPSaveableObject
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
    public long readData(DBCExecutionContext context, DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows)
        throws DBException
    {
        if (!(context instanceof JDBCExecutionContext)) {
            throw new IllegalArgumentException("Bad execution context");
        }
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
            DBCStatementType.QUERY,
            query.toString(),
            firstRow,
            maxRows);
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
                dataReceiver.fetchStart(context, dbResult);

                long rowCount;
                try {
                    rowCount = 0;
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
                } finally {
                    try {
                        dataReceiver.fetchEnd(context);
                    } catch (DBCException e) {
                        log.error("Error while finishing result set fetch", e); //$NON-NLS-1$
                    }
                }
                return rowCount;
            }
            finally {
                dbResult.close();
            }
        }
        finally {
            dbStat.close();
            dataReceiver.close();
        }
    }

    @Override
    public long readDataCount(DBCExecutionContext context, DBDDataFilter dataFilter) throws DBException
    {
        if (!(context instanceof JDBCExecutionContext)) {
            throw new IllegalArgumentException("Bad execution context");
        }
        JDBCExecutionContext jdbcContext = (JDBCExecutionContext)context;
        DBRProgressMonitor monitor = context.getProgressMonitor();

        StringBuilder query = new StringBuilder("SELECT COUNT(*) FROM "); //$NON-NLS-1$
        query.append(getFullQualifiedName());
        appendQueryConditions(query, dataFilter);
        monitor.subTask(CoreMessages.model_jdbc_fetch_table_row_count);
        JDBCStatement dbStat = jdbcContext.prepareStatement(
            DBCStatementType.QUERY, query.toString(), false, false, false);
        try {
            dbStat.setDataContainer(this);
            if (!dbStat.executeStatement()) {
                return 0;
            }
            JDBCResultSet dbResult = dbStat.openResultSet();
            if (dbResult == null) {
                return 0;
            }
            try {
                if (dbResult.nextRow()) {
                    try {
                        return dbResult.getLong(1);
                    } catch (SQLException e) {
                        throw new DBException(e);
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
    public long insertData(DBCExecutionContext context, List<DBDAttributeValue> attributes, DBDDataReceiver keysReceiver)
        throws DBException
    {
        readRequiredMeta(context.getProgressMonitor());

        // Make query
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ").append(getFullQualifiedName()).append(" ("); //$NON-NLS-1$ //$NON-NLS-2$

        boolean hasKey = false;
        for (DBDAttributeValue attribute : attributes) {
            if (DBUtils.isNullValue(attribute.getValue())) {
                // do not use null values
                continue;
            }
            if (hasKey) query.append(","); //$NON-NLS-1$
            hasKey = true;
            query.append(DBUtils.getQuotedIdentifier(getDataSource(), attribute.getAttribute().getName()));
        }
        query.append(") VALUES ("); //$NON-NLS-1$
        hasKey = false;
        for (DBDAttributeValue attribute1 : attributes) {
            if (DBUtils.isNullValue(attribute1.getValue())) {
                continue;
            }
            if (hasKey) query.append(","); //$NON-NLS-1$
            hasKey = true;
            query.append("?"); //$NON-NLS-1$
        }
        query.append(")"); //$NON-NLS-1$

        // Execute
        DBCStatement dbStat = context.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, keysReceiver != null);
        try {
            dbStat.setDataContainer(this);

            // Set parameters
            int paramNum = 0;
            for (DBDAttributeValue attribute : attributes) {
                if (DBUtils.isNullValue(attribute.getValue())) {
                    continue;
                }
                DBDValueHandler valueHandler = DBUtils.getColumnValueHandler(context, attribute.getAttribute());
                valueHandler.bindValueObject(context, dbStat, attribute.getAttribute(), paramNum++, attribute.getValue());
            }

            // Execute statement
            dbStat.executeStatement();
            long rowCount = dbStat.getUpdateRowCount();
            if (keysReceiver != null) {
                readKeys(context, dbStat, keysReceiver);
            }
            return rowCount;
        }
        finally {
            dbStat.close();
        }

    }

    @Override
    public long updateData(
        DBCExecutionContext context,
        List<DBDAttributeValue> keyAttributes,
        List<DBDAttributeValue> updateAttributes,
        DBDDataReceiver keysReceiver)
        throws DBException
    {
        readRequiredMeta(context.getProgressMonitor());

        // Make query
        StringBuilder query = new StringBuilder();
        query.append("UPDATE ").append(getFullQualifiedName()).append(" SET "); //$NON-NLS-1$ //$NON-NLS-2$

        boolean hasKey = false;
        for (DBDAttributeValue attribute : updateAttributes) {
            if (hasKey) query.append(","); //$NON-NLS-1$
            hasKey = true;
            query.append(DBUtils.getQuotedIdentifier(getDataSource(), attribute.getAttribute().getName())).append("=?"); //$NON-NLS-1$
        }
        query.append(" WHERE "); //$NON-NLS-1$
        hasKey = false;
        for (DBDAttributeValue attribute : keyAttributes) {
            if (hasKey) query.append(" AND "); //$NON-NLS-1$
            hasKey = true;
            query.append(DBUtils.getQuotedIdentifier(getDataSource(), attribute.getAttribute().getName())).append("=?"); //$NON-NLS-1$
        }

        // Execute
        DBCStatement dbStat = context.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, keysReceiver != null);
        try {
            dbStat.setDataContainer(this);

            // Set parameters
            List<DBDAttributeValue> allAttribute = new ArrayList<DBDAttributeValue>(updateAttributes.size() + keyAttributes.size());
            allAttribute.addAll(updateAttributes);
            allAttribute.addAll(keyAttributes);
            for (int i = 0; i < allAttribute.size(); i++) {
                DBDAttributeValue attribute = allAttribute.get(i);
                DBDValueHandler valueHandler = DBUtils.getColumnValueHandler(context, attribute.getAttribute());
                valueHandler.bindValueObject(context, dbStat, attribute.getAttribute(), i, attribute.getValue());
            }

            // Execute statement
            dbStat.executeStatement();
            long rowCount = dbStat.getUpdateRowCount();
            if (keysReceiver != null) {
                readKeys(context, dbStat, keysReceiver);
            }
            return rowCount;
        }
        finally {
            dbStat.close();
        }
    }

    @Override
    public long deleteData(DBCExecutionContext context, List<DBDAttributeValue> keyAttributes)
        throws DBException
    {
        readRequiredMeta(context.getProgressMonitor());

        // Make query
        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ").append(getFullQualifiedName()).append(" WHERE "); //$NON-NLS-1$ //$NON-NLS-2$

        boolean hasKey = false;
        for (DBDAttributeValue attribute : keyAttributes) {
            if (hasKey) query.append(" AND "); //$NON-NLS-1$
            hasKey = true;
            query.append(DBUtils.getQuotedIdentifier(getDataSource(), attribute.getAttribute().getName())).append("=?"); //$NON-NLS-1$
        }

        // Execute
        DBCStatement dbStat = context.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, false);
        try {
            dbStat.setDataContainer(this);

            // Set parameters
            for (int i = 0; i < keyAttributes.size(); i++) {
                DBDAttributeValue attribute = keyAttributes.get(i);
                DBDValueHandler valueHandler = DBUtils.getColumnValueHandler(context, attribute.getAttribute());
                valueHandler.bindValueObject(context, dbStat, attribute.getAttribute(), i, attribute.getValue());
            }

            // Execute statement
            dbStat.executeStatement();
            return dbStat.getUpdateRowCount();
        }
        finally {
            dbStat.close();
        }
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
            if (!CommonUtils.isEmpty(dataFilter.getOrderColumns()) || !CommonUtils.isEmpty(dataFilter.getOrder())) {
                query.append(" ORDER BY "); //$NON-NLS-1$
                boolean hasOrder = false;
                for (DBQOrderColumn co : dataFilter.getOrderColumns()) {
                    if (hasOrder) query.append(',');
                    query.append(DBUtils.getQuotedIdentifier(getDataSource(), co.getColumnName()));
                    if (co.isDescending()) {
                        query.append(" DESC"); //$NON-NLS-1$
                    }
                    hasOrder = true;
                }
                if (!CommonUtils.isEmpty(dataFilter.getOrder())) {
                    if (hasOrder) query.append(',');
                    query.append(dataFilter.getOrder());
                }
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
        throws DBException
    {
        try {
            getAttributes(monitor);
        }
        catch (DBException e) {
            throw new DBException("Could not cache table columns", e);
        }
    }

}
