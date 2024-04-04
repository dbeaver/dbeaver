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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt2;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstrainable;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintInfo;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generic table
 */
public class GenericTable extends GenericTableBase implements DBPScriptObjectExt2, DBSEntityConstrainable {

    private String ddl;

    public GenericTable(
        GenericStructContainer container,
        @Nullable String tableName,
        @Nullable String tableType,
        @Nullable JDBCResultSet dbResult)
    {
        super(container, tableName, tableType, dbResult);
    }

    // Constructor for tests
    public GenericTable(
        @NotNull GenericStructContainer container,
        @Nullable String tableName,
        @NotNull String tableCatalogName,
        @NotNull String tableSchemaName
    ) {
        super(container, tableName, "TABLE", tableCatalogName, tableSchemaName);
    }

    @Override
    public boolean isView() {
        return false;
    }

    public String getDDL() {
        return ddl;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (CommonUtils.getOption(options, DBPScriptObject.OPTION_REFRESH)) {
            ddl = null;
        }
        if (!isPersisted()) {
            return DBStructUtils.generateTableDDL(monitor, this, options, false);
        }

        if (ddl == null || !isCacheDDL()) {
            ddl = getDataSource().getMetaModel().getTableDDL(monitor, this, options);
        }
        return ddl;
    }

    protected boolean isCacheDDL() {
        return true;
    }

    @Override
    public boolean supportsObjectDefinitionOption(String option) {
        if (OPTION_DDL_ONLY_FOREIGN_KEYS.equals(option) || OPTION_DDL_SKIP_FOREIGN_KEYS.equals(option)) {
            // DDL split supported only by base meta model
            return !isPersisted() || getDataSource().getMetaModel().supportsTableDDLSplit(this);
        }
        return false;
    }

    @NotNull
    @Override
    public List<DBSEntityConstraintInfo> getSupportedConstraints() {
        boolean isSupportCheckConstraint = getDataSource().getMetaModel().supportsCheckConstraints();
        List<DBSEntityConstraintInfo> result = new ArrayList<>();
        result.add(DBSEntityConstraintInfo.of(DBSEntityConstraintType.PRIMARY_KEY, GenericUniqueKey.class));
        if (getDataSource().getMetaModel().supportsUniqueKeys()) {
            result.add(DBSEntityConstraintInfo.of(DBSEntityConstraintType.UNIQUE_KEY, GenericUniqueKey.class));
        }
        if (isSupportCheckConstraint) {
            result.add(DBSEntityConstraintInfo.of(DBSEntityConstraintType.CHECK, GenericTableConstraint.class));
        }
        return result;
    }
}
