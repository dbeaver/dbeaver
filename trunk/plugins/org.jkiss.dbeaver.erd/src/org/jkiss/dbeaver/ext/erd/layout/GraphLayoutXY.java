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
 * Created on Jul 21, 2004
 */
package org.jkiss.dbeaver.ext.erd.layout;

import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;


/**
 * Subclass of XYLayout which can use the child figures actual bounds as a constraint
 * when doing manual layout (XYLayout)
 *
 * @author Serge Rieder
 */
public class GraphLayoutXY extends FreeformLayout {
    private DiagramPart diagram;

    public GraphLayoutXY(DiagramPart diagram)
    {
        this.diagram = diagram;
    }

    @Override
    public void layout(IFigure container)
    {
        super.layout(container);
        diagram.setTableModelBounds();
    }

    @Override
    public Object getConstraint(IFigure child)
    {
/*
        if (child instanceof EntityFigure) {
            final ERDEntity erdTable = ((EntityFigure) child).getTable();
            Rectangle bounds = diagram.getDiagram().getInitBounds(erdTable);
            if (bounds != null) {
                bounds.width = child.getSize().width;
                bounds.height = child.getSize().height;
                return bounds;
            }
        }
*/
        Object constraint = constraints.get(child);
        if (constraint instanceof Rectangle) {
            return constraint;
        } else {
            Rectangle currentBounds = child.getBounds();
            return new Rectangle(currentBounds.x, currentBounds.y, -1, -1);
        }
    }

    public void cleanupConstraints()
    {
        constraints.clear();
    }
}
