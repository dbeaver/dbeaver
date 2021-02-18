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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

/**
 * GenericTableConstraint
 */
public abstract class GenericTableConstraint extends JDBCTableConstraint<GenericTableBase>
{
    protected GenericTableConstraint(GenericTableBase table, String name, String remarks, DBSEntityConstraintType constraintType, boolean persisted)
    {
        super(table, name, remarks, constraintType, persisted);
    }

    /**
     * Copy constructor
     * @param constraint source
     */
    protected GenericTableConstraint(GenericTableConstraint constraint)
    {
        super(constraint.getTable(), constraint.getName(), constraint.getDescription(), constraint.getConstraintType(), constraint.isPersisted());
    }

    @NotNull
    @Override
    public GenericDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getCatalog(),
            getTable().getSchema(),
            getTable(),
            this);
    }

}
