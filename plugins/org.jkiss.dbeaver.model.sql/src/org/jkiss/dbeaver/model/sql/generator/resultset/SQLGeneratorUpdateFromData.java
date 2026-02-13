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
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDResultSetDataProvider;
import org.jkiss.dbeaver.model.data.DBDValueRow;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQueryGeneratorUpdate;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

public class SQLGeneratorUpdateFromData extends SQLGeneratorResultSet {

    @Override
    public boolean isDMLOption() {
        return true;
    }

    @Override
    protected void generateSQL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull StringBuilder sql,
        @NotNull DBDResultSetDataProvider dataProvider
    ) throws DBException {
        DBSEntity dbsEntity = dataProvider.getSingleSource();
        String entityName = getEntityName(dbsEntity);
        String separator = getLineSeparator();
        for (DBDValueRow firstRow : dataProvider.getSelectedRows()) {
            Collection<DBDAttributeBinding> keyAttributes = getKeyAttributes(monitor, dataProvider);
            Collection<? extends DBSAttributeBase> valueAttributes = getValueAttributes(monitor, dataProvider, keyAttributes);
            if (dbsEntity instanceof SQLQueryGeneratorUpdate dataStatement) {
                sql.append(dataStatement.generateTableUpdateBegin(entityName));
                String updateSet = dataStatement.generateTableUpdateSet();
                if (CommonUtils.isNotEmpty(updateSet)) {
                    sql.append(separator).append(updateSet);
                }
            } else {
                sql.append("UPDATE ").append(entityName);
                sql.append(separator).append("SET ");
            }
            boolean hasAttr = false;
            if (CommonUtils.isEmpty(valueAttributes)) {
                valueAttributes = keyAttributes;
            }
            for (DBSAttributeBase attr : valueAttributes) {
                if (DBUtils.isPseudoAttribute(attr) || DBUtils.isHiddenObject(attr)) {
                    continue;
                }
                if (hasAttr) sql.append(", ");
                sql.append(DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML)).append("=");
                DBDAttributeBinding binding = DBUtils.findBinding(dataProvider.getAttributes(), attr);
                if (binding == null) {
                    appendDefaultValue(sql, attr);
                } else {
                    appendAttributeValue(dataProvider, sql, binding, firstRow, true);
                }

                hasAttr = true;
            }
            if (!CommonUtils.isEmpty(keyAttributes)) {
                sql.append(separator).append("WHERE ");
                appendKeyConditions(sql, keyAttributes, firstRow);
            }
            sql.append(";\n");
        }
    }
}
