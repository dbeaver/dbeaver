/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.layout;

import org.eclipse.draw2d.AbstractLayout;
import org.eclipse.draw2d.Animation;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;

import java.util.List;


/**
 * Uses the DirectedGraphLayoutVisitor to automatically lay out figures on diagram
 * @author Serge Rieder
 */
public class GraphLayoutManager extends AbstractLayout
{

	private DiagramPart diagram;

	public GraphLayoutManager(DiagramPart diagram)
	{
		this.diagram = diagram;
	}

	
	protected Dimension calculatePreferredSize(IFigure container, int wHint, int hHint)
	{		
		container.validate();
		List<?> children = container.getChildren();
		Rectangle result = new Rectangle().setLocation(container.getClientArea().getLocation());
		for (int i = 0; i < children.size(); i++)
			result.union(((IFigure) children.get(i)).getBounds());
		result.resize(container.getInsets().getWidth(), container.getInsets().getHeight());
		return result.getSize();		
	}

	
	public void layout(IFigure container)
	{
        Animation.markBegin();
/*
		GraphAnimation.recordInitialState(container);
		if (GraphAnimation.playbackState(container))
			return;
*/

        // TODO: REPLACE WITH ZEST!
		new DirectedGraphLayoutVisitor().layoutDiagram(diagram);
        diagram.setTableModelBounds();
        //new ZestGraphLayout().layoutDiagram(diagram);

        Animation.run(400);
	}
	
}