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

package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPOverloadedObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TiberoTableIndex extends JDBCTableIndex<TiberoSchema, TiberoTable>
    implements DBPScriptObject, DBPOverloadedObject {

    private final String ownerName;
    private final boolean nonUnique;
    private final boolean tableNameDescription;
    private List<TiberoTableIndexColumn> columns;
    private String indexDDL;

    public TiberoTableIndex(
        @NotNull TiberoSchema schema,
        @NotNull TiberoTable table,
        @NotNull String ownerName,
        @NotNull String indexName,
        boolean nonUnique,
        @NotNull DBSIndexType indexType,
        boolean tableNameDescription
    ) {
        super(schema, table, indexName, indexType, true);
        this.ownerName = ownerName;
        this.nonUnique = nonUnique;
        this.tableNameDescription = tableNameDescription;
    }

    @NotNull
    @Override
    public TiberoDataSource getDataSource() {
        return getTable().getDataSource();
    }

    @Override
    public boolean isUnique() {
        return !nonUnique;
    }

    @Override
    public boolean isPrimary() {
        return false;
    }

    @Nullable
    @Override
    public String getDescription() {
        return tableNameDescription ? getTable().getName() : null;
    }

    @NotNull
    @Override
    public String getOverloadedName() {
        return getName();
    }

    @Nullable
    @Override
    public List<TiberoTableIndexColumn> getAttributeReferences(@Nullable DBRProgressMonitor monitor) {
        return columns;
    }

    public void setColumns(@NotNull List<TiberoTableIndexColumn> columns) {
        this.columns = columns;
    }

    public void addColumn(@NotNull TiberoTableIndexColumn column) {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        columns.add(column);
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), getContainer(), this);
    }

    @NotNull
    @Override
    public String getObjectDefinitionText(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Map<String, Object> options
    ) throws DBException {
        if (indexDDL == null || CommonUtils.getOption(options, OPTION_REFRESH)) {
            indexDDL = TiberoUtils.loadObjectDDL(monitor, this, "INDEX", ownerName);
            if (indexDDL.startsWith("-- DDL not available")) {
                indexDDL = buildIndexDDL();
            }
        }
        return indexDDL;
    }

    @NotNull
    private String buildIndexDDL() {
        StringBuilder ddl = new StringBuilder("CREATE ");
        if (isUnique()) {
            ddl.append("UNIQUE ");
        }
        ddl.append("INDEX ").append(getFullyQualifiedName(DBPEvaluationContext.DDL))
            .append(" ON ").append(getTable().getFullyQualifiedName(DBPEvaluationContext.DDL))
            .append(" (");
        if (columns != null) {
            boolean first = true;
            for (TiberoTableIndexColumn column : columns) {
                if (!first) {
                    ddl.append(", ");
                }
                first = false;
                ddl.append(DBUtils.getQuotedIdentifier(column.getTableColumn()));
                if (!column.isAscending()) {
                    ddl.append(" DESC");
                }
            }
        }
        ddl.append(")");
        return ddl.toString();
    }
}
