/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
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

import org.jkiss.dbeaver.ext.erd.command.AttributeMoveCommand;
import org.jkiss.dbeaver.ext.erd.command.AttributeTransferCommand;
import org.jkiss.dbeaver.ext.erd.model.Column;
import org.jkiss.dbeaver.ext.erd.model.Table;
import org.jkiss.dbeaver.ext.erd.part.AttributePart;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;

/**
 * Handles moving of columns within and between tables
 * @author Phil Zoio
 */
public class EntityLayoutEditPolicy extends FlowLayoutEditPolicy
{

	/**
	 * Creates command to transfer child column to after column (in another
	 * table)
	 */
	protected Command createAddCommand(EditPart child, EditPart after)
	{

		if (!(child instanceof AttributePart))
			return null;
		if (!(after instanceof AttributePart))
			return null;

		Column toMove = (Column) child.getModel();
		Column afterModel = (Column) after.getModel();

		EntityPart originalEntityPart = (EntityPart) child.getParent();
		Table originalTable = (Table) originalEntityPart.getModel();
		EntityPart newEntityPart = (EntityPart) after.getParent();
		Table newTable = newEntityPart.getTable();

		int oldIndex = originalEntityPart.getChildren().indexOf(child);
		int newIndex = newEntityPart.getChildren().indexOf(after);

		AttributeTransferCommand command = new AttributeTransferCommand(toMove, afterModel, originalTable, newTable,
				oldIndex, newIndex);
		return command;

	}

	/**
	 * Creates command to transfer child column to after specified column
	 * (within table)
	 */
	protected Command createMoveChildCommand(EditPart child, EditPart after)
	{
		if (after != null)
		{
			Column childModel = (Column) child.getModel();
			Column afterModel = (Column) after.getModel();

			Table parentTable = (Table) getHost().getModel();
			int oldIndex = getHost().getChildren().indexOf(child);
			int newIndex = getHost().getChildren().indexOf(after);

			AttributeMoveCommand command = new AttributeMoveCommand(childModel, parentTable, oldIndex, newIndex);
			return command;
		}
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