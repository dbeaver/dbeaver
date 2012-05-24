/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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

	@Override
    protected Command createDeleteCommand(GroupRequest request)
	{
/*
		ERDEntity parent = (ERDEntity) (getHost().getParent().getModel());
		AttributeDeleteCommand deleteCmd = new AttributeDeleteCommand();
		deleteCmd.setTable(parent);
		deleteCmd.setColumn((ERDEntityAttribute) (getHost().getModel()));
		return deleteCmd;
*/
        return null;
	}
}