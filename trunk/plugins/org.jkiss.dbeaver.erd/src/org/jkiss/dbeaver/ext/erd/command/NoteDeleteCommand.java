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
 * Created on Jul 17, 2004
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
 * @author Serge Rieder
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

