/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;

import java.util.List;

/**
 * Figure which represents the whole diagram - the view which corresponds to the
 * Schema model object
 * @author Serge Rieder
 */
public class EntityDiagramFigure extends FreeformLayer
{
	public EntityDiagramFigure()
	{
		setOpaque(true);

	}

    @Override
    public Rectangle getBounds()
    {
        return getFreeformExtent();
/*
        final List children = getChildren();
        if (children.isEmpty()) {
            return super.getBounds();
        }
        final Rectangle bounds = new Rectangle(0, 0, 0, 0);
        for (int i = 0; i < children.size(); i++) {
            IFigure figure = (IFigure) children.get(i);
            final Rectangle rectangle = figure.getBounds();
            if (rectangle.x < bounds.x) bounds.x = rectangle.x;
            if (rectangle.y < bounds.y) bounds.y = rectangle.y;
            if (rectangle.x + rectangle.width > bounds.x + bounds.width) bounds.width += (rectangle.width + rectangle.x) - (bounds.x + bounds.width);
            if (rectangle.y + rectangle.height > bounds.y + bounds.height) bounds.height += (rectangle.height + rectangle.y) - (bounds.height + bounds.y);
        }
        return bounds;
*/
    }

}