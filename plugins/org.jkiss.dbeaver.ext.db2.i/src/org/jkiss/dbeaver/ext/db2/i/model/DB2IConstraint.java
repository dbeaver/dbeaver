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
package org.jkiss.dbeaver.ext.db2.i.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableCheckConstraint;

public class DB2IConstraint extends GenericUniqueKey implements DBSTableCheckConstraint {

    private String checkClause;

    DB2IConstraint(GenericTableBase table, String name, @Nullable String remarks, DBSEntityConstraintType constraintType, boolean persisted, @Nullable String checkClause) {
        super(table, name, remarks, constraintType, persisted);
        this.checkClause = checkClause;
    }

    @Override
    @Nullable
    @Property(viewable = true, order = 4)
    public String getCheckConstraintDefinition() {
        return checkClause;
    }

    @Override
    public void setCheckConstraintDefinition(String expression) {
        this.checkClause = expression;
    }
}
