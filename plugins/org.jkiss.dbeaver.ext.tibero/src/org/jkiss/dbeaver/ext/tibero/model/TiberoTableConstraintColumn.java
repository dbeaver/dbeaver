/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraintColumn;
import org.jkiss.dbeaver.model.meta.Property;

public class TiberoTableConstraintColumn extends AbstractTableConstraintColumn {

    private final AbstractTableConstraint<TiberoTable, TiberoTableConstraintColumn> constraint;
    private final TiberoTableColumn tableColumn;
    private final int ordinalPosition;

    public TiberoTableConstraintColumn(
        @NotNull AbstractTableConstraint<TiberoTable, TiberoTableConstraintColumn> constraint,
        @NotNull TiberoTableColumn tableColumn,
        int ordinalPosition
    ) {
        this.constraint = constraint;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return tableColumn.getName();
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 1)
    public TiberoTableColumn getAttribute() {
        return tableColumn;
    }

    @Override
    @Property(viewable = false, order = 2)
    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    @Nullable
    @Override
    public String getDescription() {
        return tableColumn.getDescription();
    }

    @Override
    public AbstractTableConstraint<TiberoTable, TiberoTableConstraintColumn> getParentObject() {
        return constraint;
    }

    @NotNull
    @Override
    public TiberoDataSource getDataSource() {
        return constraint.getTable().getDataSource();
    }
}
