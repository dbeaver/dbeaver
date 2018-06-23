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
 * Created on Jul 17, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;

import java.util.List;

/**
 * Command to delete relationship
 *
 * @author Serge Rider
 */
public class AssociationCreateCommand extends Command {

    protected ERDAssociation association;
    protected ERDEntity foreignEntity;
    protected ERDEntity primaryEntity;

    public AssociationCreateCommand() {
    }

    @Override
    public boolean canExecute()
    {

        boolean returnValue = true;
        if (foreignEntity.equals(primaryEntity)) {
            returnValue = false;
        } else {

            if (primaryEntity == null) {
                return false;
            } else {
                // Check for existence of relationship already
                List<ERDAssociation> relationships = primaryEntity.getPrimaryKeyRelationships();
                for (ERDAssociation currentRelationship : relationships) {
                    if (currentRelationship.getForeignEntity().equals(foreignEntity)) {
                        returnValue = false;
                        break;
                    }
                }
            }

        }
        return returnValue;

    }

    @Override
    public void execute()
    {
        association = new ERDAssociation(foreignEntity, primaryEntity, true);
    }

    public ERDEntity getForeignEntity()
    {
        return foreignEntity;
    }

    public void setForeignEntity(ERDEntity foreignEntity)
    {
        this.foreignEntity = foreignEntity;
    }

    public ERDEntity getPrimaryEntity()
    {
        return primaryEntity;
    }

    public void setPrimaryEntity(ERDEntity primaryEntity)
    {
        this.primaryEntity = primaryEntity;
    }

    public ERDAssociation getAssociation()
    {
        return association;
    }

    public void setAssociation(ERDAssociation association)
    {
        this.association = association;
    }

    @Override
    public void redo()
    {
        foreignEntity.addForeignKeyRelationship(association, true);
        primaryEntity.addPrimaryKeyRelationship(association, true);
    }

    @Override
    public void undo()
    {
        foreignEntity.removeForeignKeyRelationship(association, true);
        primaryEntity.removePrimaryKeyRelationship(association, true);
    }

}

