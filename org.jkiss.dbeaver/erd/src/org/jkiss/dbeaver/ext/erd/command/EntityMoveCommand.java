/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 20, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;

import org.jkiss.dbeaver.ext.erd.part.EntityPart;

/**
 * Command to move the bounds of an existing table. Only used with
 * XYLayoutEditPolicy (manual layout)
 * 
 * @author Phil Zoio
 */
public class EntityMoveCommand extends Command
{

	private EntityPart entityPart;
	private Rectangle oldBounds;
	private Rectangle newBounds;

	public EntityMoveCommand(EntityPart entityPart, Rectangle oldBounds, Rectangle newBounds)
	{
		super();
		this.entityPart = entityPart;
		this.oldBounds = oldBounds;
		this.newBounds = newBounds;
	}

	public void execute()
	{
		entityPart.modifyBounds(newBounds);
	}

	public void undo()
	{
		entityPart.modifyBounds(oldBounds);
	}

}