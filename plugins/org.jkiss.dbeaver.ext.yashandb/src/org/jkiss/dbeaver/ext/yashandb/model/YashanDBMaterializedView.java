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
package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleMaterializedView;
import org.jkiss.dbeaver.ext.oracle.model.OraclePrivTable;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * YashanDBMaterializedView
 */
public class YashanDBMaterializedView extends OracleMaterializedView {

    public YashanDBMaterializedView(OracleSchema schema, ResultSet dbResult) {
        super(schema, dbResult);
    }

    public YashanDBMaterializedView(OracleSchema schema, String name) {
        super(schema, name);
    }

    @Override
    public String getComment(DBRProgressMonitor monitor) {
        return super.getComment();
    }

    @Override
    public Collection<OraclePrivTable> getTablePrivs(DBRProgressMonitor monitor) throws DBException {
        // YashanDB not support yet.
        return Collections.emptyList();
    }

    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options)
            throws DBException {
        if (query != null) {
            return query;
        }
        return YashanDBUtils.getTableOrViewDDL(monitor, getTableTypeName(), this, options);
    }

    @Override
    public DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor) {
        // YashanDB not support
        return new DBEPersistAction[] {};
    }

    @Override
    protected String getTableTypeName() {
        return "MATERIALIZED VIEW";
    }

}
