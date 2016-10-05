/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericPrimaryKey
 */
public class MySQLTableConstraint extends JDBCTableConstraint<MySQLTable> {
    private List<MySQLTableConstraintColumn> columns;

    public MySQLTableConstraint(MySQLTable table, String name, String remarks, DBSEntityConstraintType constraintType, boolean persisted)
    {
        super(table, name, remarks, constraintType, persisted);
    }

    // Copy constructor
    protected MySQLTableConstraint(DBRProgressMonitor monitor, MySQLTable table, DBSEntityConstraint source) throws DBException {
        super(table, source, false);
        if (source instanceof DBSEntityReferrer) {
            List<? extends DBSEntityAttributeRef> columns = ((DBSEntityReferrer) source).getAttributeReferences(monitor);
            if (columns != null) {
                this.columns = new ArrayList<>(columns.size());
                for (DBSEntityAttributeRef col : columns) {
                    if (col.getAttribute() != null) {
                        MySQLTableColumn ownCol = table.getAttribute(monitor, col.getAttribute().getName());
                        this.columns.add(new MySQLTableConstraintColumn(this, ownCol, col.getAttribute().getOrdinalPosition()));
                    }
                }
            }
        }
    }

    @Override
    public List<MySQLTableConstraintColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public void addColumn(MySQLTableConstraintColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        this.columns.add(column);
    }

    void setColumns(List<MySQLTableConstraintColumn> columns)
    {
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
    public MySQLDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

}
