/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
