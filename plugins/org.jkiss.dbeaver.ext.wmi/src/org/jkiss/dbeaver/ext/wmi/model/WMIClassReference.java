/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.wmi.service.WMIObjectAttribute;

/**
 * Class association
 */
public class WMIClassReference extends WMIClassAttribute implements DBSEntityAssociation
{
    private WMIClass refClass;

    protected WMIClassReference(WMIClass wmiClass, WMIObjectAttribute attribute, WMIClass refClass)
    {
        super(wmiClass, attribute);
        this.refClass = refClass;
    }

    @NotNull
    @Override
    public DBSEntityConstraintType getConstraintType()
    {
        return DBSEntityConstraintType.ASSOCIATION;
    }

    @Override
    public DBSEntity getAssociatedEntity()
    {
        return refClass;
    }

    @Nullable
    @Override
    public DBSEntityConstraint getReferencedConstraint()
    {
        return null;
    }

}
