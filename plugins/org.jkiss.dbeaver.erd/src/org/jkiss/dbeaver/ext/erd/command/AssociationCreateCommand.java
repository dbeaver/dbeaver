/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 17, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDLogicalForeignKey;
import org.jkiss.dbeaver.ext.erd.model.ERDLogicalPrimaryKey;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;

import java.util.List;

/**
 * Command to delete relationship
 *
 * @author Serge Rieder
 */
public class AssociationCreateCommand extends Command {

    protected ERDAssociation association;
    protected ERDTable foreignTable;
    protected ERDTable primaryTable;

    /**
     * @see org.eclipse.gef.commands.Command#canExecute()
     */
    public boolean canExecute()
    {

        boolean returnValue = true;
        if (foreignTable.equals(primaryTable)) {
            returnValue = false;
        } else {

            if (primaryTable == null) {
                return false;
            } else {
                // Check for existence of relationship already
                List<ERDAssociation> relationships = primaryTable.getPrimaryKeyRelationships();
                for (int i = 0; i < relationships.size(); i++) {
                    ERDAssociation currentRelationship = relationships.get(i);
                    if (currentRelationship.getForeignKeyTable().equals(foreignTable)) {
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
        association = new ERDAssociation(foreignTable, primaryTable, true);
    }

    /**
     * @return Returns the foreignTable.
     */
    public ERDTable getForeignTable()
    {
        return foreignTable;
    }

    /**
     * @return Returns the primaryTable.
     */
    public ERDTable getPrimaryTable()
    {
        return primaryTable;
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
        foreignTable.addForeignKeyRelationship(association, true);
        primaryTable.addPrimaryKeyRelationship(association, true);
    }

    /**
     * @param foreignTable The foreignTable to set.
     */
    public void setForeignTable(ERDTable foreignTable)
    {
        this.foreignTable = foreignTable;
    }

    /**
     * @param primaryTable The primaryTable to set.
     */
    public void setPrimaryTable(ERDTable primaryTable)
    {
        this.primaryTable = primaryTable;
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
        foreignTable.removeForeignKeyRelationship(association, true);
        primaryTable.removePrimaryKeyRelationship(association, true);
    }

}

