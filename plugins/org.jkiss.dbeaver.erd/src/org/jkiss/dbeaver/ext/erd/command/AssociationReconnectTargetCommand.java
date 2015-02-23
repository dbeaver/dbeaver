/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
 * Command to change the primary key we are connecting to a particular foreign key
 * key
 * 
 * @author Serge Rieder
 */
public class AssociationReconnectTargetCommand extends Command
{

	/** source Table * */
	protected ERDEntity sourceForeignKey;
	/** target Table * */
	protected ERDEntity targetPrimaryKey;
	/** Relationship between source and target * */
	protected ERDAssociation relationship;
	/** previous source prior to command execution * */
	protected ERDEntity oldTargetPrimaryKey;

	/**
	 * Makes sure that foreign key doesn't reconnect to itself or try to create
	 * a relationship which already exists
	 */
	@Override
    public boolean canExecute()
	{

		boolean returnVal = true;

		ERDEntity foreignKeyEntity = relationship.getForeignKeyEntity();

		if (foreignKeyEntity.equals(targetPrimaryKey))
		{
			returnVal = false;
		}
		else
		{

			List<?> relationships = targetPrimaryKey.getPrimaryKeyRelationships();
			for (int i = 0; i < relationships.size(); i++)
			{

				ERDAssociation relationship = ((ERDAssociation) (relationships.get(i)));

				if (relationship.getForeignKeyEntity().equals(sourceForeignKey)
						&& relationship.getPrimaryKeyEntity().equals(targetPrimaryKey))
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
		if (targetPrimaryKey != null)
		{
			oldTargetPrimaryKey.removePrimaryKeyRelationship(relationship, true);
			relationship.setPrimaryKeyEntity(targetPrimaryKey);
			targetPrimaryKey.addPrimaryKeyRelationship(relationship, true);
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
		oldTargetPrimaryKey = relationship.getPrimaryKeyEntity();
		sourceForeignKey = relationship.getForeignKeyEntity();
	}

	/**
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	@Override
    public void undo()
	{
		targetPrimaryKey.removePrimaryKeyRelationship(relationship, true);
		relationship.setPrimaryKeyEntity(oldTargetPrimaryKey);
		oldTargetPrimaryKey.addPrimaryKeyRelationship(relationship, true);
	}
}