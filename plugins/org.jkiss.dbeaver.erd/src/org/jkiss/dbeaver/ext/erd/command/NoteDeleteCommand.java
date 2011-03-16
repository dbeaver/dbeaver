/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
	public void execute()
	{
		entityDiagram.removeNote(note, true);
	}

	/**
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	public void redo()
	{
		execute();
	}

	public void undo()
	{
		entityDiagram.addNote(note, true);
	}

}

