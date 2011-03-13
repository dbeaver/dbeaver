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

/*
    @Override
    public Rectangle getBounds()
    {
        return getFreeformExtent();
    }
*/

}