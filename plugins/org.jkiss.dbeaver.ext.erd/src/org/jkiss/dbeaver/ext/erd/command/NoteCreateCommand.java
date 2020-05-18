/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
    private Dimension size;

    public NoteCreateCommand(DiagramPart diagram, ERDNote note, Point location, Dimension size)
    {
        this.diagramPart = diagram;
        this.note = note;
        this.location = location;
        this.size = size;
    }

    @Override
    public void execute()
	{
        diagramPart.getDiagram().addNote(note, true);
        //diagramPart.getDiagram().addModelRelations(monitor, table, true);

        if (location != null) {
            // Set new note location
            for (Object diagramChild : diagramPart.getChildren()) {
                if (diagramChild instanceof NotePart) {
                    NotePart notePart = (NotePart) diagramChild;
                    if (notePart.getNote() == note) {
                        Dimension size = this.size;
                        if (size == null || size.width <= 0 || size.height <= 0) {
                            size = notePart.getFigure().getPreferredSize();
                        }
                        final Rectangle newBounds = new Rectangle(location.x, location.y, size.width, size.height);
                        notePart.modifyBounds(newBounds);
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