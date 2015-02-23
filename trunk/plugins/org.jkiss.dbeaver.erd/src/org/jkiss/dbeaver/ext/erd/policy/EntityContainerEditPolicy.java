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
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ContainerEditPolicy;
import org.eclipse.gef.requests.CreateRequest;

/**
 * Edit policy for Table as a container, handling functionality for dropping new columns into tables
 * 
 * @author Serge Rieder
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