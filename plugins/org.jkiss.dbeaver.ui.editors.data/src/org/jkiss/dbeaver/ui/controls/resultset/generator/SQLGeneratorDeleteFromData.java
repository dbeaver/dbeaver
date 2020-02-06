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
package org.jkiss.dbeaver.ui.controls.resultset.generator;

import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;

import java.util.Collection;

public class SQLGeneratorDeleteFromData extends ResultSetAnalysisRunner {

    @Override
    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, IResultSetController object) {
        for (ResultSetRow firstRow : getSelectedRows()) {

            Collection<DBDAttributeBinding> keyAttributes = getKeyAttributes(monitor, object);
            sql.append("DELETE FROM ").append(getEntityName(getSingleEntity()));
            sql.append(getLineSeparator()).append("WHERE ");
            boolean hasAttr = false;
            for (DBDAttributeBinding binding : keyAttributes) {
                if (hasAttr) sql.append(" AND ");
                appendValueCondition(getController(), sql, binding, firstRow);
                hasAttr = true;
            }
            sql.append(";\n");
        }
    }
}
