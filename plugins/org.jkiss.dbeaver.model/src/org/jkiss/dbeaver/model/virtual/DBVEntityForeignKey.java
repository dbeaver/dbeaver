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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.ArrayList;
import java.util.List;

/**
 * Virtual foreign key
 */
public class DBVEntityForeignKey implements DBSEntityConstraint, DBSEntityAssociationLazy, DBSEntityReferrer, DBSTableForeignKey {

    private static final Log log = Log.getLog(DBVEntityForeignKey.class);

    @NotNull
    private final DBVEntity entity;
    private String refEntityId;
    private String refConstraintId;
    private final List<DBVEntityForeignKeyColumn> attributes = new ArrayList<>();

    public DBVEntityForeignKey(@NotNull DBVEntity entity) {
        this.entity = entity;
    }

    public DBVEntityForeignKey(@NotNull DBVEntity entity, DBVEntityForeignKey copy) {
        this.entity = entity;
        this.refEntityId = copy.refEntityId;
        this.refConstraintId = copy.refConstraintId;
    }

    @NotNull
    @Override
    public DBSEntityConstraint getReferencedConstraint() {
        try {
            return getRealReferenceConatraint(new VoidProgressMonitor());
        } catch (DBException e) {
            throw new IllegalStateException(e);
        }
    }

    @NotNull
    @Override
    public DBSEntityConstraint getReferencedConstraint(DBRProgressMonitor monitor) throws DBException {
        return getRealReferenceConatraint(monitor);
    }

    public String getRefEntityId() {
        return refEntityId;
    }

    public String getRefConstraintId() {
        return refConstraintId;
    }

    public void setReferencedConstraint(String refEntityId, String refConsId) {
        this.refEntityId = refEntityId;
        this.refConstraintId = refConsId;
    }

    public void setReferencedConstraint(DBRProgressMonitor monitor, DBSEntityConstraint constraint) throws DBException {
        DBSEntity refEntity = constraint.getParentObject();
        if (refEntity instanceof DBVEntity) {
            refEntity = ((DBVEntity) refEntity).getRealEntity(monitor);
        }
        DBNDatabaseNode refNode = DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(monitor, refEntity, true);
        if (refNode == null) {
            log.warn("Can't find navigator node for object " + DBUtils.getObjectFullId(refEntity));
            return;
        }
        this.refEntityId = refNode.getNodeItemPath();
        this.refConstraintId = constraint.getName();
    }

    @NotNull
    public DBSEntityConstraint getRealReferenceConatraint(@NotNull DBRProgressMonitor monitor) throws DBException {
        DBNNode refNode = DBWorkbench.getPlatform().getNavigatorModel().getNodeByPath(monitor, refEntityId);
        if (!(refNode instanceof DBNDatabaseNode)) {
            throw new DBException("Can't find reference node " + refEntityId + " for virtaul foreign key");
        }
        DBSObject object = ((DBNDatabaseNode) refNode).getObject();
        if (object instanceof DBSEntity) {
            List<DBSEntityConstraint> constraints = DBVUtils.getAllConstraints(monitor, (DBSEntity) object);
            DBSObject refEntityConstraint = DBUtils.findObject(constraints, refConstraintId);
            if (refEntityConstraint == null) {
                throw new DBException("Can't find constraint " + refConstraintId + " in entity " + refEntityId);
            }
            return (DBSEntityConstraint) refEntityConstraint;
        } else {
            throw new DBException("Object " + refEntityId + " is not an entity");
        }
    }

    @Override
    public DBSEntity getAssociatedEntity() {
        return getReferencedConstraint().getParentObject();
    }

    @Override
    public DBSEntity getAssociatedEntity(DBRProgressMonitor monitor) throws DBException {
        return getReferencedConstraint(monitor).getParentObject();
    }

    @Override
    public List<DBVEntityForeignKeyColumn> getAttributeReferences(@Nullable DBRProgressMonitor monitor) throws DBException {
        return attributes;
    }

    public List<DBVEntityForeignKeyColumn> getAttributes() {
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
        return getConstraintType().getId() + "_" + entity.getName() + "_" + DBNUtils.getLastNodePathSegment(refEntityId);
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public DBSForeignKeyModifyRule getDeleteRule() {
        return DBSForeignKeyModifyRule.NO_ACTION;
    }

    @NotNull
    @Override
    public DBSForeignKeyModifyRule getUpdateRule() {
        return DBSForeignKeyModifyRule.NO_ACTION;
    }
}
