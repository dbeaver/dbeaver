/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 19, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.gef.commands.Command;

import org.jkiss.dbeaver.ext.erd.model.Column;
import org.jkiss.dbeaver.ext.erd.model.Table;

/**
 * Command to move a column to a different index location within the table
 * 
 * @author Phil Zoio
 */
public class AttributeMoveCommand extends Command
{

	private int oldIndex, newIndex;
	private Column childColumn;
	private Table parentTable;

	public AttributeMoveCommand(Column child, Table parent, int oldIndex, int newIndex)
	{
		this.childColumn = child;
		this.parentTable = parent;
		this.oldIndex = oldIndex;
		this.newIndex = newIndex;
		if (newIndex > oldIndex)
			newIndex--; //this is because the column is deleted before it is
						// added
	}

	public void execute()
	{
		parentTable.switchColumn(childColumn, newIndex);
	}

	public void undo()
	{
		parentTable.switchColumn(childColumn, oldIndex);
	}

}