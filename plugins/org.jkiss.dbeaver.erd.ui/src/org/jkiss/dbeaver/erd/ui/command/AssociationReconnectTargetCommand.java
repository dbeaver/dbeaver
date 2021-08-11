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
package org.jkiss.dbeaver.erd.ui.command;

import org.eclipse.gef3.commands.Command;
import org.jkiss.dbeaver.erd.model.ERDAssociation;
import org.jkiss.dbeaver.erd.model.ERDElement;

import java.util.List;

/**
 * Command to change the primary key we are connecting to a particular foreign key
 * key
 */
public class AssociationReconnectTargetCommand extends Command {

    protected ERDElement sourceEntity;
    protected ERDElement targetEntity;
    protected ERDAssociation relationship;
    protected ERDElement oldTargetEntity;

    /**
     * Makes sure that foreign key doesn't reconnect to itself or try to create
     * a relationship which already exists
     */
    @Override
    public boolean canExecute() {

        boolean returnVal = true;

        ERDElement foreignKeyEntity = relationship.getSourceEntity();

        if (foreignKeyEntity.equals(targetEntity)) {
            returnVal = false;
        } else {

            List<ERDAssociation> relationships = targetEntity.getReferences();
            for (ERDAssociation relationship : relationships) {
                if (relationship.getSourceEntity().equals(sourceEntity)
                    && relationship.getTargetEntity().equals(targetEntity)) {
                    returnVal = false;
                    break;
                }
            }
        }

        return returnVal;

    }

    @Override
    public void execute() {
        if (targetEntity != null) {
            oldTargetEntity.removeReferenceAssociation(relationship, true);
            relationship.setTargetEntity(targetEntity);
            targetEntity.addReferenceAssociation(relationship, true);
        }
    }

    public void setTargetEntity(ERDElement targetEntity) {
        this.targetEntity = targetEntity;
    }

    public void setRelationship(ERDAssociation relationship) {
        this.relationship = relationship;
        oldTargetEntity = relationship.getTargetEntity();
        sourceEntity = relationship.getSourceEntity();
    }

    @Override
    public void undo() {
        targetEntity.removeReferenceAssociation(relationship, true);
        relationship.setTargetEntity(oldTargetEntity);
        oldTargetEntity.addReferenceAssociation(relationship, true);
    }
}