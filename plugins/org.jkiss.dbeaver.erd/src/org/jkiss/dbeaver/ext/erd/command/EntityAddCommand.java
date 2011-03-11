/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;

/**
 * Add entity to diagram
 */
public class EntityAddCommand extends Command
{

	private EntityDiagram diagram;
	private ERDTable table;
    private Point location;

	public void execute()
	{
		diagram.addTable(table);
	}

    public void undo()
    {
        diagram.removeTable(table);
    }

	public void setDiagram(EntityDiagram diagram)
	{
		this.diagram = diagram;
	}

	/**
	 * Sets the Activity to create
	 *
	 * @param table
	 *            the Activity to create
	 */
	public void setTable(ERDTable table)
	{
		this.table = table;
	}

    public void setLocation(Point location)
    {
        this.location = location;
    }
}