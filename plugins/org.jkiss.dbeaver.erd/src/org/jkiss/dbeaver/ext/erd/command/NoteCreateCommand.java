/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

    public void undo()
    {
        diagramPart.getDiagram().removeNote(note, true);
    }

}