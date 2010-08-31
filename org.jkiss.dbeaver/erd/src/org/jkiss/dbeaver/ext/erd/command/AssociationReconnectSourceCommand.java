/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import java.util.List;

import org.eclipse.gef.commands.Command;

import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;

/**
 * Command to change the foreign key we are connecting to a particular primary
 * key
 * 
 * @author Phil Zoio
 */
public class AssociationReconnectSourceCommand extends Command
{

	/** source Table * */
	protected ERDTable sourceForeignKey;
	/** target Table * */
	protected ERDTable targetPrimaryKey;
	/** Relationship between source and target * */
	protected ERDAssociation relationship;
	/** previous source prior to command execution * */
	protected ERDTable oldSourceForeignKey;

	/**
	 * Makes sure that primary key doesn't reconnect to itself or try to create
	 * a relationship which already exists
	 */
	public boolean canExecute()
	{

		boolean returnVal = true;

		ERDTable primaryKeyTable = relationship.getPrimaryKeyTable();

		//cannot connect to itself
		if (primaryKeyTable.equals(sourceForeignKey))
		{
			returnVal = false;
		}
		else
		{

			List relationships = sourceForeignKey.getForeignKeyRelationships();
			for (int i = 0; i < relationships.size(); i++)
			{

				ERDAssociation relationship = ((ERDAssociation) (relationships.get(i)));
				if (relationship.getPrimaryKeyTable().equals(targetPrimaryKey)
						&& relationship.getForeignKeyTable().equals(sourceForeignKey))
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
		if (sourceForeignKey != null)
		{
			oldSourceForeignKey.removeForeignKeyRelationship(relationship);
			relationship.setForeignKeyTable(sourceForeignKey);
			sourceForeignKey.addForeignKeyRelationship(relationship);
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
		targetPrimaryKey = relationship.getPrimaryKeyTable();
		oldSourceForeignKey = relationship.getForeignKeyTable();
	}

	/**
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo()
	{
		sourceForeignKey.removeForeignKeyRelationship(relationship);
		relationship.setForeignKeyTable(oldSourceForeignKey);
		oldSourceForeignKey.addForeignKeyRelationship(relationship);
	}
}