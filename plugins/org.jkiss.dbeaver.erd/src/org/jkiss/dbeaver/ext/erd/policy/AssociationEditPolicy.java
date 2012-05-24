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
import org.jkiss.dbeaver.ext.erd.command.AssociationDeleteCommand;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;

/**
 * EditPolicy to handle deletion of relationships
 * @author Serge Rieder
 */
public class AssociationEditPolicy extends ComponentEditPolicy
{

	@Override
    protected Command createDeleteCommand(GroupRequest request)
	{
		ERDAssociation relationship = (ERDAssociation) getHost().getModel();
		ERDEntity primaryKeyTarget = relationship.getPrimaryKeyEntity();
		ERDEntity foreignKeySource = relationship.getForeignKeyEntity();
		AssociationDeleteCommand deleteCmd = new AssociationDeleteCommand(foreignKeySource, primaryKeyTarget, relationship);
		return deleteCmd;
	}
	
}