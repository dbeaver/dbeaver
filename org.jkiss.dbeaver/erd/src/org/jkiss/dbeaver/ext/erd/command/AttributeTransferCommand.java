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
 * Moves column to a different table
 * 
 * @author Phil Zoio
 */
public class AttributeTransferCommand extends Command
{

	private Column columnToMove;
	private Column columnAfter;
	private Table originalTable;
	private Table newTable;
	private int oldIndex, newIndex;

	public AttributeTransferCommand(Column columnToMove, Column columnAfter, Table originalTable, Table newTable,
			int oldIndex, int newIndex)
	{
		super();
		this.columnToMove = columnToMove;
		this.columnAfter = columnAfter;
		this.originalTable = originalTable;
		this.newTable = newTable;
		this.oldIndex = oldIndex;
		this.newIndex = newIndex;
	}

	/**
	 * allows for transfer only if there are one or more columns
	 */
	public boolean canExecute()
	{
		if (originalTable.getColumns().size() > 1)
			return true;
		else
			return false;
	}

	public void execute()
	{
		originalTable.removeColumn(columnToMove);
		newTable.addColumn(columnToMove, newIndex);
	}

	public void undo()
	{
		newTable.removeColumn(columnToMove);
		originalTable.addColumn(columnToMove, oldIndex);
	}

}