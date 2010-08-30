/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.GroupRequest;

import org.jkiss.dbeaver.ext.erd.command.AttributeDeleteCommand;
import org.jkiss.dbeaver.ext.erd.model.Column;
import org.jkiss.dbeaver.ext.erd.model.Table;

/**
 * Column component EditPolicy - handles column deletion
 * @author Phil Zoio
 */
public class AttributeEditPolicy extends ComponentEditPolicy
{

	protected Command createDeleteCommand(GroupRequest request)
	{
		Table parent = (Table) (getHost().getParent().getModel());
		AttributeDeleteCommand deleteCmd = new AttributeDeleteCommand();
		deleteCmd.setTable(parent);
		deleteCmd.setColumn((Column) (getHost().getModel()));
		return deleteCmd;
	}
}