/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.DirectEditPolicy;
import org.eclipse.gef.requests.DirectEditRequest;
import org.eclipse.jface.viewers.CellEditor;
import org.jkiss.dbeaver.ext.erd.part.AttributePart;

/**
 * EditPolicy for the direct editing of Column names
 * 
 * @author Serge Rider
 */
public class AttributeDirectEditPolicy extends DirectEditPolicy
{

	private String oldValue;

	/**
	 * @see DirectEditPolicy#getDirectEditCommand(org.eclipse.gef.requests.DirectEditRequest)
	 */
	@Override
    protected Command getDirectEditCommand(DirectEditRequest request)
	{
/*
		AttributeResetNameTypeCommand cmd = new AttributeResetNameTypeCommand();
		ERDEntityAttribute column = (ERDEntityAttribute) getHost().getModel();
		cmd.setSource(column);
		cmd.setOldName(column.getName());
		cmd.setOldType(column.getType());
		CellEditor cellEditor = request.getCellEditor();
		cmd.setNameType((String) cellEditor.getValue());
		return cmd;
*/
        return null;
	}

	@Override
    protected void showCurrentEditValue(DirectEditRequest request)
	{
		String value = (String) request.getCellEditor().getValue();
		AttributePart attributePart = (AttributePart) getHost();
		attributePart.handleNameChange(value);
	}

	@Override
    protected void storeOldEditValue(DirectEditRequest request)
	{
		CellEditor cellEditor = request.getCellEditor();
		oldValue = (String) cellEditor.getValue();
	}

	@Override
    protected void revertOldEditValue(DirectEditRequest request)
	{
		CellEditor cellEditor = request.getCellEditor();
		cellEditor.setValue(oldValue);
		AttributePart attributePart = (AttributePart) getHost();
		attributePart.revertNameChange(oldValue);
		
	}
}