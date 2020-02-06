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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDRowIdentifier;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.generator.BaseAnalysisRunner;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class ResultSetAnalysisRunner extends BaseAnalysisRunner<ResultSetModel> {

    ResultSetAnalysisRunner(DBPDataSource dataSource, ResultSetModel model)
    {
        super(Collections.singletonList(model));
    }

    protected abstract void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, ResultSetModel object)
        throws DBException;

    protected Collection<? extends DBSAttributeBase> getAllAttributes(DBRProgressMonitor monitor, ResultSetModel object) {
        return object.getVisibleAttributes();
    }

    void appendValueCondition(IResultSetController rsv, StringBuilder sql, DBDAttributeBinding binding, ResultSetRow firstRow) {
        Object value = rsv.getModel().getCellValue(binding, firstRow);
        sql.append(DBUtils.getObjectFullName(binding.getAttribute(), DBPEvaluationContext.DML));
        if (DBUtils.isNullValue(value)) {
            sql.append(" IS NULL");
        } else {
            sql.append("=");
            appendAttributeValue(rsv, sql, binding, firstRow);
        }
    }

    protected List<DBDAttributeBinding> getKeyAttributes(DBRProgressMonitor monitor, ResultSetModel object) {
        final DBDRowIdentifier rowIdentifier = getDefaultRowIdentifier(object);
        if (rowIdentifier == null) {
            return Collections.emptyList();
        }
        return rowIdentifier.getAttributes();
    }

    @Nullable
    private DBDRowIdentifier getDefaultRowIdentifier(ResultSetModel object) {
        for (DBDAttributeBinding attr : object.getAttributes()) {
            DBDRowIdentifier rowIdentifier = attr.getRowIdentifier();
            if (rowIdentifier != null) {
                return rowIdentifier;
            }
        }
        return null;
    }

    protected void appendAttributeValue(IResultSetController rsv, StringBuilder sql, DBDAttributeBinding binding, ResultSetRow row)
    {
        DBPDataSource dataSource = binding.getDataSource();
        Object value = rsv.getModel().getCellValue(binding, row);
        sql.append(
            SQLUtils.convertValueToSQL(dataSource, binding.getAttribute(), value));
    }

}
