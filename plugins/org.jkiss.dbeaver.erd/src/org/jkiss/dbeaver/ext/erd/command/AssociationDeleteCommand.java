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

/**
 * Command to delete relationship
 * 
 * @author Serge Rieder
 */
public class AssociationDeleteCommand extends Command
{

	private ERDEntity foreignKeySource;
	private ERDEntity primaryKeyTarget;
	private ERDAssociation relationship;

	public AssociationDeleteCommand(ERDEntity foreignKeySource, ERDEntity primaryKeyTarget, ERDAssociation relationship)
	{
		super();
		this.foreignKeySource = foreignKeySource;
		this.primaryKeyTarget = primaryKeyTarget;
		this.relationship = relationship;
	}

	/**
	 * Removes the relationship
	 */
	@Override
    public void execute()
	{
        primaryKeyTarget.removePrimaryKeyRelationship(relationship, true);
		foreignKeySource.removeForeignKeyRelationship(relationship, true);
		relationship.setForeignKeyEntity(null);
		relationship.setPrimaryKeyEntity(null);
	}

	/**
	 * Restores the relationship
	 */
	@Override
    public void undo()
	{
		relationship.setForeignKeyEntity(foreignKeySource);
		relationship.setForeignKeyEntity(primaryKeyTarget);
		foreignKeySource.addForeignKeyRelationship(relationship, true);
		primaryKeyTarget.addPrimaryKeyRelationship(relationship, true);
	}

}

