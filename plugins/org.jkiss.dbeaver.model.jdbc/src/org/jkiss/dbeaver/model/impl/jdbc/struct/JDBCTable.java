/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.data.ExecuteBatchImpl;
import org.jkiss.dbeaver.model.impl.data.ExecuteBatchWithMultipleInsert;
import org.jkiss.dbeaver.model.impl.data.ExecuteInsertBatchImpl;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStringValueHandler;
import org.jkiss.dbeaver.model.impl.struct.AbstractTable;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLExpressionFormatter;
import org.jkiss.dbeaver.model.sql.SQLQueryGeneratorUpdate;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JDBC abstract table implementation
 */
public abstract class JDBCTable<DATASOURCE extends DBPDataSource, CONTAINER extends DBSObject>
    extends AbstractTable<DATASOURCE, CONTAINER>
    implements DBSDictionary, DBSDataManipulator, DBPSaveableObject, SQLQueryGeneratorUpdate
{
    private static final Log log = Log.getLog(JDBCTable.class);

    private static final String DEFAULT_TABLE_ALIAS = "x";

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
    public String[] getSupportedFeatures()
    {
        if (isTruncateSupported()) {
            return new String[] {FEATURE_DATA_COUNT, FEATURE_DATA_FILTER, FEATURE_DATA_SEARCH, FEATURE_DATA_INSERT, FEATURE_DATA_UPDATE, FEATURE_DATA_DELETE, FEATURE_DATA_TRUNCATE};
        } else {
            return new String[] {FEATURE_DATA_COUNT, FEATURE_DATA_FILTER, FEATURE_DATA_SEARCH, FEATURE_DATA_INSERT, FEATURE_DATA_UPDATE, FEATURE_DATA_DELETE};
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Select

    @NotNull
    @Override
    public DBCStatistics readData(
        @Nullable DBCExecutionSource source,
        @NotNull DBCSession session,
        @NotNull DBDDataReceiver dataReceiver,
        @Nullable DBDDataFilter dataFilter,
        long firstRow,
        long maxRows,
        long flags,
        int fetchSize
    ) throws DBCException {
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

        // Always use alias if we have data filter or ROWID.
        // Some criteria doesn't work without alias
        // (e.g. structured attributes in Oracle or composite types in PostgreSQL requires table alias)
        String tableAlias = null;
        if (needAliasInSelect(dataFilter, rowIdAttribute, dataSource)) {
            tableAlias = DEFAULT_TABLE_ALIAS;
        }

        if (rowIdAttribute != null && tableAlias == null) {
            log.warn("Can't query ROWID - table alias not supported");
            rowIdAttribute = null;
        }

        StringBuilder query = new StringBuilder(100);
        query.append("SELECT ");
        appendSelectSource(monitor, query, tableAlias, rowIdAttribute);
        query.append(" FROM ").append(getTableName());
        if (tableAlias != null) {
            if (dataSource.getSQLDialect().supportsAsKeywordBeforeAliasInFromClause()) {
                query.append(" AS");
            }
            query.append(" ").append(tableAlias); //$NON-NLS-1$
        }
        appendExtraSelectParameters(query);
        SQLUtils.appendQueryConditions(dataSource, query, tableAlias, dataFilter);
        SQLUtils.appendQueryOrder(dataSource, query, tableAlias, dataFilter);

        String sqlQuery = query.toString();
        statistics.setQueryText(sqlQuery);

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
            if (dbStat instanceof JDBCStatement && (fetchSize > 0 || maxRows > 0)) {
                DBExecUtils.setStatementFetchSize(dbStat, firstRow, maxRows, fetchSize);
            }

            long startTime = System.currentTimeMillis();
            boolean executeResult = dbStat.executeStatement();
            statistics.setExecuteTime(System.currentTimeMillis() - startTime);
            if (executeResult) {
                DBCResultSet dbResult = dbStat.openResultSet();
                if (dbResult != null && !monitor.isCanceled()) {
                    try {
                        dataReceiver.fetchStart(session, dbResult, firstRow, maxRows);

                        DBFetchProgress fetchProgress = new DBFetchProgress(session.getProgressMonitor());
                        while (dbResult.nextRow()) {
                            if (fetchProgress.isCanceled() || (hasLimits && fetchProgress.isMaxRowsFetched(maxRows))) {
                                // Fetch not more than max rows
                                break;
                            }
                            dataReceiver.fetchRow(session, dbResult);
                            fetchProgress.monitorRowFetch();
                        }
                        fetchProgress.dumpStatistics(statistics);
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

    @NotNull
    protected String getTableName() {
        return getFullyQualifiedName(DBPEvaluationContext.DML);
    }

    protected void appendSelectSource(DBRProgressMonitor monitor, StringBuilder query, String tableAlias, DBDPseudoAttribute rowIdAttribute) throws DBCException {
        String asteriskString = getDataSource().getSQLDialect().getAllAttributesAlias();
        if (asteriskString == null) {
            // Append all table attributes
            List<? extends DBSEntityAttribute> attributes;
            try {
                attributes = CommonUtils.safeList(getAttributes(monitor));
            } catch (Exception e) {
                throw new DBCException("Error getting table attributes", e);
            }
            for (int i = 0; i < attributes.size(); i++) {
                if (i > 0) {
                    query.append(", "); //$NON-NLS-1$
                }
                if (tableAlias != null) {
                    query.append(tableAlias).append("."); //$NON-NLS-1$
                }
                query.append(attributes.get(i).getName());
            }
        } else if (rowIdAttribute != null) {
            // If we have pseudo attributes then query gonna be more complex
            query.append(tableAlias).append(".").append(asteriskString); //$NON-NLS-1$
            query.append(",").append(rowIdAttribute.translateExpression(tableAlias));
            if (rowIdAttribute.getAlias() != null) {
                query.append(" as ").append(rowIdAttribute.getAlias());
            }
        } else {
            if (tableAlias != null) {
                query.append(tableAlias).append(".");
            }
            query.append(asteriskString);
        }
    }

    protected boolean needAliasInSelect(
        @Nullable DBDDataFilter dataFilter,
        @Nullable DBDPseudoAttribute rowIdAttribute,
        @NotNull DBPDataSource dataSource
    ) {
        return (dataFilter != null || rowIdAttribute != null) && dataSource.getSQLDialect().supportsAliasInSelect();
    }

    protected void appendExtraSelectParameters(@NotNull StringBuilder query) {

    }

    ////////////////////////////////////////////////////////////////////
    // Count

    @Override
    public long countData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, @Nullable DBDDataFilter dataFilter, long flags) throws DBCException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();
        String asteriskString = getDataSource().getSQLDialect().getDefaultGroupAttribute();
        if (asteriskString == null) {
            asteriskString = "";
        }

        StringBuilder query = new StringBuilder();
        query.append("SELECT COUNT(").append(asteriskString).append(") FROM "); //$NON-NLS-1$
        query.append(getTableName());
        SQLUtils.appendQueryConditions(getDataSource(), query, null, dataFilter);
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

    ////////////////////////////////////////////////////////////////////
    // Insert

    /**
     * Inserts data row.
     * Note: if column value is NULL then it will be skipped (to let default value to be applied)
     * If ALL columns are null then explicit NULL values will be used for all of them (to let INSERT to execute - it won't work with empty column list)
     */
    @NotNull
    @Override
    public ExecuteBatch insertData(@NotNull DBCSession session, @NotNull final DBSAttributeBase[] attributes, @Nullable DBDDataReceiver keysReceiver, @NotNull final DBCExecutionSource source, @NotNull Map<String, Object> options)
        throws DBCException
    {
        readRequiredMeta(session.getProgressMonitor());

        boolean multiRowInsertSupported = getDataSource().getSQLDialect().getDefaultMultiValueInsertMode() == SQLDialect.MultiValueInsertMode.GROUP_ROWS;
        if (CommonUtils.toBoolean(options.get(DBSDataManipulator.OPTION_USE_MULTI_INSERT)) && multiRowInsertSupported) {
            return new ExecuteBatchWithMultipleInsert(attributes, keysReceiver, true, session, source, JDBCTable.this);
        }

        return new ExecuteInsertBatchImpl(attributes, keysReceiver, true, session, source, JDBCTable.this, useUpsert(session));
    }

    ////////////////////////////////////////////////////////////////////
    // Update

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
                source,
                Collections.emptyMap());
        }
        readRequiredMeta(session.getProgressMonitor());

        DBSAttributeBase[] attributes = ArrayUtils.concatArrays(updateAttributes, keyAttributes);

        return new ExecuteBatchImpl(attributes, keysReceiver, false) {
            @NotNull
            @Override
            protected DBCStatement prepareStatement(@NotNull DBCSession session, DBDValueHandler[] handlers, Object[] attributeValues, Map<String, Object> options) throws DBCException {
                String tableAlias = null;
                SQLDialect dialect = session.getDataSource().getSQLDialect();
                if (dialect.supportsAliasInUpdate()) {
                    tableAlias = DEFAULT_TABLE_ALIAS;
                }
                // Make query
                StringBuilder query = new StringBuilder();
                String tableName = DBUtils.getEntityScriptName(JDBCTable.this, options);
                query.append(generateTableUpdateBegin(tableName));
                if (tableAlias != null) {
                    query.append(' ').append(tableAlias);
                }
                String updateSet = generateTableUpdateSet();
                if (!CommonUtils.isEmpty(updateSet)) {
                    query.append("\n\t").append(updateSet); //$NON-NLS-1$ //$NON-NLS-2$
                }

                boolean hasKey = false;
                for (int i = 0; i < updateAttributes.length; i++) {
                    DBSAttributeBase attribute = updateAttributes[i];
                    if (hasKey) query.append(","); //$NON-NLS-1$
                    hasKey = true;
                    if (tableAlias != null) {
                        query.append(tableAlias).append(dialect.getStructSeparator());
                    }
                    query.append(DBStructUtils.getAttributeName(attribute, DBPAttributeReferencePurpose.UPDATE_TARGET)).append("="); //$NON-NLS-1$
                    DBDValueHandler valueHandler = handlers[i];
                    if (valueHandler instanceof DBDValueBinder) {
                        query.append(((DBDValueBinder) valueHandler).makeQueryBind(attribute, attributeValues[i]));
                    } else {
                        query.append("?"); //$NON-NLS-1$
                    }
                }
                if (keyAttributes.length > 0) {
                    query.append("\n\tWHERE "); //$NON-NLS-1$
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

    ////////////////////////////////////////////////////////////////////
    // Delete

    @NotNull
    @Override
    public ExecuteBatch deleteData(@NotNull DBCSession session, @NotNull final DBSAttributeBase[] keyAttributes, @NotNull final DBCExecutionSource source)
        throws DBCException
    {
        readRequiredMeta(session.getProgressMonitor());

        return new ExecuteBatchImpl(keyAttributes, null, false) {
            @NotNull
            @Override
            protected DBCStatement prepareStatement(@NotNull DBCSession session, DBDValueHandler[] handlers, Object[] attributeValues, Map<String, Object> options) throws DBCException {
                String tableAlias = null;
                SQLDialect dialect = session.getDataSource().getSQLDialect();
                if (dialect.supportsAliasInUpdate()) {
                    tableAlias = DEFAULT_TABLE_ALIAS;
                }

                // Make query
                StringBuilder query = new StringBuilder();
                String tableName = DBUtils.getEntityScriptName(JDBCTable.this, options);
                query.append(generateTableDeleteFrom(tableName));
                if (tableAlias != null) {
                    query.append(' ').append(tableAlias);
                }
                if (keyAttributes.length > 0) {
                    query.append("\n\tWHERE "); //$NON-NLS-1$ //$NON-NLS-2$
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

    ////////////////////////////////////////////////////////////////////
    // Dictionary

    /**
     * Enumerations supported only for unique constraints
     * @return true for unique constraint else otherwise
     */
    @Override
    public boolean supportsDictionaryEnumeration() {
        return true;
    }

    /**
     * Returns prepared statements for enumeration fetch
     *
     * @param monitor               execution context
     * @param keyColumn             enumeration column.
     * @param keyPattern            pattern for enumeration values. If null or empty then returns full enumration set
     * @param searchText
     * @param preceedingKeys        other constrain key values. May be null.
     * @param caseInsensitiveSearch use case-insensitive search for {@code keyPattern}
     * @param sortAsc               sort ascending/descending
     * @param sortByValue           sort results by eky value. If false then sort by description
     * @param offset                enumeration values offset in result set
     * @param maxResults            maximum enumeration values in result set
     */
    @NotNull
    @Override
    public List<DBDLabelValuePair> getDictionaryEnumeration(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntityAttribute keyColumn,
        @Nullable Object keyPattern,
        @Nullable String searchText,
        @Nullable List<DBDAttributeValue> preceedingKeys,
        boolean caseInsensitiveSearch,
        boolean sortAsc,
        boolean sortByValue,
        int offset,
        int maxResults
    ) throws DBException {
        return readKeyEnumeration(
            monitor,
            keyColumn,
            keyPattern,
            searchText,
            preceedingKeys,
            sortByValue,
            sortAsc,
            caseInsensitiveSearch,
            maxResults,
            offset
        );
    }

    @NotNull
    @Override
    public List<DBDLabelValuePair> getDictionaryValues(
        @NotNull DBRProgressMonitor monitor,
        @NotNull List<DBSEntityAttribute> keyColumns,
        @NotNull List<Object[]> keyValues,
        @Nullable List<DBDAttributeValue[]> preceedingKeys,
        boolean sortByValue,
        boolean sortAsc,
        boolean omitNonDescriptive) throws DBException
    {
        if (keyColumns.isEmpty()) {
            throw new DBException("Empty key columns");
        }
        String descColumns = DBVUtils.getDictionaryDescriptionColumns(monitor, keyColumns.get(0));
        if (omitNonDescriptive && (descColumns == null || descColumns.equals(DBUtils.getQuotedIdentifier(keyColumns.get(0))))) {
            return Collections.emptyList();
        }

        List<DBDValueHandler> keyValueHandler = keyColumns.stream()
            .map(c -> DBUtils.findValueHandler(c.getDataSource(), c)).toList();

        StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        for (int i = 0; i < keyColumns.size(); i++) {
            DBSEntityAttribute keyColumn = keyColumns.get(i);
            if (i > 0) query.append(",");
            query.append(DBUtils.getQuotedIdentifier(keyColumn));
        }

        if (descColumns != null) {
            query.append(", ").append(descColumns);
        }
        query.append(" FROM ").append(DBUtils.getObjectFullName(this, DBPEvaluationContext.DML)).append(" WHERE ");
        boolean hasCond = false;
        // Leading keys
        if (preceedingKeys != null && !preceedingKeys.isEmpty()) {
            for (DBDAttributeValue[] pk : preceedingKeys) {
                for (DBDAttributeValue pkColumn : pk) {
                    if (hasCond) query.append(" AND ");
                    hasCond = true;
                    query.append(DBUtils.getQuotedIdentifier(getDataSource(), pkColumn.getAttribute().getName())).append(" = ?");
                }
            }
        }
        if (hasCond) query.append(" AND ");
        if (keyColumns.size() == 1) {
            // For single column key use IN
            query.append(DBUtils.getQuotedIdentifier(keyColumns.get(0))).append(" IN (");
            for (int i = 0; i < keyValues.size(); i++) {
                if (i > 0) query.append(",");
                query.append("?");
            }
            query.append(")");
        } else {
            // For multi-column keys use series of OR
            query.append("(");
            for (int i = 0; i < keyValues.size(); i++) {
                if (i > 0) query.append(" OR ");
                query.append("(");
                for (int j = 0; j < keyColumns.size(); j++) {
                    if (j > 0) query.append(" AND ");
                    DBSEntityAttribute keyAttr = keyColumns.get(j);
                    query.append(DBUtils.getQuotedIdentifier(keyAttr)).append("=?");
                }
                query.append(")");
            }
            query.append(")");
        }

        query.append(" ORDER BY ");
        if (sortByValue) {
            for (int j = 0; j < keyColumns.size(); j++) {
                if (j > 0) query.append(",");
                DBSEntityAttribute keyAttr = keyColumns.get(j);
                query.append(DBUtils.getQuotedIdentifier(keyAttr));
            }
        } else {
            // Sort by description
            query.append(descColumns);
        }
        if (!sortAsc) {
            query.append(" DESC");
        }

        try (JDBCSession session = DBUtils.openUtilSession(monitor, this, "Load dictionary values")) {
            try (DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, false)) {
                int paramPos = 0;
                if (preceedingKeys != null && !preceedingKeys.isEmpty()) {
                    for (DBDAttributeValue[] precAttribute : preceedingKeys) {
                        for (DBDAttributeValue precAttrValue : precAttribute) {
                            DBDValueHandler precValueHandler = DBUtils.findValueHandler(session, precAttrValue.getAttribute());
                            precValueHandler.bindValueObject(session, dbStat, precAttrValue.getAttribute(), paramPos++, precAttrValue.getValue());
                        }
                    }
                }
                for (Object[] keyValue : keyValues) {
                    for (int i = 0; i < keyValue.length; i++) {
                        Object cellValue = keyValue[i];
                        keyValueHandler.get(i).bindValueObject(session, dbStat, keyColumns.get(i), paramPos++, cellValue);
                    }
                }
                dbStat.setLimit(0, keyValues.size());
                if (dbStat.executeStatement()) {
                    try (DBCResultSet dbResult = dbStat.openResultSet()) {
                        return DBVUtils.readDictionaryRows(session, keyColumns, keyValueHandler, dbResult, true, false);
                    }
                } else {
                    return Collections.emptyList();
                }
            }
        }
    }

    private List<DBDLabelValuePair> readKeyEnumeration(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntityAttribute keyColumn,
        @Nullable Object keyValue,
        @Nullable String searchText,
        @Nullable List<DBDAttributeValue> preceedingKeys,
        boolean sortByValue,
        boolean sortAsc,
        boolean caseInsensitiveSearch,
        int maxResults,
        int offset)
        throws DBException
    {
        if (keyColumn.getParentObject() != this) {
            throw new IllegalArgumentException("Bad key column argument");
        }

        DBDValueHandler keyValueHandler = DBUtils.findValueHandler(keyColumn.getDataSource(), keyColumn);

        boolean searchInKeys = keyValue != null;

        if (keyValue != null) {
            if (keyColumn.getDataKind() == DBPDataKind.NUMERIC) {
                if (keyValue instanceof Number && maxResults > 0) {
                    int gapSize;
                    if (maxResults == 1) {
                        if (offset == 0) {
                            gapSize = 0;
                        } else {
                            gapSize = offset >= 0 ? -1 : 1;
                        }
                    } else {
                        gapSize = Math.max(Math.round((float) maxResults / 2), 1) - offset;
                    }
                    boolean allowNegative = ((Number) keyValue).longValue() < 0;
                    if (keyValue instanceof Integer) {
                        int intValue = (Integer) keyValue;
                        keyValue = allowNegative || intValue > gapSize ? intValue - gapSize : 0;
                    } else if (keyValue instanceof Short) {
                        int shortValue = (Short) keyValue;
                        keyValue = allowNegative || shortValue > gapSize ? shortValue - gapSize : (short)0;
                    } else if (keyValue instanceof Long) {
                        long longValue = (Long) keyValue;
                        keyValue = allowNegative || longValue > gapSize ? longValue - gapSize : (long)0;
                    } else if (keyValue instanceof Float) {
                        float floatValue = (Float) keyValue;
                        keyValue = allowNegative || floatValue > gapSize ? floatValue - gapSize : 0.0f;
                    } else if (keyValue instanceof Double) {
                        double doubleValue = (Double) keyValue;
                        keyValue = allowNegative || doubleValue > gapSize ? doubleValue - gapSize : 0.0;
                    } else if (keyValue instanceof BigInteger biValue) {
                        keyValue = allowNegative || biValue.longValue() > gapSize ? biValue.subtract(BigInteger.valueOf(gapSize)) : new BigInteger("0");
                    } else if (keyValue instanceof BigDecimal bdValue) {
                        keyValue = allowNegative || bdValue.longValue() > gapSize ? bdValue.subtract(new BigDecimal(gapSize)) : new BigDecimal(0);
                    } else {
                        searchInKeys = false;
                    }
                } else if (keyValue instanceof String) {
                    if (((String) keyValue).isEmpty() || !Character.isDigit(((String)keyValue).charAt(0)) ) {
                        searchInKeys = false;
                    }
                    // Ignore it
                    //keyPattern = Double.parseDouble((String) keyPattern);
                }
            } else if (keyValue instanceof CharSequence) {
                // Its ok
            } else {
                searchInKeys = false;
            }
        }

        StringBuilder query = new StringBuilder();
        query.append("SELECT ").append(DBUtils.getQuotedIdentifier(keyColumn, DBPAttributeReferencePurpose.DATA_SELECTION));

        String descColumns = DBVUtils.getDictionaryDescriptionColumns(monitor, keyColumn);
        Collection<DBSEntityAttribute> descAttributes = null;
        if (descColumns != null) {
            descAttributes = DBVEntity.getDescriptionColumns(monitor, this, descColumns);
            if (DBUtils.findObject(descAttributes, keyColumn.getName(), true) != null) {
                // Add alias for value column to avoid ambiguity
                query.append(" dbvrvalue");
            }
            query.append(", ").append(descColumns);
        }
        query.append(" FROM ").append(DBUtils.getObjectFullName(this, DBPEvaluationContext.DML));

        boolean searchInDesc = searchText != null && descAttributes != null;
        if (searchInDesc) {
            boolean hasStringAttrs = false;
            for (DBSEntityAttribute descAttr : descAttributes) {
                if (descAttr.getDataKind() == DBPDataKind.STRING) {
                    hasStringAttrs = true;
                    break;
                }
            }
            if (!hasStringAttrs) {
                searchInDesc = false;
            }
        }

        if (!CommonUtils.isEmpty(preceedingKeys) || keyValue != null || searchInDesc) {
            query.append(" WHERE ");
        }
        boolean hasCond = false;
        // Preceeding keys
        if (preceedingKeys != null && !preceedingKeys.isEmpty()) {
            for (int i = 0; i < preceedingKeys.size(); i++) {
                if (hasCond) query.append(" AND ");
                query.append(DBUtils.getQuotedIdentifier(getDataSource(), preceedingKeys.get(i).getAttribute().getName())).append(" = ?");
                hasCond = true;
            }
        }
        final SQLDialect dialect = getDataSource().getSQLDialect();
        final SQLExpressionFormatter caseInsensitiveFormatter = caseInsensitiveSearch
            ? dialect.getCaseInsensitiveExpressionFormatter(DBCLogicalOperator.LIKE)
            : null;
        if (keyValue != null) {
            if (hasCond) query.append(" AND (");
            {
                final String identifier = DBUtils.getQuotedIdentifier(keyColumn);
                if (keyColumn.getDataKind() == DBPDataKind.STRING) {
                    if (caseInsensitiveSearch && caseInsensitiveFormatter != null) {
                        query.append(caseInsensitiveFormatter.format(identifier, "?"));
                    } else if (searchInKeys) {
                        query.append(identifier).append(" LIKE ?");
                    } else {
                        query.append(identifier).append(" = ?");
                    }
                } else if (keyColumn.getDataKind() == DBPDataKind.NUMERIC && searchInKeys) {
                    query.append(identifier).append(" >= ?");
                } else {
                    query.append(identifier).append(" = ?");
                }
            }
        }
        // Add desc columns conditions
        if (searchInDesc) {
            boolean hasCondition = false;
            if (keyValue != null) {
                query.append(" AND (");
            }
            for (DBSEntityAttribute descAttr : descAttributes) {
                if (descAttr.getDataKind() == DBPDataKind.STRING) {
                    final String identifier = DBUtils.getQuotedIdentifier(descAttr);
                    if (hasCondition) {
                        query.append(" OR ");
                    }
                    if (caseInsensitiveSearch && caseInsensitiveFormatter != null) {
                        query.append(caseInsensitiveFormatter.format(identifier, "?"));
                    } else {
                        query.append(identifier).append(" LIKE ?");
                    }
                    hasCondition = true;
                }
            }
            if (keyValue != null) {
                query.append(")");
            }
        }
        if (hasCond) query.append(")");
        if (searchInKeys || searchInDesc) {
            query.append(" ORDER BY ");
            if (sortByValue) {
                query.append(DBUtils.getQuotedIdentifier(keyColumn));
            } else {
                // Sort by description
                query.append(descColumns);
            }
            if (!sortAsc) {
                query.append(" DESC");
            }
        }

        try (JDBCSession session = DBUtils.openUtilSession(monitor, this, "Load attribute value enumeration")) {
            try (DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, false)) {
                int paramPos = 0;

                if (preceedingKeys != null && !preceedingKeys.isEmpty()) {
                    for (DBDAttributeValue precAttribute : preceedingKeys) {
                        DBDValueHandler precValueHandler = DBUtils.findValueHandler(session, precAttribute.getAttribute());
                        precValueHandler.bindValueObject(session, dbStat, precAttribute.getAttribute(), paramPos++, precAttribute.getValue());
                    }
                }

                if (keyValue != null) {
                    Object keyValueToSet;
                    if (searchInKeys && keyColumn.getDataKind() == DBPDataKind.STRING) {
                        keyValueToSet = "%" + keyValue + "%";
                    } else {
                        keyValueToSet = keyValue;
                    }
                    keyValueHandler.bindValueObject(session, dbStat, keyColumn, paramPos++, keyValueToSet);
                }

                if (searchInDesc) {
                    for (DBSEntityAttribute descAttr : descAttributes) {
                        if (descAttr.getDataKind() == DBPDataKind.STRING) {
                            final DBDValueHandler valueHandler = DBUtils.findValueHandler(session, descAttr);
                            valueHandler.bindValueObject(session, dbStat, descAttr, paramPos++,
                                descAttr.getDataKind() == DBPDataKind.STRING ? "%" + searchText + "%" : keyValue);
                        }
                    }
                }

                dbStat.setLimit(0, maxResults);
                if (dbStat.executeStatement()) {
                    try (DBCResultSet dbResult = dbStat.openResultSet()) {
                        return DBVUtils.readDictionaryRows(
                            session,
                            Collections.singletonList(keyColumn),
                            Collections.singletonList(keyValueHandler),
                            dbResult,
                            true,
                            false);
                    }
                } else {
                    return Collections.emptyList();
                }
            }
        }
    }
    
    @NotNull
    @Override
    public DBSDictionaryAccessor getDictionaryAccessor(
        @NotNull DBRProgressMonitor monitor,
        List<DBDAttributeValue> preceedingKeys,
        @NotNull DBSEntityAttribute keyColumn,
        boolean sortAsc,
        boolean sortByDesc
    ) throws DBException {
        return new DictionaryAccessor(monitor, preceedingKeys, keyColumn, sortAsc, sortByDesc);
    }

    ////////////////////////////////////////////////////////////////////
    // Truncate

    @NotNull
    @Override
    public DBCStatistics truncateData(@NotNull DBCSession session, @NotNull DBCExecutionSource source) throws DBCException {
        if (!isTruncateSupported()) {
            try (ExecuteBatch batch = deleteData(session, new DBSAttributeBase[0], source)) {
                batch.add(new Object[0]);
                return batch.execute(session, Collections.emptyMap());
            }
        } else {
            DBCStatistics statistics = new DBCStatistics();
            DBRProgressMonitor monitor = session.getProgressMonitor();

            monitor.subTask("Truncate data");
            try (DBCStatement dbStat = session.prepareStatement(
                DBCStatementType.QUERY,
                getTruncateTableQuery(),
                false, false, false)) {
                dbStat.setStatementSource(source);
                dbStat.executeStatement();
            }
            statistics.addStatementsCount();
            statistics.addExecuteTime();
            return statistics;
        }
    }

    protected boolean isTruncateSupported() {
        return true;
    }

    protected String getTruncateTableQuery() {
        return "TRUNCATE TABLE " + getFullyQualifiedName(DBPEvaluationContext.DML);
    }

    ////////////////////////////////////////////////////////////////////
    // Utils

    private boolean useUpsert(@NotNull DBCSession session) {
        SQLDialect dialect = session.getDataSource().getSQLDialect();
        return dialect instanceof JDBCSQLDialect && ((JDBCSQLDialect) dialect).supportsUpsertStatement();
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
            query.append(dialect.getCastedAttributeName(attribute, DBStructUtils.getAttributeName(attribute)));
        }
        if (DBUtils.isNullValue(value)) {
            query.append(" IS NULL"); //$NON-NLS-1$
        } else {
            query.append("=").append(dialect.getTypeCastClause(attribute, "?", true)); //$NON-NLS-1$
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
        if (!getDataSource().getContainer().isExtraMetadataReadEnabled()) {
            return;
        }
        try {
            getAttributes(monitor);
        }
        catch (DBException e) {
            throw new DBCException("Can't cache table columns", e);
        }
    }

    public String generateTableUpdateBegin(String tableName) {
        return "UPDATE " + tableName;
    }

    public String generateTableUpdateSet() {
        return "SET ";
    }

    public String generateTableDeleteFrom(String tableName) {
        return "DELETE FROM " + tableName;
    }

    private static class AttrInfo<T> {
        public final T attr;
        public final DBDValueHandler handler;
        
        public AttrInfo(T attr, DBDValueHandler handler) {
            this.attr = attr;
            this.handler = handler;
        }
    }
    
    protected class DictionaryAccessor implements DBSDictionaryAccessor {
        private final List<AttrInfo<DBDAttributeValue>> preceedingKeysInfo;
        private final DBSEntityAttribute keyColumn;
        private final boolean sortAsc;
        private final boolean sortByDesc;

        private final DBDValueHandler keyValueHandler;
        private final String descColumns;
        private final Collection<DBSEntityAttribute> descAttributes;
        private final Collection<AttrInfo<DBSEntityAttribute>> descAttributesInfo;
        private final boolean isKeyComparable;

        private final DBDDataFilter filter;
        private final JDBCSession session;

        public DictionaryAccessor(
            @NotNull DBRProgressMonitor monitor,
            @Nullable List<DBDAttributeValue> preceedingKeys,
            @NotNull DBSEntityAttribute keyColumn,
            boolean sortAsc,
            boolean sortByDesc
        ) throws DBException {
            this.keyColumn = keyColumn;
            this.sortAsc = sortAsc;
            this.sortByDesc = sortByDesc;

            this.keyValueHandler = DBUtils.findValueHandler(keyColumn.getDataSource(), keyColumn);
            this.descColumns = DBVUtils.getDictionaryDescriptionColumns(monitor, keyColumn);
            this.descAttributes = descColumns == null ? null : DBVEntity.getDescriptionColumns(monitor, JDBCTable.this, descColumns);
            this.isKeyComparable = ArrayUtils.contains(DBUtils.getAttributeOperators(keyColumn), DBCLogicalOperator.LESS);

            this.session = DBUtils.openUtilSession(monitor, JDBCTable.this, "Load attribute values count");
            
            {
                int capacity = preceedingKeys == null ? 0 : preceedingKeys.size() + 1;
                List<DBDAttributeConstraint> constraints = new ArrayList<>(capacity);
                List<AttrInfo<DBDAttributeValue>> preceedingKeysInfo = new ArrayList<>(capacity);
                if (preceedingKeys != null) {
                    for (DBDAttributeValue key : preceedingKeys) {
                        DBDAttributeConstraint constraint = new DBDAttributeConstraint(key.getAttribute(), constraints.size());
                        constraint.setValue(key.getValue());
                        constraint.setOperator(DBCLogicalOperator.EQUALS);
                        constraints.add(constraint);
                        
                        DBDValueHandler precValueHandler = DBUtils.findValueHandler(session, key.getAttribute());
                        preceedingKeysInfo.add(new AttrInfo<>(key, precValueHandler));
                    }
                }
                this.preceedingKeysInfo = preceedingKeysInfo;
                this.filter = new DBDDataFilter(constraints);
            }
            
            this.descAttributesInfo = descAttributes == null ? Collections.emptyList() : descAttributes.stream()
                .map(a -> new AttrInfo<>(a, DBUtils.findValueHandler(session, a))).collect(Collectors.toList());
        }

        @NotNull
        @Override
        public DBRProgressMonitor getProgressMonitor() {
            return session.getProgressMonitor();
        }

        @Override
        public boolean isKeyComparable() {
            return isKeyComparable;
        }

        private int bindPrecedingKeys(@NotNull DBCStatement dbStat) throws DBCException {
            int paramPos = 0;
            if (!preceedingKeysInfo.isEmpty()) {
                for (var k : preceedingKeysInfo) {
                    k.handler.bindValueObject(session, dbStat, k.attr.getAttribute(), paramPos++, k.attr.getValue());
                }
            }
            return paramPos;
        }
        
        @NotNull
        @Override
        public List<DBDLabelValuePair> getValueEntry(@NotNull Object keyValue) throws DBException {
            DBDDataFilter filter = new DBDDataFilter(this.filter);
            List<DBDAttributeConstraint> constraints = filter.getConstraints();
            DBDAttributeConstraint constraint = new DBDAttributeConstraint(keyColumn, constraints.size());
            constraint.setValue(keyValue);
            constraint.setOperator(DBCLogicalOperator.EQUALS);
            constraints.add(constraint);
            StringBuilder query = prepareQueryString(filter);
            try (DBCStatement dbStat = DBUtils.makeStatement(null, session, DBCStatementType.QUERY, query.toString(), 0, 1)) {
                int paramPos = bindPrecedingKeys(dbStat);
                keyValueHandler.bindValueObject(session, dbStat, keyColumn, paramPos, keyValue);
                return readValues(dbStat);
            }
        }
        
        @NotNull
        @Override
        public List<DBDLabelValuePair> getValues(long offset, int pageSize) throws DBException {
            StringBuilder query = prepareQueryString(filter);
            appendSortingClause(query, false);
            try (DBCStatement dbStat = DBUtils.makeStatement(null, session, DBCStatementType.QUERY, query.toString(), offset, pageSize)) {
                bindPrecedingKeys(dbStat);
                return readValues(dbStat);
            }
        }

        @NotNull
        @Override
        public List<DBDLabelValuePair> getValuesNear(
            @NotNull Object value,
            boolean isPreceeding,
            long offset,
            long maxResults
        ) throws DBException {
            DBDDataFilter filter = new DBDDataFilter(this.filter);
            List<DBDAttributeConstraint> constraints = filter.getConstraints();
            DBDAttributeConstraint constraint = new DBDAttributeConstraint(keyColumn, constraints.size());
            constraint.setValue(value);
            constraint.setOperator(isPreceeding ^ sortAsc? DBCLogicalOperator.GREATER_EQUALS : DBCLogicalOperator.LESS);
            constraints.add(constraint);
            StringBuilder query = prepareQueryString(filter);
            appendSortingClause(query, isPreceeding);
            try (DBCStatement dbStat = DBUtils.makeStatement(null, session, DBCStatementType.QUERY, query.toString(), offset, maxResults)) {
                int paramPos = bindPrecedingKeys(dbStat);
                keyValueHandler.bindValueObject(session, dbStat, keyColumn, paramPos, value);
                return readValues(dbStat);
            }
        }

        @NotNull
        @Override
        public List<DBDLabelValuePair> getSimilarValues(
            @NotNull Object pattern,
            boolean caseInsensitive,
            boolean byDesc,
            long offset,
            long maxResults
        ) throws DBException {
            StringBuilder query = prepareQueryString(filter);
            appendByPatternCondition(query, filter, pattern, caseInsensitive, byDesc);
            appendSortingClause(query, false);
            try (DBCStatement dbStat = DBUtils.makeStatement(null, session, DBCStatementType.QUERY, query.toString(), offset, maxResults)) {
                int paramPos = bindPrecedingKeys(dbStat);
                bindPattern(dbStat, pattern, byDesc, paramPos);
                return readValues(dbStat);
            }
        }

        @NotNull
        @Override
        public List<DBDLabelValuePair> getSimilarValuesNear(
            @NotNull Object pattern, boolean caseInsensitive, boolean byDesc,
            Object value, boolean isPreceeding, 
            long offset, long maxResults
        ) throws DBException {
            DBDDataFilter filter = new DBDDataFilter(this.filter);
            List<DBDAttributeConstraint> constraints = filter.getConstraints();
            DBDAttributeConstraint constraint = new DBDAttributeConstraint(keyColumn, constraints.size());
            constraint.setValue(value);
            constraint.setOperator(isPreceeding ^ sortAsc? DBCLogicalOperator.GREATER_EQUALS : DBCLogicalOperator.LESS);
            constraints.add(constraint);
            StringBuilder query = prepareQueryString(filter);
            appendByPatternCondition(query, filter, pattern, caseInsensitive, byDesc);
            appendSortingClause(query, isPreceeding);
            try (DBCStatement dbStat = DBUtils.makeStatement(null, session, DBCStatementType.QUERY, query.toString(), offset, maxResults)) {
                int paramPos = bindPrecedingKeys(dbStat);
                keyValueHandler.bindValueObject(session, dbStat, keyColumn, paramPos++, value);
                bindPattern(dbStat, pattern, byDesc, paramPos);
                return readValues(dbStat);
            }
        }
        
        @NotNull
        private StringBuilder prepareQueryString(@NotNull DBDDataFilter filter) {
            StringBuilder query = new StringBuilder();

            query.append("SELECT ").append(DBUtils.getQuotedIdentifier(keyColumn, DBPAttributeReferencePurpose.DATA_SELECTION));
            if (descAttributes != null) {
                if (DBUtils.findObject(descAttributes, keyColumn.getName(), true) != null) {
                    // Add alias for value column to avoid ambiguity
                    query.append(" dbvrvalue");
                }
                query.append(", ").append(descColumns);
            }
            query.append(" FROM ").append(DBUtils.getObjectFullName(JDBCTable.this, DBPEvaluationContext.DML));

            if (!filter.getConstraints().isEmpty()) {
                query.append(" WHERE ");
            }
            
            getDataSource().getSQLDialect().getQueryGenerator().appendConditionString(filter, getDataSource(), null, query, false);
            
            return query;
        }

        @NotNull
        private List<DBDLabelValuePair> readValues(@NotNull DBCStatement dbStat) throws DBException {
            if (dbStat.executeStatement()) {
                try (DBCResultSet dbResult = dbStat.openResultSet()) {
                    return DBVUtils.readDictionaryRows(
                        session,
                        Collections.singletonList(keyColumn),
                        Collections.singletonList(keyValueHandler),
                        dbResult,
                        true,
                        false);
                }
            } else {
                return Collections.emptyList();
            }
        }

        private void appendByPatternCondition(
            @NotNull StringBuilder query,
            @NotNull DBDDataFilter existingFilter,
            @NotNull Object pattern,
            boolean caseInsensitive,
            boolean byDesc
        ) {
            if (!existingFilter.getConstraints().isEmpty()) {
                query.append(" AND ");
            } else {
                query.append(" WHERE ");
            }
            DBDDataFilter patternFilter = prepareByPatternCondition(pattern, caseInsensitive, byDesc);
            query.append("(");
            getDataSource().getSQLDialect().getQueryGenerator().appendConditionString(patternFilter, getDataSource(), null, query, false);
            // handle custom expression for decription assuming it returns string
            if (CommonUtils.isNotEmpty(descColumns) && (descAttributes == null || descAttributes.isEmpty())) {
                if (patternFilter.hasConditions()) {
                    query.append(" OR ");
                }
                query.append("(").append(descColumns).append(") LIKE ").append("?");
            }
            query.append(")");
        }

        @NotNull
        private DBDDataFilter prepareByPatternCondition(@NotNull Object pattern, boolean caseInsensitive, boolean byDesc) {
            DBDDataFilter filter = new DBDDataFilter();
            filter.setAnyConstraint(true);
            
            List<DBDAttributeConstraint> constraints = filter.getConstraints();
            DBDAttributeConstraint keyConstraint = new DBDAttributeConstraint(keyColumn, constraints.size());
            if (keyColumn.getDataKind() == DBPDataKind.STRING) {
                boolean ilikeUsable = ArrayUtils.contains(keyValueHandler.getSupportedOperators(keyColumn), DBCLogicalOperator.ILIKE);
                keyConstraint.setValue("%" + pattern + "%");
                keyConstraint.setOperator(caseInsensitive && ilikeUsable ? DBCLogicalOperator.ILIKE : DBCLogicalOperator.LIKE);
            } else if (keyColumn.getDataKind() == DBPDataKind.NUMERIC) {
                if (CommonUtils.isNumber(pattern)) {
                    keyConstraint.setValue(pattern);
                    keyConstraint.setOperator(DBCLogicalOperator.GREATER_EQUALS);
                }
            } else if (pattern instanceof CharSequence) {
                keyConstraint.setValue(pattern);
                keyConstraint.setOperator(DBCLogicalOperator.EQUALS);
            }
            if (keyConstraint.getValue() != null) {
                constraints.add(keyConstraint);
            }
            // Add desc columns conditions
            if (byDesc && pattern instanceof CharSequence) {
                for (var a : descAttributesInfo) {
                    if (a.attr.getDataKind() == DBPDataKind.STRING) {
                        final DBDValueHandler valueHandler = DBUtils.findValueHandler(session, a.attr);
                        boolean ilikeUsable = ArrayUtils.contains(valueHandler.getSupportedOperators(a.attr), DBCLogicalOperator.ILIKE);
                        DBDAttributeConstraint descConstraint = new DBDAttributeConstraint(a.attr, constraints.size());
                        descConstraint.setValue("%" + pattern + "%");
                        descConstraint.setOperator(caseInsensitive && ilikeUsable ? DBCLogicalOperator.ILIKE : DBCLogicalOperator.LIKE);
                        constraints.add(descConstraint);
                    }
                }
            }

            return filter;
        }
        
        private void bindPattern(@NotNull DBCStatement dbStat, @NotNull Object pattern, boolean byDesc, int bindAt) throws DBCException {
            int paramPos = bindAt;
            
            if (keyColumn.getDataKind() == DBPDataKind.STRING) {
                keyValueHandler.bindValueObject(session, dbStat, keyColumn, paramPos++, "%" + pattern + "%");
            } else if (keyColumn.getDataKind() == DBPDataKind.NUMERIC) {
                if (CommonUtils.isNumber(pattern)) {
                    keyValueHandler.bindValueObject(session, dbStat, keyColumn, paramPos++, pattern);
                }
            } else if (pattern instanceof CharSequence) {
                keyValueHandler.bindValueObject(session, dbStat, keyColumn, paramPos++, pattern);
            }
            
            if (byDesc && pattern instanceof CharSequence) {
                for (var a : descAttributesInfo) {
                    if (a.attr.getDataKind() == DBPDataKind.STRING) {
                        a.handler.bindValueObject(session, dbStat, a.attr, paramPos++, "%" + pattern + "%");
                    }
                }
            }
            // handle custom expression for description assuming it returns string
            if (CommonUtils.isNotEmpty(descColumns) && (descAttributes == null || descAttributes.isEmpty())) {
                JDBCStringValueHandler.INSTANCE.bindValueObject(
                    session,
                    dbStat,
                    null, // It is ok to pass null here because JDBCStringValueHandler doesn't use attr type
                    paramPos++,
                    "%" + pattern + "%");
            }
        }
        
        private void appendSortingClause(@NotNull StringBuilder query, boolean isPreceeding) {
            if (isKeyComparable() || sortByDesc) {
                query.append(" ORDER BY ");
                if (sortByDesc) {
                    // Sort by description
                    query.append(descColumns);
                } else {
                    query.append(DBUtils.getQuotedIdentifier(keyColumn));
                }
                if (sortAsc ^ isPreceeding) {
                    query.append(" ASC");
                } else {
                    query.append(" DESC");
                }
            }
        }

        @Override
        public void close() throws Exception {
            this.session.close();
        }
    }
}
