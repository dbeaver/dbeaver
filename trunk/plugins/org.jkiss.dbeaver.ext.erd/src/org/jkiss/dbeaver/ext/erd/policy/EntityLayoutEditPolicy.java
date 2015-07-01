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
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.FlowLayoutEditPolicy;
import org.eclipse.gef.requests.CreateRequest;

/**
 * Handles moving of columns within and between tables
 * @author Serge Rieder
 */
public class EntityLayoutEditPolicy extends FlowLayoutEditPolicy
{

	/**
	 * Creates command to transfer child column to after column (in another
	 * table)
	 */
	@Override
    protected Command createAddCommand(EditPart child, EditPart after)
	{

/*
		if (!(child instanceof AttributePart))
			return null;
		if (!(after instanceof AttributePart))
			return null;

		ERDEntityAttribute toMove = (ERDEntityAttribute) child.getModel();
		ERDEntityAttribute afterModel = (ERDEntityAttribute) after.getModel();

		EntityPart originalEntityPart = (EntityPart) child.getParent();
		ERDEntity originalTable = (ERDEntity) originalEntityPart.getModel();
		EntityPart newEntityPart = (EntityPart) after.getParent();
		ERDEntity newTable = newEntityPart.getTable();

		int oldIndex = originalEntityPart.getChildren().indexOf(child);
		int newIndex = newEntityPart.getChildren().indexOf(after);

		AttributeTransferCommand command = new AttributeTransferCommand(toMove, afterModel, originalTable, newTable,
				oldIndex, newIndex);
		return command;
*/
        return null;
	}

	/**
	 * Creates command to transfer child column to after specified column
	 * (within table)
	 */
	@Override
    protected Command createMoveChildCommand(EditPart child, EditPart after)
	{
/*
		if (after != null)
		{
			ERDEntityAttribute childModel = (ERDEntityAttribute) child.getModel();
			ERDEntityAttribute afterModel = (ERDEntityAttribute) after.getModel();

			ERDEntity parentTable = (ERDEntity) getHost().getModel();
			int oldIndex = getHost().getChildren().indexOf(child);
			int newIndex = getHost().getChildren().indexOf(after);

			AttributeMoveCommand command = new AttributeMoveCommand(childModel, parentTable, oldIndex, newIndex);
			return command;
		}
*/
		return null;
	}

	/**
	 * @param request
	 * @return
	 */
	@Override
    protected Command getCreateCommand(CreateRequest request)
	{
		return null;
	}

	/**
	 * @param request
	 * @return
	 */
	@Override
    protected Command getDeleteDependantCommand(Request request)
	{
		return null;
	}

}