/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
 * @author Serge Rieder
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

