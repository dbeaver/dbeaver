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
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.model.ERDNote;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.part.NotePart;

/**
 * Command to delete tables from the schema
 * 
 * @author Serge Rider
 */
public class NoteDeleteCommand extends Command
{
    private NotePart notePart;
	private ERDNote note;
	private EntityDiagram entityDiagram;
	private Rectangle bounds;

    public NoteDeleteCommand(EntityDiagram entityDiagram, NotePart notePart, Rectangle originalBounds) {
        this.entityDiagram = entityDiagram;
        this.notePart = notePart;
        this.note = notePart.getNote();
        this.bounds = originalBounds;
    }

	/**
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	@Override
    public void execute()
	{
		entityDiagram.removeNote(note, true);
	}

	/**
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	@Override
    public void redo()
	{
		execute();
	}

	@Override
    public void undo()
	{
		entityDiagram.addNote(note, true);
	}

}

