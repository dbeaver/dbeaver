/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import java.util.List;

import org.eclipse.gef.commands.Command;

import org.jkiss.dbeaver.ext.erd.model.Relationship;
import org.jkiss.dbeaver.ext.erd.model.Table;

/**
 * Command to change the primary key we are connecting to a particular foreign key
 * key
 * 
 * @author Phil Zoio
 */
public class AssociationReconnectTargetCommand extends Command
{

	/** source Table * */
	protected Table sourceForeignKey;
	/** target Table * */
	protected Table targetPrimaryKey;
	/** Relationship between source and target * */
	protected Relationship relationship;
	/** previous source prior to command execution * */
	protected Table oldTargetPrimaryKey;

	/**
	 * Makes sure that foreign key doesn't reconnect to itself or try to create
	 * a relationship which already exists
	 */
	public boolean canExecute()
	{

		boolean returnVal = true;

		Table foreignKeyTable = relationship.getForeignKeyTable();

		if (foreignKeyTable.equals(targetPrimaryKey))
		{
			returnVal = false;
		}
		else
		{

			List relationships = targetPrimaryKey.getPrimaryKeyRelationships();
			for (int i = 0; i < relationships.size(); i++)
			{

				Relationship relationship = ((Relationship) (relationships.get(i)));

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
	public Table getSourceForeignKey()
	{
		return sourceForeignKey;
	}

	/**
	 * @param sourceForeignKey
	 *            The sourceForeignKey to set.
	 */
	public void setSourceForeignKey(Table sourceForeignKey)
	{
		this.sourceForeignKey = sourceForeignKey;
	}

	/**
	 * @return Returns the targetPrimaryKey.
	 */
	public Table getTargetPrimaryKey()
	{
		return targetPrimaryKey;
	}

	/**
	 * @param targetPrimaryKey
	 *            The targetPrimaryKey to set.
	 */
	public void setTargetPrimaryKey(Table targetPrimaryKey)
	{
		this.targetPrimaryKey = targetPrimaryKey;
	}

	/**
	 * @return Returns the relationship.
	 */
	public Relationship getRelationship()
	{
		return relationship;
	}

	/**
	 * Sets the Relationship associated with this
	 * 
	 * @param relationship
	 *            the Relationship
	 */
	public void setRelationship(Relationship relationship)
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