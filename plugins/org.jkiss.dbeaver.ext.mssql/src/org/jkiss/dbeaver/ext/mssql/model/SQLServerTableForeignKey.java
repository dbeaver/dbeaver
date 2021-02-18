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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableForeignKey;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKeyColumn;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericForeignKey
 */
public class SQLServerTableForeignKey extends JDBCTableForeignKey<SQLServerTableBase, DBSEntityConstraint>
{
    private List<SQLServerTableForeignKeyColumn> columns;

    public SQLServerTableForeignKey(
        SQLServerTableBase table,
        String name,
        String remarks,
        DBSEntityConstraint referencedKey,
        DBSForeignKeyModifyRule deleteRule,
        DBSForeignKeyModifyRule updateRule,
        boolean persisted)
    {
        super(table, name, remarks, referencedKey, deleteRule, updateRule, persisted);
    }

    // Copy constructor
    public SQLServerTableForeignKey(DBRProgressMonitor monitor, SQLServerTableBase table, SQLServerTableForeignKey source) throws DBException {
        super(
            monitor,
            table,
            source,
            false);
        List<? extends DBSEntityAttributeRef> columns = source.getAttributeReferences(monitor);
        if (columns != null) {
            this.columns = new ArrayList<>(columns.size());
            for (DBSEntityAttributeRef srcCol : columns) {
                if (srcCol instanceof DBSTableForeignKeyColumn) {
                    DBSTableForeignKeyColumn fkCol = (DBSTableForeignKeyColumn) srcCol;
                    this.columns.add(new SQLServerTableForeignKeyColumn(
                        this,
                        table.getAttribute(monitor, fkCol.getName()),
                        this.columns.size(),
                        table.getAttribute(monitor, fkCol.getReferencedColumn().getName())));
                }
            }
        }
    }

    @Override
    public List<SQLServerTableForeignKeyColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public void addColumn(SQLServerTableForeignKeyColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        columns.add(column);
    }

    public void setColumns(List<SQLServerTableForeignKeyColumn> columns) {
        this.columns = columns;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            getTable(),
            this);
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource()
    {
        return getTable().getDataSource();
    }
}
