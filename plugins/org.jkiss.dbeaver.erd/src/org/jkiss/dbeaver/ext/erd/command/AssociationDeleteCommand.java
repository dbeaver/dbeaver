/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 17, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;

/**
 * Command to delete relationship
 * 
 * @author Serge Rieder
 */
public class AssociationDeleteCommand extends Command
{

	private ERDTable foreignKeySource;
	private ERDTable primaryKeyTarget;
	private ERDAssociation relationship;

	public AssociationDeleteCommand(ERDTable foreignKeySource, ERDTable primaryKeyTarget, ERDAssociation relationship)
	{
		super();
		this.foreignKeySource = foreignKeySource;
		this.primaryKeyTarget = primaryKeyTarget;
		this.relationship = relationship;
	}

	/**
	 * Removes the relationship
	 */
	public void execute()
	{
		foreignKeySource.removeForeignKeyRelationship(relationship, true);
		primaryKeyTarget.removePrimaryKeyRelationship(relationship, true);
		relationship.setForeignKeyTable(null);
		relationship.setPrimaryKeyTable(null);
	}

	/**
	 * Restores the relationship
	 */
	public void undo()
	{
		relationship.setForeignKeyTable(foreignKeySource);
		relationship.setForeignKeyTable(primaryKeyTarget);
		foreignKeySource.addForeignKeyRelationship(relationship, true);
		primaryKeyTarget.addPrimaryKeyRelationship(relationship, true);
	}

}

