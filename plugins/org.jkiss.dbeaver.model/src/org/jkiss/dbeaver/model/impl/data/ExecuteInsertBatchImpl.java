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
package org.jkiss.dbeaver.model.impl.data;

import org.eclipse.core.runtime.Assert;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.sql.BaseInsertMethod;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class ExecuteInsertBatchImpl extends ExecuteBatchImpl {

    private DBCSession session;
    private final DBCExecutionSource source;
    private DBSTable table;
    private boolean useUpsert;
    private boolean allNulls;
    private boolean allColumnsDefault;

    /**
     * Constructs new batch
     *
     * @param attributes     array of attributes used in batch
     * @param keysReceiver   keys receiver (or null)
     * @param reuseStatement true if engine should reuse single prepared statement for each execution.
     */
    public ExecuteInsertBatchImpl(@NotNull DBSAttributeBase[] attributes, @Nullable DBDDataReceiver keysReceiver, boolean reuseStatement, @NotNull DBCSession session, @NotNull final DBCExecutionSource source, @NotNull DBSTable table, boolean useUpsert) {
        super(attributes, keysReceiver, reuseStatement);
        this.session = session;
        this.source = source;
        this.table = table;
        this.useUpsert = useUpsert;
    }

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
        StringBuilder queryForStatement = prepareQueryForStatement(session, handlers, attributeValues, attributes, table,false, options);
        // Execute. USe plain statement if binding was disabled
        boolean skipBindValues = CommonUtils.toBoolean(options.get(DBSDataManipulator.OPTION_SKIP_BIND_VALUES));
        DBCStatement dbStat = session.prepareStatement(
            skipBindValues ? DBCStatementType.SCRIPT : DBCStatementType.QUERY,
            queryForStatement.toString(),
            false,
            false,
            keysReceiver != null);
        dbStat.setStatementSource(source);
        return dbStat;
    }

    @Override
    protected void bindStatement(@NotNull DBDValueHandler[] handlers, @NotNull DBCStatement statement, Object[] attributeValues) throws DBCException {
        if (allColumnsDefault) {
            // There is nothing to bind in this statement
            return;
        }
        int paramIndex = 0;
        for (int k = 0; k < handlers.length; k++) {
            DBSAttributeBase attribute = attributes[k];
            if (DBUtils.isPseudoAttribute(attribute) || (!allNulls && DBUtils.isNullValue(attributeValues[k]))) {
                continue;
            }
            if (allNulls && attributeHasDefaultValue(attribute)) {
                continue;
            }
            handlers[k].bindValueObject(statement.getSession(), statement, attribute, paramIndex++, attributeValues[k]);
            if (session.getProgressMonitor().isCanceled()) {
                break;
            }
        }
    }

    StringBuilder prepareQueryForStatement(
        @NotNull DBCSession session,
        DBDValueHandler[] handlers,
        Object[] attributeValues,
        DBSAttributeBase[] attributes,
        DBSTable table,
        boolean useMultiRowInsert,
        Map<String, Object> options) throws DBCException {

        allColumnsDefault = false;
        
        Assert.isLegal(attributes.length == handlers.length);
        Assert.isLegal(useMultiRowInsert || attributes.length == attributeValues.length);

        // Make query
        String tableName = DBUtils.getEntityScriptName(table, options);
        StringBuilder query = new StringBuilder(200);

        DBDInsertReplaceMethod method = (DBDInsertReplaceMethod) options.get(DBSDataManipulator.OPTION_INSERT_REPLACE_METHOD);
        if (method == null) {
            method = new BaseInsertMethod();
        }

        if (useUpsert) {
            query.append(SQLConstants.KEYWORD_UPSERT).append(" INTO");
        } else {
            query.append(method.getOpeningClause(table, session.getProgressMonitor()));
        }
        query.append(" ").append(tableName).append(" ("); //$NON-NLS-1$ //$NON-NLS-2$


        allNulls = true;
        for (int i = 0; i < attributes.length; i++) {
            if (!DBUtils.isNullValue(attributeValues[i])) {
                allNulls = false;
                break;
            }
        }
        DBPDataSource dataSource = session.getDataSource();
        if (allNulls && !useMultiRowInsert && method instanceof BaseInsertMethod && !useUpsert && dataSource.getSQLDialect().supportsInsertAllDefaultValuesStatement()) {
            allColumnsDefault = true;
            query.setLength(0);
            query.append("INSERT INTO ").append(tableName).append(" DEFAULT VALUES");
            return query;
        }
        boolean hasKey = false;
        List<Integer> usedAttributes = new ArrayList<Integer>();
        for (int i = 0; i < attributes.length; i++) {
            DBSAttributeBase attribute = attributes[i];
            if (DBUtils.isPseudoAttribute(attribute) || (!useMultiRowInsert && (!allNulls && DBUtils.isNullValue(attributeValues[i])))) {
                continue;
            }
            if (hasKey) query.append(","); //$NON-NLS-1$
            hasKey = true;
            String attributeName = DBStructUtils.getAttributeName(attribute);
            query.append(attributeName);
            usedAttributes.add(i);
        }
        query.append(")\n\tVALUES "); //$NON-NLS-1$
        
        int usedAttributesArr[] = usedAttributes.stream().mapToInt(Integer::intValue).toArray();

        StringJoiner valuesPart = new StringJoiner(",");
        boolean skipBindValues = CommonUtils.toBoolean(options.get(DBSDataManipulator.OPTION_SKIP_BIND_VALUES));
        for (int i = 0; i < attributeValues.length / attributes.length; i++) {
            StringJoiner rowValuesPart = new StringJoiner(",", "(", ")");
            for (int j : usedAttributesArr) {
                DBSAttributeBase attribute = attributes[j];
                DBDValueHandler valueHandler = handlers[j];
                int k = i * attributes.length + j;
                if (valueHandler instanceof DBDValueBinder) {
                    rowValuesPart.add(((DBDValueBinder) valueHandler).makeQueryBind(attribute, attributeValues[k]));
                } else if (skipBindValues) {
                    rowValuesPart.add(SQLUtils.convertValueToSQL(session.getDataSource(), attribute, valueHandler, attributeValues[k], DBDDisplayFormat.NATIVE, false));
                } else if (allNulls && attributeHasDefaultValue(attribute)) {
                    rowValuesPart.add("DEFAULT");
                } else {
                    rowValuesPart.add("?");
                }
            }
            valuesPart.add(rowValuesPart.toString());
        }

        query.append(valuesPart);
        
        String trailingClause = method.getTrailingClause(table, session.getProgressMonitor(), attributes);
        if (trailingClause != null) {
            query.append(trailingClause);
        }

        return query;
    }

    private boolean attributeHasDefaultValue(@NotNull DBSAttributeBase attribute) {
        if (DBUtils.isPseudoAttribute(attribute) || DBUtils.isHiddenObject(attribute)) {
            return false;
        }
        if (attribute instanceof DBDAttributeBinding) {
            DBSEntityAttribute entityAttribute = ((DBDAttributeBinding) attribute).getEntityAttribute();
            return entityAttribute != null && (CommonUtils.isNotEmpty(entityAttribute.getDefaultValue()));
        }
        return false;
    }
}
