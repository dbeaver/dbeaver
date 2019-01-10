/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Logical foreign key
 */
public class ERDLogicalAssociation implements DBSEntityAssociation, DBSEntityReferrer {

    private DBSEntity entity;
    private String name;
    private String description;
    private ERDLogicalPrimaryKey pk;

    public ERDLogicalAssociation(ERDEntity entity, String name, String description, ERDLogicalPrimaryKey pk)
    {
        this.entity = entity.getObject();
        this.name = name;
        this.description = description;
        this.pk = pk;
    }

    @Nullable
    @Override
    public DBSEntityConstraint getReferencedConstraint()
    {
        return pk;
    }

    @Override
    public DBSEntity getAssociatedEntity()
    {
        return pk.getParentObject();
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return entity.getDataSource();
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return description;
    }

    @NotNull
    @Override
    public DBSEntity getParentObject()
    {
        return entity;
    }

    @NotNull
    @Override
    public DBSEntityConstraintType getConstraintType()
    {
        return ERDConstants.CONSTRAINT_LOGICAL_FK;
    }

    @NotNull
    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean isPersisted()
    {
        return false;
    }

    @Override
    public List<? extends DBSEntityAttributeRef> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return Collections.emptyList();
    }
}
