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

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.jkiss.dbeaver.ext.erd.command.NoteDeleteCommand;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.part.NotePart;

/**
 * Handles deletion of tables
 * @author Serge Rider
 */
public class NoteEditPolicy extends ComponentEditPolicy
{

	@Override
    protected Command createDeleteCommand(GroupRequest request)
	{
		NotePart notePart = (NotePart) getHost();
		Rectangle bounds = notePart.getFigure().getBounds().getCopy();
		EntityDiagram parent = (EntityDiagram) (notePart.getParent().getModel());
		return new NoteDeleteCommand(parent, notePart, bounds);
	}
	
}