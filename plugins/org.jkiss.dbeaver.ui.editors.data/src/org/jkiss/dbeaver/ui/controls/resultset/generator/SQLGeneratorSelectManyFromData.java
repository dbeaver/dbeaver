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
package org.jkiss.dbeaver.ui.controls.resultset.generator;

import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;

import java.util.Collection;
import java.util.List;

public class SQLGeneratorSelectManyFromData extends SQLGeneratorResultSet {

    @Override
    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, IResultSetController object) {
        Collection<DBDAttributeBinding> keyAttributes = getKeyAttributes(monitor, object);
        sql.append("SELECT ");
        boolean hasAttr = false;
        for (DBSAttributeBase attr : getAllAttributes(monitor, object)) {
            if (hasAttr) sql.append(", ");
            sql.append(DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML));
            hasAttr = true;
        }
        sql.append(getLineSeparator()).append("FROM ").append(getEntityName(getSingleEntity()));
        sql.append(getLineSeparator()).append("WHERE ");
        boolean multiKey = keyAttributes.size() > 1;
        if (multiKey) sql.append("(");
        hasAttr = false;
        for (DBDAttributeBinding binding : keyAttributes) {
            if (hasAttr) sql.append(",");
            sql.append(DBUtils.getObjectFullName(binding.getAttribute(), DBPEvaluationContext.DML));
            hasAttr = true;
        }
        if (multiKey) sql.append(")");
        sql.append(" IN (");
        if (multiKey) sql.append("\n");
        List<ResultSetRow> selectedRows = getSelectedRows();
        for (int i = 0; i < selectedRows.size(); i++) {
            ResultSetRow firstRow = selectedRows.get(i);
            if (multiKey) sql.append("(");
            hasAttr = false;
            for (DBDAttributeBinding binding : keyAttributes) {
                if (hasAttr) sql.append(",");
                appendAttributeValue(getController(), sql, binding, firstRow);
                hasAttr = true;
            }
            if (multiKey) sql.append(")");
            if (i < selectedRows.size() - 1) sql.append(",");
            if (multiKey) sql.append("\n");
        }
        sql.append(");\n");
    }
}
