/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
/*
 * Created on Jul 18, 2004
 */
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.DirectEditPolicy;
import org.eclipse.gef.requests.DirectEditRequest;
import org.eclipse.jface.viewers.CellEditor;
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
	@Override
    protected Command getDirectEditCommand(DirectEditRequest request)
	{
/*
		EntityRenameCommand cmd = new EntityRenameCommand();
		ERDEntity table = (ERDEntity) getHost().getModel();
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
	@Override
    protected void showCurrentEditValue(DirectEditRequest request)
	{
		String value = (String) request.getCellEditor().getValue();
		NotePart notePart = (NotePart) getHost();
		notePart.handleNameChange(value);
	}

	@Override
    protected void storeOldEditValue(DirectEditRequest request)
	{
		
		CellEditor cellEditor = request.getCellEditor();
		oldValue = (String) cellEditor.getValue();
	}

	/**
	 * @param request
	 */
	@Override
    protected void revertOldEditValue(DirectEditRequest request)
	{
		CellEditor cellEditor = request.getCellEditor();
		cellEditor.setValue(oldValue);
		NotePart entityPart = (NotePart) getHost();
		entityPart.revertNameChange();
	}

}