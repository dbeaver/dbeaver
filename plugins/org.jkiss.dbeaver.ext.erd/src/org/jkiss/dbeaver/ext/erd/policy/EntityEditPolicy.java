/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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