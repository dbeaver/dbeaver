/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
