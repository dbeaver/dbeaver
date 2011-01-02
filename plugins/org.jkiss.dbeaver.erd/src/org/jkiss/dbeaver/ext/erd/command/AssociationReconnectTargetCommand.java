/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;

import java.util.List;

/**
 * Command to change the primary key we are connecting to a particular foreign key
 * key
 * 
 * @author Serge Rieder
 */
public class AssociationReconnectTargetCommand extends Command
{

	/** source Table * */
	protected ERDTable sourceForeignKey;
	/** target Table * */
	protected ERDTable targetPrimaryKey;
	/** Relationship between source and target * */
	protected ERDAssociation relationship;
	/** previous source prior to command execution * */
	protected ERDTable oldTargetPrimaryKey;

	/**
	 * Makes sure that foreign key doesn't reconnect to itself or try to create
	 * a relationship which already exists
	 */
	public boolean canExecute()
	{

		boolean returnVal = true;

		ERDTable foreignKeyTable = relationship.getForeignKeyTable();

		if (foreignKeyTable.equals(targetPrimaryKey))
		{
			returnVal = false;
		}
		else
		{

			List<?> relationships = targetPrimaryKey.getPrimaryKeyRelationships();
			for (int i = 0; i < relationships.size(); i++)
			{

				ERDAssociation relationship = ((ERDAssociation) (relationships.get(i)));

				if (relationship.getForeignKeyTable().equals(sourceForeignKey)
						&& relationship.getPrimaryKeyTable().equals(targetPrimaryKey))
				{
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
	public void execute()
	{
		if (targetPrimaryKey != null)
		{
			oldTargetPrimaryKey.removePrimaryKeyRelationship(relationship);
			relationship.setPrimaryKeyTable(targetPrimaryKey);
			targetPrimaryKey.addPrimaryKeyRelationship(relationship);
		}
	}

	/**
	 * @return Returns the sourceForeignKey.
	 */
	public ERDTable getSourceForeignKey()
	{
		return sourceForeignKey;
	}

	/**
	 * @param sourceForeignKey
	 *            The sourceForeignKey to set.
	 */
	public void setSourceForeignKey(ERDTable sourceForeignKey)
	{
		this.sourceForeignKey = sourceForeignKey;
	}

	/**
	 * @return Returns the targetPrimaryKey.
	 */
	public ERDTable getTargetPrimaryKey()
	{
		return targetPrimaryKey;
	}

	/**
	 * @param targetPrimaryKey
	 *            The targetPrimaryKey to set.
	 */
	public void setTargetPrimaryKey(ERDTable targetPrimaryKey)
	{
		this.targetPrimaryKey = targetPrimaryKey;
	}

	/**
	 * @return Returns the relationship.
	 */
	public ERDAssociation getRelationship()
	{
		return relationship;
	}

	/**
	 * Sets the Relationship associated with this
	 * 
	 * @param relationship
	 *            the Relationship
	 */
	public void setRelationship(ERDAssociation relationship)
	{
		this.relationship = relationship;
		oldTargetPrimaryKey = relationship.getPrimaryKeyTable();
		sourceForeignKey = relationship.getForeignKeyTable();
	}

	/**
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo()
	{
		targetPrimaryKey.removePrimaryKeyRelationship(relationship);
		relationship.setPrimaryKeyTable(oldTargetPrimaryKey);
		oldTargetPrimaryKey.addPrimaryKeyRelationship(relationship);
	}
}