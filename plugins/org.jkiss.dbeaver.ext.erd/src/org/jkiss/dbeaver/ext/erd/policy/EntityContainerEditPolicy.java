/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ContainerEditPolicy;
import org.eclipse.gef.requests.CreateRequest;

/**
 * Edit policy for Table as a container, handling functionality for dropping new columns into tables
 * 
 * @author Serge Rider
 */
public class EntityContainerEditPolicy extends ContainerEditPolicy
{

	/**
 * @return command to handle adding a new column
 */
	@Override
    protected Command getCreateCommand(CreateRequest request)
	{
/*
		Object newObject = request.getNewObject();
		if (!(newObject instanceof ERDEntityAttribute))
		{
			return null;
		}
		
		EntityPart entityPart = (EntityPart) getHost();
		ERDEntity table = entityPart.getTable();
		ERDEntityAttribute column = (ERDEntityAttribute) newObject;
		AttributeCreateCommand command = new AttributeCreateCommand();
		command.setTable(table);
		command.setColumn(column);
		return command;
*/
        return null;
	}

}