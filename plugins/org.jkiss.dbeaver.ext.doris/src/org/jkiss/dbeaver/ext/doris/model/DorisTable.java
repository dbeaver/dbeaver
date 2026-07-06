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
package org.jkiss.dbeaver.ext.doris.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.doris.DorisUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * Doris Table - represents a table within a Doris database.
 */
public class DorisTable extends GenericTable {

    private static final String COL_CREATE_TABLE = "Create Table"; //$NON-NLS-1$

    private String ddl;

    public DorisTable(
        @NotNull GenericStructContainer container,
        @Nullable String tableName,
        @Nullable String tableType,
        @Nullable JDBCResultSet dbResult
    ) {
        super(container, tableName, tableType, dbResult);
    }

    @NotNull
    @Override
    public DorisDataSource getDataSource() {
        return (DorisDataSource) super.getDataSource();
    }

    @Nullable
    @Override
    public GenericCatalog getCatalog() {
        return DorisObjectNameUtils.getCatalog(getContainer());
    }

    @Nullable
    @Override
    public GenericSchema getSchema() {
        return DorisObjectNameUtils.getSchema(getContainer());
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        return DorisObjectNameUtils.getFullyQualifiedName(getDataSource(), getCatalog(), getSchema(), this);
    }

    @Override
    public boolean isView() {
        return false;
    }

    @Nullable
    @Override
    public String getDDL() {
        return ddl;
    }

    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
        if (CommonUtils.getOption(options, DBPScriptObject.OPTION_REFRESH)) {
            ddl = null;
        }
        if (ddl == null && isPersisted()) {
            loadDDL(monitor);
        }
        return ddl != null ? ddl : "";
    }

    private void loadDDL(@NotNull DBRProgressMonitor monitor) throws DBCException {
        ddl = DorisUtils.loadShowCreateDDL(
            monitor, this, "Load table DDL", "SHOW CREATE TABLE", COL_CREATE_TABLE); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
