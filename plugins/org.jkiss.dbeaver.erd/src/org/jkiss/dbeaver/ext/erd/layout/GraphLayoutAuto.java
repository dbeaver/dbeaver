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
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.layout;

import org.eclipse.draw2d.AbstractLayout;
import org.eclipse.draw2d.Animation;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.jkiss.dbeaver.ext.erd.layout.algorithm.direct.DirectedGraphLayoutVisitor;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;

import java.util.List;


/**
 * Uses the DirectedGraphLayoutVisitor to automatically lay out figures on diagram
 * @author Serge Rieder
 */
public class GraphLayoutAuto extends AbstractLayout
{

	private DiagramPart diagram;

	public GraphLayoutAuto(DiagramPart diagram)
	{
		this.diagram = diagram;
	}

	
	@Override
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

	
	@Override
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