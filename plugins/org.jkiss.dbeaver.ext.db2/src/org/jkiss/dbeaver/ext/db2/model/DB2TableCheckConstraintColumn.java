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
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableCheckConstraintColUsage;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraintColumn;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * DB2 Table Constraint Column
 * 
 * @author Denis Forveille
 */
public class DB2TableCheckConstraintColumn extends AbstractTableConstraintColumn {

    private AbstractTableConstraint<DB2Table> constraint;
    private DB2TableColumn tableColumn;
    private DB2TableCheckConstraintColUsage usage;

    // -----------------
    // Constructors
    // -----------------

    public DB2TableCheckConstraintColumn(AbstractTableConstraint<DB2Table> constraint, DB2TableColumn tableColumn,
        DB2TableCheckConstraintColUsage usage)
    {
        this.constraint = constraint;
        this.tableColumn = tableColumn;
        this.usage = usage;
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

    // Check Constraint columns do not have ordinal position...
    @Override
    @Property(hidden = true)
    public int getOrdinalPosition()
    {
        return 0;
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

    @Property(viewable = true, order = 2)
    public DB2TableCheckConstraintColUsage getUsage()
    {
        return usage;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return tableColumn.getDescription();
    }

}
