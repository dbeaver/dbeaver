/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ContainerEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.GroupRequest;

/**
 * Edit policy for attribute container
 *
 * @author Serge Rider
 */
public class AttributeContainerEditPolicy extends ContainerEditPolicy {

    @Override
    protected Command getAddCommand(GroupRequest request) {
        return super.getAddCommand(request);
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