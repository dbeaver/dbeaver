/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;

/**
 * Command to delete relationship
 *
 * @author Serge Rider
 */
public class AssociationDeleteCommand extends Command {

    private ERDEntity sourceEntity;
    private ERDEntity targetEntity;
    private ERDAssociation relationship;

    public AssociationDeleteCommand(ERDEntity sourceEntity, ERDEntity targetEntity, ERDAssociation relationship) {
        super();
        this.sourceEntity = sourceEntity;
        this.targetEntity = targetEntity;
        this.relationship = relationship;
    }

    /**
     * Removes the relationship
     */
    @Override
    public void execute() {
        targetEntity.removePrimaryKeyRelationship(relationship, true);
        sourceEntity.removeForeignKeyRelationship(relationship, true);
        relationship.setSourceEntity(null);
        relationship.setTargetEntity(null);
    }

    /**
     * Restores the relationship
     */
    @Override
    public void undo() {
        relationship.setSourceEntity(sourceEntity);
        relationship.setSourceEntity(targetEntity);
        sourceEntity.addForeignKeyRelationship(relationship, true);
        targetEntity.addPrimaryKeyRelationship(relationship, true);
    }

}

