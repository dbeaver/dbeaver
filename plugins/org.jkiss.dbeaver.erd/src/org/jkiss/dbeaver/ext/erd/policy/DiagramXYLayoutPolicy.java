/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 20, 2004
 */
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.jkiss.dbeaver.ext.erd.command.EntityMoveCommand;
import org.jkiss.dbeaver.ext.erd.figures.EntityFigure;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;

/**
 * Handles manual layout editing for schema diagram. Only available for
 * XYLayoutManagers, not for automatic layout
 * 
 * @author Serge Rieder
 */
public class DiagramXYLayoutPolicy extends XYLayoutEditPolicy
{

	protected Command createAddCommand(EditPart child, Object constraint)
	{
		return null;
	}

	/**
	 * Creates command to move table. Does not allow table to be resized
	 */
	protected Command createChangeConstraintCommand(EditPart child, Object constraint)
	{

		if (!(child instanceof EntityPart))
			return null;
		if (!(constraint instanceof Rectangle))
			return null;

		EntityPart entityPart = (EntityPart) child;
		EntityFigure figure = (EntityFigure) entityPart.getFigure();
		Rectangle oldBounds = figure.getBounds();
		Rectangle newBounds = (Rectangle) constraint;

		if (oldBounds.width != newBounds.width && newBounds.width != -1)
			return null;
		if (oldBounds.height != newBounds.height && newBounds.height != -1)
			return null;

		//DiagramPart diagramPart = (DiagramPart) entityPart.getParent();

		return new EntityMoveCommand(entityPart, oldBounds.getCopy(), newBounds.getCopy());
	}

	/**
	 * Returns the current bounds as the constraint if none can be found in the
	 * figures Constraint object
	 */
	public Rectangle getCurrentConstraintFor(GraphicalEditPart child)
	{
		IFigure fig = child.getFigure();
		Rectangle rectangle = (Rectangle) fig.getParent().getLayoutManager().getConstraint(fig);
		if (rectangle == null)
		{
			rectangle = fig.getBounds();
		}
		return rectangle;
	}

	protected Command getCreateCommand(CreateRequest request)
	{
		return null;
	}

	protected Command getDeleteDependantCommand(Request request)
	{
		return null;
	}

}