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
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDElement;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;

import java.util.List;

/**
 * Command to create association
 */
public class AssociationCreateCommand extends Command {

    protected ERDAssociation association;
    protected ERDElement sourceEntity;
    protected ERDElement targetEntity;

    public AssociationCreateCommand() {
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
        association = createAssociation(sourceEntity, targetEntity, true);
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
        sourceEntity.addAssociation(association, true);
        targetEntity.addReferenceAssociation(association, true);
    }

    @Override
    public void undo() {
        sourceEntity.removeAssociation(association, true);
        targetEntity.removeReferenceAssociation(association, true);
    }

    protected ERDAssociation createAssociation(ERDElement sourceEntity, ERDElement targetEntity, boolean reflect) {
        return new ERDAssociation(sourceEntity, targetEntity, true);
    }

}

