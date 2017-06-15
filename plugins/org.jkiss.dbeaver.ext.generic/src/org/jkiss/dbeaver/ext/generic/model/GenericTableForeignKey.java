/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableForeignKey;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDeferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericTableForeignKey
 */
public class GenericTableForeignKey extends JDBCTableForeignKey<GenericTable, GenericPrimaryKey>
{
    private DBSForeignKeyDeferability deferability;
    private List<GenericTableForeignKeyColumnTable> columns;

    public GenericTableForeignKey(
        GenericTable table,
        String name,
        @Nullable String remarks,
        GenericPrimaryKey referencedKey,
        DBSForeignKeyModifyRule deleteRule,
        DBSForeignKeyModifyRule updateRule,
        DBSForeignKeyDeferability deferability,
        boolean persisted)
    {
        super(table, name, remarks, referencedKey, deleteRule, updateRule, persisted);
        this.deferability = deferability;
    }

    @NotNull
    @Override
    public GenericDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Property(viewable = true, order = 7)
    public DBSForeignKeyDeferability getDeferability()
    {
        return deferability;
    }

    @Override
    public List<GenericTableForeignKeyColumnTable> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public void addColumn(GenericTableForeignKeyColumnTable column)
    {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        this.columns.add(column);
    }

    void setColumns(DBRProgressMonitor monitor, List<GenericTableForeignKeyColumnTable> columns)
    {
        this.columns = columns;
        final List<GenericTableConstraintColumn> refColumns = referencedKey.getAttributeReferences(monitor);
        if (refColumns != null && this.columns.size() > refColumns.size()) {
            // [JDBC: Progress bug. All FK columns are duplicated]
            for (int i = 0; i < this.columns.size(); ) {
                boolean duplicate = false;
                String colName = this.columns.get(i).getName();
                for (int k = i + 1; k < this.columns.size(); k++) {
                    String colName2 = this.columns.get(k).getName();
                    if (CommonUtils.equalObjects(colName, colName2)) {
                        this.columns.remove(k);
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    i++;
                }
            }
        }
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getCatalog(),
            getTable().getSchema(),
            getTable(),
            this);
    }
}
