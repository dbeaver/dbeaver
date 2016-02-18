/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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