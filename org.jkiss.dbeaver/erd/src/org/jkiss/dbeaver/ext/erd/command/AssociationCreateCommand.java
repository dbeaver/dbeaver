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
 * Command to create a new relationship between tables
 * 
 * @author Phil Zoio
 */
public class AssociationCreateCommand extends Command
{

	/** The relationship between primary and foreign key tables * */
	protected Relationship relationship;
	/** The source (foreign key) table * */
	protected Table foreignTable;
	/** The target (primary key) table * */
	protected Table primaryTable;

	/**
	 * @see org.eclipse.gef.commands.Command#canExecute()
	 */
	public boolean canExecute()
	{

		boolean returnValue = true;
		if (foreignTable.equals(primaryTable))
		{
			returnValue = false;
		}
		else
		{

			if (primaryTable == null)
			{
				return false;
			}
			else
			{
				// Check for existence of relationship already
				List relationships = primaryTable.getPrimaryKeyRelationships();
				for (int i = 0; i < relationships.size(); i++)
				{
					Relationship currentRelationship = (Relationship) relationships.get(i);
					if (currentRelationship.getForeignKeyTable().equals(foreignTable))
					{
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
		relationship = new Relationship(foreignTable, primaryTable);
	}

	/**
	 * @return Returns the foreignTable.
	 */
	public Table getForeignTable()
	{
		return foreignTable;
	}

	/**
	 * @return Returns the primaryTable.
	 */
	public Table getPrimaryTable()
	{
		return primaryTable;
	}

	/**
	 * Returns the Relationship between the primary and foreign tables
	 * 
	 * @return the transistion
	 */
	public Relationship getRelationship()
	{
		return relationship;
	}

	/**
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	public void redo()
	{
		foreignTable.addForeignKeyRelationship(relationship);
		primaryTable.addPrimaryKeyRelationship(relationship);
	}

	/**
	 * @param foreignTable
	 *            The foreignTable to set.
	 */
	public void setForeignTable(Table foreignTable)
	{
		this.foreignTable = foreignTable;
	}

	/**
	 * @param primaryTable
	 *            The primaryTable to set.
	 */
	public void setPrimaryTable(Table primaryTable)
	{
		this.primaryTable = primaryTable;
	}

	/**
	 * @param relationship
	 *            The relationship to set.
	 */
	public void setRelationship(Relationship relationship)
	{
		this.relationship = relationship;
	}

	/**
	 * Undo version of command
	 */
	public void undo()
	{
		foreignTable.removeForeignKeyRelationship(relationship);
		primaryTable.removePrimaryKeyRelationship(relationship);
	}

}