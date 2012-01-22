/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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
    public void undo()
    {
        foreignEntity.removeForeignKeyRelationship(association, true);
        primaryEntity.removePrimaryKeyRelationship(association, true);
    }

}

