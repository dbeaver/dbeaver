/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.AbstractEditPolicy;
import org.eclipse.gef.editpolicies.ContainerEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.GroupRequest;

import org.jkiss.dbeaver.ext.erd.command.EntityAddCommand;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.model.Table;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;

/**
 * Handles creation of new tables using drag and drop or point and click from the palette
 * @author Phil Zoio
 */
public class DiagramContainerEditPolicy extends ContainerEditPolicy
{

	/**
	 * @see org.eclipse.gef.editpolicies.ContainerEditPolicy#getAddCommand(org.eclipse.gef.requests.GroupRequest)
	 */
	protected Command getAddCommand(GroupRequest request)
	{
		EditPart host = getTargetEditPart(request);
		return null;
	}

	/**
	 * @see ContainerEditPolicy#getCreateCommand(org.eclipse.gef.requests.CreateRequest)
	 */
	protected Command getCreateCommand(CreateRequest request)
	{

		Object newObject = request.getNewObject();
		if (!(newObject instanceof Table))
		{
			return null;
		}
		Point location = request.getLocation();
		EditPart host = getTargetEditPart(request);
		DiagramPart diagramPart = (DiagramPart)getHost();
		EntityDiagram entityDiagram = diagramPart.getSchema();
		Table table = (Table) newObject;
		EntityAddCommand entityAddCommand = new EntityAddCommand();
		entityAddCommand.setSchema(entityDiagram);
		entityAddCommand.setTable(table);
		return entityAddCommand;
	}

	/**
	 * @see AbstractEditPolicy#getTargetEditPart(org.eclipse.gef.Request)
	 */
	public EditPart getTargetEditPart(Request request)
	{
		if (REQ_CREATE.equals(request.getType()))
			return getHost();
		if (REQ_ADD.equals(request.getType()))
			return getHost();
		if (REQ_MOVE.equals(request.getType()))
			return getHost();
		return super.getTargetEditPart(request);
	}

}