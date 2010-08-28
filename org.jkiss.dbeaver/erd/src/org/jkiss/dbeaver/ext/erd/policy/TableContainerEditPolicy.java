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

import org.jkiss.dbeaver.ext.erd.command.ColumnCreateCommand;
import org.jkiss.dbeaver.ext.erd.model.Column;
import org.jkiss.dbeaver.ext.erd.model.Table;
import org.jkiss.dbeaver.ext.erd.part.TablePart;

/**
 * Edit policy for Table as a container, handling functionality for dropping new columns into tables
 * 
 * @author Phil Zoio
 */
public class TableContainerEditPolicy extends ContainerEditPolicy
{

	/**
 * @return command to handle adding a new column
 */
	protected Command getCreateCommand(CreateRequest request)
	{
		Object newObject = request.getNewObject();
		if (!(newObject instanceof Column))
		{
			return null;
		}
		
		TablePart tablePart = (TablePart) getHost();
		Table table = tablePart.getTable();
		Column column = (Column) newObject;
		ColumnCreateCommand command = new ColumnCreateCommand();
		command.setTable(table);
		command.setColumn(column);
		return command;
	}

}