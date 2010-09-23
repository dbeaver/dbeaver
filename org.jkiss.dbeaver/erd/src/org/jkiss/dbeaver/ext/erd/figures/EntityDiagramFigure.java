/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.FreeformLayer;

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

}