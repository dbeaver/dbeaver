/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;

import java.util.Collection;

/**
 * Add entity to diagram
 */
public class EntityAddCommand extends Command
{

	private EntityDiagram diagram;
	private Collection<ERDTable> tables;
    private Point location;

	public void execute()
	{
        for (ERDTable table : tables) {
		    diagram.addTable(table, true);
        }
	}

    public void undo()
    {
        for (ERDTable table : tables) {
            diagram.removeTable(table, true);
        }
    }

	public void setDiagram(EntityDiagram diagram)
	{
		this.diagram = diagram;
	}

	/**
	 * Sets the Activity to create
	 *
	 * @param tables
	 *            the Activity to create
	 */
	public void setTables(Collection<ERDTable> tables)
	{
		this.tables = tables;
	}

    public void setLocation(Point location)
    {
        this.location = location;
    }
}