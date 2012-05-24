/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.jkiss.dbeaver.ext.erd.command.EntityDeleteCommand;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;

/**
 * Handles deletion of tables
 * @author Serge Rieder
 */
public class EntityEditPolicy extends ComponentEditPolicy
{

	@Override
    protected Command createDeleteCommand(GroupRequest request)
	{
		EntityPart entityPart = (EntityPart) getHost();
		Rectangle bounds = entityPart.getFigure().getBounds().getCopy();
		EntityDiagram parent = (EntityDiagram) (entityPart.getParent().getModel());
		return new EntityDeleteCommand(parent, entityPart, bounds);
	}
	
}