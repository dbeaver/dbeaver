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
/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;

import java.util.List;

/**
 * Command to change the foreign key we are connecting to a particular primary
 * key
 *
 * @author Serge Rider
 */
public class AssociationReconnectSourceCommand extends Command {

    protected ERDEntity sourceEntity;
    protected ERDEntity targetEntity;
    protected ERDAssociation association;
    protected ERDEntity oldSourceEntity;

    /**
     * Makes sure that primary key doesn't reconnect to itself or try to create
     * a relationship which already exists
     */
    @Override
    public boolean canExecute() {

        boolean returnVal = true;

        ERDEntity primaryKeyEntity = association.getPrimaryEntity();

        //cannot connect to itself
        if (primaryKeyEntity.equals(sourceEntity)) {
            returnVal = false;
        } else {

            List<ERDAssociation> relationships = sourceEntity.getForeignKeyRelationships();
            for (ERDAssociation relationship : relationships) {
                if (relationship.getPrimaryEntity().equals(targetEntity) &&
                    relationship.getForeignEntity().equals(sourceEntity)) {
                    returnVal = false;
                    break;
                }
            }
        }

        return returnVal;

    }

    /**
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void execute() {
        if (sourceEntity != null) {
            oldSourceEntity.removeForeignKeyRelationship(association, true);
            association.setForeignEntity(sourceEntity);
            sourceEntity.addForeignKeyRelationship(association, true);
        }
    }

    /**
     * @return Returns the sourceEntity.
     */
    public ERDEntity getSourceEntity() {
        return sourceEntity;
    }

    /**
     * @param sourceEntity The sourceEntity to set.
     */
    public void setSourceEntity(ERDEntity sourceEntity) {
        this.sourceEntity = sourceEntity;
    }

    /**
     * @return Returns the targetEntity.
     */
    public ERDEntity getTargetEntity() {
        return targetEntity;
    }

    /**
     * @param targetEntity The targetEntity to set.
     */
    public void setTargetEntity(ERDEntity targetEntity) {
        this.targetEntity = targetEntity;
    }

    /**
     * @return Returns the relationship.
     */
    public ERDAssociation getAssociation() {
        return association;
    }

    /**
     * Sets the Relationship associated with this
     *
     * @param association the Relationship
     */
    public void setAssociation(ERDAssociation association) {
        this.association = association;
        targetEntity = association.getPrimaryEntity();
        oldSourceEntity = association.getForeignEntity();
    }

    /**
     * @see org.eclipse.gef.commands.Command#undo()
     */
    @Override
    public void undo() {
        sourceEntity.removeForeignKeyRelationship(association, true);
        association.setForeignEntity(oldSourceEntity);
        oldSourceEntity.addForeignKeyRelationship(association, true);
    }
}