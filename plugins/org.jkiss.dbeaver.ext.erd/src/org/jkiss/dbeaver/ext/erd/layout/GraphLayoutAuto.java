/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
 * @author Serge Rider
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
		new DirectedGraphLayoutVisitor(diagram.getDiagram().getDecorator()).layoutDiagram(diagram);
        diagram.setTableModelBounds();
        //new ZestGraphLayout().layoutDiagram(diagram);

        Animation.run(400);
	}
	
}