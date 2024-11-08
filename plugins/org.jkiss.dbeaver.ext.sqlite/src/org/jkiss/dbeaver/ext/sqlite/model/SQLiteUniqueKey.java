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
package org.jkiss.dbeaver.ext.sqlite.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableConstraintColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.DBPInheritedObject;

import java.util.List;

public class SQLiteUniqueKey extends GenericUniqueKey implements DBPInheritedObject {

    public SQLiteUniqueKey(GenericTableBase table, String name, @Nullable String remarks, DBSEntityConstraintType constraintType, boolean persisted) {
        super(table, name, remarks, constraintType, persisted);
    }

    @Override
    public boolean isInherited() {
        /*
         * FIX: Prevent SQLITE ERROR "table has more than one primary key" when the primary key is an autoincrement (#18491)
         * Temporary workaround - To avoid generating a separate DDL for the constraint when the primary key is auto-incremented,
         * we treat the constraint as "inherited". This is not an ideal solution, as the constraint is not truly inherited.
         */
        List<GenericTableConstraintColumn> columns = this.getAttributeReferences(new VoidProgressMonitor());
        if (columns.size() == 1) {
            return columns.get(0).getAttribute().isAutoIncrement();
        }
        return false;
    }

}
