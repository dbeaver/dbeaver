/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
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
 * @author Phil Zoio
 */
public class GraphXYLayout extends FreeformLayout
{

	private DiagramPart diagram;
	
	public GraphXYLayout(DiagramPart diagram)
	{
		this.diagram = diagram;
	}
	
	public void layout(IFigure container)
	{
		super.layout(container);
		diagram.setTableModelBounds();
	}
	

	public Object getConstraint(IFigure child)
	{
		Object constraint = constraints.get(child);
		if (constraint instanceof Rectangle)
		{
			return constraint;
		}
		else
		{
			Rectangle currentBounds = child.getBounds();
			return new Rectangle(currentBounds.x, currentBounds.y, -1,-1);
		}
	}
	
}
