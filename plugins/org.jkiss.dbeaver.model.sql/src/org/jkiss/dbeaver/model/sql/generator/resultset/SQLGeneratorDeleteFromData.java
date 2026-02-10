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

public class SQLGeneratorDeleteFromData extends SQLGeneratorResultSet {

    @Override
    protected void generateSQL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull StringBuilder sql,
        @NotNull DBDResultSetDataProvider dataProvider
    ) throws DBException {
        DBSEntity dbsEntity = dataProvider.getSingleSource();
        String entityName = getEntityName(dbsEntity);
        for (DBDValueRow firstRow : dataProvider.getSelectedRows()) {
            Collection<DBDAttributeBinding> keyAttributes = getKeyAttributes(monitor, dataProvider);
            if (dbsEntity instanceof SQLQueryGeneratorUpdate) {
                sql.append(((SQLQueryGeneratorUpdate) dbsEntity).generateTableDeleteFrom(entityName));
            } else {
                sql.append("DELETE FROM ").append(entityName);
            }
            sql.append(getLineSeparator()).append("WHERE ");
            if (CommonUtils.isEmpty(keyAttributes)) {
                // For tables without keys including virtual
                Collection<? extends DBSAttributeBase> allAttributes = getAllAttributes(monitor, dataProvider);
                for (DBSAttributeBase attr : allAttributes) {
                    if (DBUtils.isPseudoAttribute(attr) || DBUtils.isHiddenObject(attr)) {
                        continue;
                    }
                    DBDAttributeBinding binding = DBUtils.findBinding(dataProvider.getAttributes(), attr);
                    if (binding != null) {
                        keyAttributes.add(binding);
                    }
                }
            }
            appendKeyConditions(sql, keyAttributes, firstRow);
            sql.append(";\n");
        }
    }
}
