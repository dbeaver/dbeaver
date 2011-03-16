/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
import org.jkiss.dbeaver.ext.erd.command.NoteDeleteCommand;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;
import org.jkiss.dbeaver.ext.erd.part.NotePart;

/**
 * Handles deletion of tables
 * @author Serge Rieder
 */
public class NoteEditPolicy extends ComponentEditPolicy
{

	protected Command createDeleteCommand(GroupRequest request)
	{
		NotePart notePart = (NotePart) getHost();
		Rectangle bounds = notePart.getFigure().getBounds().getCopy();
		EntityDiagram parent = (EntityDiagram) (notePart.getParent().getModel());
		return new NoteDeleteCommand(parent, notePart, bounds);
	}
	
}