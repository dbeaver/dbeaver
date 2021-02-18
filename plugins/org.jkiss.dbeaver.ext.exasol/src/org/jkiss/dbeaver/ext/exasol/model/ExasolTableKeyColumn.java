/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraintColumn;
import org.jkiss.dbeaver.model.meta.Property;


/**
 * @author Karl Griesser
 */
public class ExasolTableKeyColumn extends AbstractTableConstraintColumn {

    private AbstractTableConstraint<ExasolTable> constraint;
    private ExasolTableColumn tableColumn;
    private Integer ordinalPosition;


    // -----------------
    // Constructors
    // -----------------
    public ExasolTableKeyColumn(AbstractTableConstraint<ExasolTable> constraint, ExasolTableColumn tableColumn, Integer ordinalPosition) {

        this.constraint = constraint;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
    }

    @Override
    public AbstractTableConstraint<ExasolTable> getParentObject() {
        return constraint;
    }

    @Override
    @NotNull
    public DBPDataSource getDataSource() {
        return constraint.getTable().getDataSource();
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    public String getName() {
        return tableColumn.getName();
    }

    @NotNull
    @Override
    @Property(id = "name", viewable = true, order = 1)
    public ExasolTableColumn getAttribute() {
        return tableColumn;
    }

    @Override
    @Property(viewable = true, editable = false, order = 3)
    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    @Nullable
    @Override
    public String getDescription() {
        return tableColumn.getDescription();
    }


}
