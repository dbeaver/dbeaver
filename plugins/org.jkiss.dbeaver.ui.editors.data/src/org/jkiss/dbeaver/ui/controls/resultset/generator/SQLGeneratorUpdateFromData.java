/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.impl.sql.ChangeTableDataStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

public class SQLGeneratorUpdateFromData extends SQLGeneratorResultSet {

    @Override
    public boolean isDMLOption() {
        return true;
    }

    @Override
    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, IResultSetController object) throws DBException {
        DBSEntity dbsEntity = getSingleEntity();
        String entityName = getEntityName(dbsEntity);
        String separator = getLineSeparator();
        for (ResultSetRow firstRow : getSelectedRows()) {
            Collection<DBDAttributeBinding> keyAttributes = getKeyAttributes(monitor, object);
            Collection<? extends DBSAttributeBase> valueAttributes = getValueAttributes(monitor, object, keyAttributes);
            if (dbsEntity instanceof ChangeTableDataStatement) {
                ChangeTableDataStatement dataStatement = (ChangeTableDataStatement) dbsEntity;
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
                DBDAttributeBinding binding = getController().getModel().getAttributeBinding(attr);
                if (binding == null) {
                    appendDefaultValue(sql, attr);
                } else {
                    appendAttributeValue(getController(), sql, binding, firstRow);
                }

                hasAttr = true;
            }
            if (!CommonUtils.isEmpty(keyAttributes)) {
                sql.append(separator).append("WHERE ");
                hasAttr = false;
                for (DBDAttributeBinding attr : keyAttributes) {
                    if (hasAttr) sql.append(" AND ");
                    appendValueCondition(getController(), sql, attr, firstRow);
                    hasAttr = true;
                }
            }
            sql.append(";\n");
        }
    }
}
