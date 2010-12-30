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

/**
 * Column component EditPolicy - handles column deletion
 * @author Serge Rieder
 */
public class AttributeEditPolicy extends ComponentEditPolicy
{

	protected Command createDeleteCommand(GroupRequest request)
	{
/*
		ERDTable parent = (ERDTable) (getHost().getParent().getModel());
		AttributeDeleteCommand deleteCmd = new AttributeDeleteCommand();
		deleteCmd.setTable(parent);
		deleteCmd.setColumn((ERDTableColumn) (getHost().getModel()));
		return deleteCmd;
*/
        return null;
	}
}