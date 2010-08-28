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

import org.jkiss.dbeaver.ext.erd.command.DeleteRelationshipCommand;
import org.jkiss.dbeaver.ext.erd.model.Relationship;
import org.jkiss.dbeaver.ext.erd.model.Table;

/**
 * EditPolicy to handle deletion of relationships
 * @author Phil Zoio
 */
public class RelationshipEditPolicy extends ComponentEditPolicy
{

	protected Command createDeleteCommand(GroupRequest request)
	{
		Relationship relationship = (Relationship) getHost().getModel();
		Table primaryKeyTarget = relationship.getPrimaryKeyTable();
		Table foreignKeySource = relationship.getForeignKeyTable();
		DeleteRelationshipCommand deleteCmd = new DeleteRelationshipCommand(foreignKeySource, primaryKeyTarget, relationship);
		return deleteCmd;
	}
	
}