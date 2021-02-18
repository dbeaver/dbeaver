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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.impl.sql.RelationalSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.utils.CommonUtils;

class ResultSetContextImpl implements IResultSetContext {
    private final ResultSetViewer viewer;
    private final DBCResultSet resultSet;

    ResultSetContextImpl(ResultSetViewer viewer, DBCResultSet resultSet) {
        this.viewer = viewer;
        this.resultSet = resultSet;
    }

    @Override
    public boolean supportsAttributes() {
        DBDAttributeBinding[] attrs = viewer.getModel().getAttributes();
        return attrs.length > 0 &&
            (attrs[0].getDataKind() != DBPDataKind.DOCUMENT || !CommonUtils.isEmpty(attrs[0].getNestedBindings()));
    }

    @Override
    public boolean supportsDocument() {
        return viewer.getModel().getDocumentAttribute() != null;
    }

    @Override
    public boolean supportsGrouping() {
        DBPDataSource dataSource = viewer.getDataSource();
        if (dataSource != null) {
            SQLDialect sqlDialect = dataSource.getSQLDialect();
            return sqlDialect instanceof RelationalSQLDialect && ((RelationalSQLDialect) sqlDialect).supportsGroupBy();
        }
        return false;
    }

    @Override
    public boolean supportsReferences() {
        DBPDataSource dataSource = viewer.getDataSource();
        if (dataSource != null) {
            return dataSource.getInfo().supportsReferentialIntegrity();
        }
        return false;
    }

    @Override
    public String getDocumentContentType() {
        DBDAttributeBinding docAttr = viewer.getModel().getDocumentAttribute();
        return docAttr == null ? null : docAttr.getValueHandler().getValueContentType(docAttr);
    }

    @Override
    public DBCResultSet getResultSet() {
        return resultSet;
    }

    @Override
    public boolean hasAttributeOfType(String typeName) {
        for (DBDAttributeBinding attr : viewer.getModel().getAttributes()) {
            DBDValueHandler valueHandler = attr.getValueHandler();
            if (valueHandler != null) {
                Class<?> objectType = valueHandler.getValueObjectType(attr.getAttribute());
                if (objectType != null && objectType.getName().equals(typeName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
