/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableForeignKey;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;

import java.util.ArrayList;
import java.util.List;

/**
 * PostgreTableForeignKey
 */
public class PostgreTableForeignKey extends JDBCTableForeignKey<PostgreTable, PostgreTableConstraint>
{
    private List<PostgreTableForeignKeyColumnTable> columns;

    public PostgreTableForeignKey(
        PostgreTable table,
        String name,
        String remarks,
        PostgreTableConstraint referencedKey,
        DBSForeignKeyModifyRule deleteRule,
        DBSForeignKeyModifyRule updateRule,
        boolean persisted)
    {
        super(table, name, remarks, referencedKey, deleteRule, updateRule, persisted);
    }

    @Override
    public List<PostgreTableForeignKeyColumnTable> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public void addColumn(PostgreTableForeignKeyColumnTable column)
    {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        columns.add(column);
    }

    @NotNull
    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            getTable(),
            this);
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource()
    {
        return getTable().getDataSource();
    }
}
