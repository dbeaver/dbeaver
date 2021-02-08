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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKeyColumn;

/**
 * MySQLTableForeignKeyColumn
 */
public class MySQLTableForeignKeyColumn extends MySQLTableConstraintColumn implements DBSTableForeignKeyColumn
{
    private MySQLTableColumn referencedColumn;

    public MySQLTableForeignKeyColumn(
        MySQLTableForeignKey constraint,
        MySQLTableColumn tableColumn,
        int ordinalPosition,
        MySQLTableColumn referencedColumn)
    {
        super(constraint, tableColumn, ordinalPosition);
        this.referencedColumn = referencedColumn;
    }

    @Override
    @Property(id = "reference", viewable = true, order = 4)
    public MySQLTableColumn getReferencedColumn()
    {
        return referencedColumn;
    }

}
