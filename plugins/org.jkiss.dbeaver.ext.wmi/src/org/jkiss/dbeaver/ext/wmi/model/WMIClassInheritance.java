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
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

/**
 * Inheritance
 */
public class WMIClassInheritance implements DBSEntityAssociation
{

    private WMIClass superClass;
    private WMIClass subClass;

    public WMIClassInheritance(WMIClass superClass, WMIClass subClass)
    {
        this.superClass = superClass;
        this.subClass = subClass;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @NotNull
    @Override
    public String getName()
    {
        return subClass.getName() + " inherits " + superClass.getName();
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @NotNull
    @Override
    public WMIClass getParentObject()
    {
        return subClass;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return subClass.getDataSource();
    }

    @Override
    public WMIClass getAssociatedEntity()
    {
        return superClass;
    }

    @NotNull
    @Override
    public DBSEntityConstraintType getConstraintType()
    {
        return DBSEntityConstraintType.INHERITANCE;
    }

    @Nullable
    @Override
    public DBSEntityConstraint getReferencedConstraint()
    {
        return this;
    }

}
