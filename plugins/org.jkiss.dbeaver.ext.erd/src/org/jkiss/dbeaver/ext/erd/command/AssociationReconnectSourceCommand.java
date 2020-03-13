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
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDElement;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;

import java.util.List;

/**
 * Command to change the foreign key we are connecting to a particular primary
 * key
 *
 * @author Serge Rider
 */
public class AssociationReconnectSourceCommand extends Command {

    protected ERDElement sourceEntity;
    protected ERDElement targetEntity;
    protected ERDAssociation association;
    protected ERDElement oldSourceEntity;

    /**
     * Makes sure that primary key doesn't reconnect to itself or try to create
     * a relationship which already exists
     */
    @Override
    public boolean canExecute() {

        boolean returnVal = true;

        ERDElement primaryEntity = association.getTargetEntity();

        //cannot connect to itself
        if (primaryEntity.equals(sourceEntity)) {
            returnVal = false;
        } else {

            List<ERDAssociation> relationships = sourceEntity.getAssociations();
            for (ERDAssociation relationship : relationships) {
                if (relationship.getTargetEntity().equals(targetEntity) &&
                    relationship.getSourceEntity().equals(sourceEntity)) {
                    returnVal = false;
                    break;
                }
            }
        }

        return returnVal;

    }

    @Override
    public void execute() {
        if (sourceEntity != null) {
            oldSourceEntity.removeAssociation(association, true);
            association.setSourceEntity(sourceEntity);
            sourceEntity.addAssociation(association, true);
        }
    }

    public void setSourceEntity(ERDElement sourceEntity) {
        this.sourceEntity = sourceEntity;
    }

    public ERDAssociation getAssociation() {
        return association;
    }

    public void setAssociation(ERDAssociation association) {
        this.association = association;
        targetEntity = association.getTargetEntity();
        oldSourceEntity = association.getSourceEntity();
    }

    @Override
    public void undo() {
        sourceEntity.removeAssociation(association, true);
        association.setSourceEntity(oldSourceEntity);
        oldSourceEntity.addAssociation(association, true);
    }
}