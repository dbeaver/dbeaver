/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 17, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.gef.commands.Command;

import org.jkiss.dbeaver.ext.erd.model.Column;
import org.jkiss.dbeaver.ext.erd.model.Table;

/**
 * Command to delete a column object
 * 
 * @author Phil Zoio
 */
public class DeleteColumnCommand extends Command
{

	private Table table;
	private Column column;
	private int index = -1;

	public boolean canExecute()
	{
		if (table.getColumns().size() > 1)
		{
			return true;
		}
		return true;
	}

	/**
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute()
	{
		primExecute();
	}

	/**
	 * Invokes the execution of this command.
	 */
	protected void primExecute()
	{
		index = table.getColumns().indexOf(column);
		table.removeColumn(column);
	}

	/**
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	public void redo()
	{
		primExecute();
	}

	/**
	 * Sets the Table parent to the column to be deleted
	 * 
	 * @param ta
	 *            the child
	 */
	public void setTable(Table ta)
	{
		table = ta;
	}

	/**
	 * Sets the parent to the passed Schema
	 * 
	 * @param co
	 *            the parent
	 */
	public void setColumn(Column co)
	{
		column = co;
	}

	/**
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo()
	{
		table.addColumn(column, index);
	}

}

