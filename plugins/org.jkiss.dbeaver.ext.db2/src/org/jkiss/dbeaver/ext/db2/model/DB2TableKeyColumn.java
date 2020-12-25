/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
