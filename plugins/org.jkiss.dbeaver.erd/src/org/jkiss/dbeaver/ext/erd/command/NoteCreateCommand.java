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
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.model.ERDNote;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.ext.erd.part.NotePart;

/**
 * Add entity to diagram
 */
public class NoteCreateCommand extends Command
{

	private DiagramPart diagramPart;
	private ERDNote note;
    private Point location;

    public NoteCreateCommand(DiagramPart diagram, ERDNote note, Point location)
    {
        this.diagramPart = diagram;
        this.note = note;
        this.location = location;
    }

    @Override
    public void execute()
	{
        diagramPart.getDiagram().addNote(note, true);
        //diagramPart.getDiagram().addRelations(monitor, table, true);

        if (location != null) {
            // Set new note location
            for (Object diagramChild : diagramPart.getChildren()) {
                if (diagramChild instanceof NotePart) {
                    NotePart entityPart = (NotePart) diagramChild;
                    if (entityPart.getNote() == note) {
                        final Dimension size = entityPart.getFigure().getPreferredSize();
                        final Rectangle newBounds = new Rectangle(location.x, location.y, size.width, size.height);
                        entityPart.modifyBounds(newBounds);
                        break;
                    }
                }
            }
        }
	}

    @Override
    public void undo()
    {
        diagramPart.getDiagram().removeNote(note, true);
    }

}