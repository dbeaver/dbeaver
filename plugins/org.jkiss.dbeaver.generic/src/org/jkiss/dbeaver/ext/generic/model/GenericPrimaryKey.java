/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * GenericConstraint
 */
public class GenericPrimaryKey extends GenericConstraint
{
    private List<GenericConstraintColumn> columns;

    public GenericPrimaryKey(GenericTable table, String name, String remarks, DBSConstraintType constraintType, boolean persisted)
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
            this.columns = new ArrayList<GenericConstraintColumn>(constraint.columns.size());
            for (GenericConstraintColumn sourceColumn : constraint.columns) {
                this.columns.add(new GenericConstraintColumn(this, sourceColumn));
            }
        }
    }

    public List<GenericConstraintColumn> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    void addColumn(GenericConstraintColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<GenericConstraintColumn>();
        }
        this.columns.add(column);
    }

    void setColumns(List<GenericConstraintColumn> columns)
    {
        this.columns = columns;
        if (!CommonUtils.isEmpty(this.columns) && this.columns.size() > 1) {
            Collections.sort(columns, new Comparator<GenericConstraintColumn>() {
                public int compare(GenericConstraintColumn o1, GenericConstraintColumn o2)
                {
                    return o1.getOrdinalPosition() - o2.getOrdinalPosition();
                }
            });
        }
    }

    public boolean hasColumn(GenericTableColumn column)
    {
        if (this.columns != null) {
            for (GenericConstraintColumn constColumn : columns) {
                if (constColumn.getTableColumn() == column) {
                    return true;
                }
            }
        }
        return false;
    }
}