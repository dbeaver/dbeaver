/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.jkiss.dbeaver.ext.erd.command.AssociationReconnectSourceCommand;
import org.jkiss.dbeaver.ext.erd.command.AssociationReconnectTargetCommand;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;

/**
 * Handles manipulation of relationships between tables
 * @author Serge Rieder
 */
public class EntityNodeEditPolicy extends GraphicalNodeEditPolicy
{

	/**
	 * @see GraphicalNodeEditPolicy#getConnectionCreateCommand(CreateConnectionRequest)
	 */
	protected Command getConnectionCreateCommand(CreateConnectionRequest request)
	{
/*
		AssociationCreateCommand cmd = new AssociationCreateCommand();
		EntityPart part = (EntityPart) getHost();
		cmd.setForeignTable(part.getTable());
		request.setStartCommand(cmd);
		return cmd;
*/
        return null;
	}

	/**
	 * @see GraphicalNodeEditPolicy#getConnectionCompleteCommand(CreateConnectionRequest)
	 */
	protected Command getConnectionCompleteCommand(CreateConnectionRequest request)
	{
/*
		AssociationCreateCommand cmd = (AssociationCreateCommand) request.getStartCommand();
		EntityPart part = (EntityPart) request.getTargetEditPart();
		cmd.setPrimaryTable(part.getTable());
		return cmd;
*/
        return null;
	}

	/**
	 * @see GraphicalNodeEditPolicy#getReconnectSourceCommand(ReconnectRequest)
	 */
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
	protected Command getReconnectTargetCommand(ReconnectRequest request)
	{
		AssociationReconnectTargetCommand cmd = new AssociationReconnectTargetCommand();
		cmd.setRelationship((ERDAssociation) request.getConnectionEditPart().getModel());
		EntityPart entityPart = (EntityPart) getHost();
		cmd.setTargetPrimaryKey(entityPart.getTable());
		return cmd;
	}

}