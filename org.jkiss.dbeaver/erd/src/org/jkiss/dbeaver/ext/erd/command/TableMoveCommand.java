/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 20, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;

import org.jkiss.dbeaver.ext.erd.part.TablePart;

/**
 * Command to move the bounds of an existing table. Only used with
 * XYLayoutEditPolicy (manual layout)
 * 
 * @author Phil Zoio
 */
public class TableMoveCommand extends Command
{

	private TablePart tablePart;
	private Rectangle oldBounds;
	private Rectangle newBounds;

	public TableMoveCommand(TablePart tablePart, Rectangle oldBounds, Rectangle newBounds)
	{
		super();
		this.tablePart = tablePart;
		this.oldBounds = oldBounds;
		this.newBounds = newBounds;
	}

	public void execute()
	{
		tablePart.modifyBounds(newBounds);
	}

	public void undo()
	{
		tablePart.modifyBounds(oldBounds);
	}

}