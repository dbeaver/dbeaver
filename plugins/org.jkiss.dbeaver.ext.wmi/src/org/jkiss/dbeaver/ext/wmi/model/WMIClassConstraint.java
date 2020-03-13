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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.Collections;
import java.util.List;

/**
 * Class constraint
 */
public class WMIClassConstraint implements DBSEntityConstraint, DBSEntityReferrer, DBSEntityAttributeRef
{
    private final WMIClass owner;
    private final WMIClassAttribute key;

    public WMIClassConstraint(WMIClass owner, WMIClassAttribute key)
    {
        this.owner = owner;
        this.key = key;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @NotNull
    @Override
    public DBSEntity getParentObject()
    {
        return owner;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return owner.getDataSource();
    }

    @NotNull
    @Override
    public DBSEntityConstraintType getConstraintType()
    {
        return DBSEntityConstraintType.UNIQUE_KEY;
    }

    @NotNull
    @Override
    public String getName()
    {
        return key.getName();
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @Override
    public List<? extends DBSEntityAttributeRef> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return Collections.singletonList(this);
    }

    @NotNull
    @Override
    public DBSEntityAttribute getAttribute()
    {
        return key;
    }
}
