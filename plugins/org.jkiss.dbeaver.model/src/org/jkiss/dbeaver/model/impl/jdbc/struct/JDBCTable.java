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
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.struct.AbstractTable;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JDBC abstract table implementation
 */
public abstract class JDBCTable<DATASOURCE extends DBPDataSource, CONTAINER extends DBSObject>
    extends AbstractTable<DATASOURCE, CONTAINER>
    implements DBSDictionary, DBSDataManipulator, DBPSaveableObject
{
    private static final Log log = Log.getLog(JDBCTable.class);

    protected static final String CAT_STATISTICS = "Statistics";

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
    public int getSupportedFeatures()
    {
        int features = DATA_COUNT | DATA_FILTER | DATA_SEARCH | DATA_INSERT | DATA_UPDATE | DATA_DELETE;
        if (isTruncateSupported()) {
            features |= DATA_TRUNCATE;
        }
        return features;
    }

    ////////////////////////////////////////////////////////////////////
    // Select

    @NotNull
    @Override
    public DBCStatistics readData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, @NotNull DBDDataReceiver dataReceiver, @Nullable DBDDataFilter dataFilter, long firstRow, long maxRows, long flags, int fetchSize)
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
            {
                if (dataSource.getSQLDialect().supportsAliasInSelect()) {
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

    ////////////////////////////////////////////////////////////////////
    // Count

    @Override
    public long countData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, @Nullable DBDDataFilter dataFilter, long flags) throws DBCException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        StringBuilder query = new StringBuilder("SELECT COUNT(*) FROM "); //$NON-NLS-1$
        query.append(getFullyQualifiedName(DBPEvaluationContext.DML));
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
    public ExecuteBatch insertData(@NotNull DBCSession session, @NotNull final DBSAttributeBase[] attributes, @Nullable DBDDataReceiver keysReceiver, @NotNull final DBCExecutionSource source)
        throws DBCException
    {
        readRequiredMeta(session.getProgressMonitor());

        return new ExecuteBatchImpl(attributes, keysReceiver, true) {

            private boolean allNulls;

            protected int getNextUsedParamIndex(Object[] attributeValues, int paramIndex) {
                paramIndex++;
                DBSAttributeBase attribute = attributes[paramIndex];
                while (DBUtils.isPseudoAttribute(attribute) || (!allNulls && DBUtils.isNullValue(attributeValues[paramIndex]))) {
                    paramIndex++;
                }
                return paramIndex;
            }

            @NotNull
            @Override
            protected DBCStatement prepareStatement(@NotNull DBCSession session, DBDValueHandler[] handlers, Object[] attributeValues, Map<String, Object> options) throws DBCException {
                // Make query
                String tableName = DBUtils.getEntityScriptName(JDBCTable.this, options);
                StringBuilder query = new StringBuilder(200);
                query
                    .append(useUpsert(session) ? SQLConstants.KEYWORD_UPSERT : SQLConstants.KEYWORD_INSERT)
                    .append(" INTO ").append(tableName).append(" ("); //$NON-NLS-1$ //$NON-NLS-2$

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
                query.append(")\n\tVALUES ("); //$NON-NLS-1$
                hasKey = false;
                for (int i = 0; i < attributes.length; i++) {
                    DBSAttributeBase attribute = attributes[i];
                    if (DBUtils.isPseudoAttribute(attribute) || (!allNulls && DBUtils.isNullValue(attributeValues[i]))) {
                        continue;
                    }
                    if (hasKey) query.append(","); //$NON-NLS-1$
                    hasKey = true;

                    DBDValueHandler valueHandler = handlers[i];
                    if (valueHandler instanceof DBDValueBinder) {
                        query.append(((DBDValueBinder) valueHandler) .makeQueryBind(attribute, attributeValues[i]));
                    } else {
                        query.append("?"); //$NON-NLS-1$
                    }
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
                source);
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
                query.append("UPDATE ").append(tableName);
                if (tableAlias != null) {
                    query.append(' ').append(tableAlias);
                }
                query.append("\n\tSET "); //$NON-NLS-1$ //$NON-NLS-2$

                boolean hasKey = false;
                for (int i = 0; i < updateAttributes.length; i++) {
                    DBSAttributeBase attribute = updateAttributes[i];
                    if (hasKey) query.append(","); //$NON-NLS-1$
                    hasKey = true;
                    if (tableAlias != null) {
                        query.append(tableAlias).append(dialect.getStructSeparator());
                    }
                    query.append(getAttributeName(attribute)).append("="); //$NON-NLS-1$
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
                query.append("DELETE FROM ").append(tableName);
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
     * @param monitor execution context
     * @param keyColumn enumeration column.
     * @param keyPattern pattern for enumeration values. If null or empty then returns full enumration set
     * @param preceedingKeys other constrain key values. May be null.
     * @param sortByValue sort results by eky value. If false then sort by description
     * @param sortAsc sort ascending/descending
     * @param maxResults maximum enumeration values in result set     @return  @throws DBException
     */
    @NotNull
    @Override
    public List<DBDLabelValuePair> getDictionaryEnumeration(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntityAttribute keyColumn,
        Object keyPattern,
        List<DBDAttributeValue> preceedingKeys,
        boolean sortByValue,
        boolean sortAsc,
        int maxResults)
        throws DBException
    {
        // Use default one
        return readKeyEnumeration(
            monitor,
            keyColumn,
            keyPattern,
            preceedingKeys,
            sortByValue,
            sortAsc,
            maxResults);
    }

    @NotNull
    @Override
    public List<DBDLabelValuePair> getDictionaryValues(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntityAttribute keyColumn,
        @NotNull List<Object> keyValues,
        @Nullable List<DBDAttributeValue> preceedingKeys,
        boolean sortByValue,
        boolean sortAsc) throws DBException
    {
        DBDValueHandler keyValueHandler = DBUtils.findValueHandler(keyColumn.getDataSource(), keyColumn);

        StringBuilder query = new StringBuilder();
        query.append("SELECT ").append(DBUtils.getQuotedIdentifier(keyColumn));

        String descColumns = DBVUtils.getDictionaryDescriptionColumns(monitor, keyColumn);
        if (descColumns != null) {
            query.append(", ").append(descColumns);
        }
        query.append(" FROM ").append(DBUtils.getObjectFullName(this, DBPEvaluationContext.DML)).append(" WHERE ");
        boolean hasCond = false;
        // Preceeding keys
        if (preceedingKeys != null && !preceedingKeys.isEmpty()) {
            for (DBDAttributeValue pk : preceedingKeys) {
                if (hasCond) query.append(" AND ");
                query.append(DBUtils.getQuotedIdentifier(getDataSource(), pk.getAttribute().getName())).append(" = ?");
                hasCond = true;
            }
        }
        if (hasCond) query.append(" AND ");
        query.append(DBUtils.getQuotedIdentifier(keyColumn)).append(" IN (");
        for (int i = 0; i < keyValues.size(); i++) {
            if (i > 0) query.append(",");
            query.append("?");
        }
        query.append(")");

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

        try (JDBCSession session = DBUtils.openUtilSession(monitor, this, "Load dictionary values")) {
            try (DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, false)) {
                int paramPos = 0;
                if (preceedingKeys != null && !preceedingKeys.isEmpty()) {
                    for (DBDAttributeValue precAttribute : preceedingKeys) {
                        DBDValueHandler precValueHandler = DBUtils.findValueHandler(session, precAttribute.getAttribute());
                        precValueHandler.bindValueObject(session, dbStat, precAttribute.getAttribute(), paramPos++, precAttribute.getValue());
                    }
                }
                for (Object value : keyValues) {
                    keyValueHandler.bindValueObject(session, dbStat, keyColumn, paramPos++, value);
                }
                dbStat.setLimit(0, keyValues.size());
                if (dbStat.executeStatement()) {
                    try (DBCResultSet dbResult = dbStat.openResultSet()) {
                        return DBVUtils.readDictionaryRows(session, keyColumn, keyValueHandler, dbResult, true);
                    }
                } else {
                    return Collections.emptyList();
                }
            }
        }
    }

    private List<DBDLabelValuePair> readKeyEnumeration(
        DBRProgressMonitor monitor,
        DBSEntityAttribute keyColumn,
        Object keyPattern,
        List<DBDAttributeValue> preceedingKeys,
        boolean sortByValue,
        boolean sortAsc,
        int maxResults)
        throws DBException
    {
        if (keyColumn.getParentObject() != this) {
            throw new IllegalArgumentException("Bad key column argument");
        }

        DBDValueHandler keyValueHandler = DBUtils.findValueHandler(keyColumn.getDataSource(), keyColumn);

        boolean searchInKeys = keyPattern != null;

        if (keyPattern != null) {
            if (keyColumn.getDataKind() == DBPDataKind.NUMERIC) {
                if (keyPattern instanceof Number) {
                    // Subtract gap value to see some values before specified
                    int gapSize = maxResults / 2;
                    boolean allowNegative = ((Number) keyPattern).longValue() < 0;
                    if (keyPattern instanceof Integer) {
                        int intValue = (Integer) keyPattern;
                        keyPattern = allowNegative || intValue > gapSize ? intValue - gapSize : 0;
                    } else if (keyPattern instanceof Short) {
                        int shortValue = (Short) keyPattern;
                        keyPattern = allowNegative || shortValue > gapSize ? shortValue - gapSize : (short)0;
                    } else if (keyPattern instanceof Long) {
                        long longValue = (Long) keyPattern;
                        keyPattern = allowNegative || longValue > gapSize ? longValue - gapSize : (long)0;
                    } else if (keyPattern instanceof Float) {
                        float floatValue = (Float) keyPattern;
                        keyPattern = allowNegative || floatValue > gapSize ? floatValue - gapSize : 0.0f;
                    } else if (keyPattern instanceof Double) {
                        double doubleValue = (Double) keyPattern;
                        keyPattern = allowNegative || doubleValue > gapSize ? doubleValue - gapSize : 0.0;
                    } else if (keyPattern instanceof BigInteger) {
                        BigInteger biValue = (BigInteger) keyPattern;
                        keyPattern = allowNegative || biValue.longValue() > gapSize ? ((BigInteger) keyPattern).subtract(BigInteger.valueOf(gapSize)) : new BigInteger("0");
                    } else if (keyPattern instanceof BigDecimal) {
                        BigDecimal bdValue = (BigDecimal) keyPattern;
                        keyPattern = allowNegative || bdValue.longValue() > gapSize ? ((BigDecimal) keyPattern).subtract(new BigDecimal(gapSize)) : new BigDecimal(0);
                    } else {
                        searchInKeys = false;
                    }
                } else if (keyPattern instanceof String) {
                    if (((String) keyPattern).isEmpty() || !Character.isDigit(((String)keyPattern).charAt(0)) ) {
                        searchInKeys = false;
                    }
                    // Ignore it
                    //keyPattern = Double.parseDouble((String) keyPattern);
                }
            } else if (keyPattern instanceof CharSequence /*&& keyColumn.getDataKind() == DBPDataKind.STRING*/) {
                // Its ok
            } else {
                searchInKeys = false;
            }
        }
/*
        if (keyPattern instanceof CharSequence && (!searchInKeys || keyColumn.getDataKind() != DBPDataKind.NUMERIC)) {
            if (((CharSequence)keyPattern).length() > 0) {
                keyPattern = "%" + keyPattern.toString() + "%";
            } else {
                keyPattern = null;
            }
        }
*/

        StringBuilder query = new StringBuilder();
        query.append("SELECT ").append(DBUtils.getQuotedIdentifier(keyColumn));

        String descColumns = DBVUtils.getDictionaryDescriptionColumns(monitor, keyColumn);
        Collection<DBSEntityAttribute> descAttributes = null;
        if (descColumns != null) {
            descAttributes = DBVEntity.getDescriptionColumns(monitor, this, descColumns);
            query.append(", ").append(descColumns);
        }
        query.append(" FROM ").append(DBUtils.getObjectFullName(this, DBPEvaluationContext.DML));

        boolean searchInDesc = keyPattern instanceof CharSequence && descAttributes != null;
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

        if (!CommonUtils.isEmpty(preceedingKeys) || searchInKeys || searchInDesc) {
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
        if (keyPattern != null) {
            if (hasCond) query.append(" AND (");
            if (searchInKeys) {
                query.append(DBUtils.getQuotedIdentifier(keyColumn));
                if (keyColumn.getDataKind() == DBPDataKind.NUMERIC) {
                    query.append(" >= ?");
                } else if (keyColumn.getDataKind() == DBPDataKind.STRING) {
                    query.append(" LIKE ?");
                } else {
                    query.append(" = ?");
                }
            }
            // Add desc columns conditions
            if (searchInDesc) {
                boolean hasCondition = searchInKeys;
                for (DBSEntityAttribute descAttr : descAttributes) {
                    if (descAttr.getDataKind() == DBPDataKind.STRING) {
                        if (hasCondition) {
                            query.append(" OR ");
                        }
                        query.append(DBUtils.getQuotedIdentifier(descAttr)).append(" LIKE ?");
                        hasCondition = true;
                    }
                }
            }
            if (hasCond) query.append(")");
        }
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

        try (JDBCSession session = DBUtils.openUtilSession(monitor, this, "Load attribute value enumeration")) {
            try (DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, false)) {
                int paramPos = 0;

                if (preceedingKeys != null && !preceedingKeys.isEmpty()) {
                    for (DBDAttributeValue precAttribute : preceedingKeys) {
                        DBDValueHandler precValueHandler = DBUtils.findValueHandler(session, precAttribute.getAttribute());
                        precValueHandler.bindValueObject(session, dbStat, precAttribute.getAttribute(), paramPos++, precAttribute.getValue());
                    }
                }

                if (keyPattern != null && searchInKeys) {
                    keyValueHandler.bindValueObject(session, dbStat, keyColumn, paramPos++,
                        keyColumn.getDataKind() == DBPDataKind.STRING ? "%" + keyPattern + "%" : keyPattern);
                }

                if (searchInDesc) {
                    for (DBSEntityAttribute descAttr : descAttributes) {
                        if (descAttr.getDataKind() == DBPDataKind.STRING) {
                            final DBDValueHandler valueHandler = DBUtils.findValueHandler(session, descAttr);
                            valueHandler.bindValueObject(session, dbStat, descAttr, paramPos++,
                                descAttr.getDataKind() == DBPDataKind.STRING ? "%" + keyPattern + "%" : keyPattern);
                        }
                    }
                }

                dbStat.setLimit(0, maxResults);
                if (dbStat.executeStatement()) {
                    try (DBCResultSet dbResult = dbStat.openResultSet()) {
                        return DBVUtils.readDictionaryRows(session, keyColumn, keyValueHandler, dbResult, true);
                    }
                } else {
                    return Collections.emptyList();
                }
            }
        }
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
            query.append("=").append(dialect.getTypeCastClause(attribute, "?")); //$NON-NLS-1$
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
