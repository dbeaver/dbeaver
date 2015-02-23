/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.Collection;
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
