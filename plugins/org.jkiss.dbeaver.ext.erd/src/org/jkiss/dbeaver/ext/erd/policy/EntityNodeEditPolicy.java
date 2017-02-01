/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.jkiss.dbeaver.ext.erd.command.AssociationCreateCommand;
import org.jkiss.dbeaver.ext.erd.command.AssociationReconnectSourceCommand;
import org.jkiss.dbeaver.ext.erd.command.AssociationReconnectTargetCommand;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;

/**
 * Handles manipulation of relationships between tables
 * @author Serge Rider
 */
public class EntityNodeEditPolicy extends GraphicalNodeEditPolicy
{

	/**
	 * @see GraphicalNodeEditPolicy#getConnectionCreateCommand(CreateConnectionRequest)
	 */
	@Override
    protected Command getConnectionCreateCommand(CreateConnectionRequest request)
	{

		AssociationCreateCommand cmd = new AssociationCreateCommand();
		EntityPart part = (EntityPart) getHost();
		cmd.setForeignEntity(part.getTable());
		request.setStartCommand(cmd);
		return cmd;
	}

	/**
	 * @see GraphicalNodeEditPolicy#getConnectionCompleteCommand(CreateConnectionRequest)
	 */
	@Override
    protected Command getConnectionCompleteCommand(CreateConnectionRequest request)
	{
		AssociationCreateCommand cmd = (AssociationCreateCommand) request.getStartCommand();
		EntityPart part = (EntityPart) request.getTargetEditPart();
		cmd.setPrimaryEntity(part.getTable());
		return cmd;
	}

	/**
	 * @see GraphicalNodeEditPolicy#getReconnectSourceCommand(ReconnectRequest)
	 */
	@Override
    protected Command getReconnectSourceCommand(ReconnectRequest request)
	{
		
		AssociationReconnectSourceCommand cmd = new AssociationReconnectSourceCommand();
		cmd.setRelationship((ERDAssociation) request.getConnectionEditPart().getModel());
		EntityPart entityPart = (EntityPart) getHost();
		cmd.setSourceForeignKey(entityPart.getTable());
		return cmd;
	}

	/**
	 * @see GraphicalNodeEditPolicy#getReconnectTargetCommand(ReconnectRequest)
	 */
	@Override
    protected Command getReconnectTargetCommand(ReconnectRequest request)
	{
		AssociationReconnectTargetCommand cmd = new AssociationReconnectTargetCommand();
		cmd.setRelationship((ERDAssociation) request.getConnectionEditPart().getModel());
		EntityPart entityPart = (EntityPart) getHost();
		cmd.setTargetPrimaryKey(entityPart.getTable());
		return cmd;
	}

}