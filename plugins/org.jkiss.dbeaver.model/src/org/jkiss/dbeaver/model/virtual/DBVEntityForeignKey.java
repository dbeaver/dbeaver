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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Virtual foreign key
 */
public class DBVEntityForeignKey implements DBSEntityConstraint, DBSEntityReferrer, DBSEntityAssociation {

    @NotNull
    private final DBVEntity entity;
    private DBSEntityConstraint refEntityConstraint;
    private final List<DBVEntityForeignKeyColumn> attributes = new ArrayList<>();

    public DBVEntityForeignKey(@NotNull DBVEntity entity) {
        this.entity = entity;
/*
        if (refEntityConstraint instanceof DBSEntityReferrer) {
            for (DBSEntityAttributeRef attrRef : ((DBSEntityReferrer) refEntityConstraint).getAttributeReferences(monitor)) {
                DBSEntityAttribute refAttribute = attrRef.getAttribute();
            }
        }
*/
    }

    public DBVEntityForeignKey(@NotNull DBVEntity entity, DBVEntityForeignKey copy) {
        this.entity = entity;
        this.refEntityConstraint = copy.refEntityConstraint;
    }

    @Override
    public DBSEntityConstraint getReferencedConstraint() {
        return refEntityConstraint;
    }

    public void setReferencedConstraint(DBSEntityConstraint refEntityConstraint) {
        this.refEntityConstraint = refEntityConstraint;
    }

    @Override
    public DBSEntity getAssociatedEntity() {
        return refEntityConstraint.getParentObject();
    }

    @Override
    public List<DBVEntityForeignKeyColumn> getAttributeReferences(@Nullable DBRProgressMonitor monitor) {
        return attributes;
    }

    public synchronized void setAttributes(List<DBVEntityForeignKeyColumn> attrs) {
        attributes.clear();
        attributes.addAll(attrs);
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @NotNull
    @Override
    public DBVEntity getParentObject() {
        return entity;
    }

    @NotNull
    public DBVEntity getEntity() {
        return entity;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return entity.getDataSource();
    }

    @NotNull
    @Override
    public DBSEntityConstraintType getConstraintType() {
        return DBSEntityConstraintType.VIRTUAL_FOREIGN_KEY;
    }

    @NotNull
    @Override
    public String getName() {
        return entity.getName() + " " + getConstraintType().getName();
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

}
