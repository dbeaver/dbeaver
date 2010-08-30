/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.gef.commands.Command;

import org.jkiss.dbeaver.ext.erd.model.Column;
import org.jkiss.dbeaver.ext.erd.model.Table;

/**
 * Command to create a new table
 * 
 * @author Phil Zoio
 */
public class AttributeCreateCommand extends Command
{

	private Column column;
	private Table table;

	public void setColumn(Column column)
	{
		this.column = column;
		this.column.setName("COLUMN " + (table.getColumns().size() + 1));
		this.column.setType("VARCHAR");
	}

	public void setTable(Table table)
	{
		this.table = table;
	}

	public void execute()
	{
		table.addColumn(column);
	}

	public void undo()
	{
		table.removeColumn(column);
	}

}