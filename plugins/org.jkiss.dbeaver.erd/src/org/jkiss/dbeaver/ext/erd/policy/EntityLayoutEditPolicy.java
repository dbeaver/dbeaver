/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
	protected Command createAddCommand(EditPart child, EditPart after)
	{

/*
		if (!(child instanceof AttributePart))
			return null;
		if (!(after instanceof AttributePart))
			return null;

		ERDTableColumn toMove = (ERDTableColumn) child.getModel();
		ERDTableColumn afterModel = (ERDTableColumn) after.getModel();

		EntityPart originalEntityPart = (EntityPart) child.getParent();
		ERDTable originalTable = (ERDTable) originalEntityPart.getModel();
		EntityPart newEntityPart = (EntityPart) after.getParent();
		ERDTable newTable = newEntityPart.getTable();

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
	protected Command createMoveChildCommand(EditPart child, EditPart after)
	{
/*
		if (after != null)
		{
			ERDTableColumn childModel = (ERDTableColumn) child.getModel();
			ERDTableColumn afterModel = (ERDTableColumn) after.getModel();

			ERDTable parentTable = (ERDTable) getHost().getModel();
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
	protected Command getCreateCommand(CreateRequest request)
	{
		return null;
	}

	/**
	 * @param request
	 * @return
	 */
	protected Command getDeleteDependantCommand(Request request)
	{
		return null;
	}

}