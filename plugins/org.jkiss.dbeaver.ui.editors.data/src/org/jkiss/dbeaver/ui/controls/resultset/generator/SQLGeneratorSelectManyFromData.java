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
package org.jkiss.dbeaver.ui.controls.resultset.generator;

import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SQLGeneratorSelectManyFromData extends SQLGeneratorResultSet {

    @Override
    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, IResultSetController object) {
        sql.append("SELECT ");
        boolean hasAttr = false;
        for (DBSAttributeBase attr : getAllAttributes(monitor, object)) {
            if (hasAttr) sql.append(", ");
            sql.append(DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML));
            hasAttr = true;
        }

        sql.append(getLineSeparator()).append("FROM ").append(getEntityName(getSingleEntity()));

        sql.append(getLineSeparator()).append("WHERE ");
        List<DBDAttributeBinding> whereAttr = getSelectedAttributes();
        hasAttr = false;
        for (int i = 0; i < whereAttr.size(); i++) {
            if (hasAttr) sql.append(" AND ");
            appendInExpression(sql, whereAttr, i);
            hasAttr = true;
        }

        sql.append(";\n");
    }

    private void appendInExpression(StringBuilder sql, List<DBDAttributeBinding> whereAttr, int index) {
        sql.append(DBUtils.getObjectFullName(whereAttr.get(index).getAttribute(), DBPEvaluationContext.DML));

        sql.append(" IN (");
        Set<String> attrValues = new HashSet<>();
        for (ResultSetRow row : getSelectedRows()) {
            DBDAttributeBinding binding = getController().getModel().getAttributeBinding(whereAttr.get(index));
            String value = getAttributeValue(getController(), binding, row);
            attrValues.add(value);
        }
        sql.append(String.join(", ", attrValues));
        sql.append(")");
    }
}
