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

import org.jkiss.dbeaver.ext.erd.command.AssociationDeleteCommand;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;

/**
 * EditPolicy to handle deletion of relationships
 * @author Serge Rieder
 */
public class AssociationEditPolicy extends ComponentEditPolicy
{

	protected Command createDeleteCommand(GroupRequest request)
	{
		ERDAssociation relationship = (ERDAssociation) getHost().getModel();
		ERDTable primaryKeyTarget = relationship.getPrimaryKeyTable();
		ERDTable foreignKeySource = relationship.getForeignKeyTable();
		AssociationDeleteCommand deleteCmd = new AssociationDeleteCommand(foreignKeySource, primaryKeyTarget, relationship);
		return deleteCmd;
	}
	
}