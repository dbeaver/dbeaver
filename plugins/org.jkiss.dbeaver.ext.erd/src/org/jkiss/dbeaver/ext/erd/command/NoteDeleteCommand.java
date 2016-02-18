/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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

