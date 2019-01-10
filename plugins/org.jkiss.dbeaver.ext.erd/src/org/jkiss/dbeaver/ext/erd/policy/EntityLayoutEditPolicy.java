/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.FlowLayoutEditPolicy;
import org.eclipse.gef.requests.CreateRequest;

/**
 * Handles moving of columns within and between tables
 * @author Serge Rider
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