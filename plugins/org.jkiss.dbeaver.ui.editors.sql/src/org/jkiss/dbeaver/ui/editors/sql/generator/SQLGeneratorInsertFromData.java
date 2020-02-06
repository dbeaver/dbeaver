/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql.generator;

import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;

import java.util.Collection;
import java.util.List;

class SQLGeneratorInsertFromData extends ResultSetAnalysisRunner {
    private final IResultSetController rsv;
    private final List<ResultSetRow> selectedRows;
    private final DBSEntity entity;

    public SQLGeneratorInsertFromData(DBSDataContainer dataContainer, IResultSetController rsv, List<ResultSetRow> selectedRows, DBSEntity entity) {
        super(dataContainer.getDataSource(), rsv.getModel());
        this.rsv = rsv;
        this.selectedRows = selectedRows;
        this.entity = entity;
    }

    @Override
    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, ResultSetModel object) {
        for (ResultSetRow firstRow : selectedRows) {

            Collection<? extends DBSAttributeBase> allAttributes = getAllAttributes(monitor, object);
            sql.append("INSERT INTO ").append(getEntityName(entity));
            sql.append(getLineSeparator()).append("(");
            boolean hasAttr = false;
            for (DBSAttributeBase attr : allAttributes) {
                if (DBUtils.isPseudoAttribute(attr) || DBUtils.isHiddenObject(attr)) {
                    continue;
                }
                if (hasAttr) sql.append(", ");
                sql.append(DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML));
                hasAttr = true;
            }
            sql.append(")").append(getLineSeparator()).append("VALUES(");
            hasAttr = false;
            for (DBSAttributeBase attr : allAttributes) {
                if (DBUtils.isPseudoAttribute(attr) || DBUtils.isHiddenObject(attr)) {
                    continue;
                }
                if (hasAttr) sql.append(", ");
                DBDAttributeBinding binding = rsv.getModel().getAttributeBinding(attr);
                if (binding == null) {
                    appendDefaultValue(sql, attr);
                } else {
                    appendAttributeValue(rsv, sql, binding, firstRow);
                }
                hasAttr = true;
            }
            sql.append(");\n");
        }
    }
}
