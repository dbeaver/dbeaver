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
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCColumnMetaData;
import org.jkiss.dbeaver.model.impl.struct.AbstractTable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.ArrayUtils;
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
    public static final String DEFAULT_TABLE_ALIAS = "x";
    public static final int DEFAULT_READ_FETCH_SIZE = 10000;

    private boolean persisted;

    protected JDBCTable(CONTAINER container, boolean persisted)
    {
        super(container);
        this.persisted = persisted;
    }

    protected JDBCTable(CONTAINER container, @Nullable String tableName, boolean persisted)
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

    @NotNull
    @Override
    public DBCStatistics readData(@NotNull DBCSession session, @NotNull DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows, long flags)
        throws DBCException
    {
        DBCStatistics statistics = new DBCStatistics();
        boolean hasLimits = firstRow >= 0 && maxRows > 0;

        DBPDataSource dataSource = session.getDataSource();
        DBRProgressMonitor monitor = session.getProgressMonitor();
        try {
            readRequiredMeta(monitor);
        } catch (DBException e) {
            log.warn(e);
        }

        DBDPseudoAttribute rowIdAttribute = null;
        if ((flags & FLAG_READ_PSEUDO) != 0 && this instanceof DBDPseudoAttributeContainer) {
            try {
                rowIdAttribute = DBDPseudoAttribute.getAttribute(
                    ((DBDPseudoAttributeContainer) this).getPseudoAttributes(),
                    DBDPseudoAttributeType.ROWID);
            } catch (DBException e) {
                log.warn("Can't get pseudo attributes for '" + getName() + "'", e);
            }
        }
        // Always use alias. Some criteria doesn't work without alias
        // (e.g. structured attributes in Oracle requires table alias)
        String tableAlias = null;
        if (dataSource instanceof SQLDataSource) {
            if (((SQLDataSource )dataSource).getSQLDialect().supportsAliasInSelect()) {
                tableAlias = DEFAULT_TABLE_ALIAS;
            }
        }
        StringBuilder query = new StringBuilder(100);
        if (rowIdAttribute != null) {
            if (tableAlias != null) {
                // If we have pseudo attributes then query gonna be more complex
                query.append("SELECT ").append(tableAlias).append(".*"); //$NON-NLS-1$
                query.append(",").append(rowIdAttribute.translateExpression(tableAlias));
                if (rowIdAttribute.getAlias() != null) {
                    query.append(" as ").append(rowIdAttribute.getAlias());
                }
            } else {
                log.warn("Can't query ROWID - table alias not supported");
                rowIdAttribute = null;
            }
        } else {
            query.append("SELECT ");
            if (tableAlias != null) {
                query.append(tableAlias).append(".");
            }
            query.append("*"); //$NON-NLS-1$
        }
        query.append(" FROM ").append(getFullQualifiedName());
        if (tableAlias != null) {
            query.append(" ").append(tableAlias); //$NON-NLS-1$
        }
        appendQueryConditions(query, tableAlias, dataFilter);
        appendQueryOrder(query, tableAlias, dataFilter);

        String sqlQuery = query.toString();
        statistics.setQueryText(sqlQuery);

        monitor.subTask(CoreMessages.model_jdbc_fetch_table_data);
        DBCStatement dbStat = DBUtils.prepareStatement(
            session,
            DBCStatementType.SCRIPT,
            sqlQuery,
            firstRow,
            maxRows);
        try {
            dbStat.setStatementSource(this);
/*
            if (dbStat instanceof JDBCStatement) {
                try {
                    ((JDBCStatement)dbStat).setFetchSize(
                        maxRows < DEFAULT_READ_FETCH_SIZE ? DEFAULT_READ_FETCH_SIZE : (int)maxRows);
                } catch (Exception e) {
                    log.warn(e);
                }
            }
*/
            long startTime = System.currentTimeMillis();
            boolean executeResult = dbStat.executeStatement();
            statistics.setExecuteTime(System.currentTimeMillis() - startTime);
            if (executeResult) {
                DBCResultSet dbResult = dbStat.openResultSet();
                if (dbResult != null) {
                    try {
                        if (rowIdAttribute != null) {
                            String attrId = rowIdAttribute.getAlias();
                            if (CommonUtils.isEmpty(attrId)) {
                                attrId = rowIdAttribute.getName();
                            }
                            // Annotate last attribute with row id
                            List<DBCAttributeMetaData> metaAttributes = dbResult.getResultSetMetaData().getAttributes();
                            for (int i = metaAttributes.size(); i > 0; i--) {
                                DBCAttributeMetaData attr = metaAttributes.get(i - 1);
                                if (attrId.equalsIgnoreCase(attr.getName()) && attr instanceof JDBCColumnMetaData) {
                                    ((JDBCColumnMetaData)attr).setPseudoAttribute(rowIdAttribute);
                                    break;
                                }
                            }
                        }
                        dataReceiver.fetchStart(session, dbResult);

                        try {
                            startTime = System.currentTimeMillis();
                            long rowCount = 0;
                            while (dbResult.nextRow()) {
                                if (monitor.isCanceled() || (hasLimits && rowCount >= maxRows)) {
                                    // Fetch not more than max rows
                                    break;
                                }
                                dataReceiver.fetchRow(session, dbResult);
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
                                dataReceiver.fetchEnd(session);
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
    public long countData(@NotNull DBCSession session, @Nullable DBDDataFilter dataFilter) throws DBCException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        StringBuilder query = new StringBuilder("SELECT COUNT(*) FROM "); //$NON-NLS-1$
        query.append(getFullQualifiedName());
        appendQueryConditions(query, null, dataFilter);
        monitor.subTask(CoreMessages.model_jdbc_fetch_table_row_count);
        DBCStatement dbStat = session.prepareStatement(
            DBCStatementType.QUERY,
            query.toString(),
            false, false, false);
        try {
            dbStat.setStatementSource(this);
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

    @NotNull
    @Override
    public ExecuteBatch insertData(@NotNull DBCSession session, @NotNull DBSAttributeBase[] attributes, @Nullable DBDDataReceiver keysReceiver)
        throws DBCException
    {
        readRequiredMeta(session.getProgressMonitor());

        // Make query
        StringBuilder query = new StringBuilder(200);
        query.append("INSERT INTO ").append(getFullQualifiedName()).append(" ("); //$NON-NLS-1$ //$NON-NLS-2$

        boolean hasKey = false;
        for (int i = 0; i < attributes.length; i++) {
            DBSAttributeBase attribute = attributes[i];
            if (attribute.isPseudoAttribute() || attribute.isAutoGenerated()) {
                continue;
            }
            if (hasKey) query.append(","); //$NON-NLS-1$
            hasKey = true;
            query.append(getAttributeName(attribute));
        }
        query.append(") VALUES ("); //$NON-NLS-1$
        hasKey = false;
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i].isPseudoAttribute() || attributes[i].isAutoGenerated()) {
                continue;
            }
            if (hasKey) query.append(","); //$NON-NLS-1$
            hasKey = true;
            query.append("?"); //$NON-NLS-1$
        }
        query.append(")"); //$NON-NLS-1$

        // Execute
        DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, keysReceiver != null);
        dbStat.setStatementSource(this);

        return new BatchImpl(dbStat, attributes, keysReceiver, true);
    }

    @NotNull
    @Override
    public ExecuteBatch updateData(
        @NotNull DBCSession session,
        @NotNull DBSAttributeBase[] updateAttributes,
        @NotNull DBSAttributeBase[] keyAttributes,
        @Nullable DBDDataReceiver keysReceiver)
        throws DBCException
    {
        readRequiredMeta(session.getProgressMonitor());
        String tableAlias = null;
        SQLDialect dialect = ((SQLDataSource) session.getDataSource()).getSQLDialect();
        if (dialect.supportsAliasInUpdate()) {
            tableAlias = DEFAULT_TABLE_ALIAS;
        }
        // Make query
        StringBuilder query = new StringBuilder();
        query.append("UPDATE ").append(getFullQualifiedName());
        if (tableAlias != null) {
            query.append(' ').append(tableAlias);
        }
        query.append(" SET "); //$NON-NLS-1$ //$NON-NLS-2$

        boolean hasKey = false;
        for (DBSAttributeBase attribute : updateAttributes) {
            if (hasKey) query.append(","); //$NON-NLS-1$
            hasKey = true;
            if (tableAlias != null) {
                query.append(tableAlias).append(dialect.getStructSeparator());
            }
            query.append(getAttributeName(attribute)).append("=?"); //$NON-NLS-1$
        }
        query.append(" WHERE "); //$NON-NLS-1$
        hasKey = false;
        for (DBSAttributeBase attribute : keyAttributes) {
            if (hasKey) query.append(" AND "); //$NON-NLS-1$
            hasKey = true;
            appendAttributeCriteria(tableAlias, dialect, query, attribute);
        }

        // Execute
        DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, keysReceiver != null);

        dbStat.setStatementSource(this);

        DBSAttributeBase[] attributes = ArrayUtils.concatArrays(updateAttributes, keyAttributes);

        return new BatchImpl(dbStat, attributes, keysReceiver, false);
    }

    @NotNull
    @Override
    public ExecuteBatch deleteData(@NotNull DBCSession session, @NotNull DBSAttributeBase[] keyAttributes)
        throws DBCException
    {
        readRequiredMeta(session.getProgressMonitor());

        String tableAlias = null;
        SQLDialect dialect = ((SQLDataSource) session.getDataSource()).getSQLDialect();
        if (dialect.supportsAliasInUpdate()) {
            tableAlias = DEFAULT_TABLE_ALIAS;
        }

        // Make query
        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ").append(getFullQualifiedName());
        if (tableAlias != null) {
            query.append(' ').append(tableAlias);
        }
        query.append(" WHERE "); //$NON-NLS-1$ //$NON-NLS-2$

        boolean hasKey = false;
        for (DBSAttributeBase attribute : keyAttributes) {
            if (hasKey) query.append(" AND "); //$NON-NLS-1$
            hasKey = true;
            appendAttributeCriteria(tableAlias, dialect, query, attribute);
        }

        // Execute
        DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, false);
        dbStat.setStatementSource(this);
        return new BatchImpl(dbStat, keyAttributes, null, false);
    }

    private String getAttributeName(@NotNull DBSAttributeBase attribute) {
        // Do not quote pseudo attribute name
        return attribute.isPseudoAttribute() ? attribute.getName() : DBUtils.getObjectFullName(getDataSource(), attribute);
    }

    private void appendQueryConditions(@NotNull StringBuilder query, @Nullable String tableAlias, @Nullable DBDDataFilter dataFilter)
    {
        if (dataFilter != null && dataFilter.hasConditions()) {
            query.append(" WHERE "); //$NON-NLS-1$
            SQLUtils.appendConditionString(dataFilter, getDataSource(), tableAlias, query, true);
        }
    }

    private void appendQueryOrder(@NotNull StringBuilder query, @Nullable String tableAlias, @Nullable DBDDataFilter dataFilter)
    {
        if (dataFilter != null) {
            // Construct ORDER BY
            if (dataFilter.hasOrdering()) {
                query.append(" ORDER BY "); //$NON-NLS-1$
                SQLUtils.appendOrderString(dataFilter, getDataSource(), tableAlias, query);
            }
        }
    }

    private void appendAttributeCriteria(@Nullable String tableAlias, SQLDialect dialect, StringBuilder query, DBSAttributeBase attribute) {
        DBDPseudoAttribute pseudoAttribute = null;
        if (attribute.isPseudoAttribute()) {
            if (attribute instanceof DBDAttributeBinding) {
                pseudoAttribute = ((DBDAttributeBinding) attribute).getMetaAttribute().getPseudoAttribute();
            } else if (attribute instanceof DBCAttributeMetaData) {
                pseudoAttribute = ((DBCAttributeMetaData)attribute).getPseudoAttribute();
            } else {
                log.error("Unsupported attribute argument: " + attribute);
            }
        }
        if (pseudoAttribute != null) {
            if (tableAlias == null) {
                tableAlias = this.getFullQualifiedName();
            }
            String criteria = pseudoAttribute.translateExpression(tableAlias);
            query.append(criteria);
        } else {
            if (tableAlias != null) {
                query.append(tableAlias).append(dialect.getStructSeparator());
            }
            query.append(getAttributeName(attribute));
        }
        query.append("=?"); //$NON-NLS-1$
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
            keysReceiver.fetchStart(session, dbResult);
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

    /**
     * Reads and caches metadata which is required for data requests
     * @param monitor progress monitor
     * @throws DBCException on error
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
        private final DBSAttributeBase[] attributes;
        private final List<Object[]> values = new ArrayList<Object[]>();
        private final DBDDataReceiver keysReceiver;
        private final boolean skipSequences;

        private BatchImpl(@NotNull DBCStatement statement, @NotNull DBSAttributeBase[] attributes, @Nullable DBDDataReceiver keysReceiver, boolean skipSequences)
        {
            this.statement = statement;
            this.attributes = attributes;
            this.keysReceiver = keysReceiver;
            this.skipSequences = skipSequences;
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
        public DBCStatistics execute() throws DBCException
        {
            if (statement == null) {
                throw new DBCException("Execute batch closed");
            }
            DBDValueHandler[] handlers = new DBDValueHandler[attributes.length];
            for (int i = 0; i < attributes.length; i++) {
                handlers[i] = DBUtils.findValueHandler(statement.getSession(), attributes[i]);
            }

            boolean useBatch = statement.getSession().getDataSource().getInfo().supportsBatchUpdates();
            if (values.size() <= 1) {
                useBatch = false;
            }

            DBCStatistics statistics = new DBCStatistics();
            statistics.setQueryText(statement.getQueryString());
            for (Object[] rowValues : values) {
                int paramIndex = 0;
                for (int k = 0; k < handlers.length; k++) {
                    DBDValueHandler handler = handlers[k];
                    DBSAttributeBase attribute = attributes[k];
                    if (skipSequences && (attribute.isPseudoAttribute() || attribute.isAutoGenerated())) {
                        continue;
                    }
                    handler.bindValueObject(statement.getSession(), statement, attribute, paramIndex++, rowValues[k]);
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
                        readKeys(statement.getSession(), statement, keysReceiver);
                    }
                }
            }
            values.clear();

            if (useBatch) {
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
