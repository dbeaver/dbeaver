/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKeyColumn;

import java.util.List;

/**
 * DB2 Table Foreign Key Column
 */
public class DB2TableForeignKeyColumn extends DB2TableKeyColumn implements DBSTableForeignKeyColumn {

    public DB2TableForeignKeyColumn(AbstractTableConstraint<DB2Table, ? extends DB2TableKeyColumn> constraint, DB2TableColumn tableColumn, Integer ordinalPosition) {
        super(constraint, tableColumn, ordinalPosition);
    }

    @Override
    @Property(id = "reference", viewable = true, order = 4)
    public DB2TableColumn getReferencedColumn() {
        DB2TableUniqueKey referencedConstraint = ((DB2TableForeignKey) getParentObject()).getReferencedConstraint();
        if (referencedConstraint != null) {
            List<DB2TableKeyColumn> ar = referencedConstraint.getAttributeReferences(new VoidProgressMonitor());
            if (ar != null) {
                return ar.get(getOrdinalPosition() - 1).getAttribute();
            }
        }
        return null;
    }

}
