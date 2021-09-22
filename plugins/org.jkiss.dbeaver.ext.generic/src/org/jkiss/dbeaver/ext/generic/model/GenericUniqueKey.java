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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * GenericTableConstraint
 */
public class GenericUniqueKey extends GenericTableConstraint {
    private List<GenericTableConstraintColumn> columns;

    public GenericUniqueKey(GenericTableBase table, String name, @Nullable String remarks, DBSEntityConstraintType constraintType, boolean persisted) {
        super(table, name, remarks, constraintType, persisted);
    }

    /**
     * Copy constructor
     *
     * @param constraint
     */
    GenericUniqueKey(GenericUniqueKey constraint) {
        super(constraint.getTable(), constraint.getName(), constraint.getDescription(), constraint.getConstraintType(), constraint.isPersisted());
        if (constraint.columns != null) {
            this.columns = new ArrayList<>(constraint.columns.size());
            for (GenericTableConstraintColumn sourceColumn : constraint.columns) {
                this.columns.add(new GenericTableConstraintColumn(this, sourceColumn));
            }
        }
    }

    @Override
    public List<GenericTableConstraintColumn> getAttributeReferences(DBRProgressMonitor monitor) {
        return columns;
    }

    public void addColumn(GenericTableConstraintColumn column) {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        this.columns.add(column);
    }

    public void setColumns(List<GenericTableConstraintColumn> columns) {
        this.columns = columns;
        if (!CommonUtils.isEmpty(this.columns) && this.columns.size() > 1) {
            columns.sort(Comparator.comparingInt(GenericTableConstraintColumn::getOrdinalPosition));
        }
    }

    public boolean hasColumn(GenericTableColumn column) {
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