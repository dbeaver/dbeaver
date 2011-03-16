/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 18, 2004
 */
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.DirectEditPolicy;
import org.eclipse.gef.requests.DirectEditRequest;
import org.eclipse.jface.viewers.CellEditor;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;
import org.jkiss.dbeaver.ext.erd.part.NotePart;

/**
 * EditPolicy for the direct editing of table names
 * 
 * @author Serge Rieder
 */
public class NoteDirectEditPolicy extends DirectEditPolicy
{

	private String oldValue;

	/**
	 * @see org.eclipse.gef.editpolicies.DirectEditPolicy#getDirectEditCommand(org.eclipse.gef.requests.DirectEditRequest)
	 */
	protected Command getDirectEditCommand(DirectEditRequest request)
	{
/*
		EntityRenameCommand cmd = new EntityRenameCommand();
		ERDTable table = (ERDTable) getHost().getModel();
		cmd.setTable(table);
		cmd.setOldName(table.getName());
		CellEditor cellEditor = request.getCellEditor();
		cmd.setName((String) cellEditor.getValue());
		return cmd;
*/
        return null;
	}

	/**
	 * @see org.eclipse.gef.editpolicies.DirectEditPolicy#showCurrentEditValue(org.eclipse.gef.requests.DirectEditRequest)
	 */
	protected void showCurrentEditValue(DirectEditRequest request)
	{
		String value = (String) request.getCellEditor().getValue();
		NotePart notePart = (NotePart) getHost();
		notePart.handleNameChange(value);
	}

	protected void storeOldEditValue(DirectEditRequest request)
	{
		
		CellEditor cellEditor = request.getCellEditor();
		oldValue = (String) cellEditor.getValue();
	}

	/**
	 * @param request
	 */
	protected void revertOldEditValue(DirectEditRequest request)
	{
		CellEditor cellEditor = request.getCellEditor();
		cellEditor.setValue(oldValue);
		NotePart entityPart = (NotePart) getHost();
		entityPart.revertNameChange();
	}

}