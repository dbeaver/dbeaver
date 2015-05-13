/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
/*
 * Created on Jul 20, 2004
 */
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.jkiss.dbeaver.ext.erd.command.NodeMoveCommand;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;
import org.jkiss.dbeaver.ext.erd.part.NodePart;

/**
 * Handles manual layout editing for schema diagram. Only available for
 * XYLayoutManagers, not for automatic layout
 * 
 * @author Serge Rieder
 */
public class DiagramXYLayoutPolicy extends XYLayoutEditPolicy
{

	@Override
    protected Command createAddCommand(EditPart child, Object constraint)
	{
		return null;
	}

	/**
	 * Creates command to move table. Does not allow table to be resized
	 */
	@Override
    protected Command createChangeConstraintCommand(EditPart child, Object constraint)
	{

		if (!(child instanceof NodePart))
			return null;
		if (!(constraint instanceof Rectangle))
			return null;

		NodePart nodePart = (NodePart) child;
		Figure figure = (Figure) nodePart.getFigure();
		Rectangle oldBounds = figure.getBounds();
		Rectangle newBounds = (Rectangle) constraint;

        // Restrict resize for entities
        if (nodePart instanceof EntityPart) {
            if (oldBounds.width != newBounds.width && newBounds.width != -1)
                return null;
            if (oldBounds.height != newBounds.height && newBounds.height != -1)
                return null;
        }

		//DiagramPart diagramPart = (DiagramPart) nodePart.getParent();

		return new NodeMoveCommand(nodePart, oldBounds.getCopy(), newBounds.getCopy());
	}

	/**
	 * Returns the current bounds as the constraint if none can be found in the
	 * figures Constraint object
	 */
	@Override
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

	@Override
    protected Command getCreateCommand(CreateRequest request)
	{
		return null;
	}

	@Override
    protected Command getDeleteDependantCommand(Request request)
	{
		return null;
	}

}