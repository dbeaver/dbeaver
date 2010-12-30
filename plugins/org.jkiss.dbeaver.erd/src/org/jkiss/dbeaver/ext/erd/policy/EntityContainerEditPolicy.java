/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
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
	protected Command getCreateCommand(CreateRequest request)
	{
/*
		Object newObject = request.getNewObject();
		if (!(newObject instanceof ERDTableColumn))
		{
			return null;
		}
		
		EntityPart entityPart = (EntityPart) getHost();
		ERDTable table = entityPart.getTable();
		ERDTableColumn column = (ERDTableColumn) newObject;
		AttributeCreateCommand command = new AttributeCreateCommand();
		command.setTable(table);
		command.setColumn(column);
		return command;
*/
        return null;
	}

}