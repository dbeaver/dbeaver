/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraintColumn;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * DB2 Table Constraint Column
 * 
 * @author Denis Forveille
 */
public class DB2TableKeyColumn extends AbstractTableConstraintColumn {

    private AbstractTableConstraint<DB2Table> constraint;
    private DB2TableColumn tableColumn;
    private Integer ordinalPosition;

    // -----------------
    // Constructors
    // -----------------

    public DB2TableKeyColumn(AbstractTableConstraint<DB2Table> constraint, DB2TableColumn tableColumn, Integer ordinalPosition)
    {
        this.constraint = constraint;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
    }

    @Override
    public AbstractTableConstraint<DB2Table> getParentObject()
    {
        return constraint;
    }

    @NotNull
    @Override
    public DB2DataSource getDataSource()
    {
        return constraint.getTable().getDataSource();
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    public String getName()
    {
        return tableColumn.getName();
    }

    @NotNull
    @Override
    @Property(id = "name", viewable = true, order = 1)
    public DB2TableColumn getAttribute()
    {
        return tableColumn;
    }

    @Override
    @Property(viewable = true, editable = false, order = 3)
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return tableColumn.getDescription();
    }

}
