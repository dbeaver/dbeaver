/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

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

    public GenericPrimaryKey(GenericTable table, String name, String remarks, DBSEntityConstraintType constraintType, boolean persisted)
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
            this.columns = new ArrayList<GenericTableConstraintColumn>(constraint.columns.size());
            for (GenericTableConstraintColumn sourceColumn : constraint.columns) {
                this.columns.add(new GenericTableConstraintColumn(this, sourceColumn));
            }
        }
    }

    public List<GenericTableConstraintColumn> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public void addColumn(GenericTableConstraintColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<GenericTableConstraintColumn>();
        }
        this.columns.add(column);
    }

    void setColumns(List<GenericTableConstraintColumn> columns)
    {
        this.columns = columns;
        if (!CommonUtils.isEmpty(this.columns) && this.columns.size() > 1) {
            Collections.sort(columns, new Comparator<GenericTableConstraintColumn>() {
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
                if (constColumn.getTableColumn() == column) {
                    return true;
                }
            }
        }
        return false;
    }
}