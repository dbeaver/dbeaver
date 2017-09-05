/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
 * @author Serge Rider
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
            Rectangle bounds = diagram.getDiagram().getVisualInfo(erdTable);
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
