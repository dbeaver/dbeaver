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
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.data.ExecuteBatchImpl;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.struct.AbstractTable;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.ArrayUtils;

/**
 * JDBC abstract table implementation
 */
public abstract class JDBCTable<DATASOURCE extends DBPDataSource, CONTAINER extends DBSObjectContainer>
    extends AbstractTable<DATASOURCE, CONTAINER>
    implements DBSDataManipulator, DBPSaveableObject
{
    private static final Log log = Log.getLog(JDBCTable.class);
    public static final String DEFAULT_TABLE_ALIAS = "x";
    public static final int DEFAULT_READ_FETCH_SIZE = 10000;

    private boolean persisted;

    protected JDBCTable(CONTAINER container, boolean persisted)
    {
        super(container);
        this.persisted = persisted;
    }

    // Copy constructor
    protected JDBCTable(CONTAINER container, DBSEntity source, boolean persisted)
    {
        super(container, source);
        this.persisted = persisted;
    }

    protected JDBCTable(CONTAINER container, @Nullable String tableName, boolean persisted)
    {
        super(container, tableName);
        this.persisted = persisted;
    }

    public abstract JDBCStructCache<CONTAINER, ? extends DBSEntity, ? extends DBSEntityAttribute> getCache();

    @NotNull
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
        return DATA_COUNT | DATA_FILTER | DATA_SEARCH | DATA_INSERT | DATA_UPDATE | DATA_DELETE;
    }

    @NotNull
    @Override
    public DBCStatistics readData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, @NotNull DBDDataReceiver dataReceiver, @Nullable DBDDataFilter dataFilter, long firstRow, long maxRows, long flags)
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

        DBDPseudoAttribute rowIdAttribute = (flags & FLAG_READ_PSEUDO) != 0 ?
            DBUtils.getRowIdAttribute(this) : null;

        // Always use alias if we have criteria or ROWID.
        // Some criteria doesn't work without alias
        // (e.g. structured attributes in Oracle requires table alias)
        String tableAlias = null;
        if ((dataFilter != null && dataFilter.hasConditions()) || rowIdAttribute != null) {
            if (dataSource instanceof SQLDataSource) {
                if (((SQLDataSource) dataSource).getSQLDialect().supportsAliasInSelect()) {
                    tableAlias = DEFAULT_TABLE_ALIAS;
                }
            }
        }

        if (rowIdAttribute != null && tableAlias == null) {
            log.warn("Can't query ROWID - table alias not supported");
            rowIdAttribute = null;
        }

        StringBuilder query = new StringBuilder(100);
        query.append("SELECT ");
        appendSelectSource(monitor, query, tableAlias, rowIdAttribute);
        query.append(" FROM ").append(getFullyQualifiedName(DBPEvaluationContext.DML));
        if (tableAlias != null) {
            query.append(" ").append(tableAlias); //$NON-NLS-1$
        }
        appendQueryConditions(query, tableAlias, dataFilter);
        appendQueryOrder(query, tableAlias, dataFilter);

        String sqlQuery = query.toString();
        statistics.setQueryText(sqlQuery);
        statistics.addStatementsCount();

        monitor.subTask(ModelMessages.model_jdbc_fetch_table_data);

        try (DBCStatement dbStat = DBUtils.makeStatement(
            source,
            session,
            DBCStatementType.SCRIPT,
            sqlQuery,
            firstRow,
            maxRows))
        {
            if (monitor.isCanceled()) {
                return statistics;
            }
            if (dbStat instanceof JDBCStatement && maxRows > 0) {
                boolean useFetchSize = getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.RESULT_SET_USE_FETCH_SIZE);
                if (useFetchSize) {
                    try {
                        ((JDBCStatement) dbStat).setFetchSize(
                            firstRow < 0 || maxRows <= 0 ? DEFAULT_READ_FETCH_SIZE : (int) (firstRow + maxRows));
                    } catch (Exception e) {
                        log.warn(e);
                    }
                }
            }

            long startTime = System.currentTimeMillis();
            boolean executeResult = dbStat.executeStatement();
            statistics.setExecuteTime(System.currentTimeMillis() - startTime);
            if (executeResult) {
                DBCResultSet dbResult = dbStat.openResultSet();
                if (dbResult != null && !monitor.isCanceled()) {
                    try {
                        dataReceiver.fetchStart(session, dbResult, firstRow, maxRows);

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
                                monitor.subTask(rowCount + ModelMessages.model_jdbc__rows_fetched);
                                monitor.worked(100);
                            }
                        }
                        statistics.setFetchTime(System.currentTimeMillis() - startTime);
                        statistics.setRowsFetched(rowCount);
                    } finally {
                        // First - close cursor
                        try {
                            dbResult.close();
                        } catch (Throwable e) {
                            log.error("Error closing result set", e); //$NON-NLS-1$
                        }
                        // Then - signal that fetch was ended
                        try {
                            dataReceiver.fetchEnd(session, dbResult);
                        } catch (Throwable e) {
                            log.error("Error while finishing result set fetch", e); //$NON-NLS-1$
                        }
                    }
                }
            }
            return statistics;
        } finally {
            dataReceiver.close();
        }
    }

    protected void appendSelectSource(DBRProgressMonitor monitor, StringBuilder query, String tableAlias, DBDPseudoAttribute rowIdAttribute) {
        if (rowIdAttribute != null) {
            // If we have pseudo attributes then query gonna be more complex
            query.append(tableAlias).append(".*"); //$NON-NLS-1$
            query.append(",").append(rowIdAttribute.translateExpression(tableAlias));
            if (rowIdAttribute.getAlias() != null) {
                query.append(" as ").append(rowIdAttribute.getAlias());
            }
        } else {
            if (tableAlias != null) {
                query.append(tableAlias).append(".");
            }
            query.append("*"); //$NON-NLS-1$
        }
    }

    @Override
    public long countData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, @Nullable DBDDataFilter dataFilter) throws DBCException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        StringBuilder query = new StringBuilder("SELECT COUNT(*) FROM "); //$NON-NLS-1$
        query.append(getFullyQualifiedName(DBPEvaluationContext.DML));
        appendQueryConditions(query, null, dataFilter);
        monitor.subTask(ModelMessages.model_jdbc_fetch_table_row_count);
        try (DBCStatement dbStat = session.prepareStatement(
            DBCStatementType.QUERY,
            query.toString(),
            false, false, false))
        {
            dbStat.setStatementSource(source);
            if (!dbStat.executeStatement()) {
                return 0;
            }
            DBCResultSet dbResult = dbStat.openResultSet();
            if (dbResult == null) {
                return 0;
            }
            try {
                if (dbResult.nextRow()) {
                    Object result = dbResult.getAttributeValue(0);
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
            } finally {
                dbResult.close();
            }
        }
    }

    /**
     * Inserts data row.
     * Note: if column value is NULL then it will be skipped (to let default value to be applied)
     * If ALL columns are null then explicit NULL values will be used for all of them (to let INSERT to execute - it won't work with empty column list)
     */
    @NotNull
    @Override
    public ExecuteBatch insertData(@NotNull DBCSession session, @NotNull final DBSAttributeBase[] attributes, @Nullable DBDDataReceiver keysReceiver, @NotNull final DBCExecutionSource source)
        throws DBCException
    {
        readRequiredMeta(session.getProgressMonitor());

        return new ExecuteBatchImpl(attributes, keysReceiver, true) {

            private boolean allNulls;

            @NotNull
            @Override
            protected DBCStatement prepareStatement(@NotNull DBCSession session, Object[] attributeValues) throws DBCException {
                // Make query
                StringBuilder query = new StringBuilder(200);
                query
                    .append(useUpsert(session) ? "UPSERT" : "INSERT")
                    .append(" INTO ").append(getFullyQualifiedName(DBPEvaluationContext.DML)).append(" ("); //$NON-NLS-1$ //$NON-NLS-2$

                allNulls = true;
                for (int i = 0; i < attributes.length; i++) {
                    if (!DBUtils.isNullValue(attributeValues[i])) {
                        allNulls = false;
                        break;
                    }
                }
                boolean hasKey = false;
                for (int i = 0; i < attributes.length; i++) {
                    DBSAttributeBase attribute = attributes[i];
                    if (DBUtils.isPseudoAttribute(attribute) || (!allNulls && DBUtils.isNullValue(attributeValues[i]))) {
                        continue;
                    }
                    if (hasKey) query.append(","); //$NON-NLS-1$
                    hasKey = true;
                    query.append(getAttributeName(attribute));
                }
                query.append(")\nVALUES ("); //$NON-NLS-1$
                hasKey = false;
                for (int i = 0; i < attributes.length; i++) {
                    DBSAttributeBase attribute = attributes[i];
                    if (DBUtils.isPseudoAttribute(attribute) || (!allNulls && DBUtils.isNullValue(attributeValues[i]))) {
                        continue;
                    }
                    if (hasKey) query.append(","); //$NON-NLS-1$
                    hasKey = true;
                    query.append("?"); //$NON-NLS-1$
                }
                query.append(")"); //$NON-NLS-1$

                // Execute
                DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, keysReceiver != null);
                dbStat.setStatementSource(source);
                return dbStat;
            }

            @Override
            protected void bindStatement(@NotNull DBDValueHandler[] handlers, @NotNull DBCStatement statement, Object[] attributeValues) throws DBCException {
                int paramIndex = 0;
                for (int k = 0; k < handlers.length; k++) {
                    DBSAttributeBase attribute = attributes[k];
                    if (DBUtils.isPseudoAttribute(attribute) || (!allNulls && DBUtils.isNullValue(attributeValues[k]))) {
                        continue;
                    }
                    handlers[k].bindValueObject(statement.getSession(), statement, attribute, paramIndex++, attributeValues[k]);
                }
            }
        };
    }

    @NotNull
    @Override
    public ExecuteBatch updateData(
        @NotNull DBCSession session,
        @NotNull final DBSAttributeBase[] updateAttributes,
        @NotNull final DBSAttributeBase[] keyAttributes,
        @Nullable DBDDataReceiver keysReceiver, @NotNull final DBCExecutionSource source)
        throws DBCException
    {
        if (useUpsert(session)) {
            return insertData(
                session,
                ArrayUtils.concatArrays(updateAttributes, keyAttributes),
                keysReceiver,
                source);
        }
        readRequiredMeta(session.getProgressMonitor());

        DBSAttributeBase[] attributes = ArrayUtils.concatArrays(updateAttributes, keyAttributes);

        return new ExecuteBatchImpl(attributes, keysReceiver, false) {
            @NotNull
            @Override
            protected DBCStatement prepareStatement(@NotNull DBCSession session, Object[] attributeValues) throws DBCException {
                String tableAlias = null;
                SQLDialect dialect = ((SQLDataSource) session.getDataSource()).getSQLDialect();
                if (dialect.supportsAliasInUpdate()) {
                    tableAlias = DEFAULT_TABLE_ALIAS;
                }
                // Make query
                StringBuilder query = new StringBuilder();
                query.append("UPDATE ").append(getFullyQualifiedName(DBPEvaluationContext.DML));
                if (tableAlias != null) {
                    query.append(' ').append(tableAlias);
                }
                query.append("\nSET "); //$NON-NLS-1$ //$NON-NLS-2$

                boolean hasKey = false;
                for (DBSAttributeBase attribute : updateAttributes) {
                    if (hasKey) query.append(","); //$NON-NLS-1$
                    hasKey = true;
                    if (tableAlias != null) {
                        query.append(tableAlias).append(dialect.getStructSeparator());
                    }
                    query.append(getAttributeName(attribute)).append("=?"); //$NON-NLS-1$
                }
                if (keyAttributes.length > 0) {
                    query.append("\nWHERE "); //$NON-NLS-1$
                    hasKey = false;
                    for (int i = 0; i < keyAttributes.length; i++) {
                        DBSAttributeBase attribute = keyAttributes[i];
                        if (hasKey) query.append(" AND "); //$NON-NLS-1$
                        hasKey = true;
                        appendAttributeCriteria(tableAlias, dialect, query, attribute, attributeValues[updateAttributes.length + i]);
                    }
                }

                // Execute
                DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, keysReceiver != null);

                dbStat.setStatementSource(source);
                return dbStat;
            }

            @Override
            protected void bindStatement(@NotNull DBDValueHandler[] handlers, @NotNull DBCStatement statement, Object[] attributeValues) throws DBCException {
                int paramIndex = 0;
                for (int k = 0; k < handlers.length; k++) {
                    DBSAttributeBase attribute = attributes[k];
                    if (k >= updateAttributes.length && DBUtils.isNullValue(attributeValues[k])) {
                        // Skip NULL criteria binding
                        continue;
                    }
                    handlers[k].bindValueObject(statement.getSession(), statement, attribute, paramIndex++, attributeValues[k]);
                }
            }
        };
    }

    @NotNull
    @Override
    public ExecuteBatch deleteData(@NotNull DBCSession session, @NotNull final DBSAttributeBase[] keyAttributes, @NotNull final DBCExecutionSource source)
        throws DBCException
    {
        readRequiredMeta(session.getProgressMonitor());

        return new ExecuteBatchImpl(keyAttributes, null, false) {
            @NotNull
            @Override
            protected DBCStatement prepareStatement(@NotNull DBCSession session, Object[] attributeValues) throws DBCException {
                String tableAlias = null;
                SQLDialect dialect = ((SQLDataSource) session.getDataSource()).getSQLDialect();
                if (dialect.supportsAliasInUpdate()) {
                    tableAlias = DEFAULT_TABLE_ALIAS;
                }

                // Make query
                StringBuilder query = new StringBuilder();
                query.append("DELETE FROM ").append(getFullyQualifiedName(DBPEvaluationContext.DML));
                if (tableAlias != null) {
                    query.append(' ').append(tableAlias);
                }
                if (keyAttributes.length > 0) {
                    query.append("\nWHERE "); //$NON-NLS-1$ //$NON-NLS-2$
                    boolean hasKey = false;
                    for (int i = 0; i < keyAttributes.length; i++) {
                        if (hasKey) query.append(" AND "); //$NON-NLS-1$
                        hasKey = true;
                        appendAttributeCriteria(tableAlias, dialect, query, keyAttributes[i], attributeValues[i]);
                    }
                }

                // Execute
                DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, false);
                dbStat.setStatementSource(source);
                return dbStat;
            }

            @Override
            protected void bindStatement(@NotNull DBDValueHandler[] handlers, @NotNull DBCStatement statement, Object[] attributeValues) throws DBCException {
                int paramIndex = 0;
                for (int k = 0; k < handlers.length; k++) {
                    DBSAttributeBase attribute = attributes[k];
                    if (DBUtils.isNullValue(attributeValues[k])) {
                        // Skip NULL criteria binding
                        continue;
                    }
                    handlers[k].bindValueObject(statement.getSession(), statement, attribute, paramIndex++, attributeValues[k]);
                }
            }
        };
    }

    private boolean useUpsert(@NotNull DBCSession session) {
        SQLDialect dialect = session.getDataSource() instanceof SQLDataSource ?
            ((SQLDataSource) session.getDataSource()).getSQLDialect() : null;
        return dialect instanceof JDBCSQLDialect && ((JDBCSQLDialect) dialect).supportsUpsertStatement();
    }

    private String getAttributeName(@NotNull DBSAttributeBase attribute) {
        // Entity attribute obtain commented because it broke complex attributes full name construction
        // We can't use entity attr because only particular query metadata contains real structure
//        if (attribute instanceof DBDAttributeBinding) {
//            DBSEntityAttribute entityAttribute = ((DBDAttributeBinding) attribute).getEntityAttribute();
//            if (entityAttribute != null) {
//                attribute = entityAttribute;
//            }
//        }
        // Do not quote pseudo attribute name
        return DBUtils.isPseudoAttribute(attribute) ? attribute.getName() : DBUtils.getObjectFullName(getDataSource(), attribute, DBPEvaluationContext.DML);
    }

    private void appendQueryConditions(@NotNull StringBuilder query, @Nullable String tableAlias, @Nullable DBDDataFilter dataFilter)
    {
        if (dataFilter != null && dataFilter.hasConditions()) {
            query.append("\nWHERE "); //$NON-NLS-1$
            SQLUtils.appendConditionString(dataFilter, getDataSource(), tableAlias, query, true);
        }
    }

    private void appendQueryOrder(@NotNull StringBuilder query, @Nullable String tableAlias, @Nullable DBDDataFilter dataFilter)
    {
        if (dataFilter != null) {
            // Construct ORDER BY
            if (dataFilter.hasOrdering()) {
                query.append("\nORDER BY "); //$NON-NLS-1$
                SQLUtils.appendOrderString(dataFilter, getDataSource(), tableAlias, query);
            }
        }
    }

    private void appendAttributeCriteria(@Nullable String tableAlias, SQLDialect dialect, StringBuilder query, DBSAttributeBase attribute, Object value) {
        DBDPseudoAttribute pseudoAttribute = null;
        if (DBUtils.isPseudoAttribute(attribute)) {
            if (attribute instanceof DBDAttributeBindingMeta) {
                pseudoAttribute = ((DBDAttributeBindingMeta) attribute).getPseudoAttribute();
            } else {
                log.error("Unsupported attribute argument: " + attribute);
            }
        }
        if (pseudoAttribute != null) {
            if (tableAlias == null) {
                tableAlias = this.getFullyQualifiedName(DBPEvaluationContext.DML);
            }
            String criteria = pseudoAttribute.translateExpression(tableAlias);
            query.append(criteria);
        } else {
            if (tableAlias != null) {
                query.append(tableAlias).append(dialect.getStructSeparator());
            }
            query.append(getAttributeName(attribute));
        }
        if (DBUtils.isNullValue(value)) {
            query.append(" IS NULL"); //$NON-NLS-1$
        } else {
            query.append("=?"); //$NON-NLS-1$
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
            throw new DBCException("Can't cache table columns", e);
        }
    }

}
