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
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.erd.model.*;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityForeignKey;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.List;

/**
 * Command to create association
 */
public class AssociationCreateCommand extends Command {

    protected ERDAssociation association;
    protected ERDElement sourceEntity;
    protected ERDElement targetEntity;

    private List<ERDEntityAttribute> sourceAttributes;
    private List<ERDEntityAttribute> targetAttributes;

    public AssociationCreateCommand() {
    }

    public ERDEntityAttribute getSourceAttribute() {
        return CommonUtils.isEmpty(sourceAttributes) ? null : sourceAttributes.get(0);
    }

    public ERDEntityAttribute getTargetAttribute() {
        return CommonUtils.isEmpty(targetAttributes) ? null : targetAttributes.get(0);
    }

    public void setAttributes(List<ERDEntityAttribute> sourceAttributes, List<ERDEntityAttribute> targetAttributes) {
        this.sourceAttributes = sourceAttributes;
        this.targetAttributes = targetAttributes;

    }
    public void setAttributes(ERDEntityAttribute sourceAttribute, ERDEntityAttribute targetAttribute) {
        this.sourceAttributes = Collections.singletonList(sourceAttribute);
        this.targetAttributes = Collections.singletonList(targetAttribute);
    }

    @Override
    public boolean canExecute() {

        boolean returnValue = true;
        if (sourceEntity.equals(targetEntity)) {
            returnValue = false;
        } else {

            if (targetEntity == null) {
                return false;
            } else {
                // Check for existence of relationship already
                List<ERDAssociation> relationships = targetEntity.getReferences();
                for (ERDAssociation currentRelationship : relationships) {
                    if (currentRelationship.getSourceEntity().equals(sourceEntity)) {
                        returnValue = false;
                        break;
                    }
                }
            }

        }

        return returnValue;

    }

    @Override
    public void execute() {
        if (sourceEntity instanceof ERDEntity && targetEntity instanceof ERDEntity) {
            DBSEntity srcEntityObject = ((ERDEntity)sourceEntity).getObject();
            DBSEntity targetEntityObject = ((ERDEntity)targetEntity).getObject();

            List<DBSEntityAttribute> srcAttrs = ERDUtils.getObjectsFromERD(sourceAttributes);
            List<DBSEntityAttribute> refAttrs = ERDUtils.getObjectsFromERD(targetAttributes);

            DBVEntity vEntity = DBVUtils.getVirtualEntity(srcEntityObject, true);
            DBVEntityForeignKey vfk = EditForeignKeyPage.createVirtualForeignKey(
                vEntity,
                targetEntityObject,
                new EditForeignKeyPage.FKType[] {
                    EditForeignKeyPage.FK_TYPE_LOGICAL
                },
                srcAttrs,
                refAttrs);
            if (vfk == null) {
                return;
            }
            vEntity.persistConfiguration();
            association = new ERDAssociation(vfk, (ERDEntity)sourceEntity, (ERDEntity)targetEntity, true);
        } else {
            association = createAssociation(sourceEntity, targetEntity, true);
        }
    }

    public ERDElement getSourceEntity() {
        return sourceEntity;
    }

    public void setSourceEntity(ERDElement sourceEntity) {
        this.sourceEntity = sourceEntity;
    }

    public ERDElement getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(ERDElement targetEntity) {
        this.targetEntity = targetEntity;
    }

    public ERDAssociation getAssociation() {
        return association;
    }

    public void setAssociation(ERDAssociation association) {
        this.association = association;
    }

    @Override
    public void redo() {
        if (association != null) {
            sourceEntity.addAssociation(association, true);
            targetEntity.addReferenceAssociation(association, true);
        }
    }

    @Override
    public void undo() {
        if (association != null) {
            sourceEntity.removeAssociation(association, true);
            targetEntity.removeReferenceAssociation(association, true);
        }
    }

    protected ERDAssociation createAssociation(ERDElement sourceEntity, ERDElement targetEntity, boolean reflect) {
        return new ERDAssociation(sourceEntity, targetEntity, true);
    }

}

