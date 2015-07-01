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
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;

import java.util.List;

/**
 * Command to change the foreign key we are connecting to a particular primary
 * key
 * 
 * @author Serge Rieder
 */
public class AssociationReconnectSourceCommand extends Command
{

	/** source Table * */
	protected ERDEntity sourceForeignKey;
	/** target Table * */
	protected ERDEntity targetPrimaryKey;
	/** Relationship between source and target * */
	protected ERDAssociation relationship;
	/** previous source prior to command execution * */
	protected ERDEntity oldSourceForeignKey;

	/**
	 * Makes sure that primary key doesn't reconnect to itself or try to create
	 * a relationship which already exists
	 */
	@Override
    public boolean canExecute()
	{

		boolean returnVal = true;

		ERDEntity primaryKeyEntity = relationship.getPrimaryKeyEntity();

		//cannot connect to itself
		if (primaryKeyEntity.equals(sourceForeignKey))
		{
			returnVal = false;
		}
		else
		{

			List<?> relationships = sourceForeignKey.getForeignKeyRelationships();
			for (int i = 0; i < relationships.size(); i++)
			{

				ERDAssociation relationship = ((ERDAssociation) (relationships.get(i)));
				if (relationship.getPrimaryKeyEntity().equals(targetPrimaryKey)
						&& relationship.getForeignKeyEntity().equals(sourceForeignKey))
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
	@Override
    public void execute()
	{
		if (sourceForeignKey != null)
		{
			oldSourceForeignKey.removeForeignKeyRelationship(relationship, true);
			relationship.setForeignKeyEntity(sourceForeignKey);
			sourceForeignKey.addForeignKeyRelationship(relationship, true);
		}
	}

	/**
	 * @return Returns the sourceForeignKey.
	 */
	public ERDEntity getSourceForeignKey()
	{
		return sourceForeignKey;
	}

	/**
	 * @param sourceForeignKey
	 *            The sourceForeignKey to set.
	 */
	public void setSourceForeignKey(ERDEntity sourceForeignKey)
	{
		this.sourceForeignKey = sourceForeignKey;
	}

	/**
	 * @return Returns the targetPrimaryKey.
	 */
	public ERDEntity getTargetPrimaryKey()
	{
		return targetPrimaryKey;
	}

	/**
	 * @param targetPrimaryKey
	 *            The targetPrimaryKey to set.
	 */
	public void setTargetPrimaryKey(ERDEntity targetPrimaryKey)
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
		targetPrimaryKey = relationship.getPrimaryKeyEntity();
		oldSourceForeignKey = relationship.getForeignKeyEntity();
	}

	/**
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	@Override
    public void undo()
	{
		sourceForeignKey.removeForeignKeyRelationship(relationship, true);
		relationship.setForeignKeyEntity(oldSourceForeignKey);
		oldSourceForeignKey.addForeignKeyRelationship(relationship, true);
	}
}