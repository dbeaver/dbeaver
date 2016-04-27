/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.Collections;
import java.util.List;

/**
 * Pseudo attribute
 */
public class DBDPseudoReferrer implements DBSEntityReferrer, DBSEntityAttributeRef {

    private final DBSEntity entity;
    private final DBDAttributeBinding binding;

    public DBDPseudoReferrer(DBSEntity entity, DBDAttributeBinding binding) {
        this.entity = entity;
        this.binding = binding;
    }

    @Override
    public List<? extends DBSEntityAttributeRef> getAttributeReferences(DBRProgressMonitor monitor) throws DBException
    {
        return Collections.singletonList(this);
    }

    @Nullable
    @Override
    public String getDescription()
    {
        final DBSEntityAttribute attribute = getAttribute();
        return attribute == null ? null : attribute.getDescription();
    }

    @NotNull
    @Override
    public DBSEntity getParentObject()
    {
        return entity;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return entity.getDataSource();
    }

    @NotNull
    @Override
    public DBSEntityConstraintType getConstraintType()
    {
        return DBSEntityConstraintType.PSEUDO_KEY;
    }

    @NotNull
    @Override
    public String getName()
    {
        return DBSEntityConstraintType.PSEUDO_KEY.getName();
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @NotNull
    @Override
    public DBSEntityAttribute getAttribute()
    {
        return binding.getEntityAttribute();
    }
}
