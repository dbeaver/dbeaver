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
 * Created on Jul 20, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.part.NodePart;

/**
 * Command to move the bounds of an existing table. Only used with
 * XYLayoutEditPolicy (manual layout)
 * 
 * @author Serge Rieder
 */
public class NodeMoveCommand extends Command
{

	private NodePart nodePart;
	private Rectangle oldBounds;
	private Rectangle newBounds;

	public NodeMoveCommand(NodePart nodePart, Rectangle oldBounds, Rectangle newBounds)
	{
		super();
		this.nodePart = nodePart;
		this.oldBounds = oldBounds;
		this.newBounds = newBounds;
	}

	@Override
    public void execute()
	{
/*
        List tcList = nodePart.getTargetConnections();
        for (Object tc : tcList) {
            AssociationPart as = (AssociationPart)tc ;
            PolylineConnection pc = (PolylineConnection) as.getFigure();
            pc.getConnectionRouter().route(pc);
        }
*/
        nodePart.modifyBounds(newBounds);
	}

	@Override
    public void undo()
	{
		nodePart.modifyBounds(oldBounds);
	}

}