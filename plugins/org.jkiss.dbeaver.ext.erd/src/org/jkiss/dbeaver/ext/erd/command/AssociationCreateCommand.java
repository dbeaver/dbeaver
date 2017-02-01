/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

    /**
     * @see org.eclipse.gef.commands.Command#canExecute()
     */
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
                for (int i = 0; i < relationships.size(); i++) {
                    ERDAssociation currentRelationship = relationships.get(i);
                    if (currentRelationship.getForeignKeyEntity().equals(foreignEntity)) {
                        returnValue = false;
                        break;
                    }
                }
            }

        }
        return returnValue;

    }

    /**
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void execute()
    {
        association = new ERDAssociation(foreignEntity, primaryEntity, true);
    }

    /**
     * @return Returns the foreignEntity.
     */
    public ERDEntity getForeignEntity()
    {
        return foreignEntity;
    }

    /**
     * @return Returns the primaryEntity.
     */
    public ERDEntity getPrimaryEntity()
    {
        return primaryEntity;
    }

    /**
     * Returns the Relationship between the primary and foreign tables
     *
     * @return the transistion
     */
    public ERDAssociation getAssociation()
    {
        return association;
    }

    /**
     * @see org.eclipse.gef.commands.Command#redo()
     */
    @Override
    public void redo()
    {
        foreignEntity.addForeignKeyRelationship(association, true);
        primaryEntity.addPrimaryKeyRelationship(association, true);
    }

    /**
     * @param foreignEntity The foreignEntity to set.
     */
    public void setForeignEntity(ERDEntity foreignEntity)
    {
        this.foreignEntity = foreignEntity;
    }

    /**
     * @param primaryEntity The primaryEntity to set.
     */
    public void setPrimaryEntity(ERDEntity primaryEntity)
    {
        this.primaryEntity = primaryEntity;
    }

    /**
     * @param association The relationship to set.
     */
    public void setAssociation(ERDAssociation association)
    {
        this.association = association;
    }

    /**
     * Undo version of command
     */
    @Override
    public void undo()
    {
        foreignEntity.removeForeignKeyRelationship(association, true);
        primaryEntity.removePrimaryKeyRelationship(association, true);
    }

}

