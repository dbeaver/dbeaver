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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * GenericTableConstraint
 */
public class GenericPrimaryKey extends GenericTableConstraint
{
    private List<GenericTableConstraintColumn> columns;

    public GenericPrimaryKey(GenericTable table, String name, @Nullable String remarks, DBSEntityConstraintType constraintType, boolean persisted)
    {
        super(table, name, remarks, constraintType, persisted);
    }

    /**
     * Copy constructor
     * @param constraint
     */
    GenericPrimaryKey(GenericPrimaryKey constraint)
    {
        super(constraint.getTable(), constraint.getName(), constraint.getDescription(), constraint.getConstraintType(), constraint.isPersisted());
        if (constraint.columns != null) {
            this.columns = new ArrayList<>(constraint.columns.size());
            for (GenericTableConstraintColumn sourceColumn : constraint.columns) {
                this.columns.add(new GenericTableConstraintColumn(this, sourceColumn));
            }
        }
    }

    @Override
    public List<GenericTableConstraintColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public void addColumn(GenericTableConstraintColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        this.columns.add(column);
    }

    void setColumns(List<GenericTableConstraintColumn> columns)
    {
        this.columns = columns;
        if (!CommonUtils.isEmpty(this.columns) && this.columns.size() > 1) {
            Collections.sort(columns, new Comparator<GenericTableConstraintColumn>() {
                @Override
                public int compare(GenericTableConstraintColumn o1, GenericTableConstraintColumn o2)
                {
                    return o1.getOrdinalPosition() - o2.getOrdinalPosition();
                }
            });
        }
    }

    public boolean hasColumn(GenericTableColumn column)
    {
        if (this.columns != null) {
            for (GenericTableConstraintColumn constColumn : columns) {
                if (constColumn.getAttribute() == column) {
                    return true;
                }
            }
        }
        return false;
    }
}