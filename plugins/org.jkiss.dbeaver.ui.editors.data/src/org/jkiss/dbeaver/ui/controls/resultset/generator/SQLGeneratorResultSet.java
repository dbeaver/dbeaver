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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDRowIdentifier;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.generator.SQLGeneratorBase;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class SQLGeneratorResultSet extends SQLGeneratorBase<IResultSetController> {

    public IResultSetController getController() {
        return objects.get(0);
    }

    public List<ResultSetRow> getSelectedRows() {
        return getController().getSelection().getSelectedRows();
    }

    public DBSEntity getSingleEntity() {
        return getController().getModel().getSingleSource();
    }

    protected abstract void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, IResultSetController object)
        throws DBException;

    protected Collection<? extends DBSAttributeBase> getAllAttributes(DBRProgressMonitor monitor, IResultSetController object) {
        return object.getModel().getVisibleAttributes();
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

    protected List<DBDAttributeBinding> getKeyAttributes(DBRProgressMonitor monitor, IResultSetController object) {
        final DBDRowIdentifier rowIdentifier = getDefaultRowIdentifier(object);
        if (rowIdentifier == null) {
            return Collections.emptyList();
        }
        return rowIdentifier.getAttributes();
    }

    @Nullable
    private DBDRowIdentifier getDefaultRowIdentifier(IResultSetController object) {
        for (DBDAttributeBinding attr : object.getModel().getAttributes()) {
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
        DBSAttributeBase attribute = binding.getAttribute();
        if (attribute != null && attribute.getDataKind() == DBPDataKind.DATETIME && isUseCustomDataFormat()) {
            sql.append(
                    SQLUtils.quoteString(dataSource, SQLUtils.convertValueToSQL(dataSource, attribute, DBUtils.findValueHandler(dataSource, attribute), value, DBDDisplayFormat.UI)));
        } else {
            sql.append(
                    SQLUtils.convertValueToSQL(dataSource, attribute, value));
        }
    }

}
