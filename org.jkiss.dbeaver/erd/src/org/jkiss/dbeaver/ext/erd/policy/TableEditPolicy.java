/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.GroupRequest;

import org.jkiss.dbeaver.ext.erd.command.DeleteTableCommand;
import org.jkiss.dbeaver.ext.erd.model.Schema;
import org.jkiss.dbeaver.ext.erd.model.Table;
import org.jkiss.dbeaver.ext.erd.part.TablePart;

/**
 * Handles deletion of tables
 * @author Phil Zoio
 */
public class TableEditPolicy extends ComponentEditPolicy
{

	protected Command createDeleteCommand(GroupRequest request)
	{
		TablePart tablePart = (TablePart) getHost();
		Rectangle bounds = tablePart.getFigure().getBounds().getCopy();
		Schema parent = (Schema) (tablePart.getParent().getModel());
		return new DeleteTableCommand(parent, tablePart, bounds);
	}
	
}