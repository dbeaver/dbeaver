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
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;

import java.util.Collection;

/**
 * Add entity to diagram
 */
public class EntityAddCommand extends Command
{

	private DiagramPart diagramPart;
	private Collection<ERDEntity> entities;
    private Point location;

    public EntityAddCommand(DiagramPart diagram, Collection<ERDEntity> entities, Point location)
    {
        this.diagramPart = diagram;
        this.entities = entities;
        this.location = location;
    }

    @Override
    public void execute()
	{
        Point curLocation = location == null ? null : new Point(location);
        for (ERDEntity entity : entities) {
		    diagramPart.getDiagram().addTable(entity, true);
            //diagramPart.getDiagram().addRelations(monitor, entity, true);

            if (curLocation != null) {
                // Put new entities in specified location
                for (Object diagramChild : diagramPart.getChildren()) {
                    if (diagramChild instanceof EntityPart) {
                        EntityPart entityPart = (EntityPart) diagramChild;
                        if (entityPart.getTable() == entity) {
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

    @Override
    public void undo()
    {
        for (ERDEntity entity : entities) {
            diagramPart.getDiagram().removeTable(entity, true);
        }
    }

}