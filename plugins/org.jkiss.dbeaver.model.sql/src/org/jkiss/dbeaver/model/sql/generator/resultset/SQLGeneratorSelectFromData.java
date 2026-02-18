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
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;

import java.util.Collection;
import java.util.List;

public class SQLGeneratorSelectFromData extends SQLGeneratorResultSet {

    @Override
    protected void generateSQL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull StringBuilder sql,
        @NotNull DBDResultSetDataProvider dataProvider
    ) throws DBException {
        for (DBDValueRow firstRow : dataProvider.getSelectedRows()) {
            sql.append("SELECT ");
            boolean hasAttr = false;
            for (DBSAttributeBase attr : getAllAttributes(monitor, dataProvider)) {
                if (hasAttr) sql.append(", ");
                sql.append(DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML));
                hasAttr = true;
            }
            sql.append(getLineSeparator()).append("FROM ").append(getEntityName(dataProvider.getSingleSource()));
            sql.append(getLineSeparator()).append("WHERE ");
            Collection<DBDAttributeBinding> keyAttributes = getKeyAttributes(monitor, dataProvider);
            if (keyAttributes.isEmpty()) {
                // No unique key - use all columns
                keyAttributes = List.of(dataProvider.getAttributes());
            }
            appendKeyConditions(sql, keyAttributes, firstRow);
            sql.append(";\n");
        }
    }

}
