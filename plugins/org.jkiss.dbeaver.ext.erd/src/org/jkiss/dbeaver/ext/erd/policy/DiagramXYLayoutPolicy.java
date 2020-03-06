/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @author Serge Rider
 */
public class DiagramXYLayoutPolicy extends XYLayoutEditPolicy
{

	private static final boolean ALLOW_ENTITY_RESIZE = true;

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
        if (!ALLOW_ENTITY_RESIZE && nodePart instanceof EntityPart) {
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
		Object constraint = fig.getParent().getLayoutManager().getConstraint(fig);
		Rectangle rectangle = constraint instanceof Rectangle ? (Rectangle) constraint : null;
		if (rectangle == null) {
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

	@Override
	public Command getCommand(Request request) {
		if (REQ_RESIZE.equals(request.getType())) {
			return null;//getHost();
		}
		return super.getCommand(request);
	}
}