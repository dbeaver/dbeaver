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
