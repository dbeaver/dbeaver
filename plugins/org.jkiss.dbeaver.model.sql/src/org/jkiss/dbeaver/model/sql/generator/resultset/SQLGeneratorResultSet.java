/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.generator.resultset;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.generator.SQLGeneratorBase;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.utils.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class SQLGeneratorResultSet extends SQLGeneratorBase<DBDResultSetDataProvider> {

    @NotNull
    public DBDResultSetDataProvider getDataProvider() {
        return objects.getFirst();
    }

    protected abstract void generateSQL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull StringBuilder sql,
        @NotNull DBDResultSetDataProvider dataProvider
    ) throws DBException;

    @NotNull
    protected Collection<? extends DBSAttributeBase> getAllAttributes(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBDResultSetDataProvider dataProvider
    ) throws DBException {
        return Arrays.asList(dataProvider.getAttributes());
    }

    void appendKeyConditions(
        @NotNull StringBuilder sql,
        @NotNull Collection<DBDAttributeBinding> keyAttributes,
        @NotNull DBDValueRow firstRow
    ) throws DBException {
        Object[] values = firstRow.getValues();
        if (!ArrayUtils.isEmpty(values)) {
            Object firstCellValue = values[0];
            if (firstCellValue instanceof DBDDocument document) {
                Object idName = document.getDocumentProperty(DBDDocument.PROP_ID_ATTRIBUTE_NAME);
                Object documentId = document.getDocumentId();
                if (idName != null && documentId != null) {
                    sql.append(idName).append(" = ").append(
                        SQLUtils.quoteString(getDataProvider().getSingleSource(), documentId.toString())
                    );
                    return;
                }
            }
        }
        boolean hasAttr = false;
        for (DBDAttributeBinding attr : keyAttributes) {
            if (hasAttr) {
                sql.append(" AND ");
            }
            appendValueCondition(getDataProvider(), sql, attr, firstRow);
            hasAttr = true;
        }
    }

    @Override
    protected List<DBDAttributeBinding> getKeyAttributes(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBDResultSetDataProvider dataProvider
    ) throws DBException {
        final DBDRowIdentifier rowIdentifier = dataProvider.getDefaultRowIdentifier();
        if (rowIdentifier == null) {
            return Collections.emptyList();
        }
        return rowIdentifier.getAttributes();
    }

    private void appendValueCondition(
        @NotNull DBDResultSetDataProvider dataProvider,
        @NotNull StringBuilder sql,
        @NotNull DBDAttributeBinding binding,
        @NotNull DBDValueRow firstRow
    ) throws DBException {
        Object value = dataProvider.getCellValue(binding, firstRow);
        sql.append(DBUtils.getObjectFullName(binding.getAttribute(), DBPEvaluationContext.DML));
        if (DBUtils.isNullValue(value)) {
            sql.append(" IS NULL");
        } else {
            sql.append("=");
            appendAttributeValue(dataProvider, sql, binding, firstRow, true);
        }
    }

    protected void appendAttributeValue(
        @NotNull DBDResultSetDataProvider dataProvider,
        @NotNull StringBuilder sql,
        @NotNull DBDAttributeBinding binding,
        @NotNull DBDValueRow row,
        boolean isInCondition
    ) throws DBException {
        DBPDataSource dataSource = binding.getDataSource();
        Object value = dataProvider.getCellValue(binding, row);
        DBSAttributeBase attribute = binding.getAttribute();
        if (attribute.getDataKind() == DBPDataKind.DATETIME && isUseCustomDataFormat()) {
            sql.append(SQLUtils.quoteString(
                dataSource,
                SQLUtils.convertValueToSQL(
                    dataSource,
                    attribute,
                    DBUtils.findValueHandler(dataSource, attribute),
                    value,
                    DBDDisplayFormat.UI,
                    isInCondition
                )
            ));
        } else {
            sql.append(
                SQLUtils.convertValueToSQL(dataSource, attribute, value));
        }
    }
}
