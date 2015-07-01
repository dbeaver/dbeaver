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

