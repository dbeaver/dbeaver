/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @author Serge Rider
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

