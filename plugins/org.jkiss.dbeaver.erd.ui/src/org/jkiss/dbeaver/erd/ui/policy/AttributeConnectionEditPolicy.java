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
package org.jkiss.dbeaver.erd.ui.policy;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.GroupRequest;
import org.jkiss.dbeaver.erd.ui.command.AssociationCreateCommand;
import org.jkiss.dbeaver.erd.ui.part.AttributePart;

import java.util.List;

/**
 * Edit policy for logical connections creation
 */
public class AttributeConnectionEditPolicy extends AttributeContainerEditPolicy {

    private final AttributePart part;

    public AttributeConnectionEditPolicy(AttributePart part) {
        this.part = part;
    }

    @Override
    protected Command getAddCommand(GroupRequest request) {
        List srcParts = request.getEditParts();
        if (srcParts.size() != 1) {
            // Can drop only one attribute
            return null;
        }
        if (!(srcParts.get(0) instanceof AttributePart)) {
            // Not attribute
            return null;
        }
        AttributePart srcPart = (AttributePart) srcParts.get(0);
        if (srcPart.getParent() == part.getParent()) {
            // Can't drop attribute to the same parent
            return null;
        }

        AssociationCreateCommand command = new AssociationCreateCommand();

        command.setSourceEntity(srcPart.getEntity());
        command.setTargetEntity(part.getEntity());
        command.setAttributes(srcPart.getAttribute(), part.getAttribute());
        return command;
    }

    @Override
    protected Command getCloneCommand(ChangeBoundsRequest request) {
        return super.getCloneCommand(request);
    }

    @Override
    protected Command getCreateCommand(CreateRequest request) {
/*
        Object newObject = request.getNewObject();
		if (!(newObject instanceof ERDEntityAttribute))
		{
			return null;
		}
		
		EntityPart entityPart = (EntityPart) getHost();
		ERDEntity table = entityPart.getTable();
		ERDEntityAttribute column = (ERDEntityAttribute) newObject;
		AttributeCreateCommand command = new AttributeCreateCommand();
		command.setTable(table);
		command.setColumn(column);
		return command;
*/
        return null;
    }

}