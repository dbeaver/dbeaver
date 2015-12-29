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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;

import java.util.List;

/**
 * PostgreTableForeignKey
 */
public class PostgreTableForeignKey extends PostgreTableConstraintBase implements DBSTableForeignKey
{
    private List<PostgreTableForeignKeyColumnTable> columns;

    public PostgreTableForeignKey(
        PostgreTable table,
        String name,
        JDBCResultSet resultSet) throws DBException {
        super(table, name, resultSet);
    }

    @Override
    public DBSEntity getAssociatedEntity() {
        return null;
    }

    @Override
    public DBSTableConstraint getReferencedConstraint() {
        return null;
    }

    @Override
    public DBSForeignKeyModifyRule getDeleteRule() {
        return null;
    }

    @Override
    public DBSForeignKeyModifyRule getUpdateRule() {
        return null;
    }

/*
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
*/
}
