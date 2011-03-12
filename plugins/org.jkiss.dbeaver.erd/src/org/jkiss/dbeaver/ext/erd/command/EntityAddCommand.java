/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;

import java.util.Collection;

/**
 * Add entity to diagram
 */
public class EntityAddCommand extends Command
{

	private DiagramPart diagramPart;
	private Collection<ERDTable> tables;
    private Point location;

    public EntityAddCommand(DiagramPart diagram, Collection<ERDTable> tables, Point location)
    {
        this.diagramPart = diagram;
        this.tables = tables;
        this.location = location;
    }

    public void execute()
	{
        Point curLocation = location == null ? null : new Point(location);
        for (ERDTable table : tables) {
		    diagramPart.getDiagram().addTable(table, true);
            //diagramPart.getDiagram().addRelations(monitor, table, true);

            if (curLocation != null) {
                // Put new tables in specified location
                for (Object diagramChild : diagramPart.getChildren()) {
                    if (diagramChild instanceof EntityPart) {
                        EntityPart entityPart = (EntityPart) diagramChild;
                        if (entityPart.getTable() == table) {
                            final Rectangle newBounds = new Rectangle();
                            final Dimension size = entityPart.getFigure().getPreferredSize();
                            newBounds.x = curLocation.x;
                            newBounds.y = curLocation.y;
                            newBounds.width = size.width;
                            newBounds.height = size.height;
                            entityPart.modifyBounds(newBounds);

                            curLocation.x += size.width + 20;
                            break;
                        }
                    }
                }
            }
        }
	}

    public void undo()
    {
        for (ERDTable table : tables) {
            diagramPart.getDiagram().removeTable(table, true);
        }
    }

}