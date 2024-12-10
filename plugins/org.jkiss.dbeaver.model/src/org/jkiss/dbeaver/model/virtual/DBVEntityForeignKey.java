/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;

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

    // Copy constructor
    DBVEntityForeignKey(@NotNull DBVEntity entity, DBVEntityForeignKey copy, DBVModel targetModel) {
        this.entity = entity;

        this.refEntityId = copy.refEntityId;
        // Here is a tricky part
        // refEntityId may refer to the current (old model owner) datasource
        // In this case we must fix it and refer to the new model owner.
        DBPDataSourceContainer copyDS = copy.getAssociatedDataSource();
        if (copyDS != null && copyDS == copy.getParentObject().getDataSourceContainer()) {
            DBPDataSourceContainer newDS = targetModel.getDataSourceContainer();
            if (newDS != null) {
                this.refEntityId = copy.refEntityId.replace(copyDS.getId(), newDS.getId());
            }
        }

        this.refConstraintId = copy.refConstraintId;
        for (DBVEntityForeignKeyColumn fkc : copy.attributes) {
            this.attributes.add(new DBVEntityForeignKeyColumn(this, fkc));
        }

        DBVModel.addToCache(this);
    }

    void dispose() {
        if (refEntityId != null) {
            DBVModel.removeFromCache(this);
            refEntityId = null;
            refConstraintId = null;
        }
    }

    @Nullable
    @Override
    public DBSEntityConstraint getReferencedConstraint() {
        try {
            return getRealReferenceConstraint(new VoidProgressMonitor());
        } catch (DBException e) {
            //log.error(e);
            return null;
        }
    }

    @NotNull
    @Override
    public DBSEntityConstraint getReferencedConstraint(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getRealReferenceConstraint(monitor);
    }

    public String getRefEntityId() {
        return refEntityId;
    }

    public void setRefEntityId(String refEntityId) {
        this.refEntityId = refEntityId;
    }

    public String getRefConstraintId() {
        return refConstraintId;
    }

    public synchronized void setReferencedConstraint(String refEntityId, String refConsId) {
        if (this.refEntityId != null) {
            DBVModel.removeFromCache(this);
        }
        this.refEntityId = refEntityId;
        this.refConstraintId = refConsId;
        if (refEntityId != null) {
            DBVModel.addToCache(this);
        }
    }

    public synchronized void setReferencedConstraint(DBRProgressMonitor monitor, DBSEntityConstraint constraint) throws DBException {
        DBSEntity refEntity = constraint.getParentObject();
        if (refEntity instanceof DBVEntity) {
            refEntity = ((DBVEntity) refEntity).getRealEntity(monitor);
        }
        DBNDatabaseNode refNode = getParentObject().getProject().getNavigatorModel().getNodeByObject(monitor, refEntity, true);
        if (refNode == null) {
            log.warn("Can't find navigator node for object " + DBUtils.getObjectFullId(refEntity));
            return;
        }
        if (refEntityId != null) {
            DBVModel.removeFromCache(this);
        }
        this.refEntityId = refNode.getNodeUri();
        this.refConstraintId = constraint.getName();
        if (refEntityId != null) {
            DBVModel.addToCache(this);
        }
    }

    @NotNull
    public DBSEntityConstraint getRealReferenceConstraint(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (refEntityId == null) {
            throw new DBException("Ref entity ID not set for virtual FK " + getName());
        }
        DBNNode refNode = DBNUtils.getNavigatorModel(entity).getNodeByPath(monitor, refEntityId);
        if (!(refNode instanceof DBNDatabaseNode)) {
            throw new DBException("Can't find reference node " + refEntityId + " for virtual foreign key");
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

    @Nullable
    @Override
    public DBSEntity getAssociatedEntity() {
        DBSEntityConstraint refC = getReferencedConstraint();
        return refC == null ? null : refC.getParentObject();
    }

    @Nullable
    @Override
    public DBSEntity getAssociatedEntity(@NotNull DBRProgressMonitor monitor) throws DBException {
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
        return getConstraintType().getId() + "_" + entity.getName() + "_" + (refEntityId == null ? "?" : DBNUtils.getLastNodePathSegment(refEntityId));
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

    public DBPDataSourceContainer getAssociatedDataSource() {
        if (refEntityId == null) {
            return null;
        }
        DBPProject project = getParentObject().getProject();
        DBNModel navModel = project.getNavigatorModel();

        DBNDataSource dsNode = navModel == null ? null : navModel.getDataSourceByPath(project, refEntityId);
        return dsNode == null ? null : dsNode.getDataSourceContainer();
    }

    @Override
    public String toString() {
        return "VFK: " + entity.getFullyQualifiedName(DBPEvaluationContext.UI) +
            "->" + refEntityId + "." + refConstraintId + " (" + attributes + ")";
    }
}
